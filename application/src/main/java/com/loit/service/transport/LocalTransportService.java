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
package com.loit.service.transport;

import akka.actor.ActorRef;
import com.loit.common.data.id.DeviceId;
import com.loit.common.data.id.TenantId;
import com.loit.common.msg.cluster.ServerAddress;
import com.loit.dao.device.ClaimDevicesService;
import com.loit.service.encoding.DataDecodingEncodingService;
import com.loit.service.transport.msg.TransportToDeviceActorMsgWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.DonAsynchron;
import com.loit.actors.ActorSystemContext;
import com.loit.common.data.id.DeviceId;
import com.loit.common.data.id.TenantId;
import com.loit.common.msg.cluster.ServerAddress;
import com.loit.common.transport.TransportServiceCallback;
import com.loit.common.transport.service.AbstractTransportService;
import com.loit.dao.device.ClaimDevicesService;
import com.loit.service.cluster.routing.ClusterRoutingService;
import com.loit.service.cluster.rpc.ClusterRpcService;
import com.loit.service.encoding.DataDecodingEncodingService;
import com.loit.service.transport.msg.TransportToDeviceActorMsgWrapper;
import org.thingsboard.server.gen.transport.TransportProtos;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Created by ashvayka on 12.10.18.
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "transport", value = "type", havingValue = "local")
public class LocalTransportService extends AbstractTransportService implements RuleEngineTransportService {

    @Autowired
    private TransportApiService transportApiService;

    @Autowired
    private ActorSystemContext actorContext;

    //TODO: completely replace this routing with the Kafka routing by partition ids.
    @Autowired
    private ClusterRoutingService routingService;
    @Autowired
    private ClusterRpcService rpcService;
    @Autowired
    private DataDecodingEncodingService encodingService;
    @Autowired
    private ClaimDevicesService claimDevicesService;

    @PostConstruct
    public void init() {
        super.init();
    }

    @PreDestroy
    public void destroy() {
        super.destroy();
    }

    @Override
    public void process(TransportProtos.ValidateDeviceTokenRequestMsg msg, TransportServiceCallback<TransportProtos.ValidateDeviceCredentialsResponseMsg> callback) {
        DonAsynchron.withCallback(
                transportApiService.handle(TransportProtos.TransportApiRequestMsg.newBuilder().setValidateTokenRequestMsg(msg).build()),
                transportApiResponseMsg -> {
                    if (callback != null) {
                        callback.onSuccess(transportApiResponseMsg.getValidateTokenResponseMsg());
                    }
                },
                getThrowableConsumer(callback), transportCallbackExecutor);
    }

    @Override
    public void process(TransportProtos.ValidateDeviceX509CertRequestMsg msg, TransportServiceCallback<TransportProtos.ValidateDeviceCredentialsResponseMsg> callback) {
        DonAsynchron.withCallback(
                transportApiService.handle(TransportProtos.TransportApiRequestMsg.newBuilder().setValidateX509CertRequestMsg(msg).build()),
                transportApiResponseMsg -> {
                    if (callback != null) {
                        callback.onSuccess(transportApiResponseMsg.getValidateTokenResponseMsg());
                    }
                },
                getThrowableConsumer(callback), transportCallbackExecutor);
    }

    @Override
    public void process(TransportProtos.GetOrCreateDeviceFromGatewayRequestMsg msg, TransportServiceCallback<TransportProtos.GetOrCreateDeviceFromGatewayResponseMsg> callback) {
        DonAsynchron.withCallback(
                transportApiService.handle(TransportProtos.TransportApiRequestMsg.newBuilder().setGetOrCreateDeviceRequestMsg(msg).build()),
                transportApiResponseMsg -> {
                    if (callback != null) {
                        callback.onSuccess(transportApiResponseMsg.getGetOrCreateDeviceResponseMsg());
                    }
                },
                getThrowableConsumer(callback), transportCallbackExecutor);
    }

    @Override
    protected void doProcess(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.SessionEventMsg msg, TransportServiceCallback<Void> callback) {
        forwardToDeviceActor(TransportProtos.TransportToDeviceActorMsg.newBuilder().setSessionInfo(sessionInfo).setSessionEvent(msg).build(), callback);
    }

    @Override
    protected void doProcess(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.PostTelemetryMsg msg, TransportServiceCallback<Void> callback) {
        forwardToDeviceActor(TransportProtos.TransportToDeviceActorMsg.newBuilder().setSessionInfo(sessionInfo).setPostTelemetry(msg).build(), callback);
    }

    @Override
    protected void doProcess(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.PostAttributeMsg msg, TransportServiceCallback<Void> callback) {
        forwardToDeviceActor(TransportProtos.TransportToDeviceActorMsg.newBuilder().setSessionInfo(sessionInfo).setPostAttributes(msg).build(), callback);
    }

    @Override
    protected void doProcess(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.GetAttributeRequestMsg msg, TransportServiceCallback<Void> callback) {
        forwardToDeviceActor(TransportProtos.TransportToDeviceActorMsg.newBuilder().setSessionInfo(sessionInfo).setGetAttributes(msg).build(), callback);
    }

    @Override
    public void process(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.SubscriptionInfoProto msg, TransportServiceCallback<Void> callback) {
        forwardToDeviceActor(TransportProtos.TransportToDeviceActorMsg.newBuilder().setSessionInfo(sessionInfo).setSubscriptionInfo(msg).build(), callback);
    }

    @Override
    protected void doProcess(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.SubscribeToAttributeUpdatesMsg msg, TransportServiceCallback<Void> callback) {
        forwardToDeviceActor(TransportProtos.TransportToDeviceActorMsg.newBuilder().setSessionInfo(sessionInfo).setSubscribeToAttributes(msg).build(), callback);
    }

    @Override
    protected void doProcess(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.SubscribeToRPCMsg msg, TransportServiceCallback<Void> callback) {
        forwardToDeviceActor(TransportProtos.TransportToDeviceActorMsg.newBuilder().setSessionInfo(sessionInfo).setSubscribeToRPC(msg).build(), callback);
    }

    @Override
    protected void doProcess(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.ToDeviceRpcResponseMsg msg, TransportServiceCallback<Void> callback) {
        forwardToDeviceActor(TransportProtos.TransportToDeviceActorMsg.newBuilder().setSessionInfo(sessionInfo).setToDeviceRPCCallResponse(msg).build(), callback);
    }

    @Override
    protected void doProcess(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.ToServerRpcRequestMsg msg, TransportServiceCallback<Void> callback) {
        forwardToDeviceActor(TransportProtos.TransportToDeviceActorMsg.newBuilder().setSessionInfo(sessionInfo).setToServerRPCCallRequest(msg).build(), callback);
    }

    @Override
    protected void registerClaimingInfo(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.ClaimDeviceMsg msg, TransportServiceCallback<Void> callback) {
        TransportProtos.TransportToDeviceActorMsg toDeviceActorMsg = TransportProtos.TransportToDeviceActorMsg.newBuilder().setSessionInfo(sessionInfo).setClaimDevice(msg).build();

        TransportToDeviceActorMsgWrapper wrapper = new TransportToDeviceActorMsgWrapper(toDeviceActorMsg);
        Optional<ServerAddress> address = routingService.resolveById(wrapper.getDeviceId());
        if (address.isPresent()) {
            rpcService.tell(encodingService.convertToProtoDataMessage(address.get(), wrapper));
            callback.onSuccess(null);
        } else {
            TenantId tenantId = new TenantId(new UUID(sessionInfo.getTenantIdMSB(), sessionInfo.getTenantIdLSB()));
            DeviceId deviceId = new DeviceId(new UUID(msg.getDeviceIdMSB(), msg.getDeviceIdLSB()));
            DonAsynchron.withCallback(claimDevicesService.registerClaimingInfo(tenantId, deviceId, msg.getSecretKey(), msg.getDurationMs()),
                    callback::onSuccess, callback::onError);
        }
    }

    @Override
    public void process(String nodeId, TransportProtos.DeviceActorToTransportMsg msg) {
        process(nodeId, msg, null, null);
    }

    @Override
    public void process(String nodeId, TransportProtos.DeviceActorToTransportMsg msg, Runnable onSuccess, Consumer<Throwable> onFailure) {
        processToTransportMsg(msg);
        if (onSuccess != null) {
            onSuccess.run();
        }
    }

    private void forwardToDeviceActor(TransportProtos.TransportToDeviceActorMsg toDeviceActorMsg, TransportServiceCallback<Void> callback) {
        TransportToDeviceActorMsgWrapper wrapper = new TransportToDeviceActorMsgWrapper(toDeviceActorMsg);
        Optional<ServerAddress> address = routingService.resolveById(wrapper.getDeviceId());
        if (address.isPresent()) {
            rpcService.tell(encodingService.convertToProtoDataMessage(address.get(), wrapper));
        } else {
            actorContext.getAppActor().tell(wrapper, ActorRef.noSender());
        }
        if (callback != null) {
            callback.onSuccess(null);
        }
    }

    private <T> Consumer<Throwable> getThrowableConsumer(TransportServiceCallback<T> callback) {
        return e -> {
            if (callback != null) {
                callback.onError(e);
            }
        };
    }

}
