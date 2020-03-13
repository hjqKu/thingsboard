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
package com.loit.service.security.permission;

import com.loit.service.security.model.SecurityUser;
import com.loit.common.data.HasCustomerId;
import com.loit.common.data.HasTenantId;
import com.loit.common.data.exception.ThingsboardException;
import com.loit.common.data.id.EntityId;
import com.loit.common.data.id.TenantId;
import com.loit.service.security.model.SecurityUser;

public interface AccessControlService {

    void checkPermission(SecurityUser user, Resource resource, Operation operation) throws ThingsboardException;

    <I extends EntityId, T extends HasTenantId> void checkPermission(SecurityUser user, Resource resource, Operation operation, I entityId, T entity) throws ThingsboardException;

}
