package com.factorypulse.kpi.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Tests du TRS sur les 15 dernières minutes (fenêtre glissante).
 *
 * Le simulateur envoie au moins un événement toutes les 5 secondes (relevé d'énergie) :
 * on fait pareil ici pour que le temps soit bien compté.
 */
class RecentWindowTest {

    private static final long T0 = 1_700_000_040_000L;   // début d'une minute pile
    private static final double CYCLE = 2.0;

    private static MachineEvent event(EventType type, long atSecond, Boolean good) {
        return new MachineEvent("e" + type + atSecond, "M1", type, T0 + atSecond * 1000, good, null);
    }

    /** La machine tourne parfaitement de fromSecond à toSecond : une bonne pièce toutes les 2 s. */
    private static void runPerfectly(MachineKpi kpi, long fromSecond, long toSecond) {
        for (long s = fromSecond; s <= toSecond; s += 2) {
            kpi.apply(event(EventType.PIECE_PRODUCED, s, true));
        }
    }

    /** La machine est en panne de fromSecond à toSecond (relevés d'énergie toutes les 5 s). */
    private static void stayDown(MachineKpi kpi, long fromSecond, long toSecond) {
        kpi.apply(event(EventType.MACHINE_DOWN, fromSecond, null));
        for (long s = fromSecond + 5; s < toSecond; s += 5) {
            kpi.apply(event(EventType.ENERGY_READING, s, null));
        }
        kpi.apply(event(EventType.MACHINE_UP, toSecond, null));
    }

    @Test
    void auDebut_leTrsRecentEgaleLeTrsCumule() {
        MachineKpi kpi = new MachineKpi("M1", CYCLE);
        runPerfectly(kpi, 0, 120);
        stayDown(kpi, 120, 150);

        RecentKpi recent = kpi.recent();
        assertThat(recent.oee()).isCloseTo(kpi.oee(), within(1e-9));
        assertThat(recent.windowMinutes()).isEqualTo(15);
    }

    @Test
    void unePanneRecente_pesePlusSurLeTrsRecentQueSurLeCumule() {
        MachineKpi kpi = new MachineKpi("M1", CYCLE);
        runPerfectly(kpi, 0, 60 * 60);              // 1 h parfaite
        stayDown(kpi, 3600, 3600 + 5 * 60);         // puis 5 min de panne

        // Cumulé : 5 min d'arrêt sur 65 min, soit environ 92 %.
        // Récent : 5 min d'arrêt sur environ 15 min, soit environ 67 % (la minute en cours compte aussi).
        assertThat(kpi.availability()).isCloseTo(60.0 / 65, within(0.001));
        assertThat(kpi.recent().availability()).isCloseTo(10.0 / 15, within(0.05));
    }

    @Test
    void unePanneAncienne_sortDeLaFenetre() {
        MachineKpi kpi = new MachineKpi("M1", CYCLE);
        runPerfectly(kpi, 0, 60);
        stayDown(kpi, 60, 360);                      // 5 min de panne
        runPerfectly(kpi, 360, 360 + 20 * 60);       // puis 20 min parfaites

        assertThat(kpi.recent().availability()).isCloseTo(1.0, within(1e-9));
        assertThat(kpi.availability()).isLessThan(0.9);   // le cumulé, lui, s'en souvient
    }

    @Test
    void compteLesPiecesEtRebutsDeLaFenetre() {
        MachineKpi kpi = new MachineKpi("M1", CYCLE);
        kpi.apply(event(EventType.PIECE_PRODUCED, 0, true));
        kpi.apply(event(EventType.PIECE_PRODUCED, 2, false));
        kpi.apply(event(EventType.PIECE_PRODUCED, 4, true));

        assertThat(kpi.recent().piecesProduced()).isEqualTo(3);
        assertThat(kpi.recent().rejectedPieces()).isEqualTo(1);
    }

    @Test
    void neGardeQueQuinzeMinutes() {
        MachineKpi kpi = new MachineKpi("M1", CYCLE);
        runPerfectly(kpi, 0, 40 * 60);
        assertThat(kpi.state().recentMinutes()).hasSize(15);
    }

    @Test
    void restore_reprendLaFenetre() {
        MachineKpi original = new MachineKpi("M1", CYCLE);
        runPerfectly(original, 0, 300);
        stayDown(original, 300, 400);

        MachineKpi restored = MachineKpi.restore(original.state());
        assertThat(restored.recent()).isEqualTo(original.recent());
    }
}
