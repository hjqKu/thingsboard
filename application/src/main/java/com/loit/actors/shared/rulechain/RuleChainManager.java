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
package com.loit.actors.shared.rulechain;

import akka.actor.ActorRef;
import akka.japi.Creator;
import com.loit.actors.shared.EntityActorsManager;
import com.loit.dao.rule.RuleChainService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import com.loit.actors.ActorSystemContext;
import com.loit.actors.ruleChain.RuleChainActor;
import com.loit.actors.shared.EntityActorsManager;
import com.loit.common.data.id.RuleChainId;
import com.loit.common.data.rule.RuleChain;
import com.loit.dao.rule.RuleChainService;

/**
 * Created by ashvayka on 15.03.18.
 */
@Slf4j
public abstract class RuleChainManager extends EntityActorsManager<RuleChainId, RuleChainActor, RuleChain> {

    protected final RuleChainService service;
    @Getter
    protected RuleChain rootChain;
    @Getter
    protected ActorRef rootChainActor;

    public RuleChainManager(ActorSystemContext systemContext) {
        super(systemContext);
        this.service = systemContext.getRuleChainService();
    }

    @Override
    public Creator<RuleChainActor> creator(RuleChainId entityId) {
        return new RuleChainActor.ActorCreator(systemContext, getTenantId(), entityId);
    }

    @Override
    public void visit(RuleChain entity, ActorRef actorRef) {
        if (entity != null && entity.isRoot()) {
            rootChain = entity;
            rootChainActor = actorRef;
        }
    }

}
