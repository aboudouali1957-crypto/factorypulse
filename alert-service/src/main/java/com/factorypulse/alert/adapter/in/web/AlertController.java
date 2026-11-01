package com.factorypulse.alert.adapter.in.web;

import com.factorypulse.alert.application.GetAlertsUseCase;
import com.factorypulse.alert.domain.Alert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * ADAPTATEUR D'ENTRÉE : expose les alertes en API REST (JSON).
 *
 * GET /api/alerts?limit=50  → les dernières alertes, actives ou résolues (la plus récente en premier)
 * GET /api/alerts/active    → les alertes encore actives
 */
@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final GetAlertsUseCase useCase;

    public AlertController(GetAlertsUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    public List<Alert> recent(@RequestParam(defaultValue = "50") int limit) {
        return useCase.recent(limit);
    }

    @GetMapping("/active")
    public List<Alert> active() {
        return useCase.active();
    }
}
