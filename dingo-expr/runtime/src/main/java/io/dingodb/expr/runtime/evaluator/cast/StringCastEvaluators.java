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

package io.dingodb.expr.runtime.evaluator.cast;

import io.dingodb.expr.annotations.Evaluators;
import io.dingodb.expr.runtime.evaluator.base.Evaluator;
import io.dingodb.expr.runtime.evaluator.base.EvaluatorFactory;
import io.dingodb.expr.runtime.evaluator.base.EvaluatorKey;
import io.dingodb.expr.runtime.evaluator.base.UniversalEvaluator;
import io.dingodb.expr.runtime.utils.DateTimeUtils;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

@Evaluators(
    evaluatorKey = EvaluatorKey.class,
    evaluatorBase = Evaluator.class,
    evaluatorFactory = EvaluatorFactory.class,
    universalEvaluator = UniversalEvaluator.class,
    induceSequence = {}
)
final class StringCastEvaluators {
    private StringCastEvaluators() {
    }

    static @NonNull String stringCast(int value) {
        return Integer.toString(value);
    }

    static @NonNull String stringCast(long value) {
        return Long.toString(value);
    }

    static @NonNull String stringCast(double value) {
        return Double.toString(value);
    }

    static @NonNull String stringCast(boolean value) {
        return Boolean.toString(value);
    }

    static @NonNull String stringCast(@NonNull BigDecimal value) {
        return value.toPlainString();
    }

    static @NonNull String stringCast(@NonNull String value) {
        return value;
    }

    static @NonNull String stringCast(@NonNull Date value) {
        return DateTimeUtils.dateFormat(value);
    }

    static @NonNull String stringCast(@NonNull Time value) {
        return DateTimeUtils.timeFormat(value);
    }

    static @NonNull String stringCast(@NonNull Timestamp value) {
        return DateTimeUtils.timestampFormat(value);
    }

    static @NonNull String stringCast(byte[] value) {
        return new String(value, StandardCharsets.UTF_8);
    }
}
