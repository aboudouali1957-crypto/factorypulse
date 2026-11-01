# Jour 5 : sécurité avec Keycloak (OAuth2, OpenID Connect, JWT)

## Ce qu'on a construit

1. **Keycloak**, un serveur d'identité open source, ajouté à Docker Compose. Il gère les utilisateurs, les mots de passe, la page de connexion et les rôles.
2. **Le dashboard** connecte l'utilisateur avant d'afficher quoi que ce soit, puis joint un **jeton JWT** à chaque appel de l'API et à chaque connexion WebSocket.
3. **Le kpi-service et l'alert-service** sont devenus des *resource servers* OAuth2 : sans jeton valide, ils répondent **401** et refusent la connexion WebSocket.

```
                 1. redirection vers la page de connexion
 navigateur ───────────────────────────────────────────► Keycloak (port 8180)
 (dashboard) ◄─────────────────────────────────────────── 2. jeton JWT (signé par Keycloak)
      │
      │ 3. Authorization: Bearer <jeton>   (API REST et trame STOMP CONNECT)
      ▼
 kpi-service / alert-service ── 4. vérifient la signature avec les clés publiques de Keycloak
```

## Les comptes de démonstration

Le realm `factorypulse` est importé automatiquement au démarrage de Keycloak depuis `keycloak/factorypulse-realm.json`.

| Identifiant | Mot de passe | Rôle |
|---|---|---|
| `operateur` | `operateur` | `OPERATEUR` : suit la production et traite les alertes |
| `responsable` | `responsable` | `RESPONSABLE` : supervise la ligne et les interventions |

Console d'administration : http://localhost:8180 (`admin` / `admin`). Ces mots de passe sont réservés au développement local.

Les rôles sont déjà présents dans les jetons et lus par les services. Ils serviront au jour 6, pour les actions réservées à un rôle (acquitter une alerte, saisir une intervention…).

## Le flux de connexion : Authorization Code + PKCE

1. Le dashboard s'ouvre et `keycloak-js` voit que l'utilisateur n'est pas connecté : il le **redirige vers la page de connexion de Keycloak**.
2. L'utilisateur saisit son mot de passe **chez Keycloak**. Le dashboard ne voit jamais le mot de passe.
3. Keycloak renvoie vers le dashboard avec un **code** à usage unique.
4. `keycloak-js` échange ce code contre un **jeton d'accès** (JWT, valable 5 minutes), puis le renouvelle automatiquement avant expiration.

**PKCE** (*Proof Key for Code Exchange*) : le dashboard est une application publique, il ne peut pas cacher de secret. À la place, il génère un code aléatoire pour chaque connexion, et seul celui qui l'a généré peut échanger le code contre un jeton. Un code intercepté ne sert donc à rien.

## Côté services : la vérification du jeton

| Fichier | Rôle |
|---|---|
| `config/SecurityConfig` | `/api/**` exige un jeton ; pas de session ni de cookie (API *stateless*) |
| `config/KeycloakRoles` | Lit les rôles dans `realm_access.roles` du jeton (testé) |
| `adapter/in/websocket/StompAuthInterceptor` | Vérifie le jeton envoyé dans la trame STOMP `CONNECT` |
| `application.yml` | Adresse de Keycloak : émetteur attendu et clés publiques |

Pour chaque requête, Spring Security vérifie :
- la **signature** du jeton, avec la clé publique de Keycloak (téléchargée une fois, puis gardée en cache) ;
- la **date d'expiration** ;
- l'**émetteur** (`iss`), qui doit être le realm `factorypulse`.

Les services n'appellent pas Keycloak à chaque requête : la vérification est locale, grâce à la signature. Le domaine et les cas d'usage n'ont pas changé, car la sécurité reste dans la configuration et les adaptateurs.

## Choix techniques

- **Keycloak plutôt qu'une authentification maison** : les mots de passe, la page de connexion et la gestion des jetons sont faits par un outil éprouvé, utilisé en entreprise. Les services ne font que vérifier des jetons, avec la configuration standard de Spring Security.
- **Le jeton WebSocket dans la trame STOMP `CONNECT`** : un navigateur ne peut pas ajouter d'en-tête `Authorization` à l'ouverture d'un WebSocket. La poignée de main HTTP est donc ouverte, mais la connexion STOMP est refusée sans jeton valide, et aucune donnée n'est envoyée avant cette vérification.
- **Deux adresses pour Keycloak** : le navigateur le joint sur `localhost:8180`, les services sur `keycloak:8080` (réseau Docker). `KC_HOSTNAME` fixe l'adresse qui apparaît dans les jetons, et les services téléchargent les clés par le réseau interne (`KEYCLOAK_JWKS`).
- **Jetons courts (5 min)** renouvelés automatiquement : un jeton volé n'est utilisable que peu de temps.

**Limite connue** : le jeton d'une connexion WebSocket n'est vérifié qu'à l'ouverture. Une connexion déjà ouverte reste active jusqu'à sa fermeture, même après la déconnexion de l'utilisateur.

## Vérifier

```bash
docker compose up -d --build
```

1. Ouvre **http://localhost:4300** : tu es redirigé vers la page de connexion de Keycloak.
2. Connecte-toi avec `responsable` / `responsable` : le dashboard s'affiche, avec ton nom et ton rôle en haut à droite.
3. Sans jeton, l'API refuse de répondre :
   ```bash
   curl -i http://localhost:8082/api/kpis      # HTTP/1.1 401
   ```
4. Pour appeler l'API avec un jeton : dans le dashboard, ouvre les outils de développement (F12) → *Network* → une requête `kpis` → copie la valeur de l'en-tête `Authorization`, puis :
   ```bash
   curl -H "Authorization: Bearer <jeton>" http://localhost:8082/api/kpis
   ```
