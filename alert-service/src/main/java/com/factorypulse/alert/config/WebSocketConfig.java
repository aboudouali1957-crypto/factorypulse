package com.factorypulse.alert.config;

import com.factorypulse.alert.adapter.in.websocket.StompAuthInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
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
        registry.addEndpoint("/ws-alerts").setAllowedOriginPatterns(allowedOrigins);
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
