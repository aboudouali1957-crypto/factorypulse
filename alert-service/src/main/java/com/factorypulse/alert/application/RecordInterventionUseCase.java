package com.factorypulse.alert.application;

import com.factorypulse.alert.application.port.out.AlertRepository;
import com.factorypulse.alert.application.port.out.InterventionRepository;
import com.factorypulse.alert.domain.Alert;
import com.factorypulse.alert.domain.Intervention;
import com.factorypulse.alert.domain.InterventionCause;
import com.factorypulse.alert.domain.NotFoundException;

import java.time.Clock;
import java.util.function.Supplier;

/**
 * CAS D'USAGE : "un opérateur saisit une intervention de maintenance".
 *
 * Le rôle (OPERATEUR) est vérifié avant, par la sécurité. L'opérateur est lu dans son jeton.
 */
public class RecordInterventionUseCase {

    /** Ce que l'opérateur saisit dans le formulaire. */
    public record Command(String machineId, String alertId, InterventionCause cause,
                          String description, int durationMinutes) {
    }

    private final InterventionRepository interventions;
    private final AlertRepository alerts;
    private final Clock clock;
    private final Supplier<String> idGenerator;

    public RecordInterventionUseCase(InterventionRepository interventions, AlertRepository alerts,
                                     Clock clock, Supplier<String> idGenerator) {
        this.interventions = interventions;
        this.alerts = alerts;
        this.clock = clock;
        this.idGenerator = idGenerator;
    }

    /**
     * @throws IllegalArgumentException si la saisie est invalide
     * @throws NotFoundException        si l'alerte indiquée n'existe pas
     */
    public Intervention record(Command command, String username) {
        String alertId = command.alertId() == null || command.alertId().isBlank() ? null : command.alertId();
        if (alertId != null) {
            Alert alert = alerts.findById(alertId)
                    .orElseThrow(() -> new NotFoundException("Alerte introuvable : " + alertId));
            if (!alert.machineId().equals(command.machineId())) {
                throw new IllegalArgumentException(
                        "L'alerte " + alertId + " concerne " + alert.machineId() + ", pas " + command.machineId());
            }
        }
        Intervention intervention = new Intervention(idGenerator.get(), command.machineId(), alertId,
                command.cause(), command.description(), command.durationMinutes(), username, clock.millis());
        interventions.save(intervention);
        return intervention;
    }
}
