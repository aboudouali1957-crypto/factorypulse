package com.factorypulse.alert.domain;

/** L'élément demandé n'existe pas. L'API la traduit en réponse HTTP 404. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
