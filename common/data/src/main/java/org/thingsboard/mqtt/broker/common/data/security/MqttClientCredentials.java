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
package org.thingsboard.mqtt.broker.common.data.security;

import lombok.*;
import org.thingsboard.mqtt.broker.common.data.BaseData;
import org.thingsboard.mqtt.broker.common.data.ClientType;

import java.util.UUID;

@Data
@ToString
@EqualsAndHashCode(callSuper = true)
public class MqttClientCredentials extends BaseData {
    private String credentialsId;
    private String name;
    private ClientType clientType;
    private ClientCredentialsType credentialsType;
    private String credentialsValue;

    public MqttClientCredentials() {
    }

    public MqttClientCredentials(UUID id) {
        super(id);
    }

    public MqttClientCredentials(MqttClientCredentials mqttClientCredentials) {
        super(mqttClientCredentials);
        this.name = mqttClientCredentials.name;
        this.clientType = mqttClientCredentials.clientType;
        this.credentialsId = mqttClientCredentials.credentialsId;
        this.credentialsType = mqttClientCredentials.credentialsType;
    }

}
