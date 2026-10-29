package com.factorypulse.kpi.domain;

/**
 * Les indicateurs d'une machine à un instant donné. C'est ce qu'on affiche (API REST, puis tableau de bord au jour 3).
 *
 * Les ratios sont entre 0 et 1 (0,85 = 85 %).
 *
 * @param machineId         identifiant de la machine
 * @param running           la machine tourne-t-elle actuellement ?
 * @param availability      disponibilité = temps de marche / temps total
 * @param performance       performance = (pièces × cycle idéal) / temps de marche
 * @param quality           qualité = pièces bonnes / pièces produites
 * @param oee               TRS (en anglais OEE) = disponibilité × performance × qualité
 * @param piecesProduced    nombre total de pièces produites
 * @param goodPieces        nombre de pièces bonnes
 * @param rejectedPieces    nombre de rebuts
 * @param rejectRate        taux de rebut = rebuts / pièces produites
 * @param throughputPerHour cadence = pièces produites par heure
 * @param energyKwh         énergie totale consommée, en kWh
 * @param downCount         nombre de pannes
 * @param updatedAt         date du dernier événement pris en compte (millisecondes epoch)
 */
public record KpiSnapshot(
        String machineId,
        boolean running,
        double availability,
        double performance,
        double quality,
        double oee,
        long piecesProduced,
        long goodPieces,
        long rejectedPieces,
        double rejectRate,
        double throughputPerHour,
        double energyKwh,
        long downCount,
        Long updatedAt) {
}
