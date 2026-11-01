package com.factorypulse.alert.application;

import com.factorypulse.alert.application.port.out.InterventionRepository;
import com.factorypulse.alert.domain.Intervention;

import java.util.List;

/** CAS D'USAGE : consulter l'historique des interventions (réservé au responsable). */
public class GetInterventionsUseCase {

    public static final int MAX_LIMIT = 500;

    private final InterventionRepository repository;

    public GetInterventionsUseCase(InterventionRepository repository) {
        this.repository = repository;
    }

    public List<Intervention> recent(int limit) {
        return repository.findRecent(Math.max(1, Math.min(limit, MAX_LIMIT)));
    }
}
