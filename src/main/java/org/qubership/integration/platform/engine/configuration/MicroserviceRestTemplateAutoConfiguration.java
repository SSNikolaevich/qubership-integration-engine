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

package org.qubership.integration.platform.engine.configuration;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.function.Supplier;

@AutoConfiguration
public class MicroserviceRestTemplateAutoConfiguration {

    public static final Duration CONSUL_REST_TEMPLATE_TIMEOUT = Duration.ofMillis(25_000);

    private final Duration defaultRestTemplateTimeout;

    @Autowired
    public MicroserviceRestTemplateAutoConfiguration(@Value("${qip.restclient.timeout}") long restTemplateTimeout) {
        defaultRestTemplateTimeout = Duration.ofMillis(restTemplateTimeout);
    }

    @Primary
    @Bean("restTemplateMS")
    @ConditionalOnMissingBean(name = "restTemplateMS")
    public RestTemplate restTemplateMS(RestTemplateBuilder builder) {
        return builder
            .requestFactory(getClientHttpRequestFactorySupplier())
            .setConnectTimeout(defaultRestTemplateTimeout)
            .setReadTimeout(defaultRestTemplateTimeout)
            .build();
    }

    @Bean("consulRestTemplateMS")
    @ConditionalOnMissingBean(name = "consulRestTemplateMS")
    public RestTemplate consulRestTemplateMS(RestTemplateBuilder builder) {
        return builder
            .requestFactory(getClientHttpRequestFactorySupplier())
            .setConnectTimeout(CONSUL_REST_TEMPLATE_TIMEOUT)
            .setReadTimeout(CONSUL_REST_TEMPLATE_TIMEOUT)
            .build();
    }

    private static @NotNull Supplier<ClientHttpRequestFactory> getClientHttpRequestFactorySupplier() {
        return () -> new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory());
    }
}
