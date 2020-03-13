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

import com.loit.common.data.id.TenantId;
import com.loit.common.msg.plugin.ComponentLifecycleMsg;
import com.loit.actors.ActorSystemContext;
import com.loit.actors.service.ComponentActor;
import com.loit.actors.service.ContextBasedCreator;
import com.loit.common.data.id.RuleChainId;
import com.loit.common.data.id.RuleNodeId;
import com.loit.common.data.id.TenantId;
import com.loit.common.msg.TbActorMsg;
import com.loit.common.msg.plugin.ComponentLifecycleMsg;

public class RuleNodeActor extends ComponentActor<RuleNodeId, RuleNodeActorMessageProcessor> {

    private final RuleChainId ruleChainId;

    private RuleNodeActor(ActorSystemContext systemContext, TenantId tenantId, RuleChainId ruleChainId, RuleNodeId ruleNodeId) {
        super(systemContext, tenantId, ruleNodeId);
        this.ruleChainId = ruleChainId;
        setProcessor(new RuleNodeActorMessageProcessor(tenantId, ruleChainId, ruleNodeId, systemContext,
                context().parent(), context().self()));
    }

    @Override
    protected boolean process(TbActorMsg msg) {
        switch (msg.getMsgType()) {
            case COMPONENT_LIFE_CYCLE_MSG:
                onComponentLifecycleMsg((ComponentLifecycleMsg) msg);
                break;
            case RULE_CHAIN_TO_RULE_MSG:
                onRuleChainToRuleNodeMsg((RuleChainToRuleNodeMsg) msg);
                break;
            case RULE_TO_SELF_ERROR_MSG:
                onRuleNodeToSelfErrorMsg((RuleNodeToSelfErrorMsg) msg);
                break;
            case RULE_TO_SELF_MSG:
                onRuleNodeToSelfMsg((RuleNodeToSelfMsg) msg);
                break;
            case STATS_PERSIST_TICK_MSG:
                onStatsPersistTick(id);
                break;
            default:
                return false;
        }
        return true;
    }

    private void onRuleNodeToSelfMsg(RuleNodeToSelfMsg msg) {
        if (log.isDebugEnabled()) {
            log.debug("[{}][{}][{}] Going to process rule msg: {}", ruleChainId, id, processor.getComponentName(), msg.getMsg());
        }
        try {
            processor.onRuleToSelfMsg(msg);
            increaseMessagesProcessedCount();
        } catch (Exception e) {
            logAndPersist("onRuleMsg", e);
        }
    }

    private void onRuleChainToRuleNodeMsg(RuleChainToRuleNodeMsg msg) {
        if (log.isDebugEnabled()) {
            log.debug("[{}][{}][{}] Going to process rule msg: {}", ruleChainId, id, processor.getComponentName(), msg.getMsg());
        }
        try {
            processor.onRuleChainToRuleNodeMsg(msg);
            increaseMessagesProcessedCount();
        } catch (Exception e) {
            logAndPersist("onRuleMsg", e);
        }
    }

    private void onRuleNodeToSelfErrorMsg(RuleNodeToSelfErrorMsg msg) {
        logAndPersist("onRuleMsg", ActorSystemContext.toException(msg.getError()));
    }

    public static class ActorCreator extends ContextBasedCreator<RuleNodeActor> {
        private static final long serialVersionUID = 1L;

        private final TenantId tenantId;
        private final RuleChainId ruleChainId;
        private final RuleNodeId ruleNodeId;

        public ActorCreator(ActorSystemContext context, TenantId tenantId, RuleChainId ruleChainId, RuleNodeId ruleNodeId) {
            super(context);
            this.tenantId = tenantId;
            this.ruleChainId = ruleChainId;
            this.ruleNodeId = ruleNodeId;

        }

        @Override
        public RuleNodeActor create() throws Exception {
            return new RuleNodeActor(context, tenantId, ruleChainId, ruleNodeId);
        }
    }

    @Override
    protected long getErrorPersistFrequency() {
        return systemContext.getRuleNodeErrorPersistFrequency();
    }

}
