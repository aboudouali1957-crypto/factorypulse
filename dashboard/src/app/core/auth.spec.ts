import { hasRole, rolesLabel, userFromClaims } from './auth';

describe('userFromClaims', () => {
  it('lit le nom et les rôles FactoryPulse du jeton', () => {
    const user = userFromClaims({
      preferred_username: 'responsable',
      given_name: 'Karim',
      family_name: 'Benali',
      realm_access: { roles: ['RESPONSABLE', 'offline_access', 'default-roles-factorypulse'] },
    });
    expect(user.username).toBe('responsable');
    expect(user.displayName).toBe('Karim Benali');
    expect(user.roles).toEqual(['RESPONSABLE']);   // les rôles techniques de Keycloak sont ignorés
  });

  it('utilise l\'identifiant quand le nom complet manque', () => {
    expect(userFromClaims({ preferred_username: 'operateur' }).displayName).toBe('operateur');
  });

  it('résiste à un jeton vide', () => {
    expect(userFromClaims(undefined)).toEqual({ username: 'inconnu', displayName: 'inconnu', roles: [] });
  });
});

describe('hasRole', () => {
  it('indique si l\'utilisateur a un rôle', () => {
    const operateur = userFromClaims({ preferred_username: 'operateur', realm_access: { roles: ['OPERATEUR'] } });
    expect(hasRole(operateur, 'OPERATEUR')).toBeTrue();
    expect(hasRole(operateur, 'RESPONSABLE')).toBeFalse();
  });
});

describe('rolesLabel', () => {
  it('affiche les rôles en français', () => {
    expect(rolesLabel(['OPERATEUR'])).toBe('Opérateur');
    expect(rolesLabel([])).toBe('Aucun rôle');
  });
});
