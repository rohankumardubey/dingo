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
import io.dingodb.calcite.rel.DingoTableScan;
import io.dingodb.calcite.rel.LogicalDingoTableScan;
import org.apache.calcite.plan.Convention;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.convert.ConverterRule;

public class DingoTableScanRule extends ConverterRule {
    public static final Config DEFAULT = Config.INSTANCE
        .withConversion(
            LogicalDingoTableScan.class,
            Convention.NONE,
            DingoConventions.DISTRIBUTED,
            "DingoTableScanRule.DISTRIBUTED"
        )
        .withRuleFactory(DingoTableScanRule::new);

    protected DingoTableScanRule(Config config) {
        super(config);
    }

    @Override
    public RelNode convert(RelNode rel) {
        LogicalDingoTableScan scan = (LogicalDingoTableScan) rel;
        return new DingoTableScan(
            scan.getCluster(),
            scan.getTraitSet().replace(DingoConventions.DISTRIBUTED),
            scan.getHints(),
            scan.getTable(),
            scan.getFilter(),
            scan.getSelection()
        );
    }
}
