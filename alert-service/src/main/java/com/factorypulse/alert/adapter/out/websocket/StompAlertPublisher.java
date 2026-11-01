package com.factorypulse.alert.adapter.out.websocket;

import com.factorypulse.alert.application.port.out.AlertPublisher;
import com.factorypulse.alert.domain.Alert;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * ADAPTATEUR DE SORTIE : pousse chaque alerte vers les navigateurs connectés,
 * par WebSocket (STOMP), sur la destination "/topic/alerts".
 */
@Component
public class StompAlertPublisher implements AlertPublisher {

    static final String DESTINATION = "/topic/alerts";

    private final SimpMessagingTemplate messagingTemplate;

    public StompAlertPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void publish(Alert alert) {
        messagingTemplate.convertAndSend(DESTINATION, alert);
    }
}
