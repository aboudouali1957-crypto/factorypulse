package com.factorypulse.alert.adapter.in.web;

import com.factorypulse.alert.application.AcknowledgeAlertUseCase;
import com.factorypulse.alert.application.GetAlertsUseCase;
import com.factorypulse.alert.domain.Alert;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * ADAPTATEUR D'ENTRÉE : expose les alertes en API REST (JSON).
 *
 * GET  /api/alerts?limit=50           → les dernières alertes, actives ou résolues (tout utilisateur connecté)
 * GET  /api/alerts/active             → les alertes encore actives (tout utilisateur connecté)
 * POST /api/alerts/{id}/acknowledge   → prise en charge par un opérateur (rôle OPERATEUR, voir SecurityConfig)
 */
@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final GetAlertsUseCase getAlerts;
    private final AcknowledgeAlertUseCase acknowledgeAlert;

    public AlertController(GetAlertsUseCase getAlerts, AcknowledgeAlertUseCase acknowledgeAlert) {
        this.getAlerts = getAlerts;
        this.acknowledgeAlert = acknowledgeAlert;
    }

    @GetMapping
    public List<Alert> recent(@RequestParam(defaultValue = "50") int limit) {
        return getAlerts.recent(limit);
    }

    @GetMapping("/active")
    public List<Alert> active() {
        return getAlerts.active();
    }

    /** L'opérateur est lu dans son jeton (authentication.getName() = preferred_username), jamais dans la requête. */
    @PostMapping("/{id}/acknowledge")
    public Alert acknowledge(@PathVariable String id, Authentication authentication) {
        return acknowledgeAlert.acknowledge(id, authentication.getName());
    }
}
