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
package com.loit.actors;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Scheduler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.loit.actors.service.ActorService;
import com.loit.actors.tenant.DebugTbRateLimits;
import com.loit.common.data.DataConstants;
import com.loit.common.data.Event;
import com.loit.common.data.id.EntityId;
import com.loit.common.data.id.TenantId;
import com.loit.common.data.plugin.ComponentLifecycleEvent;
import com.loit.common.msg.TbMsg;
import com.loit.common.msg.cluster.ServerAddress;
import com.loit.common.msg.tools.TbRateLimits;
import com.loit.common.transport.auth.DeviceAuthService;
import com.loit.dao.alarm.AlarmService;
import com.loit.dao.asset.AssetService;
import com.loit.dao.attributes.AttributesService;
import com.loit.dao.audit.AuditLogService;
import com.loit.dao.cassandra.CassandraCluster;
import com.loit.dao.customer.CustomerService;
import com.loit.dao.dashboard.DashboardService;
import com.loit.dao.device.DeviceService;
import com.loit.dao.entityview.EntityViewService;
import com.loit.dao.event.EventService;
import com.loit.dao.nosql.CassandraBufferedRateExecutor;
import com.loit.dao.relation.RelationService;
import com.loit.dao.rule.RuleChainService;
import com.loit.dao.tenant.TenantService;
import com.loit.dao.timeseries.TimeseriesService;
import com.loit.dao.user.UserService;
import com.loit.kafka.TbNodeIdProvider;
import com.loit.service.cluster.discovery.DiscoveryService;
import com.loit.service.cluster.routing.ClusterRoutingService;
import com.loit.service.cluster.rpc.ClusterRpcService;
import com.loit.service.component.ComponentDiscoveryService;
import com.loit.service.encoding.DataDecodingEncodingService;
import com.loit.service.executors.ClusterRpcCallbackExecutorService;
import com.loit.service.executors.DbCallbackExecutorService;
import com.loit.service.executors.ExternalCallExecutorService;
import com.loit.service.executors.SharedEventLoopGroupService;
import com.loit.service.mail.MailExecutorService;
import com.loit.service.rpc.DeviceRpcService;
import com.loit.service.script.JsExecutorService;
import com.loit.service.script.JsInvokeService;
import com.loit.service.session.DeviceSessionCacheService;
import com.loit.service.state.DeviceStateService;
import com.loit.service.telemetry.TelemetrySubscriptionService;
import com.loit.service.transport.RuleEngineTransportService;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.thingsboard.rule.engine.api.MailService;
import org.thingsboard.rule.engine.api.RuleChainTransactionService;
import com.loit.actors.service.ActorService;
import com.loit.actors.tenant.DebugTbRateLimits;
import com.loit.common.data.DataConstants;
import com.loit.common.data.Event;
import com.loit.common.data.id.EntityId;
import com.loit.common.data.id.TenantId;
import com.loit.common.data.plugin.ComponentLifecycleEvent;
import com.loit.common.msg.TbMsg;
import com.loit.common.msg.cluster.ServerAddress;
import com.loit.common.msg.tools.TbRateLimits;
import com.loit.common.transport.auth.DeviceAuthService;
import com.loit.dao.alarm.AlarmService;
import com.loit.dao.asset.AssetService;
import com.loit.dao.attributes.AttributesService;
import com.loit.dao.audit.AuditLogService;
import com.loit.dao.cassandra.CassandraCluster;
import com.loit.dao.customer.CustomerService;
import com.loit.dao.dashboard.DashboardService;
import com.loit.dao.device.DeviceService;
import com.loit.dao.entityview.EntityViewService;
import com.loit.dao.event.EventService;
import com.loit.dao.nosql.CassandraBufferedRateExecutor;
import com.loit.dao.relation.RelationService;
import com.loit.dao.rule.RuleChainService;
import com.loit.dao.tenant.TenantService;
import com.loit.dao.timeseries.TimeseriesService;
import com.loit.dao.user.UserService;
import com.loit.kafka.TbNodeIdProvider;
import com.loit.service.cluster.discovery.DiscoveryService;
import com.loit.service.cluster.routing.ClusterRoutingService;
import com.loit.service.cluster.rpc.ClusterRpcService;
import com.loit.service.component.ComponentDiscoveryService;
import com.loit.service.encoding.DataDecodingEncodingService;
import com.loit.service.executors.ClusterRpcCallbackExecutorService;
import com.loit.service.executors.DbCallbackExecutorService;
import com.loit.service.executors.ExternalCallExecutorService;
import com.loit.service.executors.SharedEventLoopGroupService;
import com.loit.service.mail.MailExecutorService;
import com.loit.service.rpc.DeviceRpcService;
import com.loit.service.script.JsExecutorService;
import com.loit.service.script.JsInvokeService;
import com.loit.service.session.DeviceSessionCacheService;
import com.loit.service.state.DeviceStateService;
import com.loit.service.telemetry.TelemetrySubscriptionService;
import com.loit.service.transport.RuleEngineTransportService;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class ActorSystemContext {
    private static final String AKKA_CONF_FILE_NAME = "actor-system.conf";

    protected final ObjectMapper mapper = new ObjectMapper();

    private final ConcurrentMap<TenantId, DebugTbRateLimits> debugPerTenantLimits = new ConcurrentHashMap<>();

    public ConcurrentMap<TenantId, DebugTbRateLimits> getDebugPerTenantLimits() {
        return debugPerTenantLimits;
    }

    @Getter
    @Setter
    private ActorService actorService;

    @Autowired
    @Getter
    private DiscoveryService discoveryService;

    @Autowired
    @Getter
    @Setter
    private ComponentDiscoveryService componentService;

    @Autowired
    @Getter
    private ClusterRoutingService routingService;

    @Autowired
    @Getter
    private ClusterRpcService rpcService;

    @Autowired
    @Getter
    private DataDecodingEncodingService encodingService;

    @Autowired
    @Getter
    private DeviceAuthService deviceAuthService;

    @Autowired
    @Getter
    private DeviceService deviceService;

    @Autowired
    @Getter
    private AssetService assetService;

    @Autowired
    @Getter
    private DashboardService dashboardService;

    @Autowired
    @Getter
    private TenantService tenantService;

    @Autowired
    @Getter
    private CustomerService customerService;

    @Autowired
    @Getter
    private UserService userService;

    @Autowired
    @Getter
    private RuleChainService ruleChainService;

    @Autowired
    @Getter
    private TimeseriesService tsService;

    @Autowired
    @Getter
    private AttributesService attributesService;

    @Autowired
    @Getter
    private EventService eventService;

    @Autowired
    @Getter
    private AlarmService alarmService;

    @Autowired
    @Getter
    private RelationService relationService;

    @Autowired
    @Getter
    private AuditLogService auditLogService;

    @Autowired
    @Getter
    private EntityViewService entityViewService;

    @Autowired
    @Getter
    private TelemetrySubscriptionService tsSubService;

    @Autowired
    @Getter
    private DeviceRpcService deviceRpcService;

    @Autowired
    @Getter
    private JsInvokeService jsSandbox;

    @Autowired
    @Getter
    private JsExecutorService jsExecutor;

    @Autowired
    @Getter
    private MailExecutorService mailExecutor;

    @Autowired
    @Getter
    private ClusterRpcCallbackExecutorService clusterRpcCallbackExecutor;

    @Autowired
    @Getter
    private DbCallbackExecutorService dbCallbackExecutor;

    @Autowired
    @Getter
    private ExternalCallExecutorService externalCallExecutorService;

    @Autowired
    @Getter
    private SharedEventLoopGroupService sharedEventLoopGroupService;

    @Autowired
    @Getter
    private MailService mailService;

    @Autowired
    @Getter
    private DeviceStateService deviceStateService;

    @Autowired
    @Getter
    private DeviceSessionCacheService deviceSessionCacheService;

    @Lazy
    @Autowired
    @Getter
    private RuleEngineTransportService ruleEngineTransportService;

    @Lazy
    @Autowired
    @Getter
    private RuleChainTransactionService ruleChainTransactionService;

    @Value("${cluster.partition_id}")
    @Getter
    private long queuePartitionId;

    @Value("${actors.session.max_concurrent_sessions_per_device:1}")
    @Getter
    private long maxConcurrentSessionsPerDevice;

    @Value("${actors.session.sync.timeout}")
    @Getter
    private long syncSessionTimeout;

    @Value("${actors.queue.enabled}")
    @Getter
    private boolean queuePersistenceEnabled;

    @Value("${actors.queue.timeout}")
    @Getter
    private long queuePersistenceTimeout;

    @Value("${actors.client_side_rpc.timeout}")
    @Getter
    private long clientSideRpcTimeout;

    @Value("${actors.rule.chain.error_persist_frequency}")
    @Getter
    private long ruleChainErrorPersistFrequency;

    @Value("${actors.rule.node.error_persist_frequency}")
    @Getter
    private long ruleNodeErrorPersistFrequency;

    @Value("${actors.statistics.enabled}")
    @Getter
    private boolean statisticsEnabled;

    @Value("${actors.statistics.persist_frequency}")
    @Getter
    private long statisticsPersistFrequency;

    @Getter
    private final AtomicInteger jsInvokeRequestsCount = new AtomicInteger(0);
    @Getter
    private final AtomicInteger jsInvokeResponsesCount = new AtomicInteger(0);
    @Getter
    private final AtomicInteger jsInvokeFailuresCount = new AtomicInteger(0);

    @Scheduled(fixedDelayString = "${js.remote.stats.print_interval_ms}")
    public void printStats() {
        if (statisticsEnabled) {
            if (jsInvokeRequestsCount.get() > 0 || jsInvokeResponsesCount.get() > 0 || jsInvokeFailuresCount.get() > 0) {
                log.info("Rule Engine JS Invoke Stats: requests [{}] responses [{}] failures [{}]",
                        jsInvokeRequestsCount.getAndSet(0), jsInvokeResponsesCount.getAndSet(0), jsInvokeFailuresCount.getAndSet(0));
            }
        }
    }

    @Value("${actors.tenant.create_components_on_init}")
    @Getter
    private boolean tenantComponentsInitEnabled;

    @Value("${actors.rule.allow_system_mail_service}")
    @Getter
    private boolean allowSystemMailService;

    @Value("${transport.sessions.inactivity_timeout}")
    @Getter
    private long sessionInactivityTimeout;

    @Value("${transport.sessions.report_timeout}")
    @Getter
    private long sessionReportTimeout;

    @Value("${actors.rule.chain.debug_mode_rate_limits_per_tenant.enabled}")
    @Getter
    private boolean debugPerTenantEnabled;

    @Value("${actors.rule.chain.debug_mode_rate_limits_per_tenant.configuration}")
    @Getter
    private String debugPerTenantLimitsConfiguration;

    @Getter
    @Setter
    private ActorSystem actorSystem;

    @Autowired
    @Getter
    private TbNodeIdProvider nodeIdProvider;

    @Getter
    @Setter
    private ActorRef appActor;

    @Getter
    @Setter
    private ActorRef statsActor;

    @Getter
    private final Config config;

    @Autowired(required = false)
    @Getter
    private CassandraCluster cassandraCluster;

    @Autowired(required = false)
    @Getter
    private CassandraBufferedRateExecutor cassandraBufferedRateExecutor;

    @Autowired(required = false)
    @Getter
    private RedisTemplate<String, Object> redisTemplate;

    public ActorSystemContext() {
        config = ConfigFactory.parseResources(AKKA_CONF_FILE_NAME).withFallback(ConfigFactory.load());
    }

    public Scheduler getScheduler() {
        return actorSystem.scheduler();
    }

    public void persistError(TenantId tenantId, EntityId entityId, String method, Exception e) {
        Event event = new Event();
        event.setTenantId(tenantId);
        event.setEntityId(entityId);
        event.setType(DataConstants.ERROR);
        event.setBody(toBodyJson(discoveryService.getCurrentServer().getServerAddress(), method, toString(e)));
        persistEvent(event);
    }

    public void persistLifecycleEvent(TenantId tenantId, EntityId entityId, ComponentLifecycleEvent lcEvent, Exception e) {
        Event event = new Event();
        event.setTenantId(tenantId);
        event.setEntityId(entityId);
        event.setType(DataConstants.LC_EVENT);
        event.setBody(toBodyJson(discoveryService.getCurrentServer().getServerAddress(), lcEvent, Optional.ofNullable(e)));
        persistEvent(event);
    }

    private void persistEvent(Event event) {
        eventService.save(event);
    }

    private String toString(Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    private JsonNode toBodyJson(ServerAddress server, ComponentLifecycleEvent event, Optional<Exception> e) {
        ObjectNode node = mapper.createObjectNode().put("server", server.toString()).put("event", event.name());
        if (e.isPresent()) {
            node = node.put("success", false);
            node = node.put("error", toString(e.get()));
        } else {
            node = node.put("success", true);
        }
        return node;
    }

    private JsonNode toBodyJson(ServerAddress server, String method, String body) {
        return mapper.createObjectNode().put("server", server.toString()).put("method", method).put("error", body);
    }

    public String getServerAddress() {
        return discoveryService.getCurrentServer().getServerAddress().toString();
    }

    public void persistDebugInput(TenantId tenantId, EntityId entityId, TbMsg tbMsg, String relationType) {
        persistDebugAsync(tenantId, entityId, "IN", tbMsg, relationType, null);
    }

    public void persistDebugInput(TenantId tenantId, EntityId entityId, TbMsg tbMsg, String relationType, Throwable error) {
        persistDebugAsync(tenantId, entityId, "IN", tbMsg, relationType, error);
    }

    public void persistDebugOutput(TenantId tenantId, EntityId entityId, TbMsg tbMsg, String relationType, Throwable error) {
        persistDebugAsync(tenantId, entityId, "OUT", tbMsg, relationType, error);
    }

    public void persistDebugOutput(TenantId tenantId, EntityId entityId, TbMsg tbMsg, String relationType) {
        persistDebugAsync(tenantId, entityId, "OUT", tbMsg, relationType, null);
    }

    private void persistDebugAsync(TenantId tenantId, EntityId entityId, String type, TbMsg tbMsg, String relationType, Throwable error) {
        if (checkLimits(tenantId, tbMsg, error)) {
            try {
                Event event = new Event();
                event.setTenantId(tenantId);
                event.setEntityId(entityId);
                event.setType(DataConstants.DEBUG_RULE_NODE);

                String metadata = mapper.writeValueAsString(tbMsg.getMetaData().getData());

                ObjectNode node = mapper.createObjectNode()
                        .put("type", type)
                        .put("server", getServerAddress())
                        .put("entityId", tbMsg.getOriginator().getId().toString())
                        .put("entityName", tbMsg.getOriginator().getEntityType().name())
                        .put("msgId", tbMsg.getId().toString())
                        .put("msgType", tbMsg.getType())
                        .put("dataType", tbMsg.getDataType().name())
                        .put("relationType", relationType)
                        .put("data", tbMsg.getData())
                        .put("metadata", metadata);

                if (error != null) {
                    node = node.put("error", toString(error));
                }

                event.setBody(node);
                ListenableFuture<Event> future = eventService.saveAsync(event);
                Futures.addCallback(future, new FutureCallback<Event>() {
                    @Override
                    public void onSuccess(@Nullable Event event) {

                    }

                    @Override
                    public void onFailure(Throwable th) {
                        log.error("Could not save debug Event for Node", th);
                    }
                });
            } catch (IOException ex) {
                log.warn("Failed to persist rule node debug message", ex);
            }
        }
    }

    private boolean checkLimits(TenantId tenantId, TbMsg tbMsg, Throwable error) {
        if (debugPerTenantEnabled) {
            DebugTbRateLimits debugTbRateLimits = debugPerTenantLimits.computeIfAbsent(tenantId, id ->
                    new DebugTbRateLimits(new TbRateLimits(debugPerTenantLimitsConfiguration), false));

            if (!debugTbRateLimits.getTbRateLimits().tryConsume()) {
                if (!debugTbRateLimits.isRuleChainEventSaved()) {
                    persistRuleChainDebugModeEvent(tenantId, tbMsg.getRuleChainId(), error);
                    debugTbRateLimits.setRuleChainEventSaved(true);
                }
                if (log.isTraceEnabled()) {
                    log.trace("[{}] Tenant level debug mode rate limit detected: {}", tenantId, tbMsg);
                }
                return false;
            }
        }
        return true;
    }

    private void persistRuleChainDebugModeEvent(TenantId tenantId, EntityId entityId, Throwable error) {
        Event event = new Event();
        event.setTenantId(tenantId);
        event.setEntityId(entityId);
        event.setType(DataConstants.DEBUG_RULE_CHAIN);

        ObjectNode node = mapper.createObjectNode()
                //todo: what fields are needed here?
                .put("server", getServerAddress())
                .put("message", "Reached debug mode rate limit!");

        if (error != null) {
            node = node.put("error", toString(error));
        }

        event.setBody(node);
        ListenableFuture<Event> future = eventService.saveAsync(event);
        Futures.addCallback(future, new FutureCallback<Event>() {
            @Override
            public void onSuccess(@Nullable Event event) {

            }

            @Override
            public void onFailure(Throwable th) {
                log.error("Could not save debug Event for Rule Chain", th);
            }
        });
    }

    public static Exception toException(Throwable error) {
        return Exception.class.isInstance(error) ? (Exception) error : new Exception(error);
    }

}
