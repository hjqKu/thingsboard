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
package com.loit.dao.sql.entityview;

import com.google.common.util.concurrent.ListenableFuture;
import com.loit.dao.DaoUtil;
import com.loit.dao.entityview.EntityViewDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import com.loit.common.data.EntitySubtype;
import com.loit.common.data.EntityType;
import com.loit.common.data.EntityView;
import com.loit.common.data.UUIDConverter;
import com.loit.common.data.id.TenantId;
import com.loit.common.data.page.TextPageLink;
import com.loit.dao.DaoUtil;
import com.loit.dao.entityview.EntityViewDao;
import com.loit.dao.model.sql.EntityViewEntity;
import com.loit.dao.sql.JpaAbstractSearchTextDao;
import com.loit.dao.util.SqlDao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.loit.common.data.UUIDConverter.fromTimeUUID;
import static com.loit.dao.model.ModelConstants.NULL_UUID_STR;

/**
 * Created by Victor Basanets on 8/31/2017.
 */
@Component
@SqlDao
public class JpaEntityViewDao extends JpaAbstractSearchTextDao<EntityViewEntity, EntityView>
        implements EntityViewDao {

    @Autowired
    private EntityViewRepository entityViewRepository;

    @Override
    protected Class<EntityViewEntity> getEntityClass() {
        return EntityViewEntity.class;
    }

    @Override
    protected CrudRepository<EntityViewEntity, String> getCrudRepository() {
        return entityViewRepository;
    }

    @Override
    public List<EntityView> findEntityViewsByTenantId(UUID tenantId, TextPageLink pageLink) {
        return DaoUtil.convertDataList(
                entityViewRepository.findByTenantId(
                        fromTimeUUID(tenantId),
                        Objects.toString(pageLink.getTextSearch(), ""),
                        pageLink.getIdOffset() == null ? NULL_UUID_STR : fromTimeUUID(pageLink.getIdOffset()),
                        new PageRequest(0, pageLink.getLimit())));
    }

    @Override
    public List<EntityView> findEntityViewsByTenantIdAndType(UUID tenantId, String type, TextPageLink pageLink) {
        return DaoUtil.convertDataList(
                entityViewRepository.findByTenantIdAndType(
                        fromTimeUUID(tenantId),
                        type,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        pageLink.getIdOffset() == null ? NULL_UUID_STR : fromTimeUUID(pageLink.getIdOffset()),
                        new PageRequest(0, pageLink.getLimit())));
    }

    @Override
    public Optional<EntityView> findEntityViewByTenantIdAndName(UUID tenantId, String name) {
        return Optional.ofNullable(
                DaoUtil.getData(entityViewRepository.findByTenantIdAndName(fromTimeUUID(tenantId), name)));
    }

    @Override
    public List<EntityView> findEntityViewsByTenantIdAndCustomerId(UUID tenantId,
                                                                   UUID customerId,
                                                                   TextPageLink pageLink) {
        return DaoUtil.convertDataList(
                entityViewRepository.findByTenantIdAndCustomerId(
                        fromTimeUUID(tenantId),
                        fromTimeUUID(customerId),
                        Objects.toString(pageLink.getTextSearch(), ""),
                        pageLink.getIdOffset() == null ? NULL_UUID_STR : fromTimeUUID(pageLink.getIdOffset()),
                        new PageRequest(0, pageLink.getLimit())
                ));
    }

    @Override
    public List<EntityView> findEntityViewsByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type, TextPageLink pageLink) {
        return DaoUtil.convertDataList(
                entityViewRepository.findByTenantIdAndCustomerIdAndType(
                        fromTimeUUID(tenantId),
                        fromTimeUUID(customerId),
                        type,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        pageLink.getIdOffset() == null ? NULL_UUID_STR : fromTimeUUID(pageLink.getIdOffset()),
                        new PageRequest(0, pageLink.getLimit())
                ));
    }

    @Override
    public ListenableFuture<List<EntityView>> findEntityViewsByTenantIdAndEntityIdAsync(UUID tenantId, UUID entityId) {
        return service.submit(() -> DaoUtil.convertDataList(
                entityViewRepository.findAllByTenantIdAndEntityId(UUIDConverter.fromTimeUUID(tenantId), UUIDConverter.fromTimeUUID(entityId))));
    }

    @Override
    public ListenableFuture<List<EntitySubtype>> findTenantEntityViewTypesAsync(UUID tenantId) {
        return service.submit(() -> convertTenantEntityViewTypesToDto(tenantId, entityViewRepository.findTenantEntityViewTypes(fromTimeUUID(tenantId))));
    }

    private List<EntitySubtype> convertTenantEntityViewTypesToDto(UUID tenantId, List<String> types) {
        List<EntitySubtype> list = Collections.emptyList();
        if (types != null && !types.isEmpty()) {
            list = new ArrayList<>();
            for (String type : types) {
                list.add(new EntitySubtype(new TenantId(tenantId), EntityType.ENTITY_VIEW, type));
            }
        }
        return list;
    }
}
