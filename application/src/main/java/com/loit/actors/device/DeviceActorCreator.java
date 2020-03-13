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
package com.loit.actors.device;

import com.loit.common.data.id.DeviceId;
import com.loit.common.data.id.TenantId;
import com.loit.actors.ActorSystemContext;
import com.loit.actors.service.ContextBasedCreator;
import com.loit.common.data.id.DeviceId;
import com.loit.common.data.id.TenantId;

public class DeviceActorCreator extends ContextBasedCreator<DeviceActor> {
    private static final long serialVersionUID = 1L;

    private final TenantId tenantId;
    private final DeviceId deviceId;

    public DeviceActorCreator(ActorSystemContext context, TenantId tenantId, DeviceId deviceId) {
        super(context);
        this.tenantId = tenantId;
        this.deviceId = deviceId;
    }

    @Override
    public DeviceActor create() {
        return new DeviceActor(context, tenantId, deviceId);
    }
}
