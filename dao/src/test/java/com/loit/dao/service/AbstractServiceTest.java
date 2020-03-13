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
package com.loit.dao.service;

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loit.common.data.BaseData;
import com.loit.common.data.EntityType;
import com.loit.common.data.Event;
import com.loit.common.data.id.EntityId;
import com.loit.common.data.id.TenantId;
import com.loit.common.data.id.UUIDBased;
import com.loit.dao.alarm.AlarmService;
import com.loit.dao.asset.AssetService;
import com.loit.dao.component.ComponentDescriptorService;
import com.loit.dao.customer.CustomerService;
import com.loit.dao.dashboard.DashboardService;
import com.loit.dao.device.DeviceCredentialsService;
import com.loit.dao.device.DeviceService;
import com.loit.dao.entityview.EntityViewService;
import com.loit.dao.event.EventService;
import com.loit.dao.relation.RelationService;
import com.loit.dao.rule.RuleChainService;
import com.loit.dao.settings.AdminSettingsService;
import com.loit.dao.tenant.TenantService;
import com.loit.dao.timeseries.TimeseriesService;
import com.loit.dao.user.UserService;
import com.loit.dao.widget.WidgetTypeService;
import com.loit.dao.widget.WidgetsBundleService;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import com.loit.dao.audit.AuditLogLevelFilter;
import com.loit.dao.audit.AuditLogLevelMask;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;


@RunWith(SpringRunner.class)
@ContextConfiguration(classes = AbstractServiceTest.class, loader = AnnotationConfigContextLoader.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Configuration
@ComponentScan("org.thingsboard.server")
public abstract class AbstractServiceTest {

    protected ObjectMapper mapper = new ObjectMapper();

    public static final TenantId SYSTEM_TENANT_ID = new TenantId(EntityId.NULL_UUID);

    @Autowired
    protected UserService userService;

    @Autowired
    protected AdminSettingsService adminSettingsService;

    @Autowired
    protected TenantService tenantService;

    @Autowired
    protected CustomerService customerService;

    @Autowired
    protected DeviceService deviceService;

    @Autowired
    protected AssetService assetService;

    @Autowired
    protected EntityViewService entityViewService;

    @Autowired
    protected DeviceCredentialsService deviceCredentialsService;

    @Autowired
    protected WidgetsBundleService widgetsBundleService;

    @Autowired
    protected WidgetTypeService widgetTypeService;

    @Autowired
    protected DashboardService dashboardService;

    @Autowired
    protected TimeseriesService tsService;

    @Autowired
    protected EventService eventService;

    @Autowired
    protected RelationService relationService;

    @Autowired
    protected AlarmService alarmService;

    @Autowired
    protected RuleChainService ruleChainService;

    @Autowired
    private ComponentDescriptorService componentDescriptorService;

    class IdComparator<D extends BaseData<? extends UUIDBased>> implements Comparator<D> {
        @Override
        public int compare(D o1, D o2) {
            return o1.getId().getId().compareTo(o2.getId().getId());
        }
    }


    protected Event generateEvent(TenantId tenantId, EntityId entityId, String eventType, String eventUid) throws IOException {
        if (tenantId == null) {
            tenantId = new TenantId(UUIDs.timeBased());
        }
        Event event = new Event();
        event.setTenantId(tenantId);
        event.setEntityId(entityId);
        event.setType(eventType);
        event.setUid(eventUid);
        event.setBody(readFromResource("TestJsonData.json"));
        return event;
    }
//
//    private ComponentDescriptor getOrCreateDescriptor(ComponentScope scope, ComponentType type, String clazz, String configurationDescriptorResource) throws IOException {
//        return getOrCreateDescriptor(scope, type, clazz, configurationDescriptorResource, null);
//    }
//
//    private ComponentDescriptor getOrCreateDescriptor(ComponentScope scope, ComponentType type, String clazz, String configurationDescriptorResource, String actions) throws IOException {
//        ComponentDescriptor descriptor = componentDescriptorService.findByClazz(clazz);
//        if (descriptor == null) {
//            descriptor = new ComponentDescriptor();
//            descriptor.setName("test");
//            descriptor.setClazz(clazz);
//            descriptor.setScope(scope);
//            descriptor.setType(type);
//            descriptor.setActions(actions);
//            descriptor.setConfigurationDescriptor(readFromResource(configurationDescriptorResource));
//            componentDescriptorService.saveComponent(descriptor);
//        }
//        return descriptor;
//    }

    public JsonNode readFromResource(String resourceName) throws IOException {
        return mapper.readTree(this.getClass().getClassLoader().getResourceAsStream(resourceName));
    }

    @Bean
    public AuditLogLevelFilter auditLogLevelFilter() {
        Map<String,String> mask = new HashMap<>();
        for (EntityType entityType : EntityType.values()) {
            mask.put(entityType.name().toLowerCase(), AuditLogLevelMask.RW.name());
        }
        return new AuditLogLevelFilter(mask);
    }

}
