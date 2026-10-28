package com.factorypulse.simulator.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Crée le topic Kafka au démarrage s'il n'existe pas encore.
 *
 * 3 partitions : les événements sont répartis entre partitions selon leur clé (machineId).
 * Tous les événements d'une même machine vont donc dans la même partition,
 * ce qui garantit qu'ils sont lus dans l'ordre où ils ont été émis.
 */
@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic machineEventsTopic(SimulatorProperties properties) {
        return TopicBuilder.name(properties.topic())
                .partitions(3)
                .replicas(1)
                .build();
    }
}
