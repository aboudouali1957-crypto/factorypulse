package com.factorypulse.simulator.domain;

import java.util.UUID;

/**
 * Un événement émis par une machine et publié dans Kafka.
 *
 * @param eventId   identifiant unique de l'événement (utile pour détecter les doublons)
 * @param machineId identifiant de la machine, sert aussi de clé Kafka
 * @param type      type d'événement
 * @param timestamp date de l'événement en millisecondes depuis 1970 (epoch), format simple et sans ambiguïté
 * @param good      pour PIECE_PRODUCED : true = pièce bonne, false = rebut ; null sinon
 * @param energyKwh pour ENERGY_READING : énergie consommée en kWh ; null sinon
 */
public record MachineEvent(
        String eventId,
        String machineId,
        EventType type,
        long timestamp,
        Boolean good,
        Double energyKwh) {

    public static MachineEvent piece(String machineId, long timestamp, boolean good) {
        return new MachineEvent(newId(), machineId, EventType.PIECE_PRODUCED, timestamp, good, null);
    }

    public static MachineEvent down(String machineId, long timestamp) {
        return new MachineEvent(newId(), machineId, EventType.MACHINE_DOWN, timestamp, null, null);
    }

    public static MachineEvent up(String machineId, long timestamp) {
        return new MachineEvent(newId(), machineId, EventType.MACHINE_UP, timestamp, null, null);
    }

    public static MachineEvent energy(String machineId, long timestamp, double energyKwh) {
        return new MachineEvent(newId(), machineId, EventType.ENERGY_READING, timestamp, null, energyKwh);
    }

    private static String newId() {
        return UUID.randomUUID().toString();
    }
}
