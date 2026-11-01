package com.factorypulse.alert.application.port.out;

import com.factorypulse.alert.domain.Intervention;

import java.util.List;

/** PORT DE SORTIE : où sont gardées les interventions de maintenance (MongoDB). */
public interface InterventionRepository {

    void save(Intervention intervention);

    /** Les dernières interventions, de la plus récente à la plus ancienne. */
    List<Intervention> findRecent(int limit);
}
