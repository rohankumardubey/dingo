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

package io.dingodb.common.type;

import io.dingodb.common.type.converter.CsvConverter;
import io.dingodb.common.type.converter.DataConverter;
import io.dingodb.expr.runtime.TypeCode;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@EqualsAndHashCode(of = {"typeCode"})
public abstract class AbstractDingoType implements DingoType {
    @Getter
    @Setter
    protected int typeCode;
    @Getter
    @Setter
    private Integer id;

    protected AbstractDingoType(int typeCode) {
        this.typeCode = typeCode;
    }

    protected abstract @Nullable Object convertValueTo(@NonNull Object value, @NonNull DataConverter converter);

    protected abstract @Nullable Object convertValueFrom(@NonNull Object value, @NonNull DataConverter converter);

    @Override
    public int fieldCount() {
        return 0;
    }

    @Override
    public DingoType getChild(@NonNull Object index) {
        throw new IllegalStateException("Get child of type \"" + TypeCode.nameOf(typeCode) + "\" is stupid.");
    }

    @Override
    public @NonNull DingoType select(@NonNull TupleMapping mapping) {
        throw new IllegalStateException("Selecting fields from type \"" + TypeCode.nameOf(typeCode) + "\" is stupid.");
    }

    @Override
    public Object convertTo(@Nullable Object value, @NonNull DataConverter converter) {
        if (value != null) {
            return convertValueTo(value, converter);
        }
        return null;
    }

    @Override
    public Object convertFrom(@Nullable Object value, @NonNull DataConverter converter) {
        if (value != null && !converter.isNull(value)) {
            return convertValueFrom(value, converter);
        }
        return null;
    }

    @Override
    public Object parse(@Nullable Object value) {
        return convertFrom(value, CsvConverter.INSTANCE);
    }
}
