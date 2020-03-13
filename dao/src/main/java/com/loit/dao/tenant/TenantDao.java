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
package com.loit.dao.tenant;

import com.loit.common.data.Tenant;
import com.loit.common.data.id.TenantId;
import com.loit.common.data.page.TextPageLink;
import com.loit.dao.Dao;

import java.util.List;

public interface TenantDao extends Dao<Tenant> {

    /**
     * Save or update tenant object
     *
     * @param tenant the tenant object
     * @return saved tenant object
     */
    Tenant save(TenantId tenantId, Tenant tenant);
    
    /**
     * Find tenants by region and page link.
     * 
     * @param region the region
     * @param pageLink the page link
     * @return the list of tenant objects
     */
    List<Tenant> findTenantsByRegion(TenantId tenantId, String region, TextPageLink pageLink);
    
}
