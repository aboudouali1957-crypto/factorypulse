package com.factorypulse.kpi.domain;

/**
 * Les formules du TRS, partagées par le calcul cumulé ({@link MachineKpi}) et le calcul
 * sur les dernières minutes ({@link RecentWindow}). Tous les ratios sont entre 0 et 1.
 */
final class OeeFormulas {

    private OeeFormulas() {
    }

    /** Disponibilité = temps de marche / (temps de marche + temps d'arrêt). */
    static double availability(long runMillis, long downMillis) {
        long total = runMillis + downMillis;
        return total == 0 ? 0 : (double) runMillis / total;
    }

    /** Performance = (pièces produites × cycle idéal) / temps de marche, plafonnée à 100 %. */
    static double performance(long piecesProduced, double idealCycleSeconds, long runMillis) {
        if (runMillis == 0) {
            return 0;
        }
        double idealMillis = piecesProduced * idealCycleSeconds * 1000;
        return Math.min(1.0, idealMillis / runMillis);
    }

    /** Qualité = pièces bonnes / pièces produites. */
    static double quality(long goodPieces, long piecesProduced) {
        return piecesProduced == 0 ? 0 : (double) goodPieces / piecesProduced;
    }
}
