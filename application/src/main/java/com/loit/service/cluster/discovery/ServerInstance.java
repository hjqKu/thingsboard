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
package com.loit.service.cluster.discovery;

import com.loit.common.msg.cluster.ServerAddress;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * @author Andrew Shvayka
 */
@ToString
@EqualsAndHashCode(exclude = {"serverInfo", "serverAddress"})
public final class ServerInstance implements Comparable<ServerInstance> {

    @Getter
    private final String host;
    @Getter
    private final int port;
    @Getter
    private final ServerAddress serverAddress;

    public ServerInstance(ServerAddress serverAddress) {
        this.serverAddress = serverAddress;
        this.host = serverAddress.getHost();
        this.port = serverAddress.getPort();
    }

    @Override
    public int compareTo(ServerInstance o) {
        return this.serverAddress.compareTo(o.serverAddress);
    }
}
