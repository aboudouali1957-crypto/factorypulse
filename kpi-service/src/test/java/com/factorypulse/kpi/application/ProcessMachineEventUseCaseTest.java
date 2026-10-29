package com.factorypulse.kpi.application;

import com.factorypulse.kpi.application.port.out.MachineKpiRepository;
import com.factorypulse.kpi.domain.EventType;
import com.factorypulse.kpi.domain.KpiSnapshot;
import com.factorypulse.kpi.domain.MachineEvent;
import com.factorypulse.kpi.domain.MachineKpi;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests des cas d'usage SANS MongoDB ni Kafka : on branche un faux repository en mémoire.
 */
class ProcessMachineEventUseCaseTest {

    /** Faux adaptateur : garde les machines dans une Map au lieu de MongoDB. */
    static class InMemoryRepository implements MachineKpiRepository {
        final Map<String, MachineKpi> store = new HashMap<>();
        int saves = 0;

        @Override
        public Optional<MachineKpi> findById(String machineId) {
            // On renvoie une copie, comme le ferait une vraie base : le cas d'usage doit bien appeler save().
            return Optional.ofNullable(store.get(machineId)).map(k -> MachineKpi.restore(k.state()));
        }

        @Override
        public List<MachineKpi> findAll() {
            return new ArrayList<>(store.values());
        }

        @Override
        public void save(MachineKpi kpi) {
            store.put(kpi.machineId(), MachineKpi.restore(kpi.state()));
            saves++;
        }
    }

    private static final long T0 = 1_700_000_000_000L;
    private static final Map<String, Double> CYCLES = Map.of("M1", 2.0, "M2", 3.0);

    private static MachineEvent event(String machine, EventType type, long atSecond, Boolean good) {
        return new MachineEvent(machine + type + atSecond, machine, type, T0 + atSecond * 1000, good, null);
    }

    @Test
    void creeLaMachineAuPremierEvenement_puisLaMetAJour() {
        InMemoryRepository repo = new InMemoryRepository();
        ProcessMachineEventUseCase useCase = new ProcessMachineEventUseCase(CYCLES, repo);

        useCase.handle(event("M1", EventType.PIECE_PRODUCED, 0, true));
        Optional<KpiSnapshot> snapshot = useCase.handle(event("M1", EventType.PIECE_PRODUCED, 2, false));

        assertThat(snapshot).isPresent();
        assertThat(snapshot.get().piecesProduced()).isEqualTo(2);
        assertThat(snapshot.get().rejectedPieces()).isEqualTo(1);
        assertThat(repo.saves).isEqualTo(2);
        assertThat(repo.store.get("M1").snapshot().piecesProduced()).isEqualTo(2);
    }

    @Test
    void separeLesIndicateursDeChaqueMachine() {
        InMemoryRepository repo = new InMemoryRepository();
        ProcessMachineEventUseCase useCase = new ProcessMachineEventUseCase(CYCLES, repo);

        useCase.handle(event("M1", EventType.PIECE_PRODUCED, 0, true));
        useCase.handle(event("M2", EventType.PIECE_PRODUCED, 0, true));
        useCase.handle(event("M2", EventType.PIECE_PRODUCED, 3, true));

        assertThat(repo.store.get("M1").snapshot().piecesProduced()).isEqualTo(1);
        assertThat(repo.store.get("M2").snapshot().piecesProduced()).isEqualTo(2);
    }

    @Test
    void ignoreUneMachineInconnue() {
        InMemoryRepository repo = new InMemoryRepository();
        ProcessMachineEventUseCase useCase = new ProcessMachineEventUseCase(CYCLES, repo);

        Optional<KpiSnapshot> result = useCase.handle(event("M9", EventType.PIECE_PRODUCED, 0, true));

        assertThat(result).isEmpty();
        assertThat(repo.store).isEmpty();
    }

    @Test
    void reprendLeCalculApresUnRedemarrage() {
        InMemoryRepository repo = new InMemoryRepository();
        new ProcessMachineEventUseCase(CYCLES, repo).handle(event("M1", EventType.PIECE_PRODUCED, 0, true));

        // "Redémarrage" : un nouveau cas d'usage, mais le même stockage.
        ProcessMachineEventUseCase afterRestart = new ProcessMachineEventUseCase(CYCLES, repo);
        Optional<KpiSnapshot> snapshot = afterRestart.handle(event("M1", EventType.PIECE_PRODUCED, 2, true));

        assertThat(snapshot.get().piecesProduced()).isEqualTo(2);
    }

    @Test
    void getKpis_renvoieToutesLesMachinesTriees() {
        InMemoryRepository repo = new InMemoryRepository();
        ProcessMachineEventUseCase process = new ProcessMachineEventUseCase(CYCLES, repo);
        process.handle(event("M2", EventType.PIECE_PRODUCED, 0, true));
        process.handle(event("M1", EventType.PIECE_PRODUCED, 0, true));

        GetKpisUseCase get = new GetKpisUseCase(repo);

        assertThat(get.all()).extracting(KpiSnapshot::machineId).containsExactly("M1", "M2");
        assertThat(get.byMachine("M1")).isPresent();
        assertThat(get.byMachine("M9")).isEmpty();
    }
}
