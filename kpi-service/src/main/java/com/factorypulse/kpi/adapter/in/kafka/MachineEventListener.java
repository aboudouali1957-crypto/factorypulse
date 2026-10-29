package com.factorypulse.kpi.adapter.in.kafka;

import com.factorypulse.kpi.application.ProcessMachineEventUseCase;
import com.factorypulse.kpi.domain.MachineEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * ADAPTATEUR D'ENTRÉE : lit les événements dans le topic Kafka et les confie au cas d'usage.
 *
 * Spring Kafka appelle {@link #onEvent} pour chaque message reçu. Le JSON est déjà converti
 * en {@link MachineEvent} (voir application.yml). Après le traitement, Spring Kafka enregistre
 * l'offset : si le service redémarre, il reprend là où il s'était arrêté.
 */
@Component
public class MachineEventListener {

    private static final Logger log = LoggerFactory.getLogger(MachineEventListener.class);

    private final ProcessMachineEventUseCase useCase;

    public MachineEventListener(ProcessMachineEventUseCase useCase) {
        this.useCase = useCase;
    }

    @KafkaListener(topics = "${kpi.topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void onEvent(MachineEvent event) {
        useCase.handle(event).ifPresentOrElse(
                snapshot -> log.debug("{} : TRS = {}", snapshot.machineId(), snapshot.oee()),
                () -> log.warn("Machine inconnue ignorée : {}", event.machineId()));
    }
}
