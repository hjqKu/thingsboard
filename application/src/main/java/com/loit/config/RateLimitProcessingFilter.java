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
package com.loit.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;
import com.loit.common.data.EntityType;
import com.loit.common.data.id.CustomerId;
import com.loit.common.data.id.TenantId;
import com.loit.common.msg.tools.TbRateLimits;
import com.loit.common.msg.tools.TbRateLimitsException;
import com.loit.service.security.model.SecurityUser;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class RateLimitProcessingFilter extends GenericFilterBean {

    @Value("${server.rest.limits.tenant.enabled:false}")
    private boolean perTenantLimitsEnabled;
    @Value("${server.rest.limits.tenant.configuration:}")
    private String perTenantLimitsConfiguration;
    @Value("${server.rest.limits.customer.enabled:false}")
    private boolean perCustomerLimitsEnabled;
    @Value("${server.rest.limits.customer.configuration:}")
    private String perCustomerLimitsConfiguration;


    private ConcurrentMap<TenantId, TbRateLimits> perTenantLimits = new ConcurrentHashMap<>();
    private ConcurrentMap<CustomerId, TbRateLimits> perCustomerLimits = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        SecurityUser user = getCurrentUser();
        if (user != null && !user.isSystemAdmin()) {
            if (perTenantLimitsEnabled) {
                TbRateLimits rateLimits = perTenantLimits.computeIfAbsent(user.getTenantId(), id -> new TbRateLimits(perTenantLimitsConfiguration));
                if (!rateLimits.tryConsume()) {
                    //errorResponseHandler.handle(new TbRateLimitsException(EntityType.TENANT), (HttpServletResponse) response);
                    return;
                }
            }
            if (perCustomerLimitsEnabled && user.isCustomerUser()) {
                TbRateLimits rateLimits = perCustomerLimits.computeIfAbsent(user.getCustomerId(), id -> new TbRateLimits(perCustomerLimitsConfiguration));
                if (!rateLimits.tryConsume()) {
                    //errorResponseHandler.handle(new TbRateLimitsException(EntityType.CUSTOMER), (HttpServletResponse) response);
                    return;
                }
            }
        }
        chain.doFilter(request, response);
    }

    protected SecurityUser getCurrentUser() {
        return new SecurityUser();
    }

}
