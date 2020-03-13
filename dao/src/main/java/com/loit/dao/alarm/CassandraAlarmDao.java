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
package com.loit.dao.alarm;

import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.loit.common.data.EntityType;
import com.loit.common.data.alarm.Alarm;
import com.loit.common.data.alarm.AlarmInfo;
import com.loit.common.data.alarm.AlarmQuery;
import com.loit.common.data.alarm.AlarmSearchStatus;
import com.loit.common.data.id.EntityId;
import com.loit.common.data.id.TenantId;
import com.loit.common.data.relation.EntityRelation;
import com.loit.common.data.relation.RelationTypeGroup;
import com.loit.dao.model.ModelConstants;
import com.loit.dao.model.nosql.AlarmEntity;
import com.loit.dao.nosql.CassandraAbstractModelDao;
import com.loit.dao.relation.RelationDao;
import com.loit.dao.util.NoSqlDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;

@Component
@Slf4j
@NoSqlDao
public class CassandraAlarmDao extends CassandraAbstractModelDao<AlarmEntity, Alarm> implements AlarmDao {

    @Autowired
    private RelationDao relationDao;

    @Override
    protected Class<AlarmEntity> getColumnFamilyClass() {
        return AlarmEntity.class;
    }

    @Override
    protected String getColumnFamilyName() {
        return ModelConstants.ALARM_COLUMN_FAMILY_NAME;
    }

    protected boolean isDeleteOnSave() {
        return false;
    }

    @Override
    public Alarm save(TenantId tenantId, Alarm alarm) {
        log.debug("Save asset [{}] ", alarm);
        return super.save(tenantId, alarm);
    }

    @Override
    public Boolean deleteAlarm(TenantId tenantId, Alarm alarm) {
        Statement delete = QueryBuilder.delete().all().from(getColumnFamilyName()).where(QueryBuilder.eq(ModelConstants.ID_PROPERTY, alarm.getId().getId()))
            .and(QueryBuilder.eq(ModelConstants.ALARM_TENANT_ID_PROPERTY, tenantId.getId()))
            .and(QueryBuilder.eq(ModelConstants.ALARM_ORIGINATOR_ID_PROPERTY, alarm.getOriginator().getId()))
            .and(QueryBuilder.eq(ModelConstants.ALARM_ORIGINATOR_TYPE_PROPERTY, alarm.getOriginator().getEntityType()))
            .and(QueryBuilder.eq(ModelConstants.ALARM_TYPE_PROPERTY, alarm.getType()));
        log.debug("Remove request: {}", delete.toString());
        return executeWrite(tenantId, delete).wasApplied();
    }

    @Override
    public ListenableFuture<Alarm> findLatestByOriginatorAndType(TenantId tenantId, EntityId originator, String type) {
        Select select = select().from(ModelConstants.ALARM_COLUMN_FAMILY_NAME);
        Select.Where query = select.where();
        query.and(QueryBuilder.eq(ModelConstants.ALARM_TENANT_ID_PROPERTY, tenantId.getId()));
        query.and(QueryBuilder.eq(ModelConstants.ALARM_ORIGINATOR_ID_PROPERTY, originator.getId()));
        query.and(QueryBuilder.eq(ModelConstants.ALARM_ORIGINATOR_TYPE_PROPERTY, originator.getEntityType()));
        query.and(QueryBuilder.eq(ModelConstants.ALARM_TYPE_PROPERTY, type));
        query.limit(1);
        query.orderBy(QueryBuilder.asc(ModelConstants.ALARM_TYPE_PROPERTY), QueryBuilder.desc(ModelConstants.ID_PROPERTY));
        return findOneByStatementAsync(tenantId, query);
    }

    @Override
    public ListenableFuture<List<AlarmInfo>> findAlarms(TenantId tenantId, AlarmQuery query) {
        log.trace("Try to find alarms by entity [{}], searchStatus [{}], status [{}] and pageLink [{}]", query.getAffectedEntityId(), query.getSearchStatus(), query.getStatus(), query.getPageLink());
        EntityId affectedEntity = query.getAffectedEntityId();
        String searchStatusName;
        if (query.getSearchStatus() == null && query.getStatus() == null) {
            searchStatusName = AlarmSearchStatus.ANY.name();
        } else if (query.getSearchStatus() != null) {
            searchStatusName = query.getSearchStatus().name();
        } else {
            searchStatusName = query.getStatus().name();
        }
        String relationType = BaseAlarmService.ALARM_RELATION_PREFIX + searchStatusName;
        ListenableFuture<List<EntityRelation>> relations = relationDao.findRelations(tenantId, affectedEntity, relationType, RelationTypeGroup.ALARM, EntityType.ALARM, query.getPageLink());
        return Futures.transformAsync(relations, input -> {
            List<ListenableFuture<AlarmInfo>> alarmFutures = new ArrayList<>(input.size());
            for (EntityRelation relation : input) {
                alarmFutures.add(Futures.transform(
                        findAlarmByIdAsync(tenantId, relation.getTo().getId()),
                        AlarmInfo::new));
            }
            return Futures.successfulAsList(alarmFutures);
        });
    }

    @Override
    public ListenableFuture<Alarm> findAlarmByIdAsync(TenantId tenantId, UUID key) {
        log.debug("Get alarm by id {}", key);
        Select.Where query = select().from(ModelConstants.ALARM_BY_ID_VIEW_NAME).where(eq(ModelConstants.ID_PROPERTY, key));
        query.limit(1);
        log.trace("Execute query {}", query);
        return findOneByStatementAsync(tenantId, query);
    }
}
