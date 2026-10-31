package com.factorypulse.kpi.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configure le WebSocket (protocole STOMP).
 *
 *  - "/ws"     : l'adresse à laquelle le navigateur ouvre la connexion WebSocket ;
 *  - "/topic"  : le préfixe des destinations auxquelles on peut s'abonner (ex. "/topic/kpis").
 *
 * Le "simple broker" est un petit relais en mémoire fourni par Spring : il renvoie chaque message
 * publié sur "/topic/kpis" à tous les navigateurs abonnés.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // En local, le tableau de bord est servi par un proxy sur le même domaine ; on autorise
        // aussi les autres origines locales (ex. ng serve) pour faciliter le développement.
        registry.addEndpoint("/ws").setAllowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }
}
