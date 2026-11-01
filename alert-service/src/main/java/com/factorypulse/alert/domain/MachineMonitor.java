package com.factorypulse.alert.domain;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

/**
 * Surveille UNE machine et décide quand lever, aggraver ou résoudre une alerte.
 *
 * Java pur, sans Spring ni Kafka : on le teste avec des événements écrits à la main.
 *
 * Règles :
 *  1. PANNE : à MACHINE_DOWN, alerte "en panne" (WARNING). Si la panne dure plus que le seuil,
 *     l'alerte passe CRITICAL. À MACHINE_UP, l'alerte est résolue avec la durée de la panne.
 *  2. REBUTS : on garde la qualité des N dernières pièces. Si le taux de rebut dépasse le seuil,
 *     alerte (WARNING). Elle est résolue quand le taux redescend sous le seuil de résolution.
 *
 * Le temps vient de la date des événements, pas de l'horloge du serveur. Le simulateur envoie
 * un relevé d'énergie toutes les 5 s, même en panne : c'est ce qui fait "avancer" la panne.
 */
public class MachineMonitor {

    private final String machineId;
    private final AlertRules rules;

    private Alert downtimeAlert;   // alerte de panne en cours, ou null
    private Alert rejectAlert;     // alerte de rebut en cours, ou null

    /** Qualité des dernières pièces : true = bonne, false = rebut. */
    private final Deque<Boolean> lastPieces = new ArrayDeque<>();
    private int rejectsInWindow;

    public MachineMonitor(String machineId, AlertRules rules) {
        this.machineId = machineId;
        this.rules = rules;
    }

    /**
     * Reprend la surveillance après un redémarrage, à partir des alertes encore actives.
     * (Les dernières pièces ne sont pas sauvegardées : la fenêtre se remplit à nouveau.)
     */
    public static MachineMonitor resume(String machineId, AlertRules rules, Collection<Alert> activeAlerts) {
        MachineMonitor monitor = new MachineMonitor(machineId, rules);
        for (Alert alert : activeAlerts) {
            switch (alert.type()) {
                case DOWNTIME -> monitor.downtimeAlert = alert;
                case HIGH_REJECT_RATE -> monitor.rejectAlert = alert;
            }
        }
        return monitor;
    }

    /**
     * Prend en compte un événement de cette machine.
     *
     * @return les alertes créées ou modifiées par cet événement (souvent aucune)
     */
    public List<Alert> apply(MachineEvent event) {
        if (!machineId.equals(event.machineId())) {
            throw new IllegalArgumentException(
                    "Événement de " + event.machineId() + " reçu par le moniteur de " + machineId);
        }
        List<Alert> changes = new ArrayList<>();
        long at = event.timestamp();

        switch (event.type()) {
            case MACHINE_DOWN -> onDown(at, changes);
            case MACHINE_UP -> onUp(at, changes);
            case PIECE_PRODUCED -> onPiece(Boolean.TRUE.equals(event.good()), at, changes);
            case ENERGY_READING -> { /* rien de particulier : sert seulement d'horloge */ }
        }
        checkProlongedDowntime(at, changes);
        return changes;
    }

    // ------------------------- Règle 1 : pannes -------------------------

    private void onDown(long at, List<Alert> changes) {
        if (downtimeAlert != null) {
            return;   // déjà en panne
        }
        downtimeAlert = Alert.raise(machineId, AlertType.DOWNTIME, Severity.WARNING,
                machineId + " en panne", at);
        changes.add(downtimeAlert);
    }

    private void checkProlongedDowntime(long at, List<Alert> changes) {
        if (downtimeAlert == null || downtimeAlert.severity() == Severity.CRITICAL) {
            return;
        }
        long downSeconds = (at - downtimeAlert.raisedAt()) / 1000;
        if (downSeconds >= rules.prolongedDowntimeSeconds()) {
            downtimeAlert = downtimeAlert.escalate(Severity.CRITICAL,
                    machineId + " en panne depuis plus de " + rules.prolongedDowntimeSeconds() + " s", at);
            changes.add(downtimeAlert);
        }
    }

    private void onUp(long at, List<Alert> changes) {
        if (downtimeAlert == null) {
            return;
        }
        long downSeconds = Math.max(0, (at - downtimeAlert.raisedAt()) / 1000);
        changes.add(downtimeAlert.resolve(machineId + " remise en marche après " + downSeconds + " s de panne", at));
        downtimeAlert = null;
    }

    // ------------------------- Règle 2 : rebuts -------------------------

    private void onPiece(boolean good, long at, List<Alert> changes) {
        lastPieces.addLast(good);
        if (!good) {
            rejectsInWindow++;
        }
        if (lastPieces.size() > rules.rejectWindowSize() && !lastPieces.removeFirst()) {
            rejectsInWindow--;
        }
        if (lastPieces.size() < rules.rejectWindowSize()) {
            return;   // pas encore assez de pièces pour juger
        }

        double rate = (double) rejectsInWindow / rules.rejectWindowSize();
        if (rejectAlert == null && rate > rules.rejectRateThreshold()) {
            rejectAlert = Alert.raise(machineId, AlertType.HIGH_REJECT_RATE, Severity.WARNING,
                    rejectMessage(rate), at);
            changes.add(rejectAlert);
        } else if (rejectAlert != null && rate <= rules.rejectRateClearThreshold()) {
            changes.add(rejectAlert.resolve(machineId + " : taux de rebut revenu à " + percent(rate), at));
            rejectAlert = null;
        }
    }

    private String rejectMessage(double rate) {
        return machineId + " : taux de rebut de " + percent(rate)
                + " sur les " + rules.rejectWindowSize() + " dernières pièces";
    }

    private static String percent(double rate) {
        return Math.round(rate * 100) + " %";
    }

    /**
     * Prise en charge d'une alerte active de cette machine par un opérateur.
     * Le moniteur garde ainsi la bonne version : quand l'alerte s'aggravera ou sera résolue,
     * la prise en charge sera conservée.
     *
     * @return la nouvelle version de l'alerte, ou vide si ce moniteur n'a pas d'alerte active avec cet identifiant
     */
    public Optional<Alert> acknowledge(String alertId, String username, long at) {
        if (downtimeAlert != null && downtimeAlert.id().equals(alertId)) {
            downtimeAlert = downtimeAlert.acknowledge(username, at);
            return Optional.of(downtimeAlert);
        }
        if (rejectAlert != null && rejectAlert.id().equals(alertId)) {
            rejectAlert = rejectAlert.acknowledge(username, at);
            return Optional.of(rejectAlert);
        }
        return Optional.empty();
    }

    public String machineId() {
        return machineId;
    }
}
