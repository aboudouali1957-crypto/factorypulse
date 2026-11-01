package com.factorypulse.alert.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AlertTest {

    private static final Alert PANNE = Alert.raise("M1", AlertType.DOWNTIME, Severity.WARNING, "M1 en panne", 1000);

    @Test
    void acquitter_enregistreQuiEtQuand() {
        Alert acked = PANNE.acknowledge("operateur", 5000);
        assertThat(acked.acknowledgedBy()).isEqualTo("operateur");
        assertThat(acked.acknowledgedAt()).isEqualTo(5000L);
        assertThat(acked.updatedAt()).isEqualTo(5000L);
        assertThat(acked.isActive()).isTrue();
    }

    @Test
    void laDateDeMiseAJourNeReculeJamais() {
        // Un événement daté de 4 s arrive après un acquittement à 5 s.
        Alert critical = PANNE.acknowledge("operateur", 5000).escalate(Severity.CRITICAL, "longue", 4000);
        assertThat(critical.updatedAt()).isEqualTo(5000L);
        assertThat(critical.acknowledgedBy()).isEqualTo("operateur");
    }

    @Test
    void refuseDAcquitterSansNom() {
        assertThatThrownBy(() -> PANNE.acknowledge(" ", 5000)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void refuseDAcquitterUneAlerteResolue() {
        Alert resolved = PANNE.resolve("M1 remise en marche", 2000);
        assertThatThrownBy(() -> resolved.acknowledge("operateur", 5000))
                .isInstanceOf(AlertActionRejectedException.class);
    }
}
