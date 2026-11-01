package com.factorypulse.alert.config;

import com.factorypulse.alert.application.DetectAlertsUseCase;
import com.factorypulse.alert.application.GetAlertsUseCase;
import com.factorypulse.alert.application.port.out.AlertPublisher;
import com.factorypulse.alert.application.port.out.AlertRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Assemble l'application : crée les cas d'usage (Java pur) et leur branche les adaptateurs.
 *
 * Spring trouve TOUS les adaptateurs qui implémentent {@link AlertPublisher} (Kafka et WebSocket)
 * et les donne sous forme de liste : pour ajouter une sortie (e-mail, SMS…), il suffit
 * d'écrire un nouvel adaptateur, sans toucher au cas d'usage.
 */
@Configuration
public class UseCaseConfig {

    @Bean
    public DetectAlertsUseCase detectAlertsUseCase(AlertProperties properties,
                                                   AlertRepository repository,
                                                   List<AlertPublisher> publishers) {
        return new DetectAlertsUseCase(properties.rules(), repository, publishers);
    }

    @Bean
    public GetAlertsUseCase getAlertsUseCase(AlertRepository repository) {
        return new GetAlertsUseCase(repository);
    }
}
