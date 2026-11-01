package com.factorypulse.kpi.config;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Lit les rôles d'un utilisateur dans un jeton émis par Keycloak.
 *
 * Keycloak range les rôles du realm dans la revendication (claim) "realm_access" :
 * <pre>{ "realm_access": { "roles": ["OPERATEUR", "offline_access", ...] } }</pre>
 *
 * Java pur, sans Spring : testé avec de simples Map.
 */
public final class KeycloakRoles {

    private KeycloakRoles() {
    }

    /** @return les rôles du realm, ou une liste vide si le jeton n'en contient pas */
    public static List<String> fromClaims(Map<String, Object> claims) {
        if (!(claims.get("realm_access") instanceof Map<?, ?> realmAccess)) {
            return List.of();
        }
        if (!(realmAccess.get("roles") instanceof Collection<?> roles)) {
            return List.of();
        }
        return roles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .toList();
    }
}
