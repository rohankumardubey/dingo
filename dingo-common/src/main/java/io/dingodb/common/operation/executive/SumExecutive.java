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

package io.dingodb.common.operation.executive;

import io.dingodb.common.operation.DingoExecResult;
import io.dingodb.common.operation.Value;
import io.dingodb.common.operation.compute.NumericType;
import io.dingodb.common.operation.compute.number.ComputeNumber;
import io.dingodb.common.operation.context.BasicContext;
import io.dingodb.common.store.KeyValue;
import io.dingodb.common.table.ColumnDefinition;
import io.dingodb.common.table.TableDefinition;
import io.dingodb.common.type.DingoType;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Slf4j
public class SumExecutive extends NumberExecutive<BasicContext, Iterator<KeyValue>, Object> {

    @Override
    public DingoExecResult execute(BasicContext context, Iterator<KeyValue> records) {
        int[] keyIndex = getKeyIndex(context);
        int[] valueIndex = getValueIndex(context);

        Map<String, ComputeNumber> map = new HashMap<>();
        Map<String, Value> result = new HashMap<>();

        TableDefinition definition = context.definition;
        while (records.hasNext()) {
            KeyValue keyValue = records.next();
            if (context.filter == null || context.filter.filter(context, keyValue)) {
                try {
                    if (keyIndex.length > 0) {
                        Object[] objects = context.dingoKeyCodec().decodeKey(keyValue.getKey(), keyIndex);
                        for (int i = 0; i < objects.length; i++) {
                            DingoType dingoType = definition.getColumn(keyIndex[i]).getDingoType();
                            ComputeNumber number = convertType(objects[i], dingoType);
                            map.merge(definition.getColumn(keyIndex[i]).getName(), number, ComputeNumber::add);
                        }
                    }
                    if (valueIndex.length > 0) {
                        Object[] objects = context.dingoValueCodec().decode(keyValue.getValue(), valueIndex);
                        int keyCount = definition.getPrimaryKeyCount();
                        for (int i = 0; i < objects.length; i++) {
                            ColumnDefinition columnDefinition = definition.getColumn(valueIndex[i] + keyCount);
                            ComputeNumber number = convertType(objects[i], columnDefinition.getDingoType());
                            map.merge(columnDefinition.getName(), number, ComputeNumber::add);
                        }
                    }
                } catch (IOException e) {
                    log.error("Column:{} decode failed", Arrays.stream(context.columns).map(col -> col.name).toArray());
                    return new DingoExecResult(false, "Sum operation decode failed, " + e.getMessage());
                }
            }
        }
        map.forEach((key, value) -> result.put(key, value.value()));
        return new DingoExecResult(result, true, "OK", NumericType.SUM.name());
    }
}
