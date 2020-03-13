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
package com.loit.dao.entity;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.loit.common.data.EntityType;
import com.loit.common.data.HasName;
import com.loit.common.data.alarm.AlarmId;
import com.loit.common.data.id.*;
import com.loit.dao.alarm.AlarmService;
import com.loit.dao.asset.AssetService;
import com.loit.dao.customer.CustomerService;
import com.loit.dao.dashboard.DashboardService;
import com.loit.dao.device.DeviceService;
import com.loit.dao.entityview.EntityViewService;
import com.loit.dao.rule.RuleChainService;
import com.loit.dao.tenant.TenantService;
import com.loit.dao.user.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.loit.common.data.HasName;
import com.loit.common.data.alarm.AlarmId;
import org.thingsboard.server.common.data.id.*;
import com.loit.dao.alarm.AlarmService;
import com.loit.dao.asset.AssetService;
import com.loit.dao.customer.CustomerService;
import com.loit.dao.dashboard.DashboardService;
import com.loit.dao.device.DeviceService;
import com.loit.dao.entityview.EntityViewService;
import com.loit.dao.rule.RuleChainService;
import com.loit.dao.tenant.TenantService;
import com.loit.dao.user.UserService;

/**
 * Created by ashvayka on 04.05.17.
 */
@Service
@Slf4j
public class BaseEntityService extends AbstractEntityService implements EntityService {

    @Autowired
    private AssetService assetService;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private EntityViewService entityViewService;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private UserService userService;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private AlarmService alarmService;

    @Autowired
    private RuleChainService ruleChainService;

    @Override
    public void deleteEntityRelations(TenantId tenantId, EntityId entityId) {
        super.deleteEntityRelations(tenantId, entityId);
    }

    @Override
    public ListenableFuture<String> fetchEntityNameAsync(TenantId tenantId, EntityId entityId) {
        log.trace("Executing fetchEntityNameAsync [{}]", entityId);
        ListenableFuture<String> entityName;
        ListenableFuture<? extends HasName> hasName;
        switch (entityId.getEntityType()) {
            case EntityType.ASSET:
                hasName = assetService.findAssetByIdAsync(tenantId, new AssetId(entityId.getId()));
                break;
            case EntityType.DEVICE:
                hasName = deviceService.findDeviceByIdAsync(tenantId, new DeviceId(entityId.getId()));
                break;
            case EntityType.ENTITY_VIEW:
                hasName = entityViewService.findEntityViewByIdAsync(tenantId, new EntityViewId(entityId.getId()));
                break;
            case EntityType.TENANT:
                hasName = tenantService.findTenantByIdAsync(tenantId, new TenantId(entityId.getId()));
                break;
            case EntityType.CUSTOMER:
                hasName = customerService.findCustomerByIdAsync(tenantId, new CustomerId(entityId.getId()));
                break;
            case EntityType.USER:
                hasName = userService.findUserByIdAsync(tenantId, new UserId(entityId.getId()));
                break;
            case EntityType.DASHBOARD:
                hasName = dashboardService.findDashboardInfoByIdAsync(tenantId, new DashboardId(entityId.getId()));
                break;
            case EntityType.ALARM:
                hasName = alarmService.findAlarmByIdAsync(tenantId, new AlarmId(entityId.getId()));
                break;
            case EntityType.RULE_CHAIN:
                hasName = ruleChainService.findRuleChainByIdAsync(tenantId, new RuleChainId(entityId.getId()));
                break;
            default:
                throw new IllegalStateException("Not Implemented!");
        }
        entityName = Futures.transform(hasName, (Function<HasName, String>) hasName1 -> hasName1 != null ? hasName1.getName() : null );
        return entityName;
    }

}
