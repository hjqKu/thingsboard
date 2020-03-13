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
package com.loit.common.transport;


import org.thingsboard.server.gen.transport.TransportProtos;

/**
 * Created by ashvayka on 04.10.18.
 */
public interface TransportService {

    void process(TransportProtos.ValidateDeviceTokenRequestMsg msg,
                 TransportServiceCallback<TransportProtos.ValidateDeviceCredentialsResponseMsg> callback);

    void process(TransportProtos.ValidateDeviceX509CertRequestMsg msg,
                 TransportServiceCallback<TransportProtos.ValidateDeviceCredentialsResponseMsg> callback);

    void process(TransportProtos.GetOrCreateDeviceFromGatewayRequestMsg msg,
                 TransportServiceCallback<TransportProtos.GetOrCreateDeviceFromGatewayResponseMsg> callback);

    boolean checkLimits(TransportProtos.SessionInfoProto sessionInfo, Object msg, TransportServiceCallback<Void> callback);

    void process(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.SessionEventMsg msg, TransportServiceCallback<Void> callback);

    void process(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.PostTelemetryMsg msg, TransportServiceCallback<Void> callback);

    void process(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.PostAttributeMsg msg, TransportServiceCallback<Void> callback);

    void process(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.GetAttributeRequestMsg msg, TransportServiceCallback<Void> callback);

    void process(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.SubscribeToAttributeUpdatesMsg msg, TransportServiceCallback<Void> callback);

    void process(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.SubscribeToRPCMsg msg, TransportServiceCallback<Void> callback);

    void process(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.ToDeviceRpcResponseMsg msg, TransportServiceCallback<Void> callback);

    void process(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.ToServerRpcRequestMsg msg, TransportServiceCallback<Void> callback);

    void process(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.SubscriptionInfoProto msg, TransportServiceCallback<Void> callback);

    void process(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.ClaimDeviceMsg msg, TransportServiceCallback<Void> callback);

    void registerAsyncSession(TransportProtos.SessionInfoProto sessionInfo, SessionMsgListener listener);

    void registerSyncSession(TransportProtos.SessionInfoProto sessionInfo, SessionMsgListener listener, long timeout);

    void reportActivity(TransportProtos.SessionInfoProto sessionInfo);

    void deregisterSession(TransportProtos.SessionInfoProto sessionInfo);

}
