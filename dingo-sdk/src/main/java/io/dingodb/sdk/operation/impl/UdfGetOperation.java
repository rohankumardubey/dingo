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

package io.dingodb.sdk.operation.impl;

import io.dingodb.common.CommonId;
import io.dingodb.common.store.KeyValue;
import io.dingodb.sdk.operation.ContextForStore;
import io.dingodb.sdk.operation.IStoreOperation;
import io.dingodb.sdk.operation.ResultForStore;
import io.dingodb.sdk.operation.UDFContext;
import io.dingodb.server.api.ExecutorApi;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class UdfGetOperation implements IStoreOperation {

    private static final UdfGetOperation instance = new UdfGetOperation();

    private UdfGetOperation() {
    }

    public static UdfGetOperation getInstance() {
        return instance;
    }

    @Override
    public ResultForStore doOperation(ExecutorApi executorApi,
                                      CommonId tableId,
                                      ContextForStore parameters) {
        try {
            if (parameters == null
                || parameters.getStartKeyListInBytes().size() < 1) {
                log.error("Parameters is null || table:{} has non key columns", tableId);
                return new ResultForStore(false, "Invalid parameters for get operation");
            }
            List<byte[]> keyList = parameters.getStartKeyListInBytes();
            List<KeyValue> recordList = new ArrayList<>();
            UDFContext udfContext = parameters.getUdfContext();
            for (byte[] primaryKey : keyList) {
                recordList.add(executorApi.udfGet(tableId, primaryKey, udfContext.getUdfName(),
                    udfContext.getFunctionName(), udfContext.getUdfVersion()));
            }
            return new ResultForStore(true, "OK", recordList);
        } catch (Exception e) {
            log.error("doUdfGet KeyValue error, tableId:{}, exception:{}", tableId, e.toString(), e);
            throw e;
        }
    }
}
