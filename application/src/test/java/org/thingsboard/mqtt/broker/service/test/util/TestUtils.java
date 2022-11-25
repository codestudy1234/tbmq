/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.mqtt.broker.service.test.util;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.thingsboard.mqtt.broker.common.data.ClientType;
import org.thingsboard.mqtt.broker.common.data.client.credentials.BasicMqttCredentials;
import org.thingsboard.mqtt.broker.common.data.client.credentials.PubSubAuthorizationRules;
import org.thingsboard.mqtt.broker.common.data.security.ClientCredentialsType;
import org.thingsboard.mqtt.broker.common.data.security.MqttClientCredentials;
import org.thingsboard.mqtt.broker.common.util.JacksonUtil;
import org.thingsboard.mqtt.broker.service.subscription.TopicSubscription;

import java.util.Collection;
import java.util.List;

public class TestUtils {

    public static void clearPersistedClient(MqttClient persistedClient, MqttClient newClientToClear) throws Exception {
        if (persistedClient.isConnected()) {
            persistedClient.disconnect();
            persistedClient.close();
        }
        MqttConnectOptions connectOptions = new MqttConnectOptions();
        connectOptions.setCleanSession(true);
        newClientToClear.connect(connectOptions);
        newClientToClear.disconnect();
        newClientToClear.close();
    }

    public static String[] getTopicNames(Collection<TopicSubscription> topicSubscriptions) {
        return topicSubscriptions.stream().map(TopicSubscription::getTopic).toArray(String[]::new);
    }

    public static int[] getQoSLevels(Collection<TopicSubscription> topicSubscriptions) {
        return topicSubscriptions.stream().map(TopicSubscription::getQos).mapToInt(x -> x).toArray();
    }

    public static MqttClientCredentials createApplicationClientCredentials(String clientId, String username) {
        return getMqttClientCredentials(clientId, username, null, ClientType.APPLICATION, null);
    }

    public static MqttClientCredentials createDeviceClientCredentials(String clientId, String username) {
        return getMqttClientCredentials(clientId, username, null, ClientType.DEVICE, null);
    }

    public static MqttClientCredentials createDeviceClientCredentialsWithAuth(String clientId, List<String> authRulePatterns) {
        return getMqttClientCredentials(clientId, null, null, ClientType.DEVICE, authRulePatterns);
    }

    public static MqttClientCredentials createDeviceClientCredentialsWithPass(String clientId, String password) {
        return getMqttClientCredentials(clientId, null, password, ClientType.DEVICE, null);
    }

    private static MqttClientCredentials getMqttClientCredentials(String clientId, String username, String password,
                                                                  ClientType clientType, List<String> patterns) {
        return newClientCredentials(BasicMqttCredentials.newInstance(clientId, username, password, patterns), clientType);
    }

    public static MqttClientCredentials createDeviceClientCredentialsWithPubSubAuth(String clientId, String username, String password,
                                                                                    PubSubAuthorizationRules authorizationRules) {
        return newClientCredentials(new BasicMqttCredentials(clientId, username, password, authorizationRules), ClientType.DEVICE);
    }

    private static MqttClientCredentials newClientCredentials(BasicMqttCredentials basicMqttCredentials, ClientType clientType) {
        MqttClientCredentials mqttClientCredentials = new MqttClientCredentials();
        mqttClientCredentials.setClientType(clientType);
        mqttClientCredentials.setCredentialsType(ClientCredentialsType.MQTT_BASIC);
        mqttClientCredentials.setName("credentials");
        mqttClientCredentials.setCredentialsValue(JacksonUtil.toString(basicMqttCredentials));
        return mqttClientCredentials;
    }
}
