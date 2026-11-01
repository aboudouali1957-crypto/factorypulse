package com.factorypulse.alert.application;

import com.factorypulse.alert.application.port.out.AlertRepository;
import com.factorypulse.alert.domain.AlertRules;
import com.factorypulse.alert.domain.MachineMonitor;

import java.util.HashMap;
import java.util.Map;

/**
 * Les moniteurs de toutes les machines, partagés par les cas d'usage qui modifient les alertes.
 *
 * Deux sources peuvent modifier une même alerte en même temps : les événements Kafka
 * (aggravation, résolution) et les opérateurs via l'API (acquittement). Les cas d'usage
 * travaillent donc sur les moniteurs sous un verrou commun ({@link #lock()}) :
 * une seule modification à la fois, et aucune n'écrase l'autre.
 */
public class MachineMonitors {

    private final AlertRules rules;
    private final AlertRepository repository;
    private final Map<String, MachineMonitor> monitors = new HashMap<>();
    private final Object lock = new Object();

    public MachineMonitors(AlertRules rules, AlertRepository repository) {
        this.rules = rules;
        this.repository = repository;
    }

    /** Le moniteur d'une machine, recréé à partir de ses alertes actives au premier besoin. */
    MachineMonitor of(String machineId) {
        return monitors.computeIfAbsent(machineId,
                id -> MachineMonitor.resume(id, rules, repository.findActiveByMachine(id)));
    }

    Object lock() {
        return lock;
    }
}
