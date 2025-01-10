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

package org.qubership.integration.platform.engine.configuration.datasource;

import org.qubership.integration.platform.engine.configuration.datasource.properties.HikariConfigProperties;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.SharedCacheMode;
import java.util.Properties;
import javax.sql.DataSource;
import lombok.Getter;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Getter
@AutoConfiguration
@EnableJpaAuditing
@EnableTransactionManagement
@EnableJpaRepositories(
    basePackages = "org.qubership.integration.platform.engine.persistence.shared.repository",
    transactionManagerRef = "checkpointTransactionManager",
    entityManagerFactoryRef = "checkpointEntityManagerFactory"
)
@EnableConfigurationProperties({JpaProperties.class, HikariConfigProperties.class})
public class PersistenceCheckpointConfiguration {

    public static final String JPA_ENTITIES_PACKAGE_SCAN =
        "org.qubership.integration.platform.engine.persistence.shared.entity";
    private final JpaProperties jpaProperties;
    private final HikariConfigProperties properties;

    @Autowired
    public PersistenceCheckpointConfiguration(JpaProperties jpaProperties,
        HikariConfigProperties properties) {
        this.jpaProperties = jpaProperties;
        this.properties = properties;
    }

    @Bean("checkpointDataSource")
    @ConditionalOnMissingBean(name = "checkpointDataSource")
    public DataSource checkpointDataSource() {
        return new HikariDataSource(properties.getDatasource("checkpoints-datasource"));
    }

    @Bean
    JdbcTemplate checkpointJdbcTemplate(@Qualifier("checkpointDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    NamedParameterJdbcTemplate checkpointNamedParameterJdbcTemplate(
        @Qualifier("checkpointDataSource") DataSource dataSource
    ) {
        return new NamedParameterJdbcTemplate(dataSource);
    }

    @Bean("checkpointEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean checkpointEntityManagerFactory(
        @Qualifier("checkpointDataSource") DataSource checkpointDataSource
    ) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();

        HibernateJpaVendorAdapter jpaVendorAdapter = new HibernateJpaVendorAdapter();
        jpaVendorAdapter.setDatabase(jpaProperties.getDatabase());
        jpaVendorAdapter.setGenerateDdl(jpaProperties.isGenerateDdl());
        jpaVendorAdapter.setShowSql(jpaProperties.isShowSql());

        em.setDataSource(checkpointDataSource);
        em.setJpaVendorAdapter(jpaVendorAdapter);
        em.setPackagesToScan(JPA_ENTITIES_PACKAGE_SCAN);
        em.setPersistenceProvider(new HibernatePersistenceProvider());
        em.setJpaProperties(additionalProperties());
        em.setSharedCacheMode(SharedCacheMode.ENABLE_SELECTIVE);
        return em;
    }

    @Bean("checkpointTransactionManager")
    public PlatformTransactionManager checkpointTransactionManager(
        @Qualifier("checkpointEntityManagerFactory") LocalContainerEntityManagerFactoryBean checkpointEntityManagerFactory
    ) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(checkpointEntityManagerFactory.getObject());
        return transactionManager;
    }

    private Properties additionalProperties() {
        Properties properties = new Properties();
        if (jpaProperties != null) {
            properties.putAll(jpaProperties.getProperties());
        }
        return properties;
    }
}