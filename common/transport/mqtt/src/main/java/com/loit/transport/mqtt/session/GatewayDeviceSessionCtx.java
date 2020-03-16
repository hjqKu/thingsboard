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
package com.loit.transport.mqtt.session;

import com.loit.common.transport.SessionMsgListener;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by ashvayka on 19.01.17.
 */
@Slf4j
public class GatewayDeviceSessionCtx extends MqttDeviceAwareSessionContext implements SessionMsgListener {

    private final GatewaySessionHandler parent;
    private final TransportProtos.SessionInfoProto sessionInfo;

    public GatewayDeviceSessionCtx(GatewaySessionHandler parent, TransportProtos.DeviceInfoProto deviceInfo, ConcurrentMap<MqttTopicMatcher, Integer> mqttQoSMap) {
        super(UUID.randomUUID(), mqttQoSMap);
        this.parent = parent;
        this.sessionInfo = TransportProtos.SessionInfoProto.newBuilder()
                .setNodeId(parent.getNodeId())
                .setSessionIdMSB(sessionId.getMostSignificantBits())
                .setSessionIdLSB(sessionId.getLeastSignificantBits())
                .setDeviceIdMSB(deviceInfo.getDeviceIdMSB())
                .setDeviceIdLSB(deviceInfo.getDeviceIdLSB())
                .setTenantIdMSB(deviceInfo.getTenantIdMSB())
                .setTenantIdLSB(deviceInfo.getTenantIdLSB())
                .build();
        setDeviceInfo(deviceInfo);
    }

    @Override
    public UUID getSessionId() {
        return sessionId;
    }

    @Override
    public int nextMsgId() {
        return parent.nextMsgId();
    }

    TransportProtos.SessionInfoProto getSessionInfo() {
        return sessionInfo;
    }

    @Override
    public void onGetAttributesResponse(TransportProtos.GetAttributeResponseMsg response) {
        try {
            parent.getAdaptor().convertToGatewayPublish(this, getDeviceInfo().getDeviceName(), response).ifPresent(parent::writeAndFlush);
        } catch (Exception e) {
            log.trace("[{}] Failed to convert device attributes response to MQTT msg", sessionId, e);
        }
    }

    @Override
    public void onAttributeUpdate(TransportProtos.AttributeUpdateNotificationMsg notification) {
        try {
            parent.getAdaptor().convertToGatewayPublish(this, getDeviceInfo().getDeviceName(), notification).ifPresent(parent::writeAndFlush);
        } catch (Exception e) {
            log.trace("[{}] Failed to convert device attributes response to MQTT msg", sessionId, e);
        }
    }

    @Override
    public void onRemoteSessionCloseCommand(TransportProtos.SessionCloseNotificationProto sessionCloseNotification) {
        parent.deregisterSession(getDeviceInfo().getDeviceName());
    }

    @Override
    public void onToDeviceRpcRequest(TransportProtos.ToDeviceRpcRequestMsg request) {
        try {
            parent.getAdaptor().convertToGatewayPublish(this, getDeviceInfo().getDeviceName(), request).ifPresent(parent::writeAndFlush);
        } catch (Exception e) {
            log.trace("[{}] Failed to convert device attributes response to MQTT msg", sessionId, e);
        }
    }

    @Override
    public void onToServerRpcResponse(TransportProtos.ToServerRpcResponseMsg toServerResponse) {
        // This feature is not supported in the TB IoT Gateway yet.
    }
}
