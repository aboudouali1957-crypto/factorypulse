package com.factorypulse.simulator.application;

import com.factorypulse.simulator.application.port.out.MachineEventPublisher;
import com.factorypulse.simulator.domain.MachineConfig;
import com.factorypulse.simulator.domain.MachineEvent;
import com.factorypulse.simulator.domain.MachineSimulator;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * CAS D'USAGE : "faire avancer toute la ligne de production d'un tick".
 *
 * C'est la couche application : elle orchestre le domaine (les machines)
 * et utilise le port de sortie pour publier les événements.
 * Aucune annotation Spring, aucune dépendance à Kafka : c'est du Java pur, testable seul.
 */
public class SimulateProductionLineUseCase {

    private final List<MachineSimulator> simulators;
    private final MachineEventPublisher publisher;
    private final double tickSeconds;

    public SimulateProductionLineUseCase(List<MachineConfig> machines,
                                         RandomGenerator random,
                                         double tickSeconds,
                                         double energyReportSeconds,
                                         MachineEventPublisher publisher) {
        if (machines == null || machines.isEmpty()) {
            throw new IllegalArgumentException("La ligne doit contenir au moins une machine");
        }
        if (tickSeconds <= 0) {
            throw new IllegalArgumentException("tickSeconds doit être > 0");
        }
        this.simulators = machines.stream()
                .map(machine -> new MachineSimulator(machine, random, energyReportSeconds))
                .toList();
        this.publisher = publisher;
        this.tickSeconds = tickSeconds;
    }

    /**
     * Fait avancer chaque machine d'un tick et publie tous les événements produits.
     *
     * @param now date courante en millisecondes epoch
     * @return les événements publiés pendant ce tick
     */
    public List<MachineEvent> runTick(long now) {
        List<MachineEvent> published = new ArrayList<>();
        for (MachineSimulator simulator : simulators) {
            for (MachineEvent event : simulator.tick(now, tickSeconds)) {
                publisher.publish(event);
                published.add(event);
            }
        }
        return published;
    }

    public int machineCount() {
        return simulators.size();
    }
}
