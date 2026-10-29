package com.factorypulse.kpi.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Tests du calcul des indicateurs avec des dates choisies à la main : on connaît le résultat exact attendu.
 */
class MachineKpiTest {

    private static final long T0 = 1_700_000_000_000L;   // une date de départ quelconque
    private static final double CYCLE = 2.0;              // cycle idéal : une pièce toutes les 2 s

    private static MachineEvent piece(long atSecond, boolean good) {
        return new MachineEvent("e" + atSecond + good, "M1", EventType.PIECE_PRODUCED, T0 + atSecond * 1000, good, null);
    }

    private static MachineEvent down(long atSecond) {
        return new MachineEvent("d" + atSecond, "M1", EventType.MACHINE_DOWN, T0 + atSecond * 1000, null, null);
    }

    private static MachineEvent up(long atSecond) {
        return new MachineEvent("u" + atSecond, "M1", EventType.MACHINE_UP, T0 + atSecond * 1000, null, null);
    }

    private static MachineEvent energy(long atSecond, double kwh) {
        return new MachineEvent("n" + atSecond, "M1", EventType.ENERGY_READING, T0 + atSecond * 1000, null, kwh);
    }

    /**
     * Scénario de référence sur 10 secondes :
     *  - 0 s à 8 s : en marche, 4 pièces (dont 1 rebut) ;
     *  - 8 s : panne ; 10 s : réparation.
     * Temps de marche = 8 s, temps d'arrêt = 2 s.
     */
    private static MachineKpi referenceScenario() {
        MachineKpi kpi = new MachineKpi("M1", CYCLE);
        kpi.apply(energy(0, 0.0));        // premier événement : démarre l'horloge
        kpi.apply(piece(2, true));
        kpi.apply(piece(4, true));
        kpi.apply(piece(6, false));       // rebut
        kpi.apply(piece(8, true));
        kpi.apply(down(8));
        kpi.apply(up(10));
        return kpi;
    }

    @Test
    void disponibilite_tempsDeMarcheSurTempsTotal() {
        // 8 s de marche sur 10 s
        assertThat(referenceScenario().availability()).isCloseTo(0.8, within(1e-9));
    }

    @Test
    void performance_piecesFoisCycleIdealSurTempsDeMarche() {
        // 4 pièces × 2 s = 8 s de travail idéal, pour 8 s de marche => 100 %
        assertThat(referenceScenario().performance()).isCloseTo(1.0, within(1e-9));
    }

    @Test
    void qualite_etTauxDeRebut() {
        MachineKpi kpi = referenceScenario();
        assertThat(kpi.quality()).isCloseTo(0.75, within(1e-9));      // 3 bonnes sur 4
        assertThat(kpi.rejectRate()).isCloseTo(0.25, within(1e-9));   // 1 rebut sur 4
        assertThat(kpi.rejectedPieces()).isEqualTo(1);
    }

    @Test
    void trs_estLeProduitDesTroisIndicateurs() {
        // TRS = 0,8 × 1,0 × 0,75 = 0,6
        assertThat(referenceScenario().oee()).isCloseTo(0.6, within(1e-9));
    }

    @Test
    void performance_baisseQuandLaMachineEstPlusLenteQueLeCycleIdeal() {
        MachineKpi kpi = new MachineKpi("M1", CYCLE);
        kpi.apply(energy(0, 0.0));
        kpi.apply(piece(4, true));
        kpi.apply(piece(8, true));
        kpi.apply(energy(10, 0.0));
        // 2 pièces × 2 s = 4 s de travail idéal, pour 10 s de marche => 40 %
        assertThat(kpi.performance()).isCloseTo(0.4, within(1e-9));
    }

    @Test
    void cadence_piecesParHeure() {
        // 4 pièces en 10 s => 4 × 360 = 1 440 pièces par heure
        assertThat(referenceScenario().throughputPerHour()).isCloseTo(1440, within(1e-6));
    }

    @Test
    void energie_additionneLesReleves() {
        MachineKpi kpi = new MachineKpi("M1", CYCLE);
        kpi.apply(energy(5, 0.02));
        kpi.apply(energy(10, 0.03));
        assertThat(kpi.snapshot().energyKwh()).isCloseTo(0.05, within(1e-9));
    }

    @Test
    void compteLesPannes_etLEtatCourant() {
        MachineKpi kpi = referenceScenario();
        assertThat(kpi.snapshot().downCount()).isEqualTo(1);
        assertThat(kpi.snapshot().running()).isTrue();     // réparée à 10 s
    }

    @Test
    void unLongTrouEntreDeuxEvenements_nestPasCompte() {
        MachineKpi kpi = new MachineKpi("M1", CYCLE);
        kpi.apply(energy(0, 0.0));
        kpi.apply(energy(5, 0.0));         // 5 s comptées
        kpi.apply(energy(3605, 0.0));      // 1 h plus tard : le simulateur était arrêté, on ne compte pas
        kpi.apply(energy(3610, 0.0));      // 5 s comptées
        assertThat(kpi.state().runMillis()).isEqualTo(10_000);
    }

    @Test
    void sansAucunEvenement_tousLesIndicateursValentZero() {
        MachineKpi kpi = new MachineKpi("M1", CYCLE);
        assertThat(kpi.oee()).isZero();
        assertThat(kpi.availability()).isZero();
        assertThat(kpi.throughputPerHour()).isZero();
    }

    @Test
    void restore_reprendExactementLesMemesIndicateurs() {
        MachineKpi original = referenceScenario();
        MachineKpi restored = MachineKpi.restore(original.state());
        assertThat(restored.snapshot()).isEqualTo(original.snapshot());
    }

    @Test
    void refuseUnEvenementDUneAutreMachine() {
        MachineKpi kpi = new MachineKpi("M1", CYCLE);
        MachineEvent other = new MachineEvent("x", "M2", EventType.PIECE_PRODUCED, T0, true, null);
        assertThatThrownBy(() -> kpi.apply(other)).isInstanceOf(IllegalArgumentException.class);
    }
}
