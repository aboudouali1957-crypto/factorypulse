package com.factorypulse.simulator.adapter.in.scheduler;

import com.factorypulse.simulator.application.SimulateProductionLineUseCase;
import com.factorypulse.simulator.domain.EventType;
import com.factorypulse.simulator.domain.MachineEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * ADAPTATEUR D'ENTRÉE : déclenche le cas d'usage toutes les {@code simulator.tick-millis} ms.
 *
 * C'est le "déclencheur". Demain, on pourrait ajouter un autre adaptateur d'entrée
 * (par exemple un endpoint REST "avancer d'un tick" pour les tests) sans toucher au cas d'usage.
 */
@Component
public class SimulationScheduler {

    private static final Logger log = LoggerFactory.getLogger(SimulationScheduler.class);

    private final SimulateProductionLineUseCase useCase;

    public SimulationScheduler(SimulateProductionLineUseCase useCase) {
        this.useCase = useCase;
    }

    @Scheduled(fixedRateString = "${simulator.tick-millis}")
    public void tick() {
        for (MachineEvent event : useCase.runTick(System.currentTimeMillis())) {
            if (event.type() == EventType.MACHINE_DOWN || event.type() == EventType.MACHINE_UP) {
                log.info("Machine {} : {}", event.machineId(), event.type());
            }
        }
    }
}
