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

package io.dingodb.common.hash;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Setter;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Objects;

@JsonTypeName("simple")
public final class SimpleHashStrategy implements HashStrategy {
    @Setter
    private int outputNum;

    @Override
    public int selectOutput(Object @NonNull [] tuple) {
        int hash = Objects.hash(tuple);
        int index = hash % outputNum;
        return index >= 0 ? index : index + outputNum;
    }

    @Override
    public @NonNull String toString() {
        return this.getClass().getSimpleName();
    }
}
