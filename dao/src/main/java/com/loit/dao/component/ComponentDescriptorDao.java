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
package com.loit.dao.component;

import com.loit.common.data.id.ComponentDescriptorId;
import com.loit.common.data.id.TenantId;
import com.loit.common.data.page.TextPageLink;
import com.loit.common.data.plugin.ComponentDescriptor;
import com.loit.common.data.plugin.ComponentScope;
import com.loit.common.data.plugin.ComponentType;
import com.loit.dao.Dao;

import java.util.List;
import java.util.Optional;

/**
 * @author Andrew Shvayka
 */
public interface ComponentDescriptorDao extends Dao<ComponentDescriptor> {

    Optional<ComponentDescriptor> saveIfNotExist(TenantId tenantId, ComponentDescriptor component);

    ComponentDescriptor findById(TenantId tenantId, ComponentDescriptorId componentId);

    ComponentDescriptor findByClazz(TenantId tenantId, String clazz);

    List<ComponentDescriptor> findByTypeAndPageLink(TenantId tenantId, ComponentType type, TextPageLink pageLink);

    List<ComponentDescriptor> findByScopeAndTypeAndPageLink(TenantId tenantId, ComponentScope scope, ComponentType type, TextPageLink pageLink);

    void deleteById(TenantId tenantId, ComponentDescriptorId componentId);

    void deleteByClazz(TenantId tenantId, String clazz);

}
