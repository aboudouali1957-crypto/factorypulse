package com.factorypulse.simulator.domain;

/**
 * Caractéristiques d'une machine simulée (lues dans application.yml).
 *
 * @param id                 identifiant court, ex. "M1"
 * @param name               nom lisible, ex. "Presse"
 * @param idealCycleSeconds  temps de cycle idéal pour produire une pièce, en secondes
 * @param failureProbability probabilité de tomber en panne à chaque seconde de marche (ex. 0.005)
 * @param repairProbability  probabilité d'être réparée à chaque seconde d'arrêt (ex. 0.05)
 * @param rejectRate         part des pièces produites qui sont des rebuts (ex. 0.03 = 3 %)
 * @param powerKw            puissance consommée en marche, en kW
 */
public record MachineConfig(
        String id,
        String name,
        double idealCycleSeconds,
        double failureProbability,
        double repairProbability,
        double rejectRate,
        double powerKw) {

    public MachineConfig {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("L'id de la machine est obligatoire");
        }
        if (idealCycleSeconds <= 0) {
            throw new IllegalArgumentException("idealCycleSeconds doit être > 0 pour " + id);
        }
        checkProbability("failureProbability", failureProbability, id);
        checkProbability("repairProbability", repairProbability, id);
        checkProbability("rejectRate", rejectRate, id);
        if (powerKw < 0) {
            throw new IllegalArgumentException("powerKw doit être >= 0 pour " + id);
        }
    }

    private static void checkProbability(String field, double value, String id) {
        if (value < 0 || value > 1) {
            throw new IllegalArgumentException(field + " doit être entre 0 et 1 pour " + id);
        }
    }
}
