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
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.dingodb.common.Location;
import io.dingodb.common.type.DingoType;
import io.dingodb.exec.base.Id;
import io.dingodb.exec.base.Job;
import io.dingodb.exec.base.Task;
import io.dingodb.expr.json.runtime.Parser;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashMap;
import java.util.Map;

@JsonPropertyOrder({"tasks"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class JobImpl implements Job {
    public static final Parser PARSER = Parser.JSON;

    @JsonProperty("jobId")
    private final Id jobId;
    @JsonProperty("tasks")
    @JsonSerialize(contentAs = TaskImpl.class)
    @JsonDeserialize(contentAs = TaskImpl.class)
    @Getter
    private final Map<Id, Task> tasks;

    // Need not serialize this, for it is serialized in tasks.
    private final DingoType parasType;

    @JsonCreator
    public JobImpl(@JsonProperty("jobId") Id jobId) {
        this(jobId, null);
    }

    public JobImpl(@JsonProperty("jobId") Id jobId, @Nullable DingoType parasType) {
        this.jobId = jobId;
        this.tasks = new HashMap<>();
        this.parasType = parasType;
    }

    public static JobImpl fromString(String str) throws JsonProcessingException {
        return PARSER.parse(str, JobImpl.class);
    }

    @Override
    public @NonNull Task create(Id id, Location location, DingoType parasType) {
        if (tasks.containsKey(id)) {
            throw new IllegalArgumentException("The task \"" + id + "\" already exists in job \"" + jobId + "\".");
        }
        Task task = new TaskImpl(id, jobId, location, parasType);
        tasks.put(id, task);
        return task;
    }

    @Nullable
    public DingoType getParasType() {
        return parasType;
    }

    @Override
    public String toString() {
        try {
            return PARSER.stringify(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
