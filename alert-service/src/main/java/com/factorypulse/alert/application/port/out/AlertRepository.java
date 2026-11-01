package com.factorypulse.alert.application.port.out;

import com.factorypulse.alert.domain.Alert;

import java.util.List;

/**
 * PORT DE SORTIE : où sont gardées les alertes (implémenté avec MongoDB par un adaptateur).
 */
public interface AlertRepository {

    /** Enregistre une alerte, ou remplace la version précédente qui a le même identifiant. */
    void save(Alert alert);

    /** Les alertes encore actives, toutes machines confondues. */
    List<Alert> findActive();

    /** Les alertes encore actives d'une machine. */
    List<Alert> findActiveByMachine(String machineId);

    /** Les dernières alertes (actives ou résolues), de la plus récente à la plus ancienne. */
    List<Alert> findRecent(int limit);
}
