package com.factorypulse.kpi.application;

import com.factorypulse.kpi.application.port.out.MachineKpiRepository;
import com.factorypulse.kpi.domain.KpiSnapshot;
import com.factorypulse.kpi.domain.MachineKpi;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * CAS D'USAGE : "consulter les indicateurs" (toutes les machines, ou une seule).
 */
public class GetKpisUseCase {

    private final MachineKpiRepository repository;

    public GetKpisUseCase(MachineKpiRepository repository) {
        this.repository = repository;
    }

    /** Les indicateurs de toutes les machines, triés par identifiant (M1, M2…). */
    public List<KpiSnapshot> all() {
        return repository.findAll().stream()
                .map(MachineKpi::snapshot)
                .sorted(Comparator.comparing(KpiSnapshot::machineId))
                .toList();
    }

    public Optional<KpiSnapshot> byMachine(String machineId) {
        return repository.findById(machineId).map(MachineKpi::snapshot);
    }
}
