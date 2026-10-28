package com.factorypulse.simulator.config;

import com.factorypulse.simulator.domain.MachineConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Paramètres du simulateur, lus dans application.yml sous la clé "simulator".
 *
 * @param topic               nom du topic Kafka où publier les événements
 * @param tickMillis          intervalle entre deux ticks de simulation, en millisecondes
 * @param energyReportSeconds intervalle entre deux relevés d'énergie, en secondes
 * @param machines            liste des machines de la ligne
 */
@ConfigurationProperties(prefix = "simulator")
public record SimulatorProperties(
        String topic,
        long tickMillis,
        double energyReportSeconds,
        List<MachineConfig> machines) {
}
