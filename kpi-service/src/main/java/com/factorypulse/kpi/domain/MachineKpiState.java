package com.factorypulse.kpi.domain;

import java.util.List;

/**
 * Les compteurs bruts d'une machine : tout ce qu'il faut sauvegarder pour reprendre le calcul
 * après un redémarrage du service. Les indicateurs (TRS…) se recalculent à partir de ces compteurs.
 *
 * {@code recentMinutes} contient les compteurs minute par minute des 15 dernières minutes.
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
        long downCount,
        List<MinuteBucket> recentMinutes) {
}
