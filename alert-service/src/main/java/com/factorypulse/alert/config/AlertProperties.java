package com.factorypulse.alert.config;

import com.factorypulse.alert.domain.AlertRules;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Paramètres du service, lus dans application.yml sous la clé "alert".
 *
 * @param eventsTopic              topic Kafka à lire (événements des machines)
 * @param alertsTopic              topic Kafka où publier les alertes
 * @param prolongedDowntimeSeconds au-delà de cette durée, une panne devient critique
 * @param rejectWindowSize         nombre de dernières pièces observées pour le taux de rebut
 * @param rejectRateThreshold      taux de rebut qui déclenche une alerte
 * @param rejectRateClearThreshold taux de rebut sous lequel l'alerte est résolue
 */
@ConfigurationProperties(prefix = "alert")
public record AlertProperties(
        String eventsTopic,
        String alertsTopic,
        long prolongedDowntimeSeconds,
        int rejectWindowSize,
        double rejectRateThreshold,
        double rejectRateClearThreshold) {

    public AlertRules rules() {
        return new AlertRules(prolongedDowntimeSeconds, rejectWindowSize, rejectRateThreshold, rejectRateClearThreshold);
    }
}
