package com.factorypulse.kpi.config;

import com.factorypulse.kpi.application.GetKpisUseCase;
import com.factorypulse.kpi.application.ProcessMachineEventUseCase;
import com.factorypulse.kpi.application.port.out.KpiUpdatePublisher;
import com.factorypulse.kpi.application.port.out.MachineKpiRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Assemble l'application : crée les cas d'usage (Java pur) et leur branche
 * les adaptateurs de sortie choisis par Spring (MongoDB pour le stockage, WebSocket pour la diffusion).
 */
@Configuration
public class UseCaseConfig {

    @Bean
    public ProcessMachineEventUseCase processMachineEventUseCase(KpiProperties properties,
                                                                 MachineKpiRepository repository,
                                                                 KpiUpdatePublisher publisher) {
        return new ProcessMachineEventUseCase(properties.idealCycleSeconds(), repository, publisher);
    }

    @Bean
    public GetKpisUseCase getKpisUseCase(MachineKpiRepository repository) {
        return new GetKpisUseCase(repository);
    }
}
