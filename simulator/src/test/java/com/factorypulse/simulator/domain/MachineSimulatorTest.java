package com.factorypulse.simulator.domain;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MachineSimulatorTest {

    private static final long NOW = 1_700_000_000_000L;

    private static MachineConfig machine(double failure, double repair, double reject) {
        return new MachineConfig("M1", "Presse", 2.0, failure, repair, reject, 10);
    }

    private static List<MachineEvent> run(MachineSimulator simulator, int seconds) {
        List<MachineEvent> all = new ArrayList<>();
        for (int i = 0; i < seconds; i++) {
            all.addAll(simulator.tick(NOW + i * 1000L, 1.0));
        }
        return all;
    }

    private static long count(List<MachineEvent> events, EventType type) {
        return events.stream().filter(e -> e.type() == type).count();
    }

    @Test
    void sansPanneNiRebut_produitDesPiecesToutesBonnes_aUneCadenceProcheDeLaCadenceIdeale() {
        MachineSimulator simulator = new MachineSimulator(machine(0, 0, 0), new Random(42), 5);

        List<MachineEvent> events = run(simulator, 100);

        long pieces = count(events, EventType.PIECE_PRODUCED);
        // Cycle idéal de 2 s sur 100 s = 50 pièces max ; vitesse réelle entre 80 % et 100 % => au moins 40.
        assertThat(pieces).isBetween(40L, 50L);
        assertThat(events).filteredOn(e -> e.type() == EventType.PIECE_PRODUCED)
                .allMatch(e -> Boolean.TRUE.equals(e.good()));
        assertThat(count(events, EventType.MACHINE_DOWN)).isZero();
    }

    @Test
    void avecUnTauxDeRebutDe100Pourcent_toutesLesPiecesSontDesRebuts() {
        MachineSimulator simulator = new MachineSimulator(machine(0, 0, 1), new Random(1), 5);

        List<MachineEvent> events = run(simulator, 20);

        assertThat(count(events, EventType.PIECE_PRODUCED)).isPositive();
        assertThat(events).filteredOn(e -> e.type() == EventType.PIECE_PRODUCED)
                .allMatch(e -> Boolean.FALSE.equals(e.good()));
    }

    @Test
    void uneMachineEnPanne_neProduitPlus_puisRedemarreQuandElleEstReparee() {
        MachineSimulator simulator = new MachineSimulator(machine(1, 0, 0), new Random(7), 5);

        List<MachineEvent> first = simulator.tick(NOW, 1.0);
        assertThat(first).extracting(MachineEvent::type).contains(EventType.MACHINE_DOWN);
        assertThat(simulator.isRunning()).isFalse();

        // Réparation impossible : la machine reste à l'arrêt et ne produit rien.
        List<MachineEvent> whileDown = run(simulator, 10);
        assertThat(count(whileDown, EventType.PIECE_PRODUCED)).isZero();

        MachineSimulator repairable = new MachineSimulator(machine(1, 1, 0), new Random(7), 5);
        repairable.tick(NOW, 1.0);
        List<MachineEvent> next = repairable.tick(NOW + 1000, 1.0);
        assertThat(next).extracting(MachineEvent::type).contains(EventType.MACHINE_UP);
        assertThat(repairable.isRunning()).isTrue();
    }

    @Test
    void publieUnReleveDEnergieToutesLesCinqSecondes_avecLaBonneValeur() {
        MachineSimulator simulator = new MachineSimulator(machine(0, 0, 0), new Random(3), 5);

        List<MachineEvent> events = run(simulator, 10);

        List<MachineEvent> energy = events.stream().filter(e -> e.type() == EventType.ENERGY_READING).toList();
        assertThat(energy).hasSize(2);
        // 10 kW pendant 5 s = 10 * 5 / 3600 kWh
        assertThat(energy.get(0).energyKwh()).isCloseTo(10 * 5 / 3600.0, org.assertj.core.data.Offset.offset(1e-9));
    }

    @Test
    void refuseUneConfigurationInvalide() {
        assertThatThrownBy(() -> new MachineConfig("M1", "Presse", 0, 0, 0, 0, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MachineConfig("M1", "Presse", 2, 1.5, 0, 0, 10))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
