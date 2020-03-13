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
import com.loit.actors.shared.rulechain.RuleChainManager;
import com.loit.common.data.EntityType;
import com.loit.common.data.id.EntityId;
import com.loit.dao.rule.RuleChainService;
import com.loit.actors.ActorSystemContext;
import com.loit.actors.service.ContextAwareActor;
import com.loit.actors.shared.rulechain.RuleChainManager;
import com.loit.common.data.id.EntityId;
import com.loit.common.data.id.RuleChainId;
import com.loit.dao.rule.RuleChainService;

/**
 * Created by ashvayka on 15.03.18.
 */
public abstract class RuleChainManagerActor extends ContextAwareActor {

    protected final RuleChainManager ruleChainManager;
    protected final RuleChainService ruleChainService;

    public RuleChainManagerActor(ActorSystemContext systemContext, RuleChainManager ruleChainManager) {
        super(systemContext);
        this.ruleChainManager = ruleChainManager;
        this.ruleChainService = systemContext.getRuleChainService();
    }

    protected void initRuleChains() {
        ruleChainManager.init(this.context());
    }

    protected ActorRef getEntityActorRef(EntityId entityId) {
        ActorRef target = null;
        switch (entityId.getEntityType()) {
            case EntityType.RULE_CHAIN:
                target = ruleChainManager.getOrCreateActor(this.context(), (RuleChainId) entityId);
                break;
        }
        return target;
    }

    protected void broadcast(Object msg) {
        ruleChainManager.broadcast(msg);
    }
}
