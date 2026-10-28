package com.factorypulse.simulator.config;

import com.factorypulse.simulator.application.SimulateProductionLineUseCase;
import com.factorypulse.simulator.application.port.out.MachineEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Random;

/**
 * Assemble l'application : crée le cas d'usage (Java pur) et lui branche
 * l'adaptateur de sortie choisi par Spring (ici, Kafka).
 *
 * C'est le seul endroit qui relie la couche application au framework.
 */
@Configuration
public class UseCaseConfig {

    @Bean
    public SimulateProductionLineUseCase simulateProductionLineUseCase(SimulatorProperties properties,
                                                                       MachineEventPublisher publisher) {
        return new SimulateProductionLineUseCase(
                properties.machines(),
                new Random(),
                properties.tickMillis() / 1000.0,
                properties.energyReportSeconds(),
                publisher);
    }
}
