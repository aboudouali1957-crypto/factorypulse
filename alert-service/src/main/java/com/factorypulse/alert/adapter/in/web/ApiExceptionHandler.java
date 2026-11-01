package com.factorypulse.alert.adapter.in.web;

import com.factorypulse.alert.domain.AlertActionRejectedException;
import com.factorypulse.alert.domain.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduit les erreurs métier en réponses HTTP claires, au format standard "Problem Details" (RFC 9457) :
 * { "status": 409, "title": "Conflict", "detail": "L'alerte ... est déjà prise en charge par operateur" }
 *
 * Le domaine lance des exceptions Java ; c'est l'adaptateur web qui décide du code HTTP.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /** Saisie invalide (ex. durée négative, description vide). */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail invalidInput(IllegalArgumentException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail notFound(NotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    /** Action impossible dans l'état actuel (ex. alerte déjà résolue ou déjà prise en charge). */
    @ExceptionHandler(AlertActionRejectedException.class)
    public ProblemDetail conflict(AlertActionRejectedException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }
}
