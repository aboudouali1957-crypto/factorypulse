package com.factorypulse.kpi.config;

import com.factorypulse.kpi.adapter.in.websocket.StompAuthInterceptor;
import org.springframework.beans.factory.annotation.Value;
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
    private final String[] allowedOrigins;

    /**
     * @param allowedOrigins origines autorisées à ouvrir le WebSocket : les adresses locales en développement,
     *                       l'adresse publique du site en production (variable ALLOWED_ORIGINS)
     */
    public WebSocketConfig(StompAuthInterceptor stompAuthInterceptor,
                           @Value("${factorypulse.allowed-origins}") String[] allowedOrigins) {
        this.stompAuthInterceptor = stompAuthInterceptor;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns(allowedOrigins);
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
