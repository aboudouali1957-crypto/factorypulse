package com.factorypulse.alert.application;

import com.factorypulse.alert.domain.Alert;
import com.factorypulse.alert.domain.AlertActionRejectedException;
import com.factorypulse.alert.domain.AlertRules;
import com.factorypulse.alert.domain.EventType;
import com.factorypulse.alert.domain.MachineEvent;
import com.factorypulse.alert.domain.NotFoundException;
import com.factorypulse.alert.domain.Severity;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AcknowledgeAlertUseCaseTest {

    private static final long T0 = 1_700_000_000_000L;
    private static final AlertRules RULES = new AlertRules(30, 10, 0.20, 0.10);
    /** Horloge figée : 10 s après le début de la panne. */
    private static final Clock CLOCK = Clock.fixed(Instant.ofEpochMilli(T0 + 10_000), ZoneOffset.UTC);

    private static MachineEvent event(EventType type, long atSecond) {
        return new MachineEvent("e" + atSecond + type, "M1", type, T0 + atSecond * 1000, null, null);
    }

    private final InMemoryAdapters.AlertStore repository = new InMemoryAdapters.AlertStore();
    private final InMemoryAdapters.Publisher publisher = new InMemoryAdapters.Publisher();
    private final MachineMonitors monitors = new MachineMonitors(RULES, repository);
    private final DetectAlertsUseCase detect = new DetectAlertsUseCase(monitors, repository, List.of(publisher));
    private final AcknowledgeAlertUseCase acknowledge = new AcknowledgeAlertUseCase(monitors, repository, List.of(publisher), CLOCK);

    private String raisePanne() {
        return detect.handle(event(EventType.MACHINE_DOWN, 0)).get(0).id();
    }

    @Test
    void lOperateurPrendLAlerteEnCharge() {
        String id = raisePanne();

        Alert alert = acknowledge.acknowledge(id, "operateur");

        assertThat(alert.acknowledgedBy()).isEqualTo("operateur");
        assertThat(alert.acknowledgedAt()).isEqualTo(T0 + 10_000);
        assertThat(repository.findById(id).get().isAcknowledged()).isTrue();
        assertThat(publisher.published.get(publisher.published.size() - 1)).isEqualTo(alert);
    }

    @Test
    void laPriseEnChargeEstConservee_quandLAlerteSAggraveEtSeResout() {
        String id = raisePanne();
        acknowledge.acknowledge(id, "operateur");

        detect.handle(event(EventType.ENERGY_READING, 30));          // devient critique
        Alert critical = repository.findById(id).get();
        assertThat(critical.severity()).isEqualTo(Severity.CRITICAL);
        assertThat(critical.acknowledgedBy()).isEqualTo("operateur");

        detect.handle(event(EventType.MACHINE_UP, 45));              // résolue
        assertThat(repository.findById(id).get().acknowledgedBy()).isEqualTo("operateur");
    }

    @Test
    void refuseUneDeuxiemePriseEnCharge() {
        String id = raisePanne();
        acknowledge.acknowledge(id, "operateur");

        assertThatThrownBy(() -> acknowledge.acknowledge(id, "autre"))
                .isInstanceOf(AlertActionRejectedException.class)
                .hasMessageContaining("operateur");
    }

    @Test
    void refuseUneAlerteDejaResolue() {
        String id = raisePanne();
        detect.handle(event(EventType.MACHINE_UP, 5));

        assertThatThrownBy(() -> acknowledge.acknowledge(id, "operateur"))
                .isInstanceOf(AlertActionRejectedException.class);
    }

    @Test
    void alerteInconnue_introuvable() {
        assertThatThrownBy(() -> acknowledge.acknowledge("M9-DOWNTIME-1", "operateur"))
                .isInstanceOf(NotFoundException.class);
    }
}
