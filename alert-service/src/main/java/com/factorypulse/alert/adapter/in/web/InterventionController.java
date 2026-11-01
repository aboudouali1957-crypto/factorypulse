package com.factorypulse.alert.adapter.in.web;

import com.factorypulse.alert.application.GetInterventionsUseCase;
import com.factorypulse.alert.application.RecordInterventionUseCase;
import com.factorypulse.alert.domain.Intervention;
import com.factorypulse.alert.domain.InterventionCause;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * ADAPTATEUR D'ENTRÉE : les interventions de maintenance.
 *
 * POST /api/interventions          → saisir une intervention (rôle OPERATEUR)
 * GET  /api/interventions?limit=100 → l'historique (rôle RESPONSABLE)
 *
 * Les rôles sont vérifiés dans SecurityConfig : sans le bon rôle, la réponse est 403.
 */
@RestController
@RequestMapping("/api/interventions")
public class InterventionController {

    /** Le corps JSON envoyé par le formulaire. Le nom de l'opérateur n'y figure pas : il vient du jeton. */
    public record InterventionRequest(String machineId, String alertId, InterventionCause cause,
                                      String description, int durationMinutes) {
    }

    private final RecordInterventionUseCase recordIntervention;
    private final GetInterventionsUseCase getInterventions;

    public InterventionController(RecordInterventionUseCase recordIntervention, GetInterventionsUseCase getInterventions) {
        this.recordIntervention = recordIntervention;
        this.getInterventions = getInterventions;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Intervention record(@RequestBody InterventionRequest request, Authentication authentication) {
        RecordInterventionUseCase.Command command = new RecordInterventionUseCase.Command(request.machineId(),
                request.alertId(), request.cause(), request.description(), request.durationMinutes());
        return recordIntervention.record(command, authentication.getName());
    }

    @GetMapping
    public List<Intervention> recent(@RequestParam(defaultValue = "100") int limit) {
        return getInterventions.recent(limit);
    }
}
