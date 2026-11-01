package com.factorypulse.alert.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Crée le topic des alertes au démarrage s'il n'existe pas encore.
 * Comme pour les événements : 3 partitions, clé = machineId.
 */
@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic machineAlertsTopic(AlertProperties properties) {
        return TopicBuilder.name(properties.alertsTopic())
                .partitions(3)
                .replicas(1)
                .build();
    }
}
