package com.factorypulse.alert.domain;

/**
 * Une intervention de maintenance sur une machine, saisie par l'opérateur qui l'a réalisée.
 *
 * Les règles de validité sont vérifiées à la création : un objet Intervention est toujours valide.
 *
 * @param id              identifiant unique
 * @param machineId       machine concernée (ex. "M1")
 * @param alertId         alerte à l'origine de l'intervention, ou null (intervention préventive)
 * @param cause           cause identifiée
 * @param description     action réalisée
 * @param durationMinutes durée de l'intervention, en minutes
 * @param performedBy     opérateur qui l'a réalisée (lu dans son jeton, jamais dans la requête)
 * @param createdAt       date de saisie (millisecondes epoch)
 */
public record Intervention(
        String id,
        String machineId,
        String alertId,
        InterventionCause cause,
        String description,
        int durationMinutes,
        String performedBy,
        long createdAt) {

    public static final int MAX_DESCRIPTION_LENGTH = 500;
    public static final int MAX_DURATION_MINUTES = 24 * 60;

    public Intervention {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("L'identifiant est obligatoire");
        }
        if (machineId == null || !machineId.matches("M\\d{1,3}")) {
            throw new IllegalArgumentException("Machine invalide : " + machineId);
        }
        if (cause == null) {
            throw new IllegalArgumentException("La cause est obligatoire");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("La description de l'action réalisée est obligatoire");
        }
        description = description.strip();
        if (description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("La description dépasse " + MAX_DESCRIPTION_LENGTH + " caractères");
        }
        if (durationMinutes < 1 || durationMinutes > MAX_DURATION_MINUTES) {
            throw new IllegalArgumentException("La durée doit être comprise entre 1 et " + MAX_DURATION_MINUTES + " minutes");
        }
        if (performedBy == null || performedBy.isBlank()) {
            throw new IllegalArgumentException("Il faut savoir qui a réalisé l'intervention");
        }
        if (alertId != null && alertId.isBlank()) {
            alertId = null;
        }
    }
}
