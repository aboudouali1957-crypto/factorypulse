package com.factorypulse.simulator.application.port.out;

import com.factorypulse.simulator.domain.MachineEvent;

/**
 * PORT DE SORTIE : "j'ai besoin de publier un événement quelque part".
 *
 * L'application ne sait pas OÙ l'événement part (Kafka, fichier, base de données…).
 * Elle connaît seulement cette interface. L'implémentation concrète est un adaptateur
 * (ici {@code KafkaMachineEventPublisher}), branché par Spring au démarrage.
 *
 * Avantage : dans les tests, on remplace Kafka par une fausse implémentation en mémoire.
 */
public interface MachineEventPublisher {

    void publish(MachineEvent event);
}
