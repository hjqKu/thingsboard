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
package com.loit.dao.sql.device;

import com.loit.common.data.UUIDConverter;
import com.loit.common.data.id.TenantId;
import com.loit.common.data.security.DeviceCredentials;
import com.loit.dao.DaoUtil;
import com.loit.dao.device.DeviceCredentialsDao;
import com.loit.dao.model.sql.DeviceCredentialsEntity;
import com.loit.dao.util.SqlDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import com.loit.dao.sql.JpaAbstractDao;

import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
@Component
@SqlDao
public class JpaDeviceCredentialsDao extends JpaAbstractDao<DeviceCredentialsEntity, DeviceCredentials> implements DeviceCredentialsDao {

    @Autowired
    private DeviceCredentialsRepository deviceCredentialsRepository;

    @Override
    protected Class<DeviceCredentialsEntity> getEntityClass() {
        return DeviceCredentialsEntity.class;
    }

    @Override
    protected CrudRepository<DeviceCredentialsEntity, String> getCrudRepository() {
        return deviceCredentialsRepository;
    }

    @Override
    public DeviceCredentials findByDeviceId(TenantId tenantId, UUID deviceId) {
        return DaoUtil.getData(deviceCredentialsRepository.findByDeviceId(UUIDConverter.fromTimeUUID(deviceId)));
    }

    @Override
    public DeviceCredentials findByCredentialsId(TenantId tenantId, String credentialsId) {
        return DaoUtil.getData(deviceCredentialsRepository.findByCredentialsId(credentialsId));
    }
}
