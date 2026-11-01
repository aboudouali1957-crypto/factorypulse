package com.factorypulse.alert.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Sécurité du service : c'est un "resource server" OAuth2.
 *
 * Le service ne gère ni mots de passe ni page de connexion : c'est le rôle de Keycloak.
 * Chaque requête doit porter un jeton JWT ("Authorization: Bearer ...") signé par Keycloak.
 * Spring vérifie la signature avec les clés publiques de Keycloak, la date d'expiration
 * et l'émetteur (voir application.yml), puis lit l'utilisateur et ses rôles dans le jeton.
 *
 *  - /api/**      : jeton obligatoire (sinon 401) ;
 *  - /ws-alerts          : la poignée de main WebSocket est ouverte, mais la connexion STOMP
 *                   exige un jeton valide (voir StompAuthInterceptor).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // API sans session ni cookie : chaque requête porte son jeton, pas de risque CSRF.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/ws-alerts", "/ws-alerts/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakJwtConverter())));
        return http.build();
    }

    /**
     * Transforme un jeton Keycloak en utilisateur Spring :
     * nom = "preferred_username", rôles = realm_access.roles préfixés par "ROLE_"
     * (ex. OPERATEUR → ROLE_OPERATEUR, utilisable avec hasRole("OPERATEUR")).
     */
    public static JwtAuthenticationConverter keycloakJwtConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setPrincipalClaimName("preferred_username");
        converter.setJwtGrantedAuthoritiesConverter(jwt -> KeycloakRoles.fromClaims(jwt.getClaims()).stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                .toList());
        return converter;
    }
}
