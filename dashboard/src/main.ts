import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { AppComponent } from './app/app.component';
import { initKeycloak, KEYCLOAK } from './app/core/auth.service';

// 1. Connexion avec Keycloak (redirection vers la page de connexion si besoin),
// 2. puis démarrage de l'application, avec l'instance Keycloak disponible partout.
initKeycloak()
  .then((keycloak) =>
    bootstrapApplication(AppComponent, {
      ...appConfig,
      providers: [...appConfig.providers, { provide: KEYCLOAK, useValue: keycloak }],
    }),
  )
  .catch((err) => {
    console.error(err);
    document.body.innerHTML =
      '<p style="padding:40px;font-family:system-ui;color:#f0efec">Serveur de connexion (Keycloak) injoignable. ' +
      'Vérifie qu\'il tourne sur le port 8180, puis recharge la page.</p>';
  });
