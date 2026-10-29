package com.factorypulse.kpi.adapter.out.mongo;

import com.factorypulse.kpi.application.port.out.MachineKpiRepository;
import com.factorypulse.kpi.domain.MachineKpi;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * ADAPTATEUR DE SORTIE : implémente le port {@link MachineKpiRepository} avec MongoDB.
 *
 * Il traduit entre le domaine (MachineKpi) et le format de stockage (MachineKpiDocument).
 */
@Component
public class MongoMachineKpiRepository implements MachineKpiRepository {

    private final SpringDataMachineKpiRepository springData;

    public MongoMachineKpiRepository(SpringDataMachineKpiRepository springData) {
        this.springData = springData;
    }

    @Override
    public Optional<MachineKpi> findById(String machineId) {
        return springData.findById(machineId).map(MachineKpiDocument::toDomain);
    }

    @Override
    public List<MachineKpi> findAll() {
        return springData.findAll().stream().map(MachineKpiDocument::toDomain).toList();
    }

    @Override
    public void save(MachineKpi kpi) {
        springData.save(MachineKpiDocument.from(kpi));
    }
}
