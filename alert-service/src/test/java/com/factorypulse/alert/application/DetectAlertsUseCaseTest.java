package com.factorypulse.alert.application;

import com.factorypulse.alert.domain.AlertRules;
import com.factorypulse.alert.domain.EventType;
import com.factorypulse.alert.domain.MachineEvent;
import com.factorypulse.alert.domain.Severity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests des cas d'usage SANS MongoDB, Kafka ni WebSocket : on branche de faux adaptateurs en mémoire.
 */
class DetectAlertsUseCaseTest {

    private static final long T0 = 1_700_000_000_000L;
    private static final AlertRules RULES = new AlertRules(30, 10, 0.20, 0.10);

    private static MachineEvent event(String machine, EventType type, long atSecond) {
        return new MachineEvent(machine + atSecond + type, machine, type, T0 + atSecond * 1000, null, null);
    }

    private final InMemoryAdapters.AlertStore repository = new InMemoryAdapters.AlertStore();
    private final InMemoryAdapters.Publisher kafka = new InMemoryAdapters.Publisher();
    private final InMemoryAdapters.Publisher websocket = new InMemoryAdapters.Publisher();
    private final DetectAlertsUseCase useCase = new DetectAlertsUseCase(
            new MachineMonitors(RULES, repository), repository, List.of(kafka, websocket));

    @Test
    void uneAlerte_estSauvegardee_etPublieePartout() {
        useCase.handle(event("M1", EventType.MACHINE_DOWN, 0));

        assertThat(repository.findActive()).hasSize(1);
        assertThat(kafka.published).hasSize(1);
        assertThat(websocket.published).hasSize(1);
    }

    @Test
    void leCycleDeVieComplet_uneSeuleAlerteEnBase() {
        useCase.handle(event("M1", EventType.MACHINE_DOWN, 0));
        useCase.handle(event("M1", EventType.ENERGY_READING, 30));   // devient critique
        useCase.handle(event("M1", EventType.MACHINE_UP, 40));       // résolue

        assertThat(repository.store).hasSize(1);                      // toujours la même alerte
        assertThat(repository.findActive()).isEmpty();
        assertThat(kafka.published).hasSize(3);                       // créée, aggravée, résolue
        assertThat(repository.findRecent(10).get(0).severity()).isEqualTo(Severity.CRITICAL);
    }

    @Test
    void chaqueMachineEstSurveilleeSeparement() {
        useCase.handle(event("M1", EventType.MACHINE_DOWN, 0));
        useCase.handle(event("M2", EventType.MACHINE_DOWN, 1));
        useCase.handle(event("M1", EventType.MACHINE_UP, 5));

        assertThat(repository.findActive()).hasSize(1);
        assertThat(repository.findActive().get(0).machineId()).isEqualTo("M2");
    }

    @Test
    void apresUnRedemarrage_reprendLesAlertesActives() {
        useCase.handle(event("M1", EventType.MACHINE_DOWN, 0));

        // Nouveaux moniteurs = service redémarré, mais même base de données.
        DetectAlertsUseCase restarted = new DetectAlertsUseCase(
                new MachineMonitors(RULES, repository), repository, List.of(kafka));
        restarted.handle(event("M1", EventType.MACHINE_UP, 10));

        assertThat(repository.findActive()).isEmpty();
        assertThat(repository.store).hasSize(1);
    }

    @Test
    void unEvenementSansConsequence_nePublieRien() {
        useCase.handle(event("M1", EventType.ENERGY_READING, 0));
        assertThat(kafka.published).isEmpty();
        assertThat(repository.store).isEmpty();
    }

    @Test
    void getAlerts_limiteLeNombreDeResultats() {
        GetAlertsUseCase get = new GetAlertsUseCase(repository);
        useCase.handle(event("M1", EventType.MACHINE_DOWN, 0));
        useCase.handle(event("M2", EventType.MACHINE_DOWN, 1));

        assertThat(get.recent(1)).hasSize(1);
        assertThat(get.recent(1).get(0).machineId()).isEqualTo("M2");   // la plus récente
        assertThat(get.active()).hasSize(2);
    }
}
