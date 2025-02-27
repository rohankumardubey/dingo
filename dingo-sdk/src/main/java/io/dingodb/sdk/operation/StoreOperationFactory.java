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

package io.dingodb.sdk.operation;

import io.dingodb.sdk.operation.impl.DeleteOperation;
import io.dingodb.sdk.operation.impl.GetOperation;
import io.dingodb.sdk.operation.impl.PutOperation;
import io.dingodb.sdk.operation.impl.QueryOperation;
import io.dingodb.sdk.operation.impl.UdfGetOperation;
import io.dingodb.sdk.operation.impl.UdfUpdateOperation;

public final class StoreOperationFactory {
    public static IStoreOperation getStoreOperation(StoreOperationType type) {
        switch (type) {
            case PUT:
                return PutOperation.getInstance();
            case GET:
                return GetOperation.getInstance();
            case DELETE:
                return DeleteOperation.getInstance();
            case QUERY:
                return QueryOperation.getInstance();
            case GET_UDF:
                return UdfGetOperation.getInstance();
            case UPDATE_UDF:
                return UdfUpdateOperation.getInstance();
            default:
                return null;
        }
    }
}
