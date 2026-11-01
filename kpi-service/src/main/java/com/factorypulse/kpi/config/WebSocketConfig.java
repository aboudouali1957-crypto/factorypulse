package com.factorypulse.kpi.config;

import com.factorypulse.kpi.adapter.in.websocket.StompAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
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

    private final StompAuthInterceptor stompAuthInterceptor;

    public WebSocketConfig(StompAuthInterceptor stompAuthInterceptor) {
        this.stompAuthInterceptor = stompAuthInterceptor;
    }

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

    /** Chaque message entrant (dont la connexion STOMP) passe d'abord par la vérification du jeton. */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthInterceptor);
    }
}
