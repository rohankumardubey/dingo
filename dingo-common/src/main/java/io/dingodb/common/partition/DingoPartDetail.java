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

package io.dingodb.common.partition;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@ToString
public class DingoPartDetail implements Serializable {

    private static final long serialVersionUID = -2687766895780655226L;

    @JsonProperty("partNm")
    String partNm = null;

    @JsonProperty("operator")
    String operator = null;

    @JsonProperty("operand")
    List<Object> operand = null;

    @JsonProperty("function")
    String function = null;

    public DingoPartDetail(Object partNm, String operator, List<Object> operand) {
        if (partNm != null) {
            this.partNm = partNm.toString();
        }
        this.operator = operator;
        this.operand = operand;
    }

}
