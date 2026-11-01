package com.factorypulse.alert.application.port.out;

import com.factorypulse.alert.domain.Alert;

/**
 * PORT DE SORTIE : "prévenir l'extérieur qu'une alerte a changé", sans savoir comment.
 *
 * Deux adaptateurs l'implémentent : l'un publie dans le topic Kafka "machine-alerts"
 * (pour d'autres services), l'autre pousse l'alerte vers le tableau de bord par WebSocket.
 */
public interface AlertPublisher {

    void publish(Alert alert);
}
