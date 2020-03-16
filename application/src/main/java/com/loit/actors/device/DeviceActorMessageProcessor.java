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

import akka.actor.ActorContext;
import com.datastax.driver.core.utils.UUIDs;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.protobuf.InvalidProtocolBufferException;
import com.loit.common.data.DataConstants;
import com.loit.common.data.Device;
import com.loit.common.data.id.DeviceId;
import com.loit.common.data.id.TenantId;
import com.loit.common.data.kv.AttributeKey;
import com.loit.common.data.kv.AttributeKvEntry;
import com.loit.common.data.kv.KvEntry;
import com.loit.common.data.rpc.ToDeviceRpcRequestBody;
import com.loit.common.msg.TbMsg;
import com.loit.common.msg.TbMsgDataType;
import com.loit.common.msg.TbMsgMetaData;
import com.loit.common.msg.rpc.ToDeviceRpcRequest;
import com.loit.common.msg.session.SessionMsgType;
import com.loit.common.msg.timeout.DeviceActorClientSideRpcTimeoutMsg;
import com.loit.common.msg.timeout.DeviceActorServerSideRpcTimeoutMsg;
import com.loit.service.rpc.FromDeviceRpcResponse;
import com.loit.service.rpc.ToDeviceRpcRequestActorMsg;
import com.loit.service.rpc.ToServerRpcResponseActorMsg;
import com.loit.service.transport.msg.TransportToDeviceActorMsgWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.thingsboard.rule.engine.api.RpcError;
import org.thingsboard.rule.engine.api.msg.DeviceAttributesEventNotificationMsg;
import org.thingsboard.rule.engine.api.msg.DeviceNameOrTypeUpdateMsg;
import com.loit.actors.ActorSystemContext;
import com.loit.actors.shared.AbstractContextAwareMsgProcessor;
import org.thingsboard.server.gen.transport.TransportProtos;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author Andrew Shvayka
 */
@Slf4j
class DeviceActorMessageProcessor extends AbstractContextAwareMsgProcessor {

    final TenantId tenantId;
    final DeviceId deviceId;
    private final Map<UUID, SessionInfoMetaData> sessions;
    private final Map<UUID, SessionInfo> attributeSubscriptions;
    private final Map<UUID, SessionInfo> rpcSubscriptions;
    private final Map<Integer, ToDeviceRpcRequestMetadata> toDeviceRpcPendingMap;
    private final Map<Integer, ToServerRpcRequestMetadata> toServerRpcPendingMap;

    private final Gson gson = new Gson();
    private final JsonParser jsonParser = new JsonParser();

    private int rpcSeq = 0;
    private String deviceName;
    private String deviceType;
    private TbMsgMetaData defaultMetaData;

    DeviceActorMessageProcessor(ActorSystemContext systemContext, TenantId tenantId, DeviceId deviceId) {
        super(systemContext);
        this.tenantId = tenantId;
        this.deviceId = deviceId;
        this.sessions = new LinkedHashMap<>();
        this.attributeSubscriptions = new HashMap<>();
        this.rpcSubscriptions = new HashMap<>();
        this.toDeviceRpcPendingMap = new HashMap<>();
        this.toServerRpcPendingMap = new HashMap<>();
        if (initAttributes()) {
            restoreSessions();
        }
    }

    private boolean initAttributes() {
        Device device = systemContext.getDeviceService().findDeviceById(tenantId, deviceId);
        if (device != null) {
            this.deviceName = device.getName();
            this.deviceType = device.getType();
            this.defaultMetaData = new TbMsgMetaData();
            this.defaultMetaData.putValue("deviceName", deviceName);
            this.defaultMetaData.putValue("deviceType", deviceType);
            return true;
        } else {
            return false;
        }
    }

    void processRpcRequest(ActorContext context, ToDeviceRpcRequestActorMsg msg) {
        ToDeviceRpcRequest request = msg.getMsg();
        ToDeviceRpcRequestBody body = request.getBody();
        TransportProtos.ToDeviceRpcRequestMsg rpcRequest = TransportProtos.ToDeviceRpcRequestMsg.newBuilder().setRequestId(
                rpcSeq++).setMethodName(body.getMethod()).setParams(body.getParams()).build();

        long timeout = request.getExpirationTime() - System.currentTimeMillis();
        if (timeout <= 0) {
            log.debug("[{}][{}] Ignoring message due to exp time reached, {}", deviceId, request.getId(), request.getExpirationTime());
            return;
        }

        boolean sent = rpcSubscriptions.size() > 0;
        Set<UUID> syncSessionSet = new HashSet<>();
        rpcSubscriptions.forEach((key, value) -> {
            sendToTransport(rpcRequest, key, value.getNodeId());
            if (TransportProtos.SessionType.SYNC == value.getType()) {
                syncSessionSet.add(key);
            }
        });
        syncSessionSet.forEach(rpcSubscriptions::remove);

        if (request.isOneway() && sent) {
            log.debug("[{}] Rpc command response sent [{}]!", deviceId, request.getId());
            systemContext.getDeviceRpcService().processResponseToServerSideRPCRequestFromDeviceActor(new FromDeviceRpcResponse(msg.getMsg().getId(), null, null));
        } else {
            registerPendingRpcRequest(context, msg, sent, rpcRequest, timeout);
        }
        if (sent) {
            log.debug("[{}] RPC request {} is sent!", deviceId, request.getId());
        } else {
            log.debug("[{}] RPC request {} is NOT sent!", deviceId, request.getId());
        }
    }

    private void registerPendingRpcRequest(ActorContext context, ToDeviceRpcRequestActorMsg msg, boolean sent, TransportProtos.ToDeviceRpcRequestMsg rpcRequest, long timeout) {
        toDeviceRpcPendingMap.put(rpcRequest.getRequestId(), new ToDeviceRpcRequestMetadata(msg, sent));
        DeviceActorServerSideRpcTimeoutMsg timeoutMsg = new DeviceActorServerSideRpcTimeoutMsg(rpcRequest.getRequestId(), timeout);
        scheduleMsgWithDelay(context, timeoutMsg, timeoutMsg.getTimeout());
    }

    void processServerSideRpcTimeout(ActorContext context, DeviceActorServerSideRpcTimeoutMsg msg) {
        ToDeviceRpcRequestMetadata requestMd = toDeviceRpcPendingMap.remove(msg.getId());
        if (requestMd != null) {
            log.debug("[{}] RPC request [{}] timeout detected!", deviceId, msg.getId());
            systemContext.getDeviceRpcService().processResponseToServerSideRPCRequestFromDeviceActor(new FromDeviceRpcResponse(requestMd.getMsg().getMsg().getId(),
                    null, requestMd.isSent() ? RpcError.TIMEOUT : RpcError.NO_ACTIVE_CONNECTION));
        }
    }

    private void sendPendingRequests(ActorContext context, UUID sessionId, TransportProtos.SessionInfoProto sessionInfo) {
        TransportProtos.SessionType sessionType = getSessionType(sessionId);
        if (!toDeviceRpcPendingMap.isEmpty()) {
            log.debug("[{}] Pushing {} pending RPC messages to new async session [{}]", deviceId, toDeviceRpcPendingMap.size(), sessionId);
            if (sessionType == TransportProtos.SessionType.SYNC) {
                log.debug("[{}] Cleanup sync rpc session [{}]", deviceId, sessionId);
                rpcSubscriptions.remove(sessionId);
            }
        } else {
            log.debug("[{}] No pending RPC messages for new async session [{}]", deviceId, sessionId);
        }
        Set<Integer> sentOneWayIds = new HashSet<>();
        if (sessionType == TransportProtos.SessionType.ASYNC) {
            toDeviceRpcPendingMap.entrySet().forEach(processPendingRpc(context, sessionId, sessionInfo.getNodeId(), sentOneWayIds));
        } else {
            toDeviceRpcPendingMap.entrySet().stream().findFirst().ifPresent(processPendingRpc(context, sessionId, sessionInfo.getNodeId(), sentOneWayIds));
        }

        sentOneWayIds.forEach(toDeviceRpcPendingMap::remove);
    }

    private Consumer<Map.Entry<Integer, ToDeviceRpcRequestMetadata>> processPendingRpc(ActorContext context, UUID sessionId, String nodeId, Set<Integer> sentOneWayIds) {
        return entry -> {
            ToDeviceRpcRequest request = entry.getValue().getMsg().getMsg();
            ToDeviceRpcRequestBody body = request.getBody();
            if (request.isOneway()) {
                sentOneWayIds.add(entry.getKey());
                systemContext.getDeviceRpcService().processResponseToServerSideRPCRequestFromDeviceActor(new FromDeviceRpcResponse(request.getId(), null, null));
            }
            TransportProtos.ToDeviceRpcRequestMsg rpcRequest = TransportProtos.ToDeviceRpcRequestMsg.newBuilder().setRequestId(
                    entry.getKey()).setMethodName(body.getMethod()).setParams(body.getParams()).build();
            sendToTransport(rpcRequest, sessionId, nodeId);
        };
    }

    void process(ActorContext context, TransportToDeviceActorMsgWrapper wrapper) {
        boolean reportDeviceActivity = false;
        TransportProtos.TransportToDeviceActorMsg msg = wrapper.getMsg();
        if (msg.hasSessionEvent()) {
            processSessionStateMsgs(msg.getSessionInfo(), msg.getSessionEvent());
        }
        if (msg.hasSubscribeToAttributes()) {
            processSubscriptionCommands(context, msg.getSessionInfo(), msg.getSubscribeToAttributes());
        }
        if (msg.hasSubscribeToRPC()) {
            processSubscriptionCommands(context, msg.getSessionInfo(), msg.getSubscribeToRPC());
        }
        if (msg.hasPostAttributes()) {
            handlePostAttributesRequest(context, msg.getSessionInfo(), msg.getPostAttributes());
            reportDeviceActivity = true;
        }
        if (msg.hasPostTelemetry()) {
            handlePostTelemetryRequest(context, msg.getSessionInfo(), msg.getPostTelemetry());
            reportDeviceActivity = true;
        }
        if (msg.hasGetAttributes()) {
            handleGetAttributesRequest(context, msg.getSessionInfo(), msg.getGetAttributes());
        }
        if (msg.hasToDeviceRPCCallResponse()) {
            processRpcResponses(context, msg.getSessionInfo(), msg.getToDeviceRPCCallResponse());
        }
        if (msg.hasToServerRPCCallRequest()) {
            handleClientSideRPCRequest(context, msg.getSessionInfo(), msg.getToServerRPCCallRequest());
            reportDeviceActivity = true;
        }
        if (msg.hasSubscriptionInfo()) {
            handleSessionActivity(context, msg.getSessionInfo(), msg.getSubscriptionInfo());
        }
        if (reportDeviceActivity) {
            reportLogicalDeviceActivity();
        }
    }

    private void reportLogicalDeviceActivity() {
        systemContext.getDeviceStateService().onDeviceActivity(deviceId);
    }

    private void reportSessionOpen() {
        systemContext.getDeviceStateService().onDeviceConnect(deviceId);
    }

    private void reportSessionClose() {
        systemContext.getDeviceStateService().onDeviceDisconnect(deviceId);
    }

    private void handleGetAttributesRequest(ActorContext context, TransportProtos.SessionInfoProto sessionInfo, TransportProtos.GetAttributeRequestMsg request) {
        int requestId = request.getRequestId();
        Futures.addCallback(getAttributesKvEntries(request), new FutureCallback<List<List<AttributeKvEntry>>>() {
            @Override
            public void onSuccess(@Nullable List<List<AttributeKvEntry>> result) {
                TransportProtos.GetAttributeResponseMsg responseMsg = TransportProtos.GetAttributeResponseMsg.newBuilder()
                        .setRequestId(requestId)
                        .addAllClientAttributeList(toTsKvProtos(result.get(0)))
                        .addAllSharedAttributeList(toTsKvProtos(result.get(1)))
                        .build();
                sendToTransport(responseMsg, sessionInfo);
            }

            @Override
            public void onFailure(Throwable t) {
                TransportProtos.GetAttributeResponseMsg responseMsg = TransportProtos.GetAttributeResponseMsg.newBuilder()
                        .setError(t.getMessage())
                        .build();
                sendToTransport(responseMsg, sessionInfo);
            }
        });
    }

    private ListenableFuture<List<List<AttributeKvEntry>>> getAttributesKvEntries(TransportProtos.GetAttributeRequestMsg request) {
        ListenableFuture<List<AttributeKvEntry>> clientAttributesFuture;
        ListenableFuture<List<AttributeKvEntry>> sharedAttributesFuture;
        if (CollectionUtils.isEmpty(request.getClientAttributeNamesList()) && CollectionUtils.isEmpty(request.getSharedAttributeNamesList())) {
            clientAttributesFuture = findAllAttributesByScope(DataConstants.CLIENT_SCOPE);
            sharedAttributesFuture = findAllAttributesByScope(DataConstants.SHARED_SCOPE);
        } else if (!CollectionUtils.isEmpty(request.getClientAttributeNamesList()) && !CollectionUtils.isEmpty(request.getSharedAttributeNamesList())) {
            clientAttributesFuture = findAttributesByScope(toSet(request.getClientAttributeNamesList()), DataConstants.CLIENT_SCOPE);
            sharedAttributesFuture = findAttributesByScope(toSet(request.getSharedAttributeNamesList()), DataConstants.SHARED_SCOPE);
        } else if (CollectionUtils.isEmpty(request.getClientAttributeNamesList()) && !CollectionUtils.isEmpty(request.getSharedAttributeNamesList())) {
            clientAttributesFuture = Futures.immediateFuture(Collections.emptyList());
            sharedAttributesFuture = findAttributesByScope(toSet(request.getSharedAttributeNamesList()), DataConstants.SHARED_SCOPE);
        } else {
            sharedAttributesFuture = Futures.immediateFuture(Collections.emptyList());
            clientAttributesFuture = findAttributesByScope(toSet(request.getClientAttributeNamesList()), DataConstants.CLIENT_SCOPE);
        }
        return Futures.allAsList(Arrays.asList(clientAttributesFuture, sharedAttributesFuture));
    }

    private ListenableFuture<List<AttributeKvEntry>> findAllAttributesByScope(String scope) {
        return systemContext.getAttributesService().findAll(tenantId, deviceId, scope);
    }

    private ListenableFuture<List<AttributeKvEntry>> findAttributesByScope(Set<String> attributesSet, String scope) {
        return systemContext.getAttributesService().find(tenantId, deviceId, scope, attributesSet);
    }

    private Set<String> toSet(List<String> strings) {
        return new HashSet<>(strings);
    }

    private void handlePostAttributesRequest(ActorContext context, TransportProtos.SessionInfoProto sessionInfo, TransportProtos.PostAttributeMsg postAttributes) {
        JsonObject json = getJsonObject(postAttributes.getKvList());
        TbMsg tbMsg = new TbMsg(UUIDs.timeBased(), SessionMsgType.POST_ATTRIBUTES_REQUEST.name(), deviceId, defaultMetaData.copy(),
                TbMsgDataType.JSON, gson.toJson(json), null, null, 0L);
        pushToRuleEngine(context, tbMsg);
    }

    private void handlePostTelemetryRequest(ActorContext context, TransportProtos.SessionInfoProto sessionInfo, TransportProtos.PostTelemetryMsg postTelemetry) {
        for (TransportProtos.TsKvListProto tsKv : postTelemetry.getTsKvListList()) {
            JsonObject json = getJsonObject(tsKv.getKvList());
            TbMsgMetaData metaData = defaultMetaData.copy();
            metaData.putValue("ts", tsKv.getTs() + "");
            TbMsg tbMsg = new TbMsg(UUIDs.timeBased(), SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, metaData, TbMsgDataType.JSON, gson.toJson(json), null, null, 0L);
            pushToRuleEngine(context, tbMsg);
        }
    }

    private void handleClientSideRPCRequest(ActorContext context, TransportProtos.SessionInfoProto sessionInfo, TransportProtos.ToServerRpcRequestMsg request) {
        UUID sessionId = getSessionId(sessionInfo);
        JsonObject json = new JsonObject();
        json.addProperty("method", request.getMethodName());
        json.add("params", jsonParser.parse(request.getParams()));

        TbMsgMetaData requestMetaData = defaultMetaData.copy();
        requestMetaData.putValue("requestId", Integer.toString(request.getRequestId()));
        TbMsg tbMsg = new TbMsg(UUIDs.timeBased(), SessionMsgType.TO_SERVER_RPC_REQUEST.name(), deviceId, requestMetaData, TbMsgDataType.JSON, gson.toJson(json), null, null, 0L);
        context.parent().tell(new DeviceActorToRuleEngineMsg(context.self(), tbMsg), context.self());

        scheduleMsgWithDelay(context, new DeviceActorClientSideRpcTimeoutMsg(request.getRequestId(), systemContext.getClientSideRpcTimeout()), systemContext.getClientSideRpcTimeout());
        toServerRpcPendingMap.put(request.getRequestId(), new ToServerRpcRequestMetadata(sessionId, getSessionType(sessionId), sessionInfo.getNodeId()));
    }

    private TransportProtos.SessionType getSessionType(UUID sessionId) {
        return sessions.containsKey(sessionId) ? TransportProtos.SessionType.ASYNC : TransportProtos.SessionType.SYNC;
    }

    void processClientSideRpcTimeout(ActorContext context, DeviceActorClientSideRpcTimeoutMsg msg) {
        ToServerRpcRequestMetadata data = toServerRpcPendingMap.remove(msg.getId());
        if (data != null) {
            log.debug("[{}] Client side RPC request [{}] timeout detected!", deviceId, msg.getId());
            sendToTransport(TransportProtos.ToServerRpcResponseMsg.newBuilder()
                            .setRequestId(msg.getId()).setError("timeout").build()
                    , data.getSessionId(), data.getNodeId());
        }
    }

    void processToServerRPCResponse(ActorContext context, ToServerRpcResponseActorMsg msg) {
        int requestId = msg.getMsg().getRequestId();
        ToServerRpcRequestMetadata data = toServerRpcPendingMap.remove(requestId);
        if (data != null) {
            log.debug("[{}] Pushing reply to [{}][{}]!", deviceId, data.getNodeId(), data.getSessionId());
            sendToTransport(TransportProtos.ToServerRpcResponseMsg.newBuilder()
                            .setRequestId(requestId).setPayload(msg.getMsg().getData()).build()
                    , data.getSessionId(), data.getNodeId());
        } else {
            log.debug("[{}][{}] Pending RPC request to server not found!", deviceId, requestId);
        }
    }

    private void pushToRuleEngine(ActorContext context, TbMsg tbMsg) {
        context.parent().tell(new DeviceActorToRuleEngineMsg(context.self(), tbMsg), context.self());
    }

    void processAttributesUpdate(ActorContext context, DeviceAttributesEventNotificationMsg msg) {
        if (attributeSubscriptions.size() > 0) {
            boolean hasNotificationData = false;
            TransportProtos.AttributeUpdateNotificationMsg.Builder notification = TransportProtos.AttributeUpdateNotificationMsg.newBuilder();
            if (msg.isDeleted()) {
                List<String> sharedKeys = msg.getDeletedKeys().stream()
                        .filter(key -> DataConstants.SHARED_SCOPE.equals(key.getScope()))
                        .map(AttributeKey::getAttributeKey)
                        .collect(Collectors.toList());
                if (!sharedKeys.isEmpty()) {
                    notification.addAllSharedDeleted(sharedKeys);
                    hasNotificationData = true;
                }
            } else {
                if (DataConstants.SHARED_SCOPE.equals(msg.getScope())) {
                    List<AttributeKvEntry> attributes = new ArrayList<>(msg.getValues());
                    if (attributes.size() > 0) {
                        List<TransportProtos.TsKvProto> sharedUpdated = msg.getValues().stream().map(this::toTsKvProto)
                                .collect(Collectors.toList());
                        if (!sharedUpdated.isEmpty()) {
                            notification.addAllSharedUpdated(sharedUpdated);
                            hasNotificationData = true;
                        }
                    } else {
                        log.debug("[{}] No public server side attributes changed!", deviceId);
                    }
                }
            }
            if (hasNotificationData) {
                TransportProtos.AttributeUpdateNotificationMsg finalNotification = notification.build();
                attributeSubscriptions.entrySet().forEach(sub -> {
                    sendToTransport(finalNotification, sub.getKey(), sub.getValue().getNodeId());
                });
            }
        } else {
            log.debug("[{}] No registered attributes subscriptions to process!", deviceId);
        }
    }

    private void processRpcResponses(ActorContext context, TransportProtos.SessionInfoProto sessionInfo, TransportProtos.ToDeviceRpcResponseMsg responseMsg) {
        UUID sessionId = getSessionId(sessionInfo);
        log.debug("[{}] Processing rpc command response [{}]", deviceId, sessionId);
        ToDeviceRpcRequestMetadata requestMd = toDeviceRpcPendingMap.remove(responseMsg.getRequestId());
        boolean success = requestMd != null;
        if (success) {
            systemContext.getDeviceRpcService().processResponseToServerSideRPCRequestFromDeviceActor(new FromDeviceRpcResponse(requestMd.getMsg().getMsg().getId(),
                    responseMsg.getPayload(), null));
        } else {
            log.debug("[{}] Rpc command response [{}] is stale!", deviceId, responseMsg.getRequestId());
        }
    }

    private void processSubscriptionCommands(ActorContext context, TransportProtos.SessionInfoProto sessionInfo, TransportProtos.SubscribeToAttributeUpdatesMsg subscribeCmd) {
        UUID sessionId = getSessionId(sessionInfo);
        if (subscribeCmd.getUnsubscribe()) {
            log.debug("[{}] Canceling attributes subscription for session [{}]", deviceId, sessionId);
            attributeSubscriptions.remove(sessionId);
        } else {
            SessionInfoMetaData sessionMD = sessions.get(sessionId);
            if (sessionMD == null) {
                sessionMD = new SessionInfoMetaData(new SessionInfo(TransportProtos.SessionType.SYNC, sessionInfo.getNodeId()));
            }
            sessionMD.setSubscribedToAttributes(true);
            log.debug("[{}] Registering attributes subscription for session [{}]", deviceId, sessionId);
            attributeSubscriptions.put(sessionId, sessionMD.getSessionInfo());
            dumpSessions();
        }
    }

    private UUID getSessionId(TransportProtos.SessionInfoProto sessionInfo) {
        return new UUID(sessionInfo.getSessionIdMSB(), sessionInfo.getSessionIdLSB());
    }

    private void processSubscriptionCommands(ActorContext context, TransportProtos.SessionInfoProto sessionInfo, TransportProtos.SubscribeToRPCMsg subscribeCmd) {
        UUID sessionId = getSessionId(sessionInfo);
        if (subscribeCmd.getUnsubscribe()) {
            log.debug("[{}] Canceling rpc subscription for session [{}]", deviceId, sessionId);
            rpcSubscriptions.remove(sessionId);
        } else {
            SessionInfoMetaData sessionMD = sessions.get(sessionId);
            if (sessionMD == null) {
                sessionMD = new SessionInfoMetaData(new SessionInfo(TransportProtos.SessionType.SYNC, sessionInfo.getNodeId()));
            }
            sessionMD.setSubscribedToRPC(true);
            log.debug("[{}] Registering rpc subscription for session [{}]", deviceId, sessionId);
            rpcSubscriptions.put(sessionId, sessionMD.getSessionInfo());
            sendPendingRequests(context, sessionId, sessionInfo);
            dumpSessions();
        }
    }

    private void processSessionStateMsgs(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.SessionEventMsg msg) {
        UUID sessionId = getSessionId(sessionInfo);
        if (msg.getEvent() == TransportProtos.SessionEvent.OPEN) {
            if (sessions.containsKey(sessionId)) {
                log.debug("[{}] Received duplicate session open event [{}]", deviceId, sessionId);
                return;
            }
            log.debug("[{}] Processing new session [{}]", deviceId, sessionId);
            if (sessions.size() >= systemContext.getMaxConcurrentSessionsPerDevice()) {
                UUID sessionIdToRemove = sessions.keySet().stream().findFirst().orElse(null);
                if (sessionIdToRemove != null) {
                    notifyTransportAboutClosedSession(sessionIdToRemove, sessions.remove(sessionIdToRemove));
                }
            }
            sessions.put(sessionId, new SessionInfoMetaData(new SessionInfo(TransportProtos.SessionType.ASYNC, sessionInfo.getNodeId())));
            if (sessions.size() == 1) {
                reportSessionOpen();
            }
            dumpSessions();
        } else if (msg.getEvent() == TransportProtos.SessionEvent.CLOSED) {
            log.debug("[{}] Canceling subscriptions for closed session [{}]", deviceId, sessionId);
            sessions.remove(sessionId);
            attributeSubscriptions.remove(sessionId);
            rpcSubscriptions.remove(sessionId);
            if (sessions.isEmpty()) {
                reportSessionClose();
            }
            dumpSessions();
        }
    }

    private void handleSessionActivity(ActorContext context, TransportProtos.SessionInfoProto sessionInfoProto, TransportProtos.SubscriptionInfoProto subscriptionInfo) {
        UUID sessionId = getSessionId(sessionInfoProto);
        SessionInfoMetaData sessionMD = sessions.computeIfAbsent(sessionId,
                id -> new SessionInfoMetaData(new SessionInfo(TransportProtos.SessionType.ASYNC, sessionInfoProto.getNodeId()), 0L));

        sessionMD.setLastActivityTime(subscriptionInfo.getLastActivityTime());
        sessionMD.setSubscribedToAttributes(subscriptionInfo.getAttributeSubscription());
        sessionMD.setSubscribedToRPC(subscriptionInfo.getRpcSubscription());
        if (subscriptionInfo.getAttributeSubscription()) {
            attributeSubscriptions.putIfAbsent(sessionId, sessionMD.getSessionInfo());
        }
        if (subscriptionInfo.getRpcSubscription()) {
            rpcSubscriptions.putIfAbsent(sessionId, sessionMD.getSessionInfo());
        }
        dumpSessions();
    }

    void processCredentialsUpdate() {
        sessions.forEach(this::notifyTransportAboutClosedSession);
        attributeSubscriptions.clear();
        rpcSubscriptions.clear();
        dumpSessions();
    }

    private void notifyTransportAboutClosedSession(UUID sessionId, SessionInfoMetaData sessionMd) {
        TransportProtos.DeviceActorToTransportMsg msg = TransportProtos.DeviceActorToTransportMsg.newBuilder()
                .setSessionIdMSB(sessionId.getMostSignificantBits())
                .setSessionIdLSB(sessionId.getLeastSignificantBits())
                .setSessionCloseNotification(TransportProtos.SessionCloseNotificationProto.getDefaultInstance()).build();
        systemContext.getRuleEngineTransportService().process(sessionMd.getSessionInfo().getNodeId(), msg);
    }

    void processNameOrTypeUpdate(DeviceNameOrTypeUpdateMsg msg) {
        this.deviceName = msg.getDeviceName();
        this.deviceType = msg.getDeviceType();
        this.defaultMetaData = new TbMsgMetaData();
        this.defaultMetaData.putValue("deviceName", deviceName);
        this.defaultMetaData.putValue("deviceType", deviceType);
    }

    private JsonObject getJsonObject(List<TransportProtos.KeyValueProto> tsKv) {
        JsonObject json = new JsonObject();
        for (TransportProtos.KeyValueProto kv : tsKv) {
            switch (kv.getType()) {
                case BOOLEAN_V:
                    json.addProperty(kv.getKey(), kv.getBoolV());
                    break;
                case LONG_V:
                    json.addProperty(kv.getKey(), kv.getLongV());
                    break;
                case DOUBLE_V:
                    json.addProperty(kv.getKey(), kv.getDoubleV());
                    break;
                case STRING_V:
                    json.addProperty(kv.getKey(), kv.getStringV());
                    break;
            }
        }
        return json;
    }

    private void sendToTransport(TransportProtos.GetAttributeResponseMsg responseMsg, TransportProtos.SessionInfoProto sessionInfo) {
        TransportProtos.DeviceActorToTransportMsg msg = TransportProtos.DeviceActorToTransportMsg.newBuilder()
                .setSessionIdMSB(sessionInfo.getSessionIdMSB())
                .setSessionIdLSB(sessionInfo.getSessionIdLSB())
                .setGetAttributesResponse(responseMsg).build();
        systemContext.getRuleEngineTransportService().process(sessionInfo.getNodeId(), msg);
    }

    private void sendToTransport(TransportProtos.AttributeUpdateNotificationMsg notificationMsg, UUID sessionId, String nodeId) {
        TransportProtos.DeviceActorToTransportMsg msg = TransportProtos.DeviceActorToTransportMsg.newBuilder()
                .setSessionIdMSB(sessionId.getMostSignificantBits())
                .setSessionIdLSB(sessionId.getLeastSignificantBits())
                .setAttributeUpdateNotification(notificationMsg).build();
        systemContext.getRuleEngineTransportService().process(nodeId, msg);
    }

    private void sendToTransport(TransportProtos.ToDeviceRpcRequestMsg rpcMsg, UUID sessionId, String nodeId) {
        TransportProtos.DeviceActorToTransportMsg msg = TransportProtos.DeviceActorToTransportMsg.newBuilder()
                .setSessionIdMSB(sessionId.getMostSignificantBits())
                .setSessionIdLSB(sessionId.getLeastSignificantBits())
                .setToDeviceRequest(rpcMsg).build();
        systemContext.getRuleEngineTransportService().process(nodeId, msg);
    }

    private void sendToTransport(TransportProtos.ToServerRpcResponseMsg rpcMsg, UUID sessionId, String nodeId) {
        TransportProtos.DeviceActorToTransportMsg msg = TransportProtos.DeviceActorToTransportMsg.newBuilder()
                .setSessionIdMSB(sessionId.getMostSignificantBits())
                .setSessionIdLSB(sessionId.getLeastSignificantBits())
                .setToServerResponse(rpcMsg).build();
        systemContext.getRuleEngineTransportService().process(nodeId, msg);
    }


    private List<TransportProtos.TsKvProto> toTsKvProtos(@Nullable List<AttributeKvEntry> result) {
        List<TransportProtos.TsKvProto> clientAttributes;
        if (result == null || result.isEmpty()) {
            clientAttributes = Collections.emptyList();
        } else {
            clientAttributes = new ArrayList<>(result.size());
            for (AttributeKvEntry attrEntry : result) {
                clientAttributes.add(toTsKvProto(attrEntry));
            }
        }
        return clientAttributes;
    }

    private TransportProtos.TsKvProto toTsKvProto(AttributeKvEntry attrEntry) {
        return TransportProtos.TsKvProto.newBuilder().setTs(attrEntry.getLastUpdateTs())
                .setKv(toKeyValueProto(attrEntry)).build();
    }

    private TransportProtos.KeyValueProto toKeyValueProto(KvEntry kvEntry) {
        TransportProtos.KeyValueProto.Builder builder = TransportProtos.KeyValueProto.newBuilder();
        builder.setKey(kvEntry.getKey());
        switch (kvEntry.getDataType()) {
            case BOOLEAN:
                builder.setType(TransportProtos.KeyValueType.BOOLEAN_V);
                builder.setBoolV(kvEntry.getBooleanValue().get());
                break;
            case DOUBLE:
                builder.setType(TransportProtos.KeyValueType.DOUBLE_V);
                builder.setDoubleV(kvEntry.getDoubleValue().get());
                break;
            case LONG:
                builder.setType(TransportProtos.KeyValueType.LONG_V);
                builder.setLongV(kvEntry.getLongValue().get());
                break;
            case STRING:
                builder.setType(TransportProtos.KeyValueType.STRING_V);
                builder.setStringV(kvEntry.getStrValue().get());
                break;
        }
        return builder.build();
    }

    private void restoreSessions() {
        log.debug("[{}] Restoring sessions from cache", deviceId);
        TransportProtos.DeviceSessionsCacheEntry sessionsDump = null;
        try {
            sessionsDump = TransportProtos.DeviceSessionsCacheEntry.parseFrom(systemContext.getDeviceSessionCacheService().get(deviceId));
        } catch (InvalidProtocolBufferException e) {
            log.warn("[{}] Failed to decode device sessions from cache", deviceId);
            return;
        }
        if (sessionsDump.getSessionsCount() == 0) {
            log.debug("[{}] No session information found", deviceId);
            return;
        }
        for (TransportProtos.SessionSubscriptionInfoProto sessionSubscriptionInfoProto : sessionsDump.getSessionsList()) {
            TransportProtos.SessionInfoProto sessionInfoProto = sessionSubscriptionInfoProto.getSessionInfo();
            UUID sessionId = getSessionId(sessionInfoProto);
            SessionInfo sessionInfo = new SessionInfo(TransportProtos.SessionType.ASYNC, sessionInfoProto.getNodeId());
            TransportProtos.SubscriptionInfoProto subInfo = sessionSubscriptionInfoProto.getSubscriptionInfo();
            SessionInfoMetaData sessionMD = new SessionInfoMetaData(sessionInfo, subInfo.getLastActivityTime());
            sessions.put(sessionId, sessionMD);
            if (subInfo.getAttributeSubscription()) {
                attributeSubscriptions.put(sessionId, sessionInfo);
                sessionMD.setSubscribedToAttributes(true);
            }
            if (subInfo.getRpcSubscription()) {
                rpcSubscriptions.put(sessionId, sessionInfo);
                sessionMD.setSubscribedToRPC(true);
            }
            log.debug("[{}] Restored session: {}", deviceId, sessionMD);
        }
        log.debug("[{}] Restored sessions: {}, rpc subscriptions: {}, attribute subscriptions: {}", deviceId, sessions.size(), rpcSubscriptions.size(), attributeSubscriptions.size());
    }

    private void dumpSessions() {
        log.debug("[{}] Dumping sessions: {}, rpc subscriptions: {}, attribute subscriptions: {} to cache", deviceId, sessions.size(), rpcSubscriptions.size(), attributeSubscriptions.size());
        List<TransportProtos.SessionSubscriptionInfoProto> sessionsList = new ArrayList<>(sessions.size());
        sessions.forEach((uuid, sessionMD) -> {
            if (sessionMD.getSessionInfo().getType() == TransportProtos.SessionType.SYNC) {
                return;
            }
            SessionInfo sessionInfo = sessionMD.getSessionInfo();
            TransportProtos.SubscriptionInfoProto subscriptionInfoProto = TransportProtos.SubscriptionInfoProto.newBuilder()
                    .setLastActivityTime(sessionMD.getLastActivityTime())
                    .setAttributeSubscription(sessionMD.isSubscribedToAttributes())
                    .setRpcSubscription(sessionMD.isSubscribedToRPC()).build();
            TransportProtos.SessionInfoProto sessionInfoProto = TransportProtos.SessionInfoProto.newBuilder()
                    .setSessionIdMSB(uuid.getMostSignificantBits())
                    .setSessionIdLSB(uuid.getLeastSignificantBits())
                    .setNodeId(sessionInfo.getNodeId()).build();
            sessionsList.add(TransportProtos.SessionSubscriptionInfoProto.newBuilder()
                    .setSessionInfo(sessionInfoProto)
                    .setSubscriptionInfo(subscriptionInfoProto).build());
            log.debug("[{}] Dumping session: {}", deviceId, sessionMD);
        });
        systemContext.getDeviceSessionCacheService()
                .put(deviceId, TransportProtos.DeviceSessionsCacheEntry.newBuilder()
                        .addAllSessions(sessionsList).build().toByteArray());
    }

    void initSessionTimeout(ActorContext context) {
        schedulePeriodicMsgWithDelay(context, SessionTimeoutCheckMsg.instance(), systemContext.getSessionInactivityTimeout(), systemContext.getSessionInactivityTimeout());
    }

    void checkSessionsTimeout() {
        long expTime = System.currentTimeMillis() - systemContext.getSessionInactivityTimeout();
        Map<UUID, SessionInfoMetaData> sessionsToRemove = sessions.entrySet().stream().filter(kv -> kv.getValue().getLastActivityTime() < expTime).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        sessionsToRemove.forEach((sessionId, sessionMD) -> {
            sessions.remove(sessionId);
            rpcSubscriptions.remove(sessionId);
            attributeSubscriptions.remove(sessionId);
            notifyTransportAboutClosedSession(sessionId, sessionMD);
        });
        if (!sessionsToRemove.isEmpty()) {
            dumpSessions();
        }
    }
}
