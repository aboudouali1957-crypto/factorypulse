package com.factorypulse.kpi.application;

import com.factorypulse.kpi.application.port.out.MachineKpiRepository;
import com.factorypulse.kpi.domain.KpiSnapshot;
import com.factorypulse.kpi.domain.MachineEvent;
import com.factorypulse.kpi.domain.MachineKpi;

import java.util.Map;
import java.util.Optional;

/**
 * CAS D'USAGE : "un événement de machine arrive, mettre à jour ses indicateurs".
 *
 * 1. retrouver les indicateurs de la machine (ou les créer au premier événement) ;
 * 2. leur appliquer l'événement ;
 * 3. les sauvegarder.
 *
 * Java pur : aucune dépendance à Kafka, MongoDB ou Spring.
 */
public class ProcessMachineEventUseCase {

    private final Map<String, Double> idealCycleSecondsByMachine;
    private final MachineKpiRepository repository;

    /**
     * @param idealCycleSecondsByMachine temps de cycle idéal de chaque machine connue, ex. {"M1": 2.0}
     * @param repository                 où lire et sauvegarder les indicateurs
     */
    public ProcessMachineEventUseCase(Map<String, Double> idealCycleSecondsByMachine,
                                      MachineKpiRepository repository) {
        if (idealCycleSecondsByMachine == null || idealCycleSecondsByMachine.isEmpty()) {
            throw new IllegalArgumentException("Il faut au moins une machine connue (temps de cycle idéal)");
        }
        this.idealCycleSecondsByMachine = Map.copyOf(idealCycleSecondsByMachine);
        this.repository = repository;
    }

    /**
     * @return les nouveaux indicateurs de la machine, ou vide si la machine est inconnue
     *         (dans ce cas l'événement est ignoré : sans temps de cycle idéal, pas de calcul de performance)
     */
    public Optional<KpiSnapshot> handle(MachineEvent event) {
        Double idealCycleSeconds = idealCycleSecondsByMachine.get(event.machineId());
        if (idealCycleSeconds == null) {
            return Optional.empty();
        }

        MachineKpi kpi = repository.findById(event.machineId())
                .orElseGet(() -> new MachineKpi(event.machineId(), idealCycleSeconds));

        kpi.apply(event);
        repository.save(kpi);
        return Optional.of(kpi.snapshot());
    }

    public boolean isKnownMachine(String machineId) {
        return idealCycleSecondsByMachine.containsKey(machineId);
    }
}
