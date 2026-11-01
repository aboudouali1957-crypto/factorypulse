# Jour 7 : tests d'intégration et mise en ligne

## 1. Tests d'intégration avec Testcontainers

Les tests unitaires (jours 1 à 6) vérifient le domaine et les cas d'usage **sans** Kafka ni MongoDB, grâce à de faux adaptateurs en mémoire. Ils sont rapides, mais ne prouvent pas que les **vrais** adaptateurs fonctionnent : configuration Kafka, conversion JSON, requêtes MongoDB, règles de sécurité.

Les tests d'intégration comblent ce manque. **Testcontainers** lance de vrais conteneurs Docker (Kafka, MongoDB) pendant le test, puis démarre le service Spring Boot complet branché dessus.

| Test | Ce qu'il vérifie |
|---|---|
| `KpiPipelineIT` (kpi-service) | des messages JSON publiés dans Kafka deviennent des indicateurs sauvegardés dans MongoDB et lus par l'API ; 401 sans jeton ; un message illisible ne bloque pas la lecture des suivants |
| `AlertServiceIT` (alert-service) | une panne publiée dans Kafka devient une alerte ; **prise en charge** : 403 pour le responsable, 200 pour l'opérateur, 409 la deuxième fois ; **interventions** : saisie réservée à l'opérateur, historique réservé au responsable ; 400 avec message si la saisie est invalide |

**Les jetons sont simulés** avec `spring-security-test` (`jwt()`) : on choisit le nom et le rôle de l'utilisateur dans le test, sans lancer Keycloak. Les règles d'accès testées sont exactement celles de `SecurityConfig`.

### Lancer

```bash
cd kpi-service && mvn test      # tests unitaires seulement : rapides, sans Docker
cd kpi-service && mvn verify    # + tests d'intégration (*IT) : Docker doit tourner
```

Les classes dont le nom finit par `IT` sont lancées par le plugin Maven **failsafe**, à l'étape `verify`. Le CI GitHub exécute `mvn verify` : les tests d'intégration tournent donc à chaque push.

### Choix techniques

- **Conteneurs partagés par la classe** (`static` + `@Container`) : Kafka et MongoDB démarrent une seule fois pour tous les tests de la classe.
- **Adresses dynamiques** (`@DynamicPropertySource`) : Testcontainers choisit des ports libres, injectés dans la configuration Spring au démarrage.
- **Un producteur de test qui envoie du texte brut** : on publie le JSON exactement comme le simulateur, ce qui teste aussi la désérialisation côté service.
- **Attente active** plutôt qu'une pause fixe : le traitement Kafka est asynchrone, donc le test réessaie toutes les 200 ms (jusqu'à 60 s) au lieu d'attendre une durée arbitraire.

## 2. Préparation de la mise en ligne

La configuration de production est **séparée** du développement : `docker-compose.prod.yml` surcharge `docker-compose.yml` seulement là où c'est nécessaire.

| Fichier | Rôle |
|---|---|
| `docker-compose.prod.yml` | Un seul point d'entrée public (Caddy) ; aucun autre port publié ; Keycloak sous `/auth` avec un mot de passe secret ; mémoire Java limitée ; redémarrage automatique |
| `Caddyfile` | Reverse proxy public avec **HTTPS automatique** (Let's Encrypt) : `/auth` → Keycloak, le reste → dashboard |
| `.env.example` | Modèle des deux réglages du serveur : domaine et mot de passe administrateur. Le vrai `.env` n'est jamais commité. |

Le code s'adapte à l'adresse publique sans être modifié :
- le **realm Keycloak** accepte l'adresse publique comme URL de redirection (`${FP_PUBLIC_URL}`, remplacée à l'import) ;
- les **services** vérifient l'émetteur public des jetons (`KEYCLOAK_ISSUER`) et autorisent l'origine publique pour les WebSockets (`ALLOWED_ORIGINS`) ;
- le **dashboard** trouve Keycloak sur `:8180` en local et sous `/auth` en production.

**Pourquoi HTTPS est obligatoire** : la connexion Keycloak (PKCE) utilise l'API de chiffrement du navigateur (Web Crypto), disponible uniquement sur une page sécurisée (HTTPS, ou `localhost` en développement).

**Pourquoi Caddy** : il obtient et renouvelle seul le certificat HTTPS. La configuration tient en quelques lignes.

Le guide pas à pas pour AWS est dans [deploiement.md](deploiement.md).
