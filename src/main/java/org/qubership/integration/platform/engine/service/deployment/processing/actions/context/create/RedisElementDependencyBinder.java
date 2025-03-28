/*
 * Copyright 2024-2025 NetCracker Technology Corporation
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

package org.qubership.integration.platform.engine.service.deployment.processing.actions.context.create;

import java.time.Duration;

import org.apache.camel.spring.SpringCamelContext;
import org.qubership.integration.platform.engine.model.ChainElementType;
import org.qubership.integration.platform.engine.model.constants.CamelConstants.ChainProperties;
import org.qubership.integration.platform.engine.model.deployment.update.DeploymentInfo;
import org.qubership.integration.platform.engine.model.deployment.update.ElementProperties;
import org.qubership.integration.platform.engine.service.VariablesService;
import org.qubership.integration.platform.engine.service.deployment.processing.ElementProcessingAction;
import org.qubership.integration.platform.engine.service.deployment.processing.qualifiers.OnAfterDeploymentContextCreated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration.JedisClientConfigurationBuilder;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Component;

@Component
@OnAfterDeploymentContextCreated
public class RedisElementDependencyBinder extends ElementProcessingAction {
    private final VariablesService variablesService;

    @Autowired
    public RedisElementDependencyBinder(VariablesService variablesService) {
        this.variablesService = variablesService;
    }

    @Override
    public boolean applicableTo(ElementProperties properties) {
        String elementType = properties.getProperties().get(ChainProperties.ELEMENT_TYPE);
        ChainElementType chainElementType = ChainElementType.fromString(elementType);
        return ChainElementType.REDIS_TRIGGER.equals(chainElementType)
            || ChainElementType.REDIS_SENDER.equals(chainElementType);
    }

    @Override
    public void apply(SpringCamelContext context, ElementProperties properties, DeploymentInfo deploymentInfo) {
        bindConnectionFactory(context, properties);
        bindSerializer(context, properties);
    }

    private void bindConnectionFactory(SpringCamelContext context, ElementProperties properties) {
        RedisStandaloneConfiguration redisConfiguration = new RedisStandaloneConfiguration();
        redisConfiguration.setHostName(getPropertyValue(properties, "host"));
        redisConfiguration.setPort(Integer.valueOf(getPropertyValue(properties, "port")));
        redisConfiguration.setUsername(getPropertyValue(properties, "username"));
        redisConfiguration.setPassword(getPropertyValue(properties, "password"));

        JedisClientConfigurationBuilder configurationBuilder = JedisClientConfiguration.builder();
        configurationBuilder
            .connectTimeout(Duration.ofMillis(Long.valueOf(getPropertyValue(properties, "connectionTimeout"))))
            .readTimeout(Duration.ofMillis(Long.valueOf(getPropertyValue(properties, "readTimeout"))));
        if (Boolean.valueOf(properties.getProperties().getOrDefault("ssl", "false"))) {
            configurationBuilder.useSsl();
        }
        JedisClientConfiguration clientConfiguration = configurationBuilder.build();
        JedisConnectionFactory connectionFactory = new JedisConnectionFactory(redisConfiguration, clientConfiguration);
        connectionFactory.start();
        context.getRegistry().bind(properties.getElementId(), RedisConnectionFactory.class, connectionFactory);
    }

    private void bindSerializer(SpringCamelContext context, ElementProperties properties) {
        String serializerClassName = properties.getProperties().get("serializer");
        try {
            Class serializerClass = Class.forName(serializerClassName);
            Object serializer = serializerClass.getDeclaredConstructor().newInstance();
            context.getRegistry().bind(properties.getElementId() + "-serializer", RedisSerializer.class, serializer);
        } catch (ReflectiveOperationException exception) {
            String message = String.format("Failed to instantiate redis serializer: %s", serializerClassName);
            throw new RuntimeException(message, exception);
        }
    }

    private String getPropertyValue(ElementProperties properties, String key) {
        return variablesService.injectVariables(properties.getProperties().get(key));
    }
}
