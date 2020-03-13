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
package com.loit.actors.shared;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Creator;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.loit.common.data.SearchTextBased;
import com.loit.common.data.id.EntityId;
import com.loit.common.data.id.TenantId;
import com.loit.common.data.id.UUIDBased;
import com.loit.common.data.page.PageDataIterable;
import lombok.extern.slf4j.Slf4j;
import com.loit.actors.ActorSystemContext;
import com.loit.actors.service.ContextAwareActor;

/**
 * Created by ashvayka on 15.03.18.
 */
@Slf4j
public abstract class EntityActorsManager<T extends EntityId, A extends UntypedActor, M extends SearchTextBased<? extends UUIDBased>> {

    protected final ActorSystemContext systemContext;
    protected final BiMap<T, ActorRef> actors;

    public EntityActorsManager(ActorSystemContext systemContext) {
        this.systemContext = systemContext;
        this.actors = HashBiMap.create();
    }

    protected abstract TenantId getTenantId();

    protected abstract String getDispatcherName();

    protected abstract Creator<A> creator(T entityId);

    protected abstract PageDataIterable.FetchFunction<M> getFetchEntitiesFunction();

    public void init(ActorContext context) {
        for (M entity : new PageDataIterable<>(getFetchEntitiesFunction(), ContextAwareActor.ENTITY_PACK_LIMIT)) {
            T entityId = (T) entity.getId();
            log.debug("[{}|{}] Creating entity actor", entityId.getEntityType(), entityId.getId());
            //TODO: remove this cast making UUIDBased subclass of EntityId an interface and vice versa.
            ActorRef actorRef = getOrCreateActor(context, entityId);
            visit(entity, actorRef);
            log.debug("[{}|{}] Entity actor created.", entityId.getEntityType(), entityId.getId());
        }
    }

    public void visit(M entity, ActorRef actorRef) {
    }

    public ActorRef getOrCreateActor(ActorContext context, T entityId) {
        return actors.computeIfAbsent(entityId, eId ->
                context.actorOf(Props.create(creator(eId))
                        .withDispatcher(getDispatcherName()), eId.toString()));
    }

    public void broadcast(Object msg) {
        actors.values().forEach(actorRef -> actorRef.tell(msg, ActorRef.noSender()));
    }

    public void remove(T id) {
        actors.remove(id);
    }

}
