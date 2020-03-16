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
package com.loit.service.transport.msg;

import com.loit.common.data.id.DeviceId;
import com.loit.common.data.id.TenantId;
import com.loit.common.msg.aware.DeviceAwareMsg;
import com.loit.common.msg.aware.TenantAwareMsg;
import lombok.Data;
import com.loit.common.msg.MsgType;
import com.loit.common.msg.TbActorMsg;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.io.Serializable;
import java.util.UUID;

/**
 * Created by ashvayka on 09.10.18.
 */
@Data
public class TransportToDeviceActorMsgWrapper implements TbActorMsg, DeviceAwareMsg, TenantAwareMsg, Serializable {

    private final TenantId tenantId;
    private final DeviceId deviceId;
    private final TransportProtos.TransportToDeviceActorMsg msg;

    public TransportToDeviceActorMsgWrapper(TransportProtos.TransportToDeviceActorMsg msg) {
        this.msg = msg;
        this.tenantId = new TenantId(new UUID(msg.getSessionInfo().getTenantIdMSB(), msg.getSessionInfo().getTenantIdLSB()));
        this.deviceId = new DeviceId(new UUID(msg.getSessionInfo().getDeviceIdMSB(), msg.getSessionInfo().getDeviceIdLSB()));
    }

    @Override
    public MsgType getMsgType() {
        return MsgType.TRANSPORT_TO_DEVICE_ACTOR_MSG;
    }
}
