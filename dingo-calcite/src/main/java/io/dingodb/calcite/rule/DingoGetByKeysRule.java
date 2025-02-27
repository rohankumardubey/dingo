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
import io.dingodb.calcite.rel.DingoGetByKeys;
import io.dingodb.calcite.rel.DingoTableScan;
import io.dingodb.common.table.TableDefinition;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.plan.RelRule;
import org.apache.calcite.rex.RexLiteral;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexUtil;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.immutables.value.Value;

import java.util.Map;
import java.util.Set;

import static io.dingodb.calcite.DingoTable.dingo;

@Slf4j
@Value.Enclosing
public class DingoGetByKeysRule extends RelRule<DingoGetByKeysRule.Config> {
    public DingoGetByKeysRule(Config config) {
        super(config);
    }

    @Override
    public void onMatch(@NonNull RelOptRuleCall call) {
        final DingoTableScan rel = call.rel(0);
        RexNode rexNode = RexUtil.toDnf(rel.getCluster().getRexBuilder(), rel.getFilter());
        TableDefinition td = dingo(rel.getTable()).getTableDefinition();
        KeyFilterRexVisitor visitor = new KeyFilterRexVisitor(td, rel.getCluster().getRexBuilder());
        Set<Map<Integer, RexLiteral>> items = rexNode.accept(visitor);
        if (visitor.checkKeyItems(items)) {
            call.transformTo(new DingoGetByKeys(
                rel.getCluster(),
                rel.getTraitSet().replace(DingoConventions.DISTRIBUTED),
                rel.getHints(),
                rel.getTable(),
                rel.getFilter(),
                rel.getSelection(),
                items
            ));
        }
    }

    @Value.Immutable
    public interface Config extends RelRule.Config {
        Config DEFAULT = ImmutableDingoGetByKeysRule.Config.builder()
            .operandSupplier(b0 ->
                b0.operand(DingoTableScan.class).predicate(r -> r.getFilter() != null)
                    .noInputs()
            )
            .description("DingoGetByKeysRule")
            .build();

        @Override
        default DingoGetByKeysRule toRule() {
            return new DingoGetByKeysRule(this);
        }
    }
}
