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

import io.dingodb.calcite.utils.RelDataTypeUtils;
import io.dingodb.common.type.TupleMapping;
import lombok.Getter;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptCost;
import org.apache.calcite.plan.RelOptPlanner;
import org.apache.calcite.plan.RelOptTable;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.RelWriter;
import org.apache.calcite.rel.core.TableScan;
import org.apache.calcite.rel.hint.RelHint;
import org.apache.calcite.rel.metadata.RelMdUtil;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.util.ImmutableBitSet;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.stream.Collectors;

public class LogicalDingoTableScan extends TableScan {
    @Getter
    protected final RexNode filter;
    @Getter
    protected final TupleMapping selection;

    public LogicalDingoTableScan(
        RelOptCluster cluster,
        RelTraitSet traitSet,
        List<RelHint> hints,
        RelOptTable table,
        @Nullable RexNode filter,
        @Nullable TupleMapping selection
    ) {
        super(cluster, traitSet, hints, table);
        this.filter = filter;
        this.selection = selection;
    }

    public boolean isKey(ImmutableBitSet columns) {
        if (selection != null) {
            columns = ImmutableBitSet.of(columns.asList().stream()
                .map(selection::get)
                .collect(Collectors.toList()));
        }
        return getTable().isKey(columns);
    }

    @Override
    public double estimateRowCount(RelMetadataQuery mq) {
        double rowCount;
        if (filter != null) {
            RelNode fakeInput = new LogicalDingoTableScan(
                getCluster(),
                getTraitSet(),
                getHints(),
                table,
                null,
                null
            );
            rowCount = RelMdUtil.estimateFilteredRows(fakeInput, filter, mq);
        } else {
            rowCount = super.estimateRowCount(mq) / DingoTableScan.ASSUME_PARTS;
        }
        return rowCount;
    }

    @Override
    public @Nullable RelOptCost computeSelfCost(@NonNull RelOptPlanner planner, @NonNull RelMetadataQuery mq) {
        double rowCount = estimateRowCount(mq);
        double cpu = rowCount + 1;
        if (selection != null) {
            cpu = cpu * (double) selection.size() / (double) table.getRowType().getFieldCount();
        }
        return planner.getCostFactory().makeCost(rowCount, cpu, rowCount);
    }

    @Override
    public RelDataType deriveRowType() {
        return RelDataTypeUtils.mapType(getCluster().getTypeFactory(), table.getRowType(), selection);
    }

    @Override
    public @NonNull RelWriter explainTerms(@NonNull RelWriter pw) {
        super.explainTerms(pw);
        // crucial, this is how Calcite distinguish between different node with different props.
        pw.itemIf("filter", filter, filter != null);
        pw.itemIf("selection", selection, selection != null);
        return pw;
    }
}
