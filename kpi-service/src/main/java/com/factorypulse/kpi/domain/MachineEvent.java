package com.factorypulse.kpi.domain;

/**
 * Un événement de machine, tel que lu dans Kafka (même format JSON que celui publié par le simulateur).
 *
 * @param eventId   identifiant unique de l'événement
 * @param machineId identifiant de la machine (clé Kafka)
 * @param type      type d'événement
 * @param timestamp date de l'événement en millisecondes epoch
 * @param good      pour PIECE_PRODUCED : true = pièce bonne, false = rebut
 * @param energyKwh pour ENERGY_READING : énergie consommée en kWh
 */
public record MachineEvent(
        String eventId,
        String machineId,
        EventType type,
        long timestamp,
        Boolean good,
        Double energyKwh) {
}
