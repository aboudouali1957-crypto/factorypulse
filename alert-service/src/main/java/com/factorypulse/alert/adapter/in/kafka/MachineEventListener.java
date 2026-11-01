package com.factorypulse.alert.adapter.in.kafka;

import com.factorypulse.alert.application.DetectAlertsUseCase;
import com.factorypulse.alert.domain.MachineEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * ADAPTATEUR D'ENTRÉE : lit les événements des machines dans Kafka et les confie au cas d'usage.
 *
 * Le kpi-service lit le MÊME topic, mais avec un autre consumer group ("kpi-service").
 * Kafka garde une position de lecture (offset) par groupe : chaque service reçoit donc
 * tous les événements, sans rien enlever à l'autre.
 */
@Component
public class MachineEventListener {

    private static final Logger log = LoggerFactory.getLogger(MachineEventListener.class);

    private final DetectAlertsUseCase useCase;

    public MachineEventListener(DetectAlertsUseCase useCase) {
        this.useCase = useCase;
    }

    @KafkaListener(topics = "${alert.events-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void onEvent(MachineEvent event) {
        useCase.handle(event).forEach(alert ->
                log.info("Alerte {} {} : {}", alert.severity(), alert.isActive() ? "active" : "résolue", alert.message()));
    }
}
