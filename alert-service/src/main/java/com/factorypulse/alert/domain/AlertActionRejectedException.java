package com.factorypulse.alert.domain;

/**
 * Une action demandée n'est pas possible dans l'état actuel (ex. acquitter une alerte déjà résolue).
 * L'API la traduit en réponse HTTP 409 (Conflict).
 */
public class AlertActionRejectedException extends RuntimeException {

    public AlertActionRejectedException(String message) {
        super(message);
    }
}
