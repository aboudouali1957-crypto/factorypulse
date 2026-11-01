package com.factorypulse.alert.config;

import com.factorypulse.alert.application.AcknowledgeAlertUseCase;
import com.factorypulse.alert.application.DetectAlertsUseCase;
import com.factorypulse.alert.application.GetAlertsUseCase;
import com.factorypulse.alert.application.GetInterventionsUseCase;
import com.factorypulse.alert.application.MachineMonitors;
import com.factorypulse.alert.application.RecordInterventionUseCase;
import com.factorypulse.alert.application.port.out.AlertPublisher;
import com.factorypulse.alert.application.port.out.AlertRepository;
import com.factorypulse.alert.application.port.out.InterventionRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * Assemble l'application : crée les cas d'usage (Java pur) et leur branche les adaptateurs.
 *
 * Spring trouve TOUS les adaptateurs qui implémentent {@link AlertPublisher} (Kafka et WebSocket)
 * et les donne sous forme de liste : pour ajouter une sortie (e-mail, SMS…), il suffit
 * d'écrire un nouvel adaptateur, sans toucher au cas d'usage.
 */
@Configuration
public class UseCaseConfig {

    /** L'horloge est injectée : dans les tests, on la remplace par une horloge figée. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    /** Les moniteurs des machines, partagés par la détection et l'acquittement. */
    @Bean
    public MachineMonitors machineMonitors(AlertProperties properties, AlertRepository repository) {
        return new MachineMonitors(properties.rules(), repository);
    }

    @Bean
    public DetectAlertsUseCase detectAlertsUseCase(MachineMonitors monitors,
                                                   AlertRepository repository,
                                                   List<AlertPublisher> publishers) {
        return new DetectAlertsUseCase(monitors, repository, publishers);
    }

    @Bean
    public AcknowledgeAlertUseCase acknowledgeAlertUseCase(MachineMonitors monitors,
                                                           AlertRepository repository,
                                                           List<AlertPublisher> publishers,
                                                           Clock clock) {
        return new AcknowledgeAlertUseCase(monitors, repository, publishers, clock);
    }

    @Bean
    public GetAlertsUseCase getAlertsUseCase(AlertRepository repository) {
        return new GetAlertsUseCase(repository);
    }

    @Bean
    public RecordInterventionUseCase recordInterventionUseCase(InterventionRepository interventions,
                                                               AlertRepository alerts,
                                                               Clock clock) {
        return new RecordInterventionUseCase(interventions, alerts, clock, () -> UUID.randomUUID().toString());
    }

    @Bean
    public GetInterventionsUseCase getInterventionsUseCase(InterventionRepository interventions) {
        return new GetInterventionsUseCase(interventions);
    }
}
