package com.factorypulse.simulator.application;

import com.factorypulse.simulator.application.port.out.MachineEventPublisher;
import com.factorypulse.simulator.domain.EventType;
import com.factorypulse.simulator.domain.MachineConfig;
import com.factorypulse.simulator.domain.MachineEvent;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test du cas d'usage SANS Kafka : on branche un faux publisher en mémoire.
 * C'est tout l'intérêt du port de sortie.
 */
class SimulateProductionLineUseCaseTest {

    /** Faux adaptateur : garde les événements en mémoire au lieu de les envoyer dans Kafka. */
    static class InMemoryPublisher implements MachineEventPublisher {
        final List<MachineEvent> events = new ArrayList<>();

        @Override
        public void publish(MachineEvent event) {
            events.add(event);
        }
    }

    private static final List<MachineConfig> LINE = List.of(
            new MachineConfig("M1", "Presse", 2.0, 0, 0, 0, 15),
            new MachineConfig("M2", "Assemblage", 3.0, 0, 0, 0, 8));

    @Test
    void publieLesEvenementsDeToutesLesMachines() {
        InMemoryPublisher publisher = new InMemoryPublisher();
        SimulateProductionLineUseCase useCase = new SimulateProductionLineUseCase(LINE, new Random(42), 1.0, 5, publisher);

        for (int i = 0; i < 30; i++) {
            useCase.runTick(1_700_000_000_000L + i * 1000L);
        }

        assertThat(publisher.events)
                .filteredOn(e -> e.type() == EventType.PIECE_PRODUCED)
                .extracting(MachineEvent::machineId)
                .contains("M1", "M2");
        assertThat(useCase.machineCount()).isEqualTo(2);
    }

    @Test
    void renvoieExactementLesEvenementsPublies() {
        InMemoryPublisher publisher = new InMemoryPublisher();
        SimulateProductionLineUseCase useCase = new SimulateProductionLineUseCase(LINE, new Random(1), 1.0, 5, publisher);

        List<MachineEvent> returned = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            returned.addAll(useCase.runTick(i * 1000L));
        }

        assertThat(returned).containsExactlyElementsOf(publisher.events);
    }

    @Test
    void refuseUneLigneVide() {
        assertThatThrownBy(() -> new SimulateProductionLineUseCase(List.of(), new Random(), 1.0, 5, new InMemoryPublisher()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
