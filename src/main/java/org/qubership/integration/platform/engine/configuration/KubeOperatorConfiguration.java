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

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.credentials.AccessTokenAuthentication;
import io.kubernetes.client.util.credentials.TokenFileAuthentication;
import lombok.extern.slf4j.Slf4j;
import org.qubership.integration.platform.engine.kubernetes.KubeOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;


@Slf4j
@Configuration
public class KubeOperatorConfiguration {
    private final String uri;
    private final String namespace;
    private final String devToken;
    private final String tokenFilePath;
    private final String cert;


    @Autowired
    public KubeOperatorConfiguration(
            @Value("${kubernetes.cluster.uri}") String uri,
            @Value("${kubernetes.cluster.namespace}") String namespace,
            @Value("${kubernetes.service-account.token-file-path}") String tokenFilePath,
            @Value("${kubernetes.service-account.cert}") String cert,
            @Value("${kubernetes.cluster.token:#{null}}") Optional<String> devToken) {

        this.uri = uri;
        this.namespace = namespace;
        this.tokenFilePath = tokenFilePath;
        this.devToken = devToken.orElse(null);
        this.cert = cert;
    }

    /**
     * Inside cluster ApiClient configuration
     * Uses the service account created during deployment for the catalog pod
     */
    @Bean
    @ConditionalOnProperty(prefix = "kubernetes", name = "devmode", havingValue = "false", matchIfMissing = true)
    public KubeOperator kubeOperator() {
        try {
            log.info("Creating KubernetesOperator bean in PROD mode");

            ApiClient client = new ClientBuilder()
                    .setVerifyingSsl(false)
                    .setBasePath(uri)
                    .setCertificateAuthority(Files.readAllBytes(Paths.get(cert)))
                    .setAuthentication(new TokenFileAuthentication(tokenFilePath))
                    .build();

            return new KubeOperator(client, namespace, false);
        } catch (Exception e) {
            log.error("Invalid k8s cluster parameters, can't initialize k8s API. {}", e.getMessage());
            return new KubeOperator();
        }
    }

    /**
     * Outside cluster ApiClient configuration
     * Uses the cluster account token
     */
    @Bean
    @ConditionalOnProperty(prefix = "kubernetes", name = "devmode", havingValue = "true")
    public KubeOperator kubeOperatorDev() {
        try {
            log.info("Creating KubernetesOperator bean in DEV mode");

            ApiClient client = new ClientBuilder()
                    .setVerifyingSsl(false)
                    .setBasePath(uri)
                    .setAuthentication(new AccessTokenAuthentication(devToken))
                    .build();

            return new KubeOperator(client, namespace, true);
        } catch (Exception e) {
            log.error("Invalid k8s cluster parameters, can't initialize k8s API. {}", e.getMessage());
            return new KubeOperator();
        }
    }
}
