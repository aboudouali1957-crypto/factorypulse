package com.factorypulse.simulator.adapter.out.kafka;

import com.factorypulse.simulator.application.port.out.MachineEventPublisher;
import com.factorypulse.simulator.config.SimulatorProperties;
import com.factorypulse.simulator.domain.MachineEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * ADAPTATEUR DE SORTIE : implémente le port {@link MachineEventPublisher} avec Kafka.
 *
 * La clé du message est le machineId : Kafka envoie toujours une même clé
 * dans la même partition, donc l'ordre des événements d'une machine est conservé.
 */
@Component
public class KafkaMachineEventPublisher implements MachineEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaMachineEventPublisher.class);

    private final KafkaTemplate<String, MachineEvent> kafkaTemplate;
    private final String topic;

    public KafkaMachineEventPublisher(KafkaTemplate<String, MachineEvent> kafkaTemplate,
                                      SimulatorProperties properties) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = properties.topic();
    }

    @Override
    public void publish(MachineEvent event) {
        kafkaTemplate.send(topic, event.machineId(), event)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.error("Échec d'envoi de l'événement {} ({})", event.eventId(), event.type(), error);
                    }
                });
    }
}
