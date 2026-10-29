package com.factorypulse.kpi.application.port.out;

import com.factorypulse.kpi.domain.MachineKpi;

import java.util.List;
import java.util.Optional;

/**
 * PORT DE SORTIE : "j'ai besoin de sauvegarder et relire les indicateurs des machines".
 *
 * L'application ne sait pas OÙ c'est stocké. En production, l'adaptateur MongoDB implémente ce port ;
 * dans les tests, une simple Map en mémoire suffit.
 */
public interface MachineKpiRepository {

    Optional<MachineKpi> findById(String machineId);

    List<MachineKpi> findAll();

    void save(MachineKpi kpi);
}
