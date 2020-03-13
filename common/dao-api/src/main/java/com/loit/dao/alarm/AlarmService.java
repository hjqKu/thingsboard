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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.ListenableFuture;
import com.loit.common.data.alarm.Alarm;
import com.loit.common.data.alarm.AlarmId;
import com.loit.common.data.alarm.AlarmInfo;
import com.loit.common.data.alarm.AlarmQuery;
import com.loit.common.data.alarm.AlarmSearchStatus;
import com.loit.common.data.alarm.AlarmSeverity;
import com.loit.common.data.alarm.AlarmStatus;
import com.loit.common.data.id.EntityId;
import com.loit.common.data.id.TenantId;
import com.loit.common.data.page.TimePageData;

/**
 * Created by ashvayka on 11.05.17.
 */
public interface AlarmService {

    Alarm createOrUpdateAlarm(Alarm alarm);

    Boolean deleteAlarm(TenantId tenantId, AlarmId alarmId);

    ListenableFuture<Boolean> ackAlarm(TenantId tenantId, AlarmId alarmId, long ackTs);

    ListenableFuture<Boolean> clearAlarm(TenantId tenantId, AlarmId alarmId, JsonNode details, long ackTs);

    ListenableFuture<Alarm> findAlarmByIdAsync(TenantId tenantId, AlarmId alarmId);

    ListenableFuture<AlarmInfo> findAlarmInfoByIdAsync(TenantId tenantId, AlarmId alarmId);

    ListenableFuture<TimePageData<AlarmInfo>> findAlarms(TenantId tenantId, AlarmQuery query);

    AlarmSeverity findHighestAlarmSeverity(TenantId tenantId, EntityId entityId, AlarmSearchStatus alarmSearchStatus,
                                           AlarmStatus alarmStatus);

    ListenableFuture<Alarm> findLatestByOriginatorAndType(TenantId tenantId, EntityId originator, String type);

}
