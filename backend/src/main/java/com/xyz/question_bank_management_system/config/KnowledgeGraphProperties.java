package com.xyz.question_bank_management_system.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "app.neo4j")
public class KnowledgeGraphProperties {
    private boolean enabled;
    private String uri = "bolt://localhost:7687";
    private String username = "neo4j";
    private String password = "";
    private String database = "neo4j";
    private Duration connectionTimeout = Duration.ofSeconds(5);
}
