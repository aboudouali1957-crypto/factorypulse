/**
 * Logique pure de l'authentification (aucune dépendance à Keycloak ni à Angular) : facile à tester.
 */

export type Role = 'OPERATEUR' | 'RESPONSABLE';

export const ROLE_LABELS: Record<Role, string> = {
  OPERATEUR: 'Opérateur',
  RESPONSABLE: 'Responsable',
};

/** L'utilisateur connecté, tel qu'on l'affiche. */
export interface CurrentUser {
  username: string;
  displayName: string;
  roles: Role[];
}

/** Les informations utiles d'un jeton Keycloak déjà décodé. */
export interface TokenClaims {
  preferred_username?: string;
  given_name?: string;
  family_name?: string;
  realm_access?: { roles?: string[] };
}

/** Construit l'utilisateur à partir du jeton : nom affiché et rôles FactoryPulse uniquement. */
export function userFromClaims(claims: TokenClaims | undefined): CurrentUser {
  const username = claims?.preferred_username ?? 'inconnu';
  const fullName = [claims?.given_name, claims?.family_name].filter(Boolean).join(' ');
  const roles = (claims?.realm_access?.roles ?? []).filter(isRole);
  return { username, displayName: fullName || username, roles };
}

function isRole(value: string): value is Role {
  return value in ROLE_LABELS;
}

export function hasRole(user: CurrentUser, role: Role): boolean {
  return user.roles.includes(role);
}

/** Libellé des rôles, ex. "Responsable". */
export function rolesLabel(roles: Role[]): string {
  return roles.length === 0 ? 'Aucun rôle' : roles.map((r) => ROLE_LABELS[r]).join(', ');
}

/**
 * Adresse de Keycloak :
 *  - en local (dashboard sur le port 4300), Keycloak est sur la même machine, port 8180 ;
 *  - en production, Keycloak est servi par le même domaine, sous le chemin /auth (voir Caddyfile).
 */
export function keycloakUrl(page: Pick<Location, 'protocol' | 'hostname' | 'port' | 'origin'> = location): string {
  return page.port === '4300' ? `${page.protocol}//${page.hostname}:8180` : `${page.origin}/auth`;
}

export const KEYCLOAK_REALM = 'factorypulse';
export const KEYCLOAK_CLIENT_ID = 'factorypulse-dashboard';
