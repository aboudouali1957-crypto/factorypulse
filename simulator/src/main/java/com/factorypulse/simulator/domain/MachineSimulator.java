package com.factorypulse.simulator.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * Simule UNE machine de production, seconde après seconde.
 *
 * Cette classe est volontairement en Java pur (aucune dépendance à Spring ou Kafka) :
 * on peut la tester facilement, avec un générateur aléatoire fixé (seed) pour obtenir
 * toujours le même résultat.
 *
 * À chaque appel de {@link #tick(long, double)} :
 *  - si la machine tourne : elle peut tomber en panne, sinon elle avance dans son cycle
 *    et produit une pièce à chaque cycle terminé (bonne ou rebut) ;
 *  - si elle est en panne : elle peut être réparée ;
 *  - l'énergie consommée est cumulée et publiée toutes les {@code energyReportSeconds} secondes.
 */
public class MachineSimulator {

    /** La vitesse réelle varie entre 80 % et 100 % de la vitesse idéale : c'est ce qui fait baisser la "performance" du TRS. */
    static final double MIN_SPEED_FACTOR = 0.80;
    /** À l'arrêt, une machine consomme encore un peu d'énergie (veille). */
    static final double IDLE_POWER_RATIO = 0.10;

    private final MachineConfig config;
    private final RandomGenerator random;
    private final double energyReportSeconds;

    private boolean running = true;
    private double cycleProgressSeconds = 0;
    private double energyAccumulatedKwh = 0;
    private double secondsSinceEnergyReport = 0;

    public MachineSimulator(MachineConfig config, RandomGenerator random, double energyReportSeconds) {
        if (energyReportSeconds <= 0) {
            throw new IllegalArgumentException("energyReportSeconds doit être > 0");
        }
        this.config = config;
        this.random = random;
        this.energyReportSeconds = energyReportSeconds;
    }

    /**
     * Fait avancer la machine de {@code elapsedSeconds} secondes.
     *
     * @param now            date courante en millisecondes epoch (mise dans les événements)
     * @param elapsedSeconds durée écoulée depuis le tick précédent
     * @return les événements produits pendant ce tick (peut être vide)
     */
    public List<MachineEvent> tick(long now, double elapsedSeconds) {
        List<MachineEvent> events = new ArrayList<>();

        if (running) {
            if (random.nextDouble() < config.failureProbability()) {
                running = false;
                events.add(MachineEvent.down(config.id(), now));
            } else {
                produce(now, elapsedSeconds, events);
            }
        } else if (random.nextDouble() < config.repairProbability()) {
            running = true;
            events.add(MachineEvent.up(config.id(), now));
        }

        accumulateEnergy(now, elapsedSeconds, events);
        return events;
    }

    private void produce(long now, double elapsedSeconds, List<MachineEvent> events) {
        double speedFactor = MIN_SPEED_FACTOR + random.nextDouble() * (1 - MIN_SPEED_FACTOR);
        cycleProgressSeconds += elapsedSeconds * speedFactor;

        while (cycleProgressSeconds >= config.idealCycleSeconds()) {
            cycleProgressSeconds -= config.idealCycleSeconds();
            boolean good = random.nextDouble() >= config.rejectRate();
            events.add(MachineEvent.piece(config.id(), now, good));
        }
    }

    private void accumulateEnergy(long now, double elapsedSeconds, List<MachineEvent> events) {
        double power = running ? config.powerKw() : config.powerKw() * IDLE_POWER_RATIO;
        energyAccumulatedKwh += power * elapsedSeconds / 3600.0;
        secondsSinceEnergyReport += elapsedSeconds;

        if (secondsSinceEnergyReport >= energyReportSeconds) {
            events.add(MachineEvent.energy(config.id(), now, energyAccumulatedKwh));
            energyAccumulatedKwh = 0;
            secondsSinceEnergyReport = 0;
        }
    }

    public boolean isRunning() {
        return running;
    }

    public MachineConfig config() {
        return config;
    }
}
