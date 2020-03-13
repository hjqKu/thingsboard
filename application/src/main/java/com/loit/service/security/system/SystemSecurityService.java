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
package com.loit.service.security.system;

import com.loit.common.data.id.TenantId;
import com.loit.common.data.security.UserCredentials;
import com.loit.common.data.security.model.SecuritySettings;
import com.loit.dao.exception.DataValidationException;
import org.springframework.security.core.AuthenticationException;
import com.loit.common.data.id.TenantId;
import com.loit.common.data.security.UserCredentials;
import com.loit.dao.exception.DataValidationException;
import com.loit.common.data.security.model.SecuritySettings;

public interface SystemSecurityService {

    SecuritySettings getSecuritySettings(TenantId tenantId);

    SecuritySettings saveSecuritySettings(TenantId tenantId, SecuritySettings securitySettings);

    void validateUserCredentials(TenantId tenantId, UserCredentials userCredentials, String username, String password) throws AuthenticationException;

    void validatePassword(TenantId tenantId, String password, UserCredentials userCredentials) throws DataValidationException;

}
