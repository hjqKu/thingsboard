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
package com.loit.dao.sql.settings;

import com.loit.common.data.AdminSettings;
import com.loit.common.data.id.TenantId;
import com.loit.dao.DaoUtil;
import com.loit.dao.model.sql.AdminSettingsEntity;
import com.loit.dao.settings.AdminSettingsDao;
import com.loit.dao.util.SqlDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import com.loit.dao.sql.JpaAbstractDao;

@Component
@Slf4j
@SqlDao
public class JpaAdminSettingsDao extends JpaAbstractDao<AdminSettingsEntity, AdminSettings> implements AdminSettingsDao {

    @Autowired
    private AdminSettingsRepository adminSettingsRepository;

    @Override
    protected Class<AdminSettingsEntity> getEntityClass() {
        return AdminSettingsEntity.class;
    }

    @Override
    protected CrudRepository<AdminSettingsEntity, String> getCrudRepository() {
        return adminSettingsRepository;
    }

    @Override
    public AdminSettings findByKey(TenantId tenantId, String key) {
        return DaoUtil.getData(adminSettingsRepository.findByKey(key));
    }
}
