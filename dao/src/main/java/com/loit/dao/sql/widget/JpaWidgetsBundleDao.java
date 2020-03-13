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
package com.loit.dao.sql.widget;

import com.loit.common.data.UUIDConverter;
import com.loit.common.data.id.TenantId;
import com.loit.common.data.page.TextPageLink;
import com.loit.common.data.widget.WidgetsBundle;
import com.loit.dao.DaoUtil;
import com.loit.dao.model.ModelConstants;
import com.loit.dao.model.sql.WidgetsBundleEntity;
import com.loit.dao.util.SqlDao;
import com.loit.dao.widget.WidgetsBundleDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import com.loit.dao.sql.JpaAbstractSearchTextDao;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 4/23/2017.
 */
@Component
@SqlDao
public class JpaWidgetsBundleDao extends JpaAbstractSearchTextDao<WidgetsBundleEntity, WidgetsBundle> implements WidgetsBundleDao {

    @Autowired
    private WidgetsBundleRepository widgetsBundleRepository;

    @Override
    protected Class<WidgetsBundleEntity> getEntityClass() {
        return WidgetsBundleEntity.class;
    }

    @Override
    protected CrudRepository<WidgetsBundleEntity, String> getCrudRepository() {
        return widgetsBundleRepository;
    }

    @Override
    public WidgetsBundle findWidgetsBundleByTenantIdAndAlias(UUID tenantId, String alias) {
        return DaoUtil.getData(widgetsBundleRepository.findWidgetsBundleByTenantIdAndAlias(UUIDConverter.fromTimeUUID(tenantId), alias));
    }

    @Override
    public List<WidgetsBundle> findSystemWidgetsBundles(TenantId tenantId, TextPageLink pageLink) {
        return DaoUtil.convertDataList(
                widgetsBundleRepository
                        .findSystemWidgetsBundles(
                                ModelConstants.NULL_UUID_STR,
                                Objects.toString(pageLink.getTextSearch(), ""),
                                pageLink.getIdOffset() == null ? ModelConstants.NULL_UUID_STR : UUIDConverter.fromTimeUUID(pageLink.getIdOffset()),
                                new PageRequest(0, pageLink.getLimit())));
    }

    @Override
    public List<WidgetsBundle> findTenantWidgetsBundlesByTenantId(UUID tenantId, TextPageLink pageLink) {
        return DaoUtil.convertDataList(
                widgetsBundleRepository
                        .findTenantWidgetsBundlesByTenantId(
                                UUIDConverter.fromTimeUUID(tenantId),
                                Objects.toString(pageLink.getTextSearch(), ""),
                                pageLink.getIdOffset() == null ? ModelConstants.NULL_UUID_STR : UUIDConverter.fromTimeUUID(pageLink.getIdOffset()),
                                new PageRequest(0, pageLink.getLimit())));
    }

    @Override
    public List<WidgetsBundle> findAllTenantWidgetsBundlesByTenantId(UUID tenantId, TextPageLink pageLink) {
        return DaoUtil.convertDataList(
                widgetsBundleRepository
                        .findAllTenantWidgetsBundlesByTenantId(
                                UUIDConverter.fromTimeUUID(tenantId),
                                ModelConstants.NULL_UUID_STR,
                                Objects.toString(pageLink.getTextSearch(), ""),
                                pageLink.getIdOffset() == null ? ModelConstants.NULL_UUID_STR : UUIDConverter.fromTimeUUID(pageLink.getIdOffset()),
                                new PageRequest(0, pageLink.getLimit())));
    }
}
