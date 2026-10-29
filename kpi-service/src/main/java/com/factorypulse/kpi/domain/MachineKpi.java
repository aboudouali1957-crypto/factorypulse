package com.factorypulse.kpi.domain;

/**
 * Calcule les indicateurs d'UNE machine à partir de ses événements, un par un.
 *
 * Java pur, sans Spring ni base de données : on peut le tester avec des dates choisies à la main.
 *
 * Le principe : la machine a un état (en marche ou en panne). À chaque événement, on regarde
 * combien de temps s'est écoulé depuis le précédent, et on ajoute ce temps au compteur
 * "temps de marche" ou "temps d'arrêt" selon l'état. Ensuite on applique l'événement
 * (pièce, panne, réparation, énergie).
 */
public class MachineKpi {

    /**
     * Au-delà de cet écart entre deux événements, on considère que le simulateur était arrêté
     * (par exemple entre hier soir et aujourd'hui) : ce temps n'est compté ni en marche ni en panne.
     */
    static final long MAX_GAP_MILLIS = 10_000;

    private final String machineId;
    private final double idealCycleSeconds;

    private boolean running = true;   // le simulateur démarre ses machines en marche
    private Long lastTimestamp;       // null tant qu'aucun événement n'a été reçu
    private long runMillis;
    private long downMillis;
    private long piecesProduced;
    private long goodPieces;
    private double energyKwh;
    private long downCount;

    public MachineKpi(String machineId, double idealCycleSeconds) {
        if (machineId == null || machineId.isBlank()) {
            throw new IllegalArgumentException("machineId est obligatoire");
        }
        if (idealCycleSeconds <= 0) {
            throw new IllegalArgumentException("idealCycleSeconds doit être > 0 pour " + machineId);
        }
        this.machineId = machineId;
        this.idealCycleSeconds = idealCycleSeconds;
    }

    /** Recrée une machine à partir de ses compteurs sauvegardés. */
    public static MachineKpi restore(MachineKpiState state) {
        MachineKpi kpi = new MachineKpi(state.machineId(), state.idealCycleSeconds());
        kpi.running = state.running();
        kpi.lastTimestamp = state.lastTimestamp();
        kpi.runMillis = state.runMillis();
        kpi.downMillis = state.downMillis();
        kpi.piecesProduced = state.piecesProduced();
        kpi.goodPieces = state.goodPieces();
        kpi.energyKwh = state.energyKwh();
        kpi.downCount = state.downCount();
        return kpi;
    }

    /** Prend en compte un nouvel événement de cette machine. */
    public void apply(MachineEvent event) {
        if (!machineId.equals(event.machineId())) {
            throw new IllegalArgumentException(
                    "Événement de " + event.machineId() + " reçu par la machine " + machineId);
        }

        advanceClock(event.timestamp());

        switch (event.type()) {
            case PIECE_PRODUCED -> {
                piecesProduced++;
                if (Boolean.TRUE.equals(event.good())) {
                    goodPieces++;
                }
            }
            case MACHINE_DOWN -> {
                if (running) {
                    running = false;
                    downCount++;
                }
            }
            case MACHINE_UP -> running = true;
            case ENERGY_READING -> {
                if (event.energyKwh() != null) {
                    energyKwh += event.energyKwh();
                }
            }
        }
    }

    /** Ajoute le temps écoulé depuis le dernier événement au compteur de marche ou d'arrêt. */
    private void advanceClock(long timestamp) {
        if (lastTimestamp == null) {
            lastTimestamp = timestamp;
            return;
        }
        long elapsed = timestamp - lastTimestamp;
        if (elapsed <= 0) {
            return;                 // événement de la même seconde (ou plus ancien) : rien à ajouter
        }
        if (elapsed <= MAX_GAP_MILLIS) {
            if (running) {
                runMillis += elapsed;
            } else {
                downMillis += elapsed;
            }
        }
        lastTimestamp = timestamp;
    }

    // ------------------------- Indicateurs -------------------------

    /** Disponibilité = temps de marche / (temps de marche + temps d'arrêt). */
    public double availability() {
        long total = runMillis + downMillis;
        return total == 0 ? 0 : (double) runMillis / total;
    }

    /** Performance = (pièces produites × cycle idéal) / temps de marche, plafonnée à 100 %. */
    public double performance() {
        if (runMillis == 0) {
            return 0;
        }
        double idealMillis = piecesProduced * idealCycleSeconds * 1000;
        return Math.min(1.0, idealMillis / runMillis);
    }

    /** Qualité = pièces bonnes / pièces produites. */
    public double quality() {
        return piecesProduced == 0 ? 0 : (double) goodPieces / piecesProduced;
    }

    /** TRS = disponibilité × performance × qualité. */
    public double oee() {
        return availability() * performance() * quality();
    }

    public long rejectedPieces() {
        return piecesProduced - goodPieces;
    }

    /** Taux de rebut = rebuts / pièces produites. */
    public double rejectRate() {
        return piecesProduced == 0 ? 0 : (double) rejectedPieces() / piecesProduced;
    }

    /** Cadence = pièces produites par heure, sur le temps total observé. */
    public double throughputPerHour() {
        long total = runMillis + downMillis;
        return total == 0 ? 0 : piecesProduced / (total / 3_600_000.0);
    }

    public KpiSnapshot snapshot() {
        return new KpiSnapshot(machineId, running, availability(), performance(), quality(), oee(),
                piecesProduced, goodPieces, rejectedPieces(), rejectRate(), throughputPerHour(),
                energyKwh, downCount, lastTimestamp);
    }

    public MachineKpiState state() {
        return new MachineKpiState(machineId, idealCycleSeconds, running, lastTimestamp,
                runMillis, downMillis, piecesProduced, goodPieces, energyKwh, downCount);
    }

    public String machineId() {
        return machineId;
    }
}
