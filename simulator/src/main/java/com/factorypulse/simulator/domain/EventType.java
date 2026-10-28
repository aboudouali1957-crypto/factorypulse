package com.factorypulse.simulator.domain;

/**
 * Types d'événements qu'une machine peut émettre.
 */
public enum EventType {
    /** Une pièce vient d'être produite (bonne ou rebut). */
    PIECE_PRODUCED,
    /** La machine tombe en panne : elle ne produit plus. */
    MACHINE_DOWN,
    /** La machine est réparée et redémarre. */
    MACHINE_UP,
    /** Relevé de l'énergie consommée depuis le relevé précédent. */
    ENERGY_READING
}
