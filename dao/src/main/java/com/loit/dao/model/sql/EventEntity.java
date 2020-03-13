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

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.databind.JsonNode;
import com.loit.common.data.EntityType;
import com.loit.common.data.Event;
import com.loit.common.data.id.EntityIdFactory;
import com.loit.common.data.id.EventId;
import com.loit.common.data.id.TenantId;
import com.loit.dao.model.BaseEntity;
import com.loit.dao.model.BaseSqlEntity;
import com.loit.dao.model.ModelConstants;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import com.loit.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = ModelConstants.EVENT_COLUMN_FAMILY_NAME)
@NoArgsConstructor
public class EventEntity  extends BaseSqlEntity<Event> implements BaseEntity<Event> {

    @Column(name = ModelConstants.EVENT_TENANT_ID_PROPERTY)
    private String tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.EVENT_ENTITY_TYPE_PROPERTY)
    private EntityType entityType;

    @Column(name = ModelConstants.EVENT_ENTITY_ID_PROPERTY)
    private String entityId;

    @Column(name = ModelConstants.EVENT_TYPE_PROPERTY)
    private String eventType;

    @Column(name = ModelConstants.EVENT_UID_PROPERTY)
    private String eventUid;

    @Type(type = "json")
    @Column(name = ModelConstants.EVENT_BODY_PROPERTY)
    private JsonNode body;

    public EventEntity(Event event) {
        if (event.getId() != null) {
            this.setId(event.getId().getId());
        }
        if (event.getTenantId() != null) {
            this.tenantId = toString(event.getTenantId().getId());
        }
        if (event.getEntityId() != null) {
            this.entityType = event.getEntityId().getEntityType();
            this.entityId = toString(event.getEntityId().getId());
        }
        this.eventType = event.getType();
        this.eventUid = event.getUid();
        this.body = event.getBody();
    }


    @Override
    public Event toData() {
        Event event = new Event(new EventId(getId()));
        event.setCreatedTime(UUIDs.unixTimestamp(getId()));
        event.setTenantId(new TenantId(toUUID(tenantId)));
        event.setEntityId(EntityIdFactory.getByTypeAndUuid(entityType, toUUID(entityId)));
        event.setBody(body);
        event.setType(eventType);
        event.setUid(eventUid);
        return event;
    }
}
