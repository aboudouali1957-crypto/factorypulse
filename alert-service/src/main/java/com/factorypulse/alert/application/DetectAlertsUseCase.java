package com.factorypulse.alert.application;

import com.factorypulse.alert.application.port.out.AlertPublisher;
import com.factorypulse.alert.application.port.out.AlertRepository;
import com.factorypulse.alert.domain.Alert;
import com.factorypulse.alert.domain.MachineEvent;

import java.util.List;

/**
 * CAS D'USAGE : "un événement de machine arrive, détecter les alertes".
 *
 * 1. retrouver le moniteur de la machine (ou le créer à partir de ses alertes encore actives) ;
 * 2. lui donner l'événement : il renvoie les alertes créées, aggravées ou résolues ;
 * 3. sauvegarder chacune de ces alertes ;
 * 4. les publier vers l'extérieur (Kafka, tableau de bord).
 *
 * Java pur : aucune dépendance à Kafka, MongoDB, WebSocket ou Spring.
 *
 * Kafka livre les événements d'une même machine dans l'ordre (même clé = même partition).
 */
public class DetectAlertsUseCase {

    private final MachineMonitors monitors;
    private final AlertRepository repository;
    private final List<AlertPublisher> publishers;

    public DetectAlertsUseCase(MachineMonitors monitors, AlertRepository repository, List<AlertPublisher> publishers) {
        this.monitors = monitors;
        this.repository = repository;
        this.publishers = List.copyOf(publishers);
    }

    /** @return les alertes créées ou modifiées par cet événement */
    public List<Alert> handle(MachineEvent event) {
        synchronized (monitors.lock()) {
            List<Alert> changes = monitors.of(event.machineId()).apply(event);
            for (Alert alert : changes) {
                repository.save(alert);
                publishers.forEach(publisher -> publisher.publish(alert));
            }
            return changes;
        }
    }
}
