package com.factorypulse.alert.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configure le WebSocket (protocole STOMP), comme dans le kpi-service.
 *
 *  - "/ws-alerts"     : l'adresse à laquelle le navigateur ouvre la connexion ;
 *  - "/topic/alerts"  : la destination à laquelle le tableau de bord s'abonne.
 *
 * L'adresse est différente de celle du kpi-service ("/ws") pour que le proxy (nginx)
 * sache vers quel service envoyer chaque connexion.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-alerts").setAllowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }
}
