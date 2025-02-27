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

package io.dingodb.calcite.rel;

import io.dingodb.calcite.visitor.DingoRelVisitor;
import lombok.Getter;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptCost;
import org.apache.calcite.plan.RelOptPlanner;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.AbstractRelNode;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.RelWriter;
import org.apache.calcite.rel.SingleRel;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.calcite.rel.type.RelDataType;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

public final class DingoHash extends SingleRel implements DingoRel {
    @Getter
    private final List<Integer> keys;

    public DingoHash(RelOptCluster cluster, RelTraitSet traits, RelNode input, List<Integer> keys) {
        super(cluster, traits, input);
        this.keys = keys;
    }

    @Override
    public @NonNull RelNode copy(RelTraitSet traitSet, List<RelNode> inputs) {
        return new DingoHash(getCluster(), traitSet, AbstractRelNode.sole(inputs), keys);
    }

    @Override
    public @Nullable RelOptCost computeSelfCost(@NonNull RelOptPlanner planner, RelMetadataQuery mq) {
        return planner.getCostFactory().makeZeroCost();
    }

    @Override
    public double estimateRowCount(@NonNull RelMetadataQuery mq) {
        double rowCount = mq.getRowCount(getInput());
        // Assume hash redistribute input to 3 partitions.
        return rowCount / 3.0d;
    }

    @Override
    public @NonNull RelWriter explainTerms(RelWriter pw) {
        super.explainTerms(pw);
        // crucial, this is how Calcite distinguish between different node with different props.
        pw.itemIf("keys", keys, keys != null);
        return pw;
    }

    @Override
    protected RelDataType deriveRowType() {
        return input.getRowType();
    }

    @Override
    public <T> T accept(@NonNull DingoRelVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
