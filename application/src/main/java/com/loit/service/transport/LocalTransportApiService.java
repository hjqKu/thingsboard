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
package com.loit.service.transport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.loit.common.data.Device;
import com.loit.common.data.id.DeviceId;
import com.loit.common.data.id.TenantId;
import com.loit.common.data.relation.EntityRelation;
import com.loit.common.data.security.DeviceCredentials;
import com.loit.common.data.security.DeviceCredentialsType;
import com.loit.dao.device.DeviceCredentialsService;
import com.loit.dao.device.DeviceService;
import com.loit.dao.relation.RelationService;
import com.loit.service.executors.DbCallbackExecutorService;
import com.loit.service.state.DeviceStateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.loit.common.data.Device;
import com.loit.common.data.id.DeviceId;
import com.loit.common.data.id.TenantId;
import com.loit.common.data.relation.EntityRelation;
import com.loit.common.data.security.DeviceCredentials;
import com.loit.common.data.security.DeviceCredentialsType;
import com.loit.dao.device.DeviceCredentialsService;
import com.loit.dao.device.DeviceService;
import com.loit.dao.relation.RelationService;
import com.loit.kafka.TBKafkaConsumerTemplate;
import com.loit.kafka.TBKafkaProducerTemplate;
import com.loit.kafka.TbKafkaResponseTemplate;
import com.loit.kafka.TbKafkaSettings;
import com.loit.service.cluster.discovery.DiscoveryService;
import com.loit.service.executors.DbCallbackExecutorService;
import com.loit.service.state.DeviceStateService;
import org.thingsboard.server.gen.transport.TransportProtos;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by ashvayka on 05.10.18.
 */
@Slf4j
@Service
public class LocalTransportApiService implements TransportApiService {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private RelationService relationService;

    @Autowired
    private DeviceCredentialsService deviceCredentialsService;

    @Autowired
    private DeviceStateService deviceStateService;

    @Autowired
    private DbCallbackExecutorService dbCallbackExecutorService;

    private ReentrantLock deviceCreationLock = new ReentrantLock();

    @Override
    public ListenableFuture<TransportProtos.TransportApiResponseMsg> handle(TransportProtos.TransportApiRequestMsg transportApiRequestMsg) {
        if (transportApiRequestMsg.hasValidateTokenRequestMsg()) {
            TransportProtos.ValidateDeviceTokenRequestMsg msg = transportApiRequestMsg.getValidateTokenRequestMsg();
            return validateCredentials(msg.getToken(), DeviceCredentialsType.ACCESS_TOKEN);
        } else if (transportApiRequestMsg.hasValidateX509CertRequestMsg()) {
            TransportProtos.ValidateDeviceX509CertRequestMsg msg = transportApiRequestMsg.getValidateX509CertRequestMsg();
            return validateCredentials(msg.getHash(), DeviceCredentialsType.X509_CERTIFICATE);
        } else if (transportApiRequestMsg.hasGetOrCreateDeviceRequestMsg()) {
            return handle(transportApiRequestMsg.getGetOrCreateDeviceRequestMsg());
        }
        return getEmptyTransportApiResponseFuture();
    }

    private ListenableFuture<TransportProtos.TransportApiResponseMsg> validateCredentials(String credentialsId, DeviceCredentialsType credentialsType) {
        //TODO: Make async and enable caching
        DeviceCredentials credentials = deviceCredentialsService.findDeviceCredentialsByCredentialsId(credentialsId);
        if (credentials != null && credentials.getCredentialsType() == credentialsType) {
            return getDeviceInfo(credentials.getDeviceId(), credentials);
        } else {
            return getEmptyTransportApiResponseFuture();
        }
    }

    private ListenableFuture<TransportProtos.TransportApiResponseMsg> handle(TransportProtos.GetOrCreateDeviceFromGatewayRequestMsg requestMsg) {
        DeviceId gatewayId = new DeviceId(new UUID(requestMsg.getGatewayIdMSB(), requestMsg.getGatewayIdLSB()));
        ListenableFuture<Device> gatewayFuture = deviceService.findDeviceByIdAsync(TenantId.SYS_TENANT_ID, gatewayId);
        return Futures.transform(gatewayFuture, gateway -> {
            deviceCreationLock.lock();
            try {
                Device device = deviceService.findDeviceByTenantIdAndName(gateway.getTenantId(), requestMsg.getDeviceName());
                if (device == null) {
                    device = new Device();
                    device.setTenantId(gateway.getTenantId());
                    device.setName(requestMsg.getDeviceName());
                    device.setType(requestMsg.getDeviceType());
                    device.setCustomerId(gateway.getCustomerId());
                    device = deviceService.saveDevice(device);
                    relationService.saveRelationAsync(TenantId.SYS_TENANT_ID, new EntityRelation(gateway.getId(), device.getId(), "Created"));
                    deviceStateService.onDeviceAdded(device);
                }
                return TransportProtos.TransportApiResponseMsg.newBuilder()
                        .setGetOrCreateDeviceResponseMsg(TransportProtos.GetOrCreateDeviceFromGatewayResponseMsg.newBuilder().setDeviceInfo(getDeviceInfoProto(device)).build()).build();
            } catch (JsonProcessingException e) {
                log.warn("[{}] Failed to lookup device by gateway id and name", gatewayId, requestMsg.getDeviceName(), e);
                throw new RuntimeException(e);
            } finally {
                deviceCreationLock.unlock();
            }
        }, dbCallbackExecutorService);
    }


    private ListenableFuture<TransportProtos.TransportApiResponseMsg> getDeviceInfo(DeviceId deviceId, DeviceCredentials credentials) {
        return Futures.transform(deviceService.findDeviceByIdAsync(TenantId.SYS_TENANT_ID, deviceId), device -> {
            if (device == null) {
                log.trace("[{}] Failed to lookup device by id", deviceId);
                return getEmptyTransportApiResponse();
            }
            try {
                TransportProtos.ValidateDeviceCredentialsResponseMsg.Builder builder = TransportProtos.ValidateDeviceCredentialsResponseMsg.newBuilder();
                builder.setDeviceInfo(getDeviceInfoProto(device));
                if(!StringUtils.isEmpty(credentials.getCredentialsValue())){
                    builder.setCredentialsBody(credentials.getCredentialsValue());
                }
                return TransportProtos.TransportApiResponseMsg.newBuilder()
                        .setValidateTokenResponseMsg(builder.build()).build();
            } catch (JsonProcessingException e) {
                log.warn("[{}] Failed to lookup device by id", deviceId, e);
                return getEmptyTransportApiResponse();
            }
        });
    }

    private TransportProtos.DeviceInfoProto getDeviceInfoProto(Device device) throws JsonProcessingException {
        return TransportProtos.DeviceInfoProto.newBuilder()
                .setTenantIdMSB(device.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(device.getTenantId().getId().getLeastSignificantBits())
                .setDeviceIdMSB(device.getId().getId().getMostSignificantBits())
                .setDeviceIdLSB(device.getId().getId().getLeastSignificantBits())
                .setDeviceName(device.getName())
                .setDeviceType(device.getType())
                .setAdditionalInfo(mapper.writeValueAsString(device.getAdditionalInfo()))
                .build();
    }

    private ListenableFuture<TransportProtos.TransportApiResponseMsg> getEmptyTransportApiResponseFuture() {
        return Futures.immediateFuture(getEmptyTransportApiResponse());
    }

    private TransportProtos.TransportApiResponseMsg getEmptyTransportApiResponse() {
        return TransportProtos.TransportApiResponseMsg.newBuilder()
                .setValidateTokenResponseMsg(TransportProtos.ValidateDeviceCredentialsResponseMsg.getDefaultInstance()).build();
    }
}
