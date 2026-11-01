import { inject, Injectable, InjectionToken } from '@angular/core';
import Keycloak from 'keycloak-js';
import { CurrentUser, KEYCLOAK_CLIENT_ID, KEYCLOAK_REALM, keycloakUrl, rolesLabel, TokenClaims, userFromClaims } from './auth';

/** L'instance Keycloak, créée et connectée AVANT le démarrage d'Angular (voir main.ts). */
export const KEYCLOAK = new InjectionToken<Keycloak>('KEYCLOAK');

/**
 * Connecte l'utilisateur avant d'afficher quoi que ce soit.
 *
 * "login-required" : s'il n'est pas connecté, Keycloak le redirige vers sa page de connexion,
 * puis le renvoie ici avec un code. keycloak-js échange ce code contre les jetons
 * (flux "Authorization Code" avec PKCE : aucun secret n'est stocké dans le navigateur).
 */
export async function initKeycloak(): Promise<Keycloak> {
  const keycloak = new Keycloak({ url: keycloakUrl(), realm: KEYCLOAK_REALM, clientId: KEYCLOAK_CLIENT_ID });
  await keycloak.init({ onLoad: 'login-required', pkceMethod: 'S256', checkLoginIframe: false });
  return keycloak;
}

/** L'utilisateur connecté et son jeton, pour les autres services et composants. */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly keycloak = inject(KEYCLOAK);

  readonly user: CurrentUser = userFromClaims(this.keycloak.tokenParsed as TokenClaims | undefined);
  readonly rolesLabel = rolesLabel(this.user.roles);

  /**
   * Un jeton d'accès valide. Les jetons expirent vite (5 min) : s'il expire dans moins de 30 s,
   * on en demande d'abord un nouveau à Keycloak, sans que l'utilisateur s'en aperçoive.
   */
  async token(): Promise<string> {
    await this.keycloak.updateToken(30);
    return this.keycloak.token ?? '';
  }

  logout(): void {
    void this.keycloak.logout({ redirectUri: location.origin });
  }
}
