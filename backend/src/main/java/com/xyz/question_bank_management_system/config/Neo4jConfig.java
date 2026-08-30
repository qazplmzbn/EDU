package com.xyz.question_bank_management_system.config;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableConfigurationProperties(KnowledgeGraphProperties.class)
public class Neo4jConfig {
    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "app.neo4j", name = "enabled", havingValue = "true")
    public Driver neo4jDriver(KnowledgeGraphProperties properties) {
        Config config = Config.builder()
                .withConnectionTimeout(properties.getConnectionTimeout().toMillis(), TimeUnit.MILLISECONDS)
                .build();
        return GraphDatabase.driver(properties.getUri(),
                AuthTokens.basic(properties.getUsername(), properties.getPassword()), config);
    }
}
