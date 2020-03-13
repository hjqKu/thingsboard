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
package com.loit.dao.sql.rule;

import com.loit.common.data.page.TextPageLink;
import com.loit.dao.DaoUtil;
import com.loit.dao.rule.RuleChainDao;
import com.loit.dao.util.SqlDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import com.loit.common.data.UUIDConverter;
import com.loit.common.data.page.TextPageLink;
import com.loit.common.data.rule.RuleChain;
import com.loit.dao.DaoUtil;
import com.loit.dao.model.sql.RuleChainEntity;
import com.loit.dao.rule.RuleChainDao;
import com.loit.dao.sql.JpaAbstractSearchTextDao;
import com.loit.dao.util.SqlDao;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.loit.dao.model.ModelConstants.NULL_UUID_STR;

@Slf4j
@Component
@SqlDao
public class JpaRuleChainDao extends JpaAbstractSearchTextDao<RuleChainEntity, RuleChain> implements RuleChainDao {

    @Autowired
    private RuleChainRepository ruleChainRepository;

    @Override
    protected Class getEntityClass() {
        return RuleChainEntity.class;
    }

    @Override
    protected CrudRepository getCrudRepository() {
        return ruleChainRepository;
    }

    @Override
    public List<RuleChain> findRuleChainsByTenantId(UUID tenantId, TextPageLink pageLink) {
        return DaoUtil.convertDataList(ruleChainRepository
                .findByTenantId(
                        UUIDConverter.fromTimeUUID(tenantId),
                        Objects.toString(pageLink.getTextSearch(), ""),
                        pageLink.getIdOffset() == null ? NULL_UUID_STR : UUIDConverter.fromTimeUUID(pageLink.getIdOffset()),
                        new PageRequest(0, pageLink.getLimit())));
    }

}
