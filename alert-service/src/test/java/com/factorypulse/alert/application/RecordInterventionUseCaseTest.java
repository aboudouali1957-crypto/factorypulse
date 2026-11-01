package com.factorypulse.alert.application;

import com.factorypulse.alert.domain.Alert;
import com.factorypulse.alert.domain.AlertType;
import com.factorypulse.alert.domain.Intervention;
import com.factorypulse.alert.domain.InterventionCause;
import com.factorypulse.alert.domain.NotFoundException;
import com.factorypulse.alert.domain.Severity;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecordInterventionUseCaseTest {

    private static final Clock CLOCK = Clock.fixed(Instant.ofEpochMilli(1_700_000_000_000L), ZoneOffset.UTC);

    private final InMemoryAdapters.InterventionStore interventions = new InMemoryAdapters.InterventionStore();
    private final InMemoryAdapters.AlertStore alerts = new InMemoryAdapters.AlertStore();
    private int nextId = 1;
    private final RecordInterventionUseCase useCase =
            new RecordInterventionUseCase(interventions, alerts, CLOCK, () -> "i" + nextId++);

    private static RecordInterventionUseCase.Command command(String machine, String alertId) {
        return new RecordInterventionUseCase.Command(machine, alertId, InterventionCause.MECANIQUE,
                "Remplacement de la courroie", 25);
    }

    @Test
    void enregistreLIntervention_avecLOperateurDuJeton() {
        Intervention saved = useCase.record(command("M1", null), "operateur");

        assertThat(saved.performedBy()).isEqualTo("operateur");
        assertThat(saved.createdAt()).isEqualTo(1_700_000_000_000L);
        assertThat(interventions.store).hasSize(1);
    }

    @Test
    void peutEtreLieeAUneAlerteDeLaMemeMachine() {
        Alert panne = Alert.raise("M1", AlertType.DOWNTIME, Severity.WARNING, "M1 en panne", 1000);
        alerts.save(panne);

        assertThat(useCase.record(command("M1", panne.id()), "operateur").alertId()).isEqualTo(panne.id());
    }

    @Test
    void refuseUneAlerteDUneAutreMachine() {
        Alert panne = Alert.raise("M2", AlertType.DOWNTIME, Severity.WARNING, "M2 en panne", 1000);
        alerts.save(panne);

        assertThatThrownBy(() -> useCase.record(command("M1", panne.id()), "operateur"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(interventions.store).isEmpty();
    }

    @Test
    void refuseUneAlerteInconnue() {
        assertThatThrownBy(() -> useCase.record(command("M1", "inconnue"), "operateur"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void historique_duPlusRecentAuPlusAncien_etLimite() {
        interventions.save(new Intervention("a", "M1", null, InterventionCause.REGLAGE, "x", 5, "op", 1000));
        interventions.save(new Intervention("b", "M2", null, InterventionCause.REGLAGE, "y", 5, "op", 3000));
        interventions.save(new Intervention("c", "M3", null, InterventionCause.REGLAGE, "z", 5, "op", 2000));

        GetInterventionsUseCase get = new GetInterventionsUseCase(interventions);
        assertThat(get.recent(2)).extracting(Intervention::id).containsExactly("b", "c");
    }
}
