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
package com.loit.dao.settings;

import com.datastax.driver.core.querybuilder.Select.Where;
import com.loit.common.data.AdminSettings;
import com.loit.common.data.id.TenantId;
import com.loit.dao.DaoUtil;
import com.loit.dao.util.NoSqlDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import com.loit.dao.model.nosql.AdminSettingsEntity;
import com.loit.dao.nosql.CassandraAbstractModelDao;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static com.loit.dao.model.ModelConstants.ADMIN_SETTINGS_BY_KEY_COLUMN_FAMILY_NAME;
import static com.loit.dao.model.ModelConstants.ADMIN_SETTINGS_COLUMN_FAMILY_NAME;
import static com.loit.dao.model.ModelConstants.ADMIN_SETTINGS_KEY_PROPERTY;

@Component
@Slf4j
@NoSqlDao
public class CassandraAdminSettingsDao extends CassandraAbstractModelDao<AdminSettingsEntity, AdminSettings> implements AdminSettingsDao {

    @Override
    protected Class<AdminSettingsEntity> getColumnFamilyClass() {
        return AdminSettingsEntity.class;
    }

    @Override
    protected String getColumnFamilyName() {
        return ADMIN_SETTINGS_COLUMN_FAMILY_NAME;
    }

    @Override
    public AdminSettings findByKey(TenantId tenantId, String key) {
        log.debug("Try to find admin settings by key [{}] ", key);
        Where query = select().from(ADMIN_SETTINGS_BY_KEY_COLUMN_FAMILY_NAME).where(eq(ADMIN_SETTINGS_KEY_PROPERTY, key));
        log.trace("Execute query {}", query);
        AdminSettingsEntity adminSettingsEntity = findOneByStatement(tenantId, query);
        log.trace("Found admin settings [{}] by key [{}]", adminSettingsEntity, key);
        return DaoUtil.getData(adminSettingsEntity);
    }

}
