package com.factorypulse.kpi.domain;

/**
 * Les compteurs bruts d'une machine : tout ce qu'il faut sauvegarder pour reprendre le calcul
 * après un redémarrage du service. Les indicateurs (TRS…) se recalculent à partir de ces compteurs.
 */
public record MachineKpiState(
        String machineId,
        double idealCycleSeconds,
        boolean running,
        Long lastTimestamp,
        long runMillis,
        long downMillis,
        long piecesProduced,
        long goodPieces,
        double energyKwh,
        long downCount) {
}
