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
package org.thingsboard.rule.engine.api;

import com.datastax.driver.core.ResultSetFuture;
import io.netty.channel.EventLoopGroup;
import org.springframework.data.redis.core.RedisTemplate;
import org.thingsboard.common.util.ListeningExecutor;
import com.loit.common.data.Customer;
import com.loit.common.data.Device;
import com.loit.common.data.alarm.Alarm;
import com.loit.common.data.asset.Asset;
import com.loit.common.data.id.EntityId;
import com.loit.common.data.id.RuleNodeId;
import com.loit.common.data.id.TenantId;
import com.loit.common.data.rule.RuleNode;
import com.loit.common.msg.TbMsg;
import com.loit.common.msg.TbMsgMetaData;
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

import java.util.Set;

/**
 * Created by ashvayka on 13.01.18.
 */
public interface TbContext {

    void tellNext(TbMsg msg, String relationType);

    void tellNext(TbMsg msg, String relationType, Throwable th);

    void tellNext(TbMsg msg, Set<String> relationTypes);

    void tellSelf(TbMsg msg, long delayMs);

    boolean isLocalEntity(EntityId entityId);

    void tellFailure(TbMsg msg, Throwable th);

    void updateSelf(RuleNode self);

    void sendTbMsgToRuleEngine(TbMsg msg);

    TbMsg newMsg(String type, EntityId originator, TbMsgMetaData metaData, String data);

    TbMsg transformMsg(TbMsg origMsg, String type, EntityId originator, TbMsgMetaData metaData, String data);

    TbMsg customerCreatedMsg(Customer customer, RuleNodeId ruleNodeId);

    TbMsg deviceCreatedMsg(Device device, RuleNodeId ruleNodeId);

    TbMsg assetCreatedMsg(Asset asset, RuleNodeId ruleNodeId);

    TbMsg alarmCreatedMsg(Alarm alarm, RuleNodeId ruleNodeId);

    RuleNodeId getSelfId();

    TenantId getTenantId();

    AttributesService getAttributesService();

    CustomerService getCustomerService();

    TenantService getTenantService();

    UserService getUserService();

    AssetService getAssetService();

    DeviceService getDeviceService();

    DashboardService getDashboardService();

    AlarmService getAlarmService();

    RuleChainService getRuleChainService();

    RuleEngineRpcService getRpcService();

    RuleEngineTelemetryService getTelemetryService();

    TimeseriesService getTimeseriesService();

    RelationService getRelationService();

    EntityViewService getEntityViewService();

    ListeningExecutor getJsExecutor();

    ListeningExecutor getMailExecutor();

    ListeningExecutor getDbCallbackExecutor();

    ListeningExecutor getExternalCallExecutor();

    MailService getMailService();

    ScriptEngine createJsScriptEngine(String script, String... argNames);

    void logJsEvalRequest();

    void logJsEvalResponse();

    void logJsEvalFailure();

    String getNodeId();

    RuleChainTransactionService getRuleChainTransactionService();

    EventLoopGroup getSharedEventLoop();

    CassandraCluster getCassandraCluster();

    ResultSetFuture submitCassandraTask(CassandraStatementTask task);

    RedisTemplate<String, Object> getRedisTemplate();

    String getServerAddress();
}
