package com.factorypulse.kpi.adapter.in.web;

import com.factorypulse.kpi.application.GetKpisUseCase;
import com.factorypulse.kpi.domain.KpiSnapshot;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * ADAPTATEUR D'ENTRÉE : expose les indicateurs en API REST (JSON).
 *
 * GET /api/kpis       → toutes les machines
 * GET /api/kpis/{id}  → une machine (404 si elle n'a encore envoyé aucun événement)
 */
@RestController
@RequestMapping("/api/kpis")
public class KpiController {

    private final GetKpisUseCase useCase;

    public KpiController(GetKpisUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    public List<KpiSnapshot> all() {
        return useCase.all();
    }

    @GetMapping("/{machineId}")
    public ResponseEntity<KpiSnapshot> byMachine(@PathVariable String machineId) {
        return ResponseEntity.of(useCase.byMachine(machineId));
    }
}
