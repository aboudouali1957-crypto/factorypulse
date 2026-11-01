package com.factorypulse.alert.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Une intervention invalide ne peut pas exister : tout est vérifié à la création. */
class InterventionTest {

    private static Intervention intervention(String machine, String description, int minutes) {
        return new Intervention("i1", machine, null, InterventionCause.ELECTRIQUE, description, minutes, "operateur", 1000);
    }

    @Test
    void uneInterventionValide() {
        Intervention i = intervention("M1", "  Capteur remplacé  ", 30);
        assertThat(i.description()).isEqualTo("Capteur remplacé");   // espaces retirés
    }

    @Test
    void refuseUneMachineInvalide() {
        assertThatThrownBy(() -> intervention("machine 1", "x", 30)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> intervention(null, "x", 30)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void refuseUneDescriptionVideOuTropLongue() {
        assertThatThrownBy(() -> intervention("M1", "   ", 30)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> intervention("M1", "x".repeat(501), 30)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void refuseUneDureeHorsLimites() {
        assertThatThrownBy(() -> intervention("M1", "x", 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> intervention("M1", "x", 24 * 60 + 1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unIdentifiantDAlerteVide_devientNull() {
        Intervention i = new Intervention("i1", "M1", " ", InterventionCause.AUTRE, "x", 5, "op", 1000);
        assertThat(i.alertId()).isNull();
    }
}
