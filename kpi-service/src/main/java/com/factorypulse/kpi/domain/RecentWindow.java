package com.factorypulse.kpi.domain;

import java.util.List;
import java.util.TreeMap;
import java.util.function.UnaryOperator;

/**
 * Fenêtre glissante des 15 dernières minutes d'une machine.
 *
 * Plutôt que de garder chaque événement, on regroupe les compteurs par minute
 * ({@link MinuteBucket}) : au plus 15 petites cases par machine. Quand une nouvelle minute
 * commence, les cases plus vieilles que 15 minutes sont supprimées.
 *
 * Les minutes sont calculées à partir de la date des événements, pas de l'horloge du serveur :
 * le résultat est le même en direct, en relisant l'historique Kafka ou dans un test.
 */
public class RecentWindow {

    public static final int WINDOW_MINUTES = 15;
    static final long MINUTE_MILLIS = 60_000;

    /** Les cases, triées par minute (la plus ancienne en premier). */
    private final TreeMap<Long, MinuteBucket> buckets = new TreeMap<>();

    public RecentWindow() {
    }

    /** Recrée la fenêtre à partir des cases sauvegardées. */
    public RecentWindow(List<MinuteBucket> saved) {
        if (saved != null) {
            saved.forEach(b -> buckets.put(b.minute(), b));
        }
    }

    void addRunning(long timestamp, long millis) {
        update(timestamp, b -> b.plusRun(millis));
    }

    void addDown(long timestamp, long millis) {
        update(timestamp, b -> b.plusDown(millis));
    }

    void addPiece(long timestamp, boolean good) {
        update(timestamp, b -> b.plusPiece(good));
    }

    private void update(long timestamp, UnaryOperator<MinuteBucket> change) {
        long minute = Math.floorDiv(timestamp, MINUTE_MILLIS);
        buckets.put(minute, change.apply(buckets.getOrDefault(minute, MinuteBucket.empty(minute))));
        // On ne garde que les 15 dernières minutes (la minute en cours comprise).
        buckets.headMap(buckets.lastKey() - WINDOW_MINUTES + 1).clear();
    }

    /** Calcule les indicateurs sur les minutes de la fenêtre. */
    public RecentKpi kpi(double idealCycleSeconds) {
        long run = 0;
        long down = 0;
        long pieces = 0;
        long good = 0;
        for (MinuteBucket b : buckets.values()) {
            run += b.runMillis();
            down += b.downMillis();
            pieces += b.piecesProduced();
            good += b.goodPieces();
        }
        double availability = OeeFormulas.availability(run, down);
        double performance = OeeFormulas.performance(pieces, idealCycleSeconds, run);
        double quality = OeeFormulas.quality(good, pieces);
        return new RecentKpi(WINDOW_MINUTES, availability, performance, quality,
                availability * performance * quality, pieces, pieces - good);
    }

    /** Les cases actuelles, pour la sauvegarde. */
    public List<MinuteBucket> buckets() {
        return List.copyOf(buckets.values());
    }
}
