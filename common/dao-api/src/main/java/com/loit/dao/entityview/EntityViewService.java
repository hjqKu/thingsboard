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
package com.loit.dao.entityview;

import com.google.common.util.concurrent.ListenableFuture;
import com.loit.common.data.EntitySubtype;
import com.loit.common.data.EntityView;
import com.loit.common.data.entityview.EntityViewSearchQuery;
import com.loit.common.data.id.CustomerId;
import com.loit.common.data.id.EntityId;
import com.loit.common.data.id.EntityViewId;
import com.loit.common.data.id.TenantId;
import com.loit.common.data.page.TextPageData;
import com.loit.common.data.page.TextPageLink;

import java.util.List;

/**
 * Created by Victor Basanets on 8/27/2017.
 */
public interface EntityViewService {

    EntityView saveEntityView(EntityView entityView);

    EntityView assignEntityViewToCustomer(TenantId tenantId, EntityViewId entityViewId, CustomerId customerId);

    EntityView unassignEntityViewFromCustomer(TenantId tenantId, EntityViewId entityViewId);

    void unassignCustomerEntityViews(TenantId tenantId, CustomerId customerId);

    EntityView findEntityViewById(TenantId tenantId, EntityViewId entityViewId);

    EntityView findEntityViewByTenantIdAndName(TenantId tenantId, String name);

    TextPageData<EntityView> findEntityViewByTenantId(TenantId tenantId, TextPageLink pageLink);

    TextPageData<EntityView> findEntityViewByTenantIdAndType(TenantId tenantId, TextPageLink pageLink, String type);

    TextPageData<EntityView> findEntityViewsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, TextPageLink pageLink);

    TextPageData<EntityView> findEntityViewsByTenantIdAndCustomerIdAndType(TenantId tenantId, CustomerId customerId, TextPageLink pageLink, String type);

    ListenableFuture<List<EntityView>> findEntityViewsByQuery(TenantId tenantId, EntityViewSearchQuery query);

    ListenableFuture<EntityView> findEntityViewByIdAsync(TenantId tenantId, EntityViewId entityViewId);

    ListenableFuture<List<EntityView>> findEntityViewsByTenantIdAndEntityIdAsync(TenantId tenantId, EntityId entityId);

    void deleteEntityView(TenantId tenantId, EntityViewId entityViewId);

    void deleteEntityViewsByTenantId(TenantId tenantId);

    ListenableFuture<List<EntitySubtype>> findEntityViewTypesByTenantId(TenantId tenantId);
}
