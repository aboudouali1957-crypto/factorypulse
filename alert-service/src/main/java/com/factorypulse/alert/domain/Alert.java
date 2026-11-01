package com.factorypulse.alert.domain;

/**
 * Une alerte sur une machine. Elle naît active, peut s'aggraver, être prise en charge
 * par un opérateur (acquittée), puis est résolue.
 *
 * Objet immuable : chaque changement crée une nouvelle version de l'alerte (même identifiant).
 *
 * @param id             identifiant stable : machine + type + date de début. Si Kafka redonne
 *                       le même événement (livraison "au moins une fois"), on retrouve la même alerte
 *                       au lieu d'en créer une deuxième.
 * @param machineId      machine concernée
 * @param type           type d'alerte
 * @param severity       gravité actuelle
 * @param message        description lisible
 * @param raisedAt       date de début (millisecondes epoch, date de l'événement déclencheur)
 * @param updatedAt      date du dernier changement
 * @param resolvedAt     date de résolution, ou null si l'alerte est toujours active
 * @param acknowledgedBy opérateur qui a pris l'alerte en charge, ou null
 * @param acknowledgedAt date de la prise en charge, ou null
 */
public record Alert(
        String id,
        String machineId,
        AlertType type,
        Severity severity,
        String message,
        long raisedAt,
        long updatedAt,
        Long resolvedAt,
        String acknowledgedBy,
        Long acknowledgedAt) {

    public static Alert raise(String machineId, AlertType type, Severity severity, String message, long at) {
        String id = machineId + "-" + type + "-" + at;
        return new Alert(id, machineId, type, severity, message, at, at, null, null, null);
    }

    public boolean isActive() {
        return resolvedAt == null;
    }

    public boolean isAcknowledged() {
        return acknowledgedBy != null;
    }

    /** Nouvelle version de l'alerte, avec une gravité et un message mis à jour (la prise en charge est conservée). */
    public Alert escalate(Severity newSeverity, String newMessage, long at) {
        return new Alert(id, machineId, type, newSeverity, newMessage, raisedAt, later(at), null, acknowledgedBy, acknowledgedAt);
    }

    /** Nouvelle version de l'alerte, résolue à la date indiquée (la prise en charge est conservée). */
    public Alert resolve(String newMessage, long at) {
        return new Alert(id, machineId, type, severity, newMessage, raisedAt, later(at), at, acknowledgedBy, acknowledgedAt);
    }

    /**
     * La date de mise à jour ne recule jamais : le tableau de bord ignore une version plus ancienne
     * que celle qu'il connaît déjà (utile si un événement un peu ancien arrive juste après un acquittement).
     */
    private long later(long at) {
        return Math.max(at, updatedAt);
    }

    /**
     * Nouvelle version de l'alerte, prise en charge par un opérateur.
     *
     * @throws AlertActionRejectedException si l'alerte est déjà résolue ou déjà prise en charge
     */
    public Alert acknowledge(String username, long at) {
        if (!isActive()) {
            throw new AlertActionRejectedException("L'alerte " + id + " est déjà résolue");
        }
        if (isAcknowledged()) {
            throw new AlertActionRejectedException("L'alerte " + id + " est déjà prise en charge par " + acknowledgedBy);
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Il faut savoir qui prend l'alerte en charge");
        }
        return new Alert(id, machineId, type, severity, message, raisedAt, at, null, username, at);
    }
}
