package com.factorypulse.alert.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests des règles d'alerte avec des événements écrits à la main.
 * Seuils utilisés : panne critique après 30 s ; rebut sur 10 pièces, alerte au-dessus de 20 %,
 * résolution à 10 % ou moins.
 */
class MachineMonitorTest {

    private static final long T0 = 1_700_000_000_000L;
    private static final AlertRules RULES = new AlertRules(30, 10, 0.20, 0.10);

    private static MachineEvent event(EventType type, long atSecond, Boolean good) {
        return new MachineEvent("e" + atSecond + type, "M1", type, T0 + atSecond * 1000, good, null);
    }

    private static MachineEvent down(long atSecond) {
        return event(EventType.MACHINE_DOWN, atSecond, null);
    }

    private static MachineEvent up(long atSecond) {
        return event(EventType.MACHINE_UP, atSecond, null);
    }

    private static MachineEvent energy(long atSecond) {
        return event(EventType.ENERGY_READING, atSecond, null);
    }

    private static MachineEvent piece(long atSecond, boolean good) {
        return event(EventType.PIECE_PRODUCED, atSecond, good);
    }

    // ------------------------- Pannes -------------------------

    @Test
    void unePanne_leveUneAlerteWarning() {
        MachineMonitor monitor = new MachineMonitor("M1", RULES);

        List<Alert> changes = monitor.apply(down(0));

        assertThat(changes).hasSize(1);
        Alert alert = changes.get(0);
        assertThat(alert.type()).isEqualTo(AlertType.DOWNTIME);
        assertThat(alert.severity()).isEqualTo(Severity.WARNING);
        assertThat(alert.isActive()).isTrue();
    }

    @Test
    void unePanneLongue_devientCritique_uneSeuleFois() {
        MachineMonitor monitor = new MachineMonitor("M1", RULES);
        monitor.apply(down(0));

        assertThat(monitor.apply(energy(25))).isEmpty();          // 25 s : pas encore
        List<Alert> changes = monitor.apply(energy(30));          // 30 s : critique
        assertThat(monitor.apply(energy(35))).isEmpty();          // déjà critique : rien de nouveau

        assertThat(changes).hasSize(1);
        assertThat(changes.get(0).severity()).isEqualTo(Severity.CRITICAL);
    }

    @Test
    void laRemiseEnMarche_resoutLAlerte_avecLaDuree() {
        MachineMonitor monitor = new MachineMonitor("M1", RULES);
        Alert raised = monitor.apply(down(0)).get(0);

        List<Alert> changes = monitor.apply(up(12));

        assertThat(changes).hasSize(1);
        Alert resolved = changes.get(0);
        assertThat(resolved.id()).isEqualTo(raised.id());          // même alerte, nouvelle version
        assertThat(resolved.isActive()).isFalse();
        assertThat(resolved.resolvedAt()).isEqualTo(T0 + 12_000);
        assertThat(resolved.message()).contains("12 s");
    }

    @Test
    void uneDeuxiemeAnnonceDePanne_nouvreRienDeNouveau() {
        MachineMonitor monitor = new MachineMonitor("M1", RULES);
        monitor.apply(down(0));
        assertThat(monitor.apply(down(2))).isEmpty();
    }

    @Test
    void uneRemiseEnMarcheSansPanne_estIgnoree() {
        assertThat(new MachineMonitor("M1", RULES).apply(up(0))).isEmpty();
    }

    // ------------------------- Rebuts -------------------------

    @Test
    void tropDeRebuts_leveUneAlerte() {
        MachineMonitor monitor = new MachineMonitor("M1", RULES);
        for (int i = 0; i < 7; i++) {
            assertThat(monitor.apply(piece(i, true))).isEmpty();
        }
        assertThat(monitor.apply(piece(7, false))).isEmpty();
        assertThat(monitor.apply(piece(8, false))).isEmpty();
        // 10e pièce : 3 rebuts sur 10 = 30 % > 20 %
        List<Alert> changes = monitor.apply(piece(9, false));

        assertThat(changes).hasSize(1);
        assertThat(changes.get(0).type()).isEqualTo(AlertType.HIGH_REJECT_RATE);
        assertThat(changes.get(0).message()).contains("30 %");
    }

    @Test
    void pasDAlerteDeRebutAvantDAvoirAssezDePieces() {
        MachineMonitor monitor = new MachineMonitor("M1", RULES);
        // 5 rebuts sur 5 pièces : 100 %, mais la fenêtre de 10 pièces n'est pas pleine
        for (int i = 0; i < 5; i++) {
            assertThat(monitor.apply(piece(i, false))).isEmpty();
        }
    }

    @Test
    void lAlerteDeRebutEstResolue_quandLeTauxRedescend() {
        MachineMonitor monitor = new MachineMonitor("M1", RULES);
        for (int i = 0; i < 7; i++) {
            monitor.apply(piece(i, true));
        }
        for (int i = 7; i < 10; i++) {
            monitor.apply(piece(i, false));               // alerte levée à la 10e pièce
        }
        // Les bonnes pièces font sortir les rebuts de la fenêtre.
        // 3 rebuts → 30 %, puis 20 % (pas encore résolue : hystérésis), puis 10 % : résolue.
        monitor.apply(piece(10, true));                   // fenêtre : 7 bonnes, 3 rebuts (une bonne sortie)
        monitor.apply(piece(11, true));
        monitor.apply(piece(12, true));
        monitor.apply(piece(13, true));
        monitor.apply(piece(14, true));
        monitor.apply(piece(15, true));
        monitor.apply(piece(16, true));                   // les 7 bonnes du début sont sorties : 3 rebuts
        assertThat(monitor.apply(piece(17, true))).isEmpty();          // 2 rebuts sur 10 = 20 %
        List<Alert> changes = monitor.apply(piece(18, true));          // 1 rebut sur 10 = 10 %

        assertThat(changes).hasSize(1);
        assertThat(changes.get(0).isActive()).isFalse();
    }

    // ------------------------- Reprise et robustesse -------------------------

    @Test
    void resume_reprendUnePanneEnCours() {
        MachineMonitor first = new MachineMonitor("M1", RULES);
        Alert active = first.apply(down(0)).get(0);

        // Le service redémarre : on recrée le moniteur à partir des alertes actives sauvegardées.
        MachineMonitor resumed = MachineMonitor.resume("M1", RULES, List.of(active));

        assertThat(resumed.apply(energy(30)).get(0).severity()).isEqualTo(Severity.CRITICAL);
        assertThat(resumed.apply(up(40)).get(0).id()).isEqualTo(active.id());
    }

    @Test
    void lIdentifiantEstStable_memeSiLEvenementEstRelu() {
        Alert a = new MachineMonitor("M1", RULES).apply(down(0)).get(0);
        Alert b = new MachineMonitor("M1", RULES).apply(down(0)).get(0);
        assertThat(a.id()).isEqualTo(b.id());
    }

    @Test
    void refuseUnEvenementDUneAutreMachine() {
        MachineEvent other = new MachineEvent("x", "M2", EventType.MACHINE_DOWN, T0, null, null);
        assertThatThrownBy(() -> new MachineMonitor("M1", RULES).apply(other))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void refuseDesSeuilsIncoherents() {
        assertThatThrownBy(() -> new AlertRules(30, 10, 0.10, 0.20))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
