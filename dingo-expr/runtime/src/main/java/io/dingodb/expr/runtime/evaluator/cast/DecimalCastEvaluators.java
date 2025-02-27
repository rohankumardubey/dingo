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
import org.checkerframework.checker.nullness.qual.NonNull;

import java.math.BigDecimal;

@Evaluators(
    evaluatorKey = EvaluatorKey.class,
    evaluatorBase = Evaluator.class,
    evaluatorFactory = EvaluatorFactory.class,
    universalEvaluator = UniversalEvaluator.class,
    induceSequence = {}
)
final class DecimalCastEvaluators {
    private DecimalCastEvaluators() {
    }

    static @NonNull BigDecimal decimalCast(int value) {
        return BigDecimal.valueOf(value);
    }

    static @NonNull BigDecimal decimalCast(long value) {
        return BigDecimal.valueOf(value);
    }

    static @NonNull BigDecimal decimalCast(double value) {
        return BigDecimal.valueOf(value);
    }

    static @NonNull BigDecimal decimalCast(boolean value) {
        return value ? BigDecimal.ONE : BigDecimal.ZERO;
    }

    static @NonNull BigDecimal decimalCast(@NonNull BigDecimal value) {
        return value;
    }

    static @NonNull BigDecimal decimalCast(@NonNull String value) {
        return new BigDecimal(value);
    }
}
