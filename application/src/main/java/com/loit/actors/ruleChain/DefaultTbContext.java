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
package com.loit.actors.ruleChain;

import akka.actor.ActorRef;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.loit.common.data.Customer;
import com.loit.common.data.DataConstants;
import com.loit.common.data.Device;
import com.loit.common.data.alarm.Alarm;
import com.loit.common.data.asset.Asset;
import com.loit.common.data.id.DeviceId;
import com.loit.common.data.id.EntityId;
import com.loit.common.data.id.TenantId;
import com.loit.common.msg.cluster.ServerAddress;
import com.loit.dao.alarm.AlarmService;
import com.loit.dao.asset.AssetService;
import com.loit.dao.attributes.AttributesService;
import com.loit.dao.cassandra.CassandraCluster;
import com.loit.dao.customer.CustomerService;
import com.loit.dao.dashboard.DashboardService;
import com.loit.dao.device.DeviceService;
import com.loit.dao.entityview.EntityViewService;
import com.loit.dao.nosql.CassandraStatementTask;
import com.loit.dao.relation.RelationService;
import com.loit.dao.rule.RuleChainService;
import com.loit.dao.tenant.TenantService;
import com.loit.dao.timeseries.TimeseriesService;
import com.loit.dao.user.UserService;
import io.netty.channel.EventLoopGroup;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.api.MailService;
import org.thingsboard.rule.engine.api.RuleChainTransactionService;
import org.thingsboard.rule.engine.api.RuleEngineDeviceRpcRequest;
import org.thingsboard.rule.engine.api.RuleEngineDeviceRpcResponse;
import org.thingsboard.rule.engine.api.RuleEngineRpcService;
import org.thingsboard.rule.engine.api.RuleEngineTelemetryService;
import org.thingsboard.rule.engine.api.ScriptEngine;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbRelationTypes;
import com.loit.actors.ActorSystemContext;
import com.loit.common.data.id.RuleNodeId;
import com.loit.common.data.rpc.ToDeviceRpcRequestBody;
import com.loit.common.data.rule.RuleNode;
import com.loit.common.msg.TbMsg;
import com.loit.common.msg.TbMsgMetaData;
import com.loit.common.msg.cluster.SendToClusterMsg;
import com.loit.common.msg.cluster.ServerType;
import com.loit.common.msg.rpc.ToDeviceRpcRequest;
import com.loit.common.msg.system.ServiceToRuleEngineMsg;
import com.loit.service.script.RuleNodeJsScriptEngine;
import scala.concurrent.duration.Duration;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Created by ashvayka on 19.03.18.
 */
class DefaultTbContext implements TbContext {

    public final static ObjectMapper mapper = new ObjectMapper();

    private final ActorSystemContext mainCtx;
    private final RuleNodeCtx nodeCtx;

    public DefaultTbContext(ActorSystemContext mainCtx, RuleNodeCtx nodeCtx) {
        this.mainCtx = mainCtx;
        this.nodeCtx = nodeCtx;
    }

    @Override
    public void tellNext(TbMsg msg, String relationType) {
        tellNext(msg, Collections.singleton(relationType), null);
    }

    @Override
    public void tellNext(TbMsg msg, Set<String> relationTypes) {
        tellNext(msg, relationTypes, null);
    }

    @Override
    public void tellNext(TbMsg msg, String relationType, Throwable th) {
        tellNext(msg, Collections.singleton(relationType), th);
    }

    private void tellNext(TbMsg msg, Set<String> relationTypes, Throwable th) {
        if (nodeCtx.getSelf().isDebugMode()) {
            relationTypes.forEach(relationType -> mainCtx.persistDebugOutput(nodeCtx.getTenantId(), nodeCtx.getSelf().getId(), msg, relationType, th));
        }
        nodeCtx.getChainActor().tell(new RuleNodeToRuleChainTellNextMsg(nodeCtx.getSelf().getId(), relationTypes, msg), nodeCtx.getSelfActor());
    }

    @Override
    public void tellSelf(TbMsg msg, long delayMs) {
        //TODO: add persistence layer
        scheduleMsgWithDelay(new RuleNodeToSelfMsg(msg), delayMs, nodeCtx.getSelfActor());
    }

    @Override
    public boolean isLocalEntity(EntityId entityId) {
        Optional<ServerAddress> address = mainCtx.getRoutingService().resolveById(entityId);
        return !address.isPresent();
    }

    private void scheduleMsgWithDelay(Object msg, long delayInMs, ActorRef target) {
        mainCtx.getScheduler().scheduleOnce(Duration.create(delayInMs, TimeUnit.MILLISECONDS), target, msg, mainCtx.getActorSystem().dispatcher(), nodeCtx.getSelfActor());
    }

    @Override
    public void tellFailure(TbMsg msg, Throwable th) {
        if (nodeCtx.getSelf().isDebugMode()) {
            mainCtx.persistDebugOutput(nodeCtx.getTenantId(), nodeCtx.getSelf().getId(), msg, TbRelationTypes.FAILURE, th);
        }
        nodeCtx.getChainActor().tell(new RuleNodeToRuleChainTellNextMsg(nodeCtx.getSelf().getId(), Collections.singleton(TbRelationTypes.FAILURE), msg), nodeCtx.getSelfActor());
    }

    @Override
    public void updateSelf(RuleNode self) {
        nodeCtx.setSelf(self);
    }

    @Override
    public TbMsg newMsg(String type, EntityId originator, TbMsgMetaData metaData, String data) {
        return new TbMsg(UUIDs.timeBased(), type, originator, metaData.copy(), data, nodeCtx.getSelf().getRuleChainId(), nodeCtx.getSelf().getId(), mainCtx.getQueuePartitionId());
    }

    @Override
    public TbMsg transformMsg(TbMsg origMsg, String type, EntityId originator, TbMsgMetaData metaData, String data) {
        return new TbMsg(origMsg.getId(), type, originator, metaData.copy(), origMsg.getDataType(), data, origMsg.getTransactionData(), origMsg.getRuleChainId(), origMsg.getRuleNodeId(), mainCtx.getQueuePartitionId());
    }

    @Override
    public void sendTbMsgToRuleEngine(TbMsg msg) {
        mainCtx.getActorService().onMsg(new SendToClusterMsg(msg.getOriginator(), new ServiceToRuleEngineMsg(getTenantId(), msg)));
    }

    public TbMsg customerCreatedMsg(Customer customer, RuleNodeId ruleNodeId) {
        try {
            ObjectNode entityNode = mapper.valueToTree(customer);
            return new TbMsg(UUIDs.timeBased(), DataConstants.ENTITY_CREATED, customer.getId(), getActionMetaData(ruleNodeId), mapper.writeValueAsString(entityNode), null, null, 0L);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            throw new RuntimeException("Failed to process customer created msg: " + e);
        }
    }

    public TbMsg deviceCreatedMsg(Device device, RuleNodeId ruleNodeId) {
        try {
            ObjectNode entityNode = mapper.valueToTree(device);
            return new TbMsg(UUIDs.timeBased(), DataConstants.ENTITY_CREATED, device.getId(), getActionMetaData(ruleNodeId), mapper.writeValueAsString(entityNode), null, null, 0L);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            throw new RuntimeException("Failed to process device created msg: " + e);
        }
    }

    public TbMsg assetCreatedMsg(Asset asset, RuleNodeId ruleNodeId) {
        try {
            ObjectNode entityNode = mapper.valueToTree(asset);
            return new TbMsg(UUIDs.timeBased(), DataConstants.ENTITY_CREATED, asset.getId(), getActionMetaData(ruleNodeId), mapper.writeValueAsString(entityNode), null, null, 0L);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            throw new RuntimeException("Failed to process asset created msg: " + e);
        }
    }

    public TbMsg alarmCreatedMsg(Alarm alarm, RuleNodeId ruleNodeId) {
        try {
            ObjectNode entityNode = mapper.valueToTree(alarm);
            return new TbMsg(UUIDs.timeBased(), DataConstants.ENTITY_CREATED, alarm.getId(), getActionMetaData(ruleNodeId), mapper.writeValueAsString(entityNode), null, null, 0L);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            throw new RuntimeException("Failed to process alarm created msg: " + e);
        }
    }


    @Override
    public RuleNodeId getSelfId() {
        return nodeCtx.getSelf().getId();
    }

    @Override
    public TenantId getTenantId() {
        return nodeCtx.getTenantId();
    }

    @Override
    public ListeningExecutor getJsExecutor() {
        return mainCtx.getJsExecutor();
    }

    @Override
    public ListeningExecutor getMailExecutor() {
        return mainCtx.getMailExecutor();
    }

    @Override
    public ListeningExecutor getDbCallbackExecutor() {
        return mainCtx.getDbCallbackExecutor();
    }

    @Override
    public ListeningExecutor getExternalCallExecutor() {
        return mainCtx.getExternalCallExecutorService();
    }

    @Override
    public ScriptEngine createJsScriptEngine(String script, String... argNames) {
        return new RuleNodeJsScriptEngine(mainCtx.getJsSandbox(), nodeCtx.getSelf().getId(), script, argNames);
    }

    @Override
    public void logJsEvalRequest() {
        if (mainCtx.isStatisticsEnabled()) {
            mainCtx.getJsInvokeRequestsCount().incrementAndGet();
        }
    }

    @Override
    public void logJsEvalResponse() {
        if (mainCtx.isStatisticsEnabled()) {
            mainCtx.getJsInvokeResponsesCount().incrementAndGet();
        }
    }

    @Override
    public void logJsEvalFailure() {
        if (mainCtx.isStatisticsEnabled()) {
            mainCtx.getJsInvokeFailuresCount().incrementAndGet();
        }
    }

    @Override
    public String getNodeId() {
        return mainCtx.getNodeIdProvider().getNodeId();
    }

    @Override
    public AttributesService getAttributesService() {
        return mainCtx.getAttributesService();
    }

    @Override
    public CustomerService getCustomerService() {
        return mainCtx.getCustomerService();
    }

    @Override
    public TenantService getTenantService() {
        return mainCtx.getTenantService();
    }

    @Override
    public UserService getUserService() {
        return mainCtx.getUserService();
    }

    @Override
    public AssetService getAssetService() {
        return mainCtx.getAssetService();
    }

    @Override
    public DeviceService getDeviceService() {
        return mainCtx.getDeviceService();
    }

    @Override
    public DashboardService getDashboardService() {
        return mainCtx.getDashboardService();
    }

    @Override
    public AlarmService getAlarmService() {
        return mainCtx.getAlarmService();
    }

    @Override
    public RuleChainService getRuleChainService() {
        return mainCtx.getRuleChainService();
    }

    @Override
    public TimeseriesService getTimeseriesService() {
        return mainCtx.getTsService();
    }

    @Override
    public RuleEngineTelemetryService getTelemetryService() {
        return mainCtx.getTsSubService();
    }

    @Override
    public RelationService getRelationService() {
        return mainCtx.getRelationService();
    }

    @Override
    public EntityViewService getEntityViewService() {
        return mainCtx.getEntityViewService();
    }

    @Override
    public RuleChainTransactionService getRuleChainTransactionService() {
        return mainCtx.getRuleChainTransactionService();
    }

    @Override
    public EventLoopGroup getSharedEventLoop() {
        return mainCtx.getSharedEventLoopGroupService().getSharedEventLoopGroup();
    }

    @Override
    public MailService getMailService() {
        if (mainCtx.isAllowSystemMailService()) {
            return mainCtx.getMailService();
        } else {
            throw new RuntimeException("Access to System Mail Service is forbidden!");
        }
    }

    @Override
    public RuleEngineRpcService getRpcService() {
        return new RuleEngineRpcService() {
            @Override
            public void sendRpcReply(DeviceId deviceId, int requestId, String body) {
                mainCtx.getDeviceRpcService().sendReplyToRpcCallFromDevice(nodeCtx.getTenantId(), deviceId, requestId, body);
            }

            @Override
            public void sendRpcRequest(RuleEngineDeviceRpcRequest src, Consumer<RuleEngineDeviceRpcResponse> consumer) {
                ToDeviceRpcRequest request = new ToDeviceRpcRequest(src.getRequestUUID(), nodeCtx.getTenantId(), src.getDeviceId(),
                        src.isOneway(), src.getExpirationTime(), new ToDeviceRpcRequestBody(src.getMethod(), src.getBody()));
                mainCtx.getDeviceRpcService().forwardServerSideRPCRequestToDeviceActor(request, response -> {
                    if (src.isRestApiCall()) {
                        ServerAddress requestOriginAddress;
                        if (!StringUtils.isEmpty(src.getOriginHost())) {
                            requestOriginAddress = new ServerAddress(src.getOriginHost(), src.getOriginPort(), ServerType.CORE);
                        } else {
                            requestOriginAddress = mainCtx.getRoutingService().getCurrentServer();
                        }
                        mainCtx.getDeviceRpcService().processResponseToServerSideRPCRequestFromRuleEngine(requestOriginAddress, response);
                    }
                    consumer.accept(RuleEngineDeviceRpcResponse.builder()
                            .deviceId(src.getDeviceId())
                            .requestId(src.getRequestId())
                            .error(response.getError())
                            .response(response.getResponse())
                            .build());
                });
            }
        };
    }

    @Override
    public CassandraCluster getCassandraCluster() {
        return mainCtx.getCassandraCluster();
    }

    @Override
    public ResultSetFuture submitCassandraTask(CassandraStatementTask task) {
        return mainCtx.getCassandraBufferedRateExecutor().submit(task);
    }

    @Override
    public RedisTemplate<String, Object> getRedisTemplate() {
        return mainCtx.getRedisTemplate();
    }

    @Override
    public String getServerAddress() {
        return mainCtx.getServerAddress();
    }

    private TbMsgMetaData getActionMetaData(RuleNodeId ruleNodeId) {
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("ruleNodeId", ruleNodeId.toString());
        return metaData;
    }

}
