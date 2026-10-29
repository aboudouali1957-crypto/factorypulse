package com.factorypulse.kpi.domain;

/**
 * Types d'événements reçus du simulateur.
 *
 * C'est volontairement une COPIE de l'enum du simulateur : deux microservices
 * ne partagent pas de code. Leur seul contrat commun est le format JSON du message Kafka.
 */
public enum EventType {
    PIECE_PRODUCED,
    MACHINE_DOWN,
    MACHINE_UP,
    ENERGY_READING
}
