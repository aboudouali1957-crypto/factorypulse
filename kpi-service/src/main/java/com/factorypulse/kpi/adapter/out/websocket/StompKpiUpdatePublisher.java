package com.factorypulse.kpi.adapter.out.websocket;

import com.factorypulse.kpi.application.port.out.KpiUpdatePublisher;
import com.factorypulse.kpi.domain.KpiSnapshot;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * ADAPTATEUR DE SORTIE : pousse chaque mise à jour des indicateurs vers les navigateurs connectés,
 * par WebSocket (protocole STOMP), sur la destination "/topic/kpis".
 *
 * Le tableau de bord s'abonne à "/topic/kpis" : il reçoit les nouveaux indicateurs
 * dès qu'ils sont calculés, sans avoir à redemander (pas de rechargement de page, pas de polling).
 */
@Component
public class StompKpiUpdatePublisher implements KpiUpdatePublisher {

    static final String DESTINATION = "/topic/kpis";

    private final SimpMessagingTemplate messagingTemplate;

    public StompKpiUpdatePublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void publish(KpiSnapshot snapshot) {
        messagingTemplate.convertAndSend(DESTINATION, snapshot);   // converti en JSON automatiquement
    }
}
