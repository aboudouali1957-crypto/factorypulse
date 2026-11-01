package com.factorypulse.alert.adapter.out.kafka;

import com.factorypulse.alert.application.port.out.AlertPublisher;
import com.factorypulse.alert.config.AlertProperties;
import com.factorypulse.alert.domain.Alert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * ADAPTATEUR DE SORTIE : publie chaque alerte dans le topic Kafka "machine-alerts".
 *
 * D'autres services pourront s'y abonner sans que l'alert-service les connaisse
 * (par exemple un service de notification, ou un résumé des incidents).
 */
@Component
public class KafkaAlertPublisher implements AlertPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaAlertPublisher.class);

    private final KafkaTemplate<String, Alert> kafkaTemplate;
    private final String topic;

    public KafkaAlertPublisher(KafkaTemplate<String, Alert> kafkaTemplate, AlertProperties properties) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = properties.alertsTopic();
    }

    @Override
    public void publish(Alert alert) {
        kafkaTemplate.send(topic, alert.machineId(), alert)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.error("Échec d'envoi de l'alerte {}", alert.id(), error);
                    }
                });
    }
}
