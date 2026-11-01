package com.factorypulse.kpi.domain;

/**
 * Les indicateurs d'une machine sur les dernières minutes seulement (fenêtre glissante).
 *
 * Le TRS cumulé depuis le démarrage bouge de moins en moins avec le temps : une panne de 30 s
 * pèse peu sur des heures de production. Sur une fenêtre de 15 minutes, la même panne
 * se voit tout de suite, puis disparaît quand elle sort de la fenêtre.
 *
 * @param windowMinutes  durée de la fenêtre, en minutes
 * @param availability   disponibilité sur la fenêtre
 * @param performance    performance sur la fenêtre
 * @param quality        qualité sur la fenêtre
 * @param oee            TRS sur la fenêtre
 * @param piecesProduced pièces produites pendant la fenêtre
 * @param rejectedPieces rebuts pendant la fenêtre
 */
public record RecentKpi(
        int windowMinutes,
        double availability,
        double performance,
        double quality,
        double oee,
        long piecesProduced,
        long rejectedPieces) {
}
