package com.factorypulse.alert.domain;

/**
 * Une alerte sur une machine. Elle naît active, peut s'aggraver, puis est résolue.
 *
 * Objet immuable : chaque changement crée une nouvelle version de l'alerte (même identifiant).
 *
 * @param id         identifiant stable : machine + type + date de début. Si Kafka redonne
 *                   le même événement (livraison "au moins une fois"), on retrouve la même alerte
 *                   au lieu d'en créer une deuxième.
 * @param machineId  machine concernée
 * @param type       type d'alerte
 * @param severity   gravité actuelle
 * @param message    description lisible
 * @param raisedAt   date de début (millisecondes epoch, date de l'événement déclencheur)
 * @param updatedAt  date du dernier changement
 * @param resolvedAt date de résolution, ou null si l'alerte est toujours active
 */
public record Alert(
        String id,
        String machineId,
        AlertType type,
        Severity severity,
        String message,
        long raisedAt,
        long updatedAt,
        Long resolvedAt) {

    public static Alert raise(String machineId, AlertType type, Severity severity, String message, long at) {
        String id = machineId + "-" + type + "-" + at;
        return new Alert(id, machineId, type, severity, message, at, at, null);
    }

    public boolean isActive() {
        return resolvedAt == null;
    }

    /** Nouvelle version de l'alerte, avec une gravité et un message mis à jour. */
    public Alert escalate(Severity newSeverity, String newMessage, long at) {
        return new Alert(id, machineId, type, newSeverity, newMessage, raisedAt, at, null);
    }

    /** Nouvelle version de l'alerte, résolue à la date indiquée. */
    public Alert resolve(String newMessage, long at) {
        return new Alert(id, machineId, type, severity, newMessage, raisedAt, at, at);
    }
}
