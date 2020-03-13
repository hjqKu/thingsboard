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

import akka.actor.ActorInitializationException;
import akka.actor.OneForOneStrategy;
import akka.actor.SupervisorStrategy;
import com.loit.actors.device.DeviceActorToRuleEngineMsg;
import com.loit.common.data.id.TenantId;
import com.loit.common.msg.plugin.ComponentLifecycleMsg;
import com.loit.actors.ActorSystemContext;
import com.loit.actors.device.DeviceActorToRuleEngineMsg;
import com.loit.actors.service.ComponentActor;
import com.loit.actors.service.ContextBasedCreator;
import com.loit.common.data.id.RuleChainId;
import com.loit.common.data.id.TenantId;
import com.loit.common.msg.TbActorMsg;
import com.loit.common.msg.plugin.ComponentLifecycleMsg;
import com.loit.common.msg.system.ServiceToRuleEngineMsg;
import scala.concurrent.duration.Duration;

public class RuleChainActor extends ComponentActor<RuleChainId, RuleChainActorMessageProcessor> {

    private RuleChainActor(ActorSystemContext systemContext, TenantId tenantId, RuleChainId ruleChainId) {
        super(systemContext, tenantId, ruleChainId);
        setProcessor(new RuleChainActorMessageProcessor(tenantId, ruleChainId, systemContext,
                context().parent(), context().self()));
    }

    @Override
    protected boolean process(TbActorMsg msg) {
        switch (msg.getMsgType()) {
            case COMPONENT_LIFE_CYCLE_MSG:
                onComponentLifecycleMsg((ComponentLifecycleMsg) msg);
                break;
            case SERVICE_TO_RULE_ENGINE_MSG:
                processor.onServiceToRuleEngineMsg((ServiceToRuleEngineMsg) msg);
                break;
            case DEVICE_ACTOR_TO_RULE_ENGINE_MSG:
                processor.onDeviceActorToRuleEngineMsg((DeviceActorToRuleEngineMsg) msg);
                break;
            case RULE_TO_RULE_CHAIN_TELL_NEXT_MSG:
            case REMOTE_TO_RULE_CHAIN_TELL_NEXT_MSG:
                processor.onTellNext((RuleNodeToRuleChainTellNextMsg) msg);
                break;
            case RULE_CHAIN_TO_RULE_CHAIN_MSG:
                processor.onRuleChainToRuleChainMsg((RuleChainToRuleChainMsg) msg);
                break;
            case CLUSTER_EVENT_MSG:
                break;
            case STATS_PERSIST_TICK_MSG:
                onStatsPersistTick(id);
                break;
            default:
                return false;
        }
        return true;
    }

    public static class ActorCreator extends ContextBasedCreator<RuleChainActor> {
        private static final long serialVersionUID = 1L;

        private final TenantId tenantId;
        private final RuleChainId ruleChainId;

        public ActorCreator(ActorSystemContext context, TenantId tenantId, RuleChainId pluginId) {
            super(context);
            this.tenantId = tenantId;
            this.ruleChainId = pluginId;
        }

        @Override
        public RuleChainActor create() {
            return new RuleChainActor(context, tenantId, ruleChainId);
        }
    }

    @Override
    protected long getErrorPersistFrequency() {
        return systemContext.getRuleChainErrorPersistFrequency();
    }

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return strategy;
    }

    private final SupervisorStrategy strategy = new OneForOneStrategy(3, Duration.create("1 minute"), t -> {
        logAndPersist("Unknown Failure", ActorSystemContext.toException(t));
        return SupervisorStrategy.resume();
    });
}
