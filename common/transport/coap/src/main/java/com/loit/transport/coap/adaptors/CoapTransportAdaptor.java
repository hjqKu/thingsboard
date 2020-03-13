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
package com.loit.transport.coap.adaptors;

import com.loit.gen.transport.TransportProtos;
import com.loit.transport.coap.CoapTransportResource;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import com.loit.common.transport.adaptor.AdaptorException;
import com.loit.gen.transport.TransportProtos;
import com.loit.transport.coap.CoapTransportResource;

import java.util.UUID;

public interface CoapTransportAdaptor {

    TransportProtos.PostTelemetryMsg convertToPostTelemetry(UUID sessionId, Request inbound) throws AdaptorException;

    TransportProtos.PostAttributeMsg convertToPostAttributes(UUID sessionId, Request inbound) throws AdaptorException;

    TransportProtos.GetAttributeRequestMsg convertToGetAttributes(UUID sessionId, Request inbound) throws AdaptorException;

    TransportProtos.ToDeviceRpcResponseMsg convertToDeviceRpcResponse(UUID sessionId, Request inbound) throws AdaptorException;

    TransportProtos.ToServerRpcRequestMsg convertToServerRpcRequest(UUID sessionId, Request inbound) throws AdaptorException;

    TransportProtos.ClaimDeviceMsg convertToClaimDevice(UUID sessionId, Request inbound, TransportProtos.SessionInfoProto sessionInfo) throws AdaptorException;

    Response convertToPublish(CoapTransportResource.CoapSessionListener session, TransportProtos.GetAttributeResponseMsg responseMsg) throws AdaptorException;

    Response convertToPublish(CoapTransportResource.CoapSessionListener session, TransportProtos.AttributeUpdateNotificationMsg notificationMsg) throws AdaptorException;

    Response convertToPublish(CoapTransportResource.CoapSessionListener session, TransportProtos.ToDeviceRpcRequestMsg rpcRequest) throws AdaptorException;

    Response convertToPublish(CoapTransportResource.CoapSessionListener coapSessionListener, TransportProtos.ToServerRpcResponseMsg msg) throws AdaptorException;

}
