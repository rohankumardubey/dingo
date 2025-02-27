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

package io.dingodb.expr.runtime.op.logical;

import io.dingodb.expr.runtime.EvalContext;
import io.dingodb.expr.runtime.RtExpr;
import io.dingodb.expr.runtime.exception.FailGetEvaluator;
import org.checkerframework.checker.nullness.qual.Nullable;

public class RtOrOp extends RtLogicalOp {
    private static final long serialVersionUID = -1477334212197197621L;

    /**
     * Create an RtOrOp. RtNotOp performs logical OR operation.
     *
     * @param paras the parameters of the op
     */
    public RtOrOp(RtExpr[] paras) {
        super(paras);
    }

    @Override
    public @Nullable Object eval(EvalContext etx) throws FailGetEvaluator {
        Boolean result = Boolean.FALSE;
        for (RtExpr para : paras) {
            Object v = para.eval(etx);
            if (v == null) {
                if (result == Boolean.FALSE) {
                    result = null;
                }
            } else if (RtLogicalOp.test(v)) {
                result = Boolean.TRUE;
            }
        }
        return result;
    }
}
