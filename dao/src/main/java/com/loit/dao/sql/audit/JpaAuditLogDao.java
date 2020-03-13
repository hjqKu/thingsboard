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
package com.loit.dao.sql.audit;

import com.google.common.util.concurrent.ListenableFuture;
import com.loit.common.data.UUIDConverter;
import com.loit.common.data.audit.ActionType;
import com.loit.common.data.audit.AuditLog;
import com.loit.common.data.id.CustomerId;
import com.loit.common.data.id.EntityId;
import com.loit.common.data.id.UserId;
import com.loit.common.data.page.TimePageLink;
import com.loit.dao.DaoUtil;
import com.loit.dao.audit.AuditLogDao;
import com.loit.dao.model.ModelConstants;
import com.loit.dao.model.sql.AuditLogEntity;
import com.loit.dao.util.SqlDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import com.loit.dao.sql.JpaAbstractDao;
import com.loit.dao.sql.JpaAbstractSearchTimeDao;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.springframework.data.jpa.domain.Specifications.where;

@Component
@SqlDao
public class JpaAuditLogDao extends JpaAbstractDao<AuditLogEntity, AuditLog> implements AuditLogDao {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Override
    protected Class<AuditLogEntity> getEntityClass() {
        return AuditLogEntity.class;
    }

    @Override
    protected CrudRepository<AuditLogEntity, String> getCrudRepository() {
        return auditLogRepository;
    }

    @Override
    public ListenableFuture<Void> saveByTenantId(AuditLog auditLog) {
        return service.submit(() -> {
            save(auditLog.getTenantId(), auditLog);
            return null;
        });
    }

    @Override
    public ListenableFuture<Void> saveByTenantIdAndEntityId(AuditLog auditLog) {
        return service.submit(() -> null);
    }

    @Override
    public ListenableFuture<Void> saveByTenantIdAndCustomerId(AuditLog auditLog) {
        return service.submit(() -> null);
    }

    @Override
    public ListenableFuture<Void> saveByTenantIdAndUserId(AuditLog auditLog) {
        return service.submit(() -> null);
    }

    @Override
    public ListenableFuture<Void> savePartitionsByTenantId(AuditLog auditLog) {
        return service.submit(() -> null);
    }

    @Override
    public List<AuditLog> findAuditLogsByTenantIdAndEntityId(UUID tenantId, EntityId entityId, List<ActionType> actionTypes, TimePageLink pageLink) {
        return findAuditLogs(tenantId, entityId, null, null, actionTypes, pageLink);
    }

    @Override
    public List<AuditLog> findAuditLogsByTenantIdAndCustomerId(UUID tenantId, CustomerId customerId, List<ActionType> actionTypes, TimePageLink pageLink) {
        return findAuditLogs(tenantId, null, customerId, null, actionTypes, pageLink);
    }

    @Override
    public List<AuditLog> findAuditLogsByTenantIdAndUserId(UUID tenantId, UserId userId, List<ActionType> actionTypes, TimePageLink pageLink) {
        return findAuditLogs(tenantId, null, null, userId, actionTypes, pageLink);
    }

    @Override
    public List<AuditLog> findAuditLogsByTenantId(UUID tenantId, List<ActionType> actionTypes, TimePageLink pageLink) {
        return findAuditLogs(tenantId, null, null, null, actionTypes, pageLink);
    }

    private List<AuditLog> findAuditLogs(UUID tenantId, EntityId entityId, CustomerId customerId, UserId userId, List<ActionType> actionTypes, TimePageLink pageLink) {
        Specification<AuditLogEntity> timeSearchSpec = JpaAbstractSearchTimeDao.getTimeSearchPageSpec(pageLink, "id");
        Specification<AuditLogEntity> fieldsSpec = getEntityFieldsSpec(tenantId, entityId, customerId, userId, actionTypes);
        Sort.Direction sortDirection = pageLink.isAscOrder() ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = new PageRequest(0, pageLink.getLimit(), sortDirection, ModelConstants.ID_PROPERTY);
        return DaoUtil.convertDataList(auditLogRepository.findAll(where(timeSearchSpec).and(fieldsSpec), pageable).getContent());
    }

    private Specification<AuditLogEntity> getEntityFieldsSpec(UUID tenantId, EntityId entityId, CustomerId customerId, UserId userId, List<ActionType> actionTypes) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (tenantId != null) {
                Predicate tenantIdPredicate = criteriaBuilder.equal(root.get("tenantId"), UUIDConverter.fromTimeUUID(tenantId));
                predicates.add(tenantIdPredicate);
            }
            if (entityId != null) {
                Predicate entityTypePredicate = criteriaBuilder.equal(root.get("entityType"), entityId.getEntityType());
                predicates.add(entityTypePredicate);
                Predicate entityIdPredicate = criteriaBuilder.equal(root.get("entityId"), UUIDConverter.fromTimeUUID(entityId.getId()));
                predicates.add(entityIdPredicate);
            }
            if (customerId != null) {
                Predicate customerIdPredicate = criteriaBuilder.equal(root.get("customerId"), UUIDConverter.fromTimeUUID(customerId.getId()));
                predicates.add(customerIdPredicate);
            }
            if (userId != null) {
                Predicate userIdPredicate = criteriaBuilder.equal(root.get("userId"), UUIDConverter.fromTimeUUID(userId.getId()));
                predicates.add(userIdPredicate);
            }
            if (actionTypes != null && !actionTypes.isEmpty()) {
                predicates.add(root.get("actionType").in(actionTypes));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[]{}));
        };
    }
}
