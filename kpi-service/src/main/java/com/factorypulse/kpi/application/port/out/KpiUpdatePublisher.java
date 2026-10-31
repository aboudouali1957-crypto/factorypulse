package com.factorypulse.kpi.application.port.out;

import com.factorypulse.kpi.domain.KpiSnapshot;

/**
 * PORT DE SORTIE : "prévenir l'extérieur que les indicateurs d'une machine ont changé".
 *
 * En production, l'adaptateur WebSocket pousse la mise à jour vers le tableau de bord ;
 * dans les tests, une simple liste en mémoire suffit.
 */
public interface KpiUpdatePublisher {

    void publish(KpiSnapshot snapshot);
}
