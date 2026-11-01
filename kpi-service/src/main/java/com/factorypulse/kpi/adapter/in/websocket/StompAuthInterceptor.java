package com.factorypulse.kpi.adapter.in.websocket;

import com.factorypulse.kpi.config.SecurityConfig;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.stereotype.Component;

/**
 * Vérifie le jeton au moment où le navigateur se connecte en STOMP.
 *
 * Pourquoi pas comme pour l'API REST ? Le navigateur ne peut pas ajouter d'en-tête
 * "Authorization" à la poignée de main WebSocket. Le tableau de bord envoie donc son jeton
 * dans la trame STOMP CONNECT, et c'est ici qu'on le vérifie (même décodeur que pour l'API).
 * Jeton absent ou invalide : la connexion est refusée (trame ERROR), aucune donnée n'est envoyée.
 */
@Component
public class StompAuthInterceptor implements ChannelInterceptor {

    private static final String BEARER = "Bearer ";

    private final JwtDecoder jwtDecoder;
    private final JwtAuthenticationConverter converter = SecurityConfig.keycloakJwtConverter();

    public StompAuthInterceptor(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String header = accessor.getFirstNativeHeader("Authorization");
            if (header == null || !header.startsWith(BEARER)) {
                throw new AccessDeniedException("Connexion WebSocket refusée : jeton manquant");
            }
            Jwt jwt = jwtDecoder.decode(header.substring(BEARER.length()));   // exception si le jeton est invalide
            accessor.setUser(converter.convert(jwt));
        }
        return message;
    }
}
