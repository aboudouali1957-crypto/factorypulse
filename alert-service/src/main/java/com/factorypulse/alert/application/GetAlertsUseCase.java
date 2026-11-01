package com.factorypulse.alert.application;

import com.factorypulse.alert.application.port.out.AlertRepository;
import com.factorypulse.alert.domain.Alert;

import java.util.List;

/** CAS D'USAGE : lire les alertes (pour l'API REST). */
public class GetAlertsUseCase {

    /** Nombre maximum d'alertes renvoyées par {@link #recent(int)}. */
    public static final int MAX_LIMIT = 200;

    private final AlertRepository repository;

    public GetAlertsUseCase(AlertRepository repository) {
        this.repository = repository;
    }

    public List<Alert> active() {
        return repository.findActive();
    }

    public List<Alert> recent(int limit) {
        return repository.findRecent(Math.max(1, Math.min(limit, MAX_LIMIT)));
    }
}
