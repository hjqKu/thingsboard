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
package com.loit.service.rpc;

import com.loit.common.data.id.DeviceId;
import com.loit.common.data.id.TenantId;
import com.loit.common.msg.cluster.ServerAddress;
import com.loit.common.msg.rpc.ToDeviceRpcRequest;

import java.util.function.Consumer;

/**
 * Created by ashvayka on 16.04.18.
 */
public interface DeviceRpcService {

    void processRestAPIRpcRequestToRuleEngine(ToDeviceRpcRequest request, Consumer<FromDeviceRpcResponse> responseConsumer);

    void processResponseToServerSideRPCRequestFromRuleEngine(ServerAddress requestOriginAddress, FromDeviceRpcResponse response);

    void forwardServerSideRPCRequestToDeviceActor(ToDeviceRpcRequest request, Consumer<FromDeviceRpcResponse> responseConsumer);

    void processResponseToServerSideRPCRequestFromDeviceActor(FromDeviceRpcResponse response);

    void processResponseToServerSideRPCRequestFromRemoteServer(ServerAddress serverAddress, byte[] data);

    void sendReplyToRpcCallFromDevice(TenantId tenantId, DeviceId deviceId, int requestId, String body);
}
