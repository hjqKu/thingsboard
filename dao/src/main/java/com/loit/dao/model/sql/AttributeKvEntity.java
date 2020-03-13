/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package com.loit.dao.model.sql;

import com.loit.dao.model.ModelConstants;
import com.loit.dao.model.ToData;
import lombok.Data;
import com.loit.common.data.kv.AttributeKvEntry;
import com.loit.common.data.kv.BaseAttributeKvEntry;
import com.loit.common.data.kv.BooleanDataEntry;
import com.loit.common.data.kv.DoubleDataEntry;
import com.loit.common.data.kv.KvEntry;
import com.loit.common.data.kv.LongDataEntry;
import com.loit.common.data.kv.StringDataEntry;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;

@Data
@Entity
@Table(name = "attribute_kv")
public class AttributeKvEntity implements ToData<AttributeKvEntry>, Serializable {

    @EmbeddedId
    private AttributeKvCompositeKey id;

    @Column(name = ModelConstants.BOOLEAN_VALUE_COLUMN)
    private Boolean booleanValue;

    @Column(name = ModelConstants.STRING_VALUE_COLUMN)
    private String strValue;

    @Column(name = ModelConstants.LONG_VALUE_COLUMN)
    private Long longValue;

    @Column(name = ModelConstants.DOUBLE_VALUE_COLUMN)
    private Double doubleValue;

    @Column(name = ModelConstants.LAST_UPDATE_TS_COLUMN)
    private Long lastUpdateTs;

    @Override
    public AttributeKvEntry toData() {
        KvEntry kvEntry = null;
        if (strValue != null) {
            kvEntry = new StringDataEntry(id.getAttributeKey(), strValue);
        } else if (booleanValue != null) {
            kvEntry = new BooleanDataEntry(id.getAttributeKey(), booleanValue);
        } else if (doubleValue != null) {
            kvEntry = new DoubleDataEntry(id.getAttributeKey(), doubleValue);
        } else if (longValue != null) {
            kvEntry = new LongDataEntry(id.getAttributeKey(), longValue);
        }
        return new BaseAttributeKvEntry(kvEntry, lastUpdateTs);
    }
}
