package com.factorypulse.alert.application;

import com.factorypulse.alert.application.port.out.AlertPublisher;
import com.factorypulse.alert.application.port.out.AlertRepository;
import com.factorypulse.alert.domain.Alert;
import com.factorypulse.alert.domain.NotFoundException;

import java.time.Clock;
import java.util.List;

/**
 * CAS D'USAGE : "un opérateur prend une alerte en charge" (acquittement).
 *
 * Qui a le droit de le faire est vérifié AVANT, par la sécurité (rôle OPERATEUR) ;
 * le nom de l'opérateur vient de son jeton, jamais du contenu de la requête.
 */
public class AcknowledgeAlertUseCase {

    private final MachineMonitors monitors;
    private final AlertRepository repository;
    private final List<AlertPublisher> publishers;
    private final Clock clock;

    public AcknowledgeAlertUseCase(MachineMonitors monitors, AlertRepository repository,
                                   List<AlertPublisher> publishers, Clock clock) {
        this.monitors = monitors;
        this.repository = repository;
        this.publishers = List.copyOf(publishers);
        this.clock = clock;
    }

    /**
     * @return la nouvelle version de l'alerte
     * @throws NotFoundException si l'alerte n'existe pas
     * @throws com.factorypulse.alert.domain.AlertActionRejectedException si elle est résolue ou déjà prise en charge
     */
    public Alert acknowledge(String alertId, String username) {
        synchronized (monitors.lock()) {
            Alert stored = repository.findById(alertId)
                    .orElseThrow(() -> new NotFoundException("Alerte introuvable : " + alertId));
            long now = clock.millis();

            // Si l'alerte est active, c'est le moniteur qui la modifie (il garde ainsi la bonne version).
            // Sinon, Alert.acknowledge refuse l'action (alerte déjà résolue).
            Alert updated = monitors.of(stored.machineId())
                    .acknowledge(alertId, username, now)
                    .orElseGet(() -> stored.acknowledge(username, now));

            repository.save(updated);
            publishers.forEach(publisher -> publisher.publish(updated));
            return updated;
        }
    }
}
