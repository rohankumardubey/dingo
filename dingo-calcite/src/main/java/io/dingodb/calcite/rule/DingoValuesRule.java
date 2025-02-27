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

package io.dingodb.calcite.rule;

import io.dingodb.calcite.DingoConventions;
import io.dingodb.calcite.rel.DingoValues;
import io.dingodb.calcite.rel.LogicalDingoValues;
import org.apache.calcite.plan.Convention;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.convert.ConverterRule;
import org.checkerframework.checker.nullness.qual.NonNull;

public class DingoValuesRule extends ConverterRule {
    public static final Config ROOT = Config.INSTANCE
        .withConversion(
            LogicalDingoValues.class,
            Convention.NONE,
            DingoConventions.ROOT,
            "DingoValuesRule.ROOT"
        )
        .withRuleFactory(DingoValuesRule::new);

    public static final Config DISTRIBUTED = Config.INSTANCE
        .withConversion(
            LogicalDingoValues.class,
            Convention.NONE,
            DingoConventions.DISTRIBUTED,
            "DingoValuesRule.DISTRIBUTED"
        )
        .withRuleFactory(DingoValuesRule::new);

    protected DingoValuesRule(Config config) {
        super(config);
    }

    @Override
    public RelNode convert(@NonNull RelNode rel) {
        Convention convention = this.getOutConvention();
        return new DingoValues(
            rel.getCluster(),
            rel.getTraitSet().replace(convention),
            rel.getRowType(),
            ((LogicalDingoValues) rel).getTuples()
        );
    }
}
