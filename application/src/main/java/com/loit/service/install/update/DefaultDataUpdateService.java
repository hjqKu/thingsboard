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
package com.loit.service.install.update;

import com.loit.common.data.Tenant;
import com.loit.common.data.page.TextPageData;
import com.loit.common.data.page.TextPageLink;
import com.loit.dao.rule.RuleChainService;
import com.loit.dao.tenant.TenantService;
import com.loit.service.install.InstallScripts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import com.loit.common.data.rule.RuleChain;

@Service
@Profile("install")
@Slf4j
public class DefaultDataUpdateService implements DataUpdateService {

    @Autowired
    private TenantService tenantService;

    @Autowired
    private RuleChainService ruleChainService;

    @Autowired
    private InstallScripts installScripts;

    @Override
    public void updateData(String fromVersion) throws Exception {
        switch (fromVersion) {
            case "1.4.0":
                log.info("Updating data from version 1.4.0 to 2.0.0 ...");
                tenantsDefaultRuleChainUpdater.updateEntities(null);
                break;
            default:
                throw new RuntimeException("Unable to update data, unsupported fromVersion: " + fromVersion);
        }
    }

    private PaginatedUpdater<String, Tenant> tenantsDefaultRuleChainUpdater =
            new PaginatedUpdater<String, Tenant>() {

                @Override
                protected TextPageData<Tenant> findEntities(String region, TextPageLink pageLink) {
                    return tenantService.findTenants(pageLink);
                }

                @Override
                protected void updateEntity(Tenant tenant) {
                    try {
                        RuleChain ruleChain = ruleChainService.getRootTenantRuleChain(tenant.getId());
                        if (ruleChain == null) {
                            installScripts.createDefaultRuleChains(tenant.getId());
                        }
                    } catch (Exception e) {
                        log.error("Unable to update Tenant", e);
                    }
                }
            };

}