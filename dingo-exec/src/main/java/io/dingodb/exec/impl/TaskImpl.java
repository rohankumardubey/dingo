/*
 * Copyright 2021 DataCanvas
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.dingodb.exec.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.dingodb.common.Location;
import io.dingodb.common.concurrent.Executors;
import io.dingodb.common.type.DingoType;
import io.dingodb.common.type.converter.JsonConverter;
import io.dingodb.exec.base.Id;
import io.dingodb.exec.base.Operator;
import io.dingodb.exec.base.Task;
import io.dingodb.exec.codec.RawJsonDeserializer;
import io.dingodb.exec.fin.FinWithException;
import io.dingodb.exec.fin.TaskStatus;
import io.dingodb.exec.operator.AbstractOperator;
import io.dingodb.exec.operator.RootOperator;
import io.dingodb.exec.operator.SourceOperator;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Slf4j
@JsonPropertyOrder({"jobId", "location", "operators", "runList", "parasType"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class TaskImpl implements Task {
    @JsonProperty("id")
    @Getter
    private final Id id;
    @JsonProperty("jobId")
    @Getter
    private final Id jobId;
    @JsonProperty("location")
    @Getter
    private final Location location;
    @JsonProperty("operators")
    @JsonSerialize(contentAs = AbstractOperator.class)
    @JsonDeserialize(contentAs = AbstractOperator.class)
    @Getter
    private final Map<Id, Operator> operators;
    @JsonProperty("runList")
    @Getter
    private final List<Id> runList;
    @JsonProperty("parasType")
    @Getter
    private final @Nullable DingoType parasType;

    private Object[] paras = null;

    @Getter
    private TaskStatus taskInitStatus;

    public TaskImpl(Id id, Id jobId, Location location, @Nullable DingoType parasType) {
        this.id = id;
        this.jobId = jobId;
        this.location = location;
        this.parasType = parasType;
        this.operators = new HashMap<>();
        this.runList = new LinkedList<>();
    }

    @JsonCreator
    public static @NonNull TaskImpl fromJson(
        @JsonProperty("id") Id id,
        @JsonProperty("jobId") Id jobId,
        @JsonProperty("location") Location location,
        @Nullable @JsonProperty("parasType") DingoType parasType,
        @JsonDeserialize(using = RawJsonDeserializer.class)
        @JsonProperty("paras") JsonNode paras
    ) {
        TaskImpl task = new TaskImpl(id, jobId, location, parasType);
        if (paras != null) {
            assert parasType != null;
            task.paras = (Object[]) parasType.convertFrom(paras, JsonConverter.INSTANCE);
        }
        return task;
    }

    public static TaskImpl fromString(String str) throws JsonProcessingException {
        return JobImpl.PARSER.parse(str, TaskImpl.class);
    }

    @Override
    public RootOperator getRoot() {
        return operators.values().stream()
            .filter(o -> o instanceof RootOperator)
            .map(o -> (RootOperator) o)
            .findFirst()
            .orElse(null);
    }

    @Override
    public void putOperator(@NonNull Operator operator) {
        operator.setTask(this);
        operators.put(operator.getId(), operator);
        if (operator instanceof SourceOperator) {
            runList.add(operator.getId());
        }
    }

    @Override
    public void deleteOperator(@NonNull Operator operator) {
        operators.remove(operator.getId());
        runList.remove(operator.getId());
    }

    @Override
    public void init() {
        boolean isStatusOK = true;
        String statusErrMsg = "";
        getOperators().forEach((id, o) -> {
            o.setId(id);
            o.setTask(this);
        });

        for (Operator operator : getOperators().values()) {
            try {
                operator.init();
            } catch (Exception ex) {
                log.error("Init operator:{} in task:{} failed catch exception:{}",
                    operator.toString(), this.id.toString(), ex, ex);
                statusErrMsg = ex.toString();
                isStatusOK = false;
            }
        }
        taskInitStatus = new TaskStatus();
        taskInitStatus.setStatus(isStatusOK);
        taskInitStatus.setTaskId(this.id.toString());
        taskInitStatus.setErrorMsg(statusErrMsg);

        reset();
    }

    public void run() {
        if (log.isDebugEnabled()) {
            log.debug("Task is starting at {}...", location);
        }
        for (Id id : runList) {
            final Operator operator = operators.get(id);
            assert operator instanceof SourceOperator
                : "Operators in run list must be source operator.";

            if (taskInitStatus != null && !taskInitStatus.getStatus()) {
                log.error("Run task but check task has init failed: {}", taskInitStatus.toString());
                operator.fin(0, FinWithException.of(taskInitStatus));
                break;
            }

            Executors.execute("execute-" + jobId + "-" + id, () -> {
                final long startTime = System.currentTimeMillis();
                boolean isStatusOK = true;
                String statusErrMsg = "OK";
                try {
                    while (operator.push(0, null)) {
                        log.info("Operator {} need another pushing.", operator.getId());
                    }
                    operator.fin(0, null);
                } catch (RuntimeException e) {
                    isStatusOK = false;
                    statusErrMsg = e.toString();
                    log.error("Run Task:{} catch operator:{} run Exception:{}",
                        getId().toString(), operator.getId(), e, e);
                } finally {
                    if (!isStatusOK) {
                        TaskStatus taskStatus = new TaskStatus();
                        taskStatus.setStatus(isStatusOK);
                        taskStatus.setTaskId(operator.getTask().getId().toString());
                        taskStatus.setErrorMsg(statusErrMsg);
                        operator.fin(0, FinWithException.of(taskStatus));
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("TaskImpl run cost: {}ms.", System.currentTimeMillis() - startTime);
                    }
                }
            });
        }
    }

    @Override
    public void reset() {
        getOperators().forEach((id, o) -> o.reset());
        setParas(paras);
    }

    @Override
    public byte @NonNull [] serialize() {
        return toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void setParas(Object[] paras) {
        this.paras = paras;
        Task.super.setParas(paras);
    }

    @JsonProperty("paras")
    Object @Nullable [] getParasJson() {
        if (parasType != null) {
            return (Object[]) parasType.convertTo(paras, JsonConverter.INSTANCE);
        }
        return null;
    }

    @Override
    public String toString() {
        try {
            return JobImpl.PARSER.stringify(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
