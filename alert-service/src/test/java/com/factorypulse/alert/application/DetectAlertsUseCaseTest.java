package com.factorypulse.alert.application;

import com.factorypulse.alert.application.port.out.AlertPublisher;
import com.factorypulse.alert.application.port.out.AlertRepository;
import com.factorypulse.alert.domain.Alert;
import com.factorypulse.alert.domain.AlertRules;
import com.factorypulse.alert.domain.EventType;
import com.factorypulse.alert.domain.MachineEvent;
import com.factorypulse.alert.domain.Severity;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests des cas d'usage SANS MongoDB, Kafka ni WebSocket : on branche de faux adaptateurs en mémoire.
 */
class DetectAlertsUseCaseTest {

    /** Faux adaptateur : garde les alertes dans une Map au lieu de MongoDB. */
    static class InMemoryRepository implements AlertRepository {
        final Map<String, Alert> store = new LinkedHashMap<>();

        @Override
        public void save(Alert alert) {
            store.put(alert.id(), alert);
        }

        @Override
        public List<Alert> findActive() {
            return store.values().stream().filter(Alert::isActive).toList();
        }

        @Override
        public List<Alert> findActiveByMachine(String machineId) {
            return findActive().stream().filter(a -> a.machineId().equals(machineId)).toList();
        }

        @Override
        public List<Alert> findRecent(int limit) {
            return store.values().stream()
                    .sorted(Comparator.comparingLong(Alert::updatedAt).reversed())
                    .limit(limit)
                    .toList();
        }
    }

    /** Faux adaptateur : garde les alertes publiées dans une liste. */
    static class InMemoryPublisher implements AlertPublisher {
        final List<Alert> published = new ArrayList<>();

        @Override
        public void publish(Alert alert) {
            published.add(alert);
        }
    }

    private static final long T0 = 1_700_000_000_000L;
    private static final AlertRules RULES = new AlertRules(30, 10, 0.20, 0.10);

    private static MachineEvent event(String machine, EventType type, long atSecond) {
        return new MachineEvent(machine + atSecond + type, machine, type, T0 + atSecond * 1000, null, null);
    }

    private final InMemoryRepository repository = new InMemoryRepository();
    private final InMemoryPublisher kafka = new InMemoryPublisher();
    private final InMemoryPublisher websocket = new InMemoryPublisher();
    private final DetectAlertsUseCase useCase = new DetectAlertsUseCase(RULES, repository, List.of(kafka, websocket));

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

        // Nouveau cas d'usage = service redémarré, mais même base de données.
        DetectAlertsUseCase restarted = new DetectAlertsUseCase(RULES, repository, List.of(kafka));
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
