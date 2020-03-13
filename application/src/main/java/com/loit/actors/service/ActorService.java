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
package com.loit.actors.service;

import com.loit.common.data.id.DeviceId;
import com.loit.common.data.id.EntityId;
import com.loit.common.data.id.TenantId;
import com.loit.common.data.plugin.ComponentLifecycleEvent;
import com.loit.common.msg.cluster.SendToClusterMsg;
import com.loit.common.transport.SessionMsgProcessor;
import com.loit.service.cluster.discovery.DiscoveryServiceListener;
import com.loit.service.cluster.rpc.RpcMsgListener;

public interface ActorService extends SessionMsgProcessor, RpcMsgListener, DiscoveryServiceListener {

    void onEntityStateChange(TenantId tenantId, EntityId entityId, ComponentLifecycleEvent state);

    void onMsg(SendToClusterMsg msg);

    void onCredentialsUpdate(TenantId tenantId, DeviceId deviceId);

    void onDeviceNameOrTypeUpdate(TenantId tenantId, DeviceId deviceId, String deviceName, String deviceType);

}
