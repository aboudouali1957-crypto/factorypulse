package com.factorypulse.alert.application;

import com.factorypulse.alert.application.port.out.AlertPublisher;
import com.factorypulse.alert.application.port.out.AlertRepository;
import com.factorypulse.alert.domain.Alert;
import com.factorypulse.alert.domain.AlertRules;
import com.factorypulse.alert.domain.MachineEvent;
import com.factorypulse.alert.domain.MachineMonitor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 * Les moniteurs sont gardés en mémoire. Ce n'est pas un problème : Kafka livre les événements
 * d'une même machine dans l'ordre (même clé = même partition) et un seul thread les traite.
 */
public class DetectAlertsUseCase {

    private final AlertRules rules;
    private final AlertRepository repository;
    private final List<AlertPublisher> publishers;
    private final Map<String, MachineMonitor> monitors = new HashMap<>();

    public DetectAlertsUseCase(AlertRules rules, AlertRepository repository, List<AlertPublisher> publishers) {
        this.rules = rules;
        this.repository = repository;
        this.publishers = List.copyOf(publishers);
    }

    /** @return les alertes créées ou modifiées par cet événement */
    public List<Alert> handle(MachineEvent event) {
        MachineMonitor monitor = monitors.computeIfAbsent(event.machineId(),
                id -> MachineMonitor.resume(id, rules, repository.findActiveByMachine(id)));

        List<Alert> changes = monitor.apply(event);
        for (Alert alert : changes) {
            repository.save(alert);
            publishers.forEach(publisher -> publisher.publish(alert));
        }
        return changes;
    }
}
