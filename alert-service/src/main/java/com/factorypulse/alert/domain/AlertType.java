package com.factorypulse.alert.domain;

/** Les types d'alertes détectées. */
public enum AlertType {
    /** La machine est en panne (puis : en panne depuis trop longtemps). */
    DOWNTIME,
    /** Trop de rebuts sur les dernières pièces produites. */
    HIGH_REJECT_RATE
}
