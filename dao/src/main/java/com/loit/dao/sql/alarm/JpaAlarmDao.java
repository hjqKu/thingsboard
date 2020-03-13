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
package com.loit.dao.sql.alarm;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.loit.common.data.EntityType;
import com.loit.common.data.UUIDConverter;
import com.loit.common.data.alarm.Alarm;
import com.loit.common.data.alarm.AlarmInfo;
import com.loit.common.data.alarm.AlarmQuery;
import com.loit.common.data.alarm.AlarmSearchStatus;
import com.loit.common.data.id.EntityId;
import com.loit.common.data.id.TenantId;
import com.loit.common.data.relation.EntityRelation;
import com.loit.common.data.relation.RelationTypeGroup;
import com.loit.dao.DaoUtil;
import com.loit.dao.alarm.AlarmDao;
import com.loit.dao.alarm.BaseAlarmService;
import com.loit.dao.model.sql.AlarmEntity;
import com.loit.dao.relation.RelationDao;
import com.loit.dao.util.SqlDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import com.loit.dao.sql.JpaAbstractDao;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 5/19/2017.
 */
@Slf4j
@Component
@SqlDao
public class JpaAlarmDao extends JpaAbstractDao<AlarmEntity, Alarm> implements AlarmDao {

    @Autowired
    private AlarmRepository alarmRepository;

    @Autowired
    private RelationDao relationDao;

    @Override
    protected Class<AlarmEntity> getEntityClass() {
        return AlarmEntity.class;
    }

    @Override
    protected CrudRepository<AlarmEntity, String> getCrudRepository() {
        return alarmRepository;
    }

    @Override
    public Boolean deleteAlarm(TenantId tenantId, Alarm alarm) {
        return removeById(tenantId, alarm.getUuidId());
    }

    @Override
    public ListenableFuture<Alarm> findLatestByOriginatorAndType(TenantId tenantId, EntityId originator, String type) {
        return service.submit(() -> {
            List<AlarmEntity> latest = alarmRepository.findLatestByOriginatorAndType(
                    UUIDConverter.fromTimeUUID(originator.getId()),
                    type,
                    PageRequest.of(0, 1));
            return latest.isEmpty() ? null : DaoUtil.getData(latest.get(0));
        });
    }

    @Override
    public ListenableFuture<Alarm> findAlarmByIdAsync(TenantId tenantId, UUID key) {
        return findByIdAsync(tenantId, key);
    }

    @Override
    public ListenableFuture<List<AlarmInfo>> findAlarms(TenantId tenantId, AlarmQuery query) {
        log.trace("Try to find alarms by entity [{}], status [{}] and pageLink [{}]", query.getAffectedEntityId(), query.getStatus(), query.getPageLink());
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
}
