package com.factorypulse.alert.domain;

/**
 * Les seuils de déclenchement des alertes.
 *
 * @param prolongedDowntimeSeconds au-delà de cette durée, une panne devient CRITIQUE
 * @param rejectWindowSize         nombre de dernières pièces observées pour le taux de rebut
 * @param rejectRateThreshold      taux de rebut (0 à 1) au-dessus duquel on lève une alerte
 * @param rejectRateClearThreshold taux de rebut sous lequel l'alerte est levée (résolue).
 *                                 Plus bas que le seuil de déclenchement, pour éviter qu'une alerte
 *                                 s'ouvre et se ferme sans arrêt autour d'une même valeur (hystérésis).
 */
public record AlertRules(
        long prolongedDowntimeSeconds,
        int rejectWindowSize,
        double rejectRateThreshold,
        double rejectRateClearThreshold) {

    public AlertRules {
        if (prolongedDowntimeSeconds <= 0) {
            throw new IllegalArgumentException("prolongedDowntimeSeconds doit être > 0");
        }
        if (rejectWindowSize <= 0) {
            throw new IllegalArgumentException("rejectWindowSize doit être > 0");
        }
        if (rejectRateClearThreshold > rejectRateThreshold) {
            throw new IllegalArgumentException("le seuil de résolution doit être <= au seuil de déclenchement");
        }
    }
}
