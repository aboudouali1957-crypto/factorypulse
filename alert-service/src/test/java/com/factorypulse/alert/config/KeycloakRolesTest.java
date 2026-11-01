package com.factorypulse.alert.config;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakRolesTest {

    @Test
    void litLesRolesDuRealm() {
        Map<String, Object> claims = Map.of(
                "preferred_username", "operateur",
                "realm_access", Map.of("roles", List.of("OPERATEUR", "offline_access")));

        assertThat(KeycloakRoles.fromClaims(claims)).containsExactly("OPERATEUR", "offline_access");
    }

    @Test
    void sansRoles_renvoieUneListeVide() {
        assertThat(KeycloakRoles.fromClaims(Map.of("sub", "123"))).isEmpty();
        assertThat(KeycloakRoles.fromClaims(Map.of("realm_access", Map.of()))).isEmpty();
    }

    @Test
    void ignoreUnFormatInattendu() {
        assertThat(KeycloakRoles.fromClaims(Map.of("realm_access", "pas une map"))).isEmpty();
        assertThat(KeycloakRoles.fromClaims(Map.of("realm_access", Map.of("roles", List.of(1, "RESPONSABLE")))))
                .containsExactly("RESPONSABLE");
    }
}
