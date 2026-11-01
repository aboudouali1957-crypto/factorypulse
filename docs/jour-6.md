# Jour 6 : actions selon le rôle (alertes et interventions)

## Ce qu'on a construit

FactoryPulse ne sert plus seulement à **observer** la ligne : on peut maintenant **agir**.

| Action | Rôle exigé | API | Sinon |
|---|---|---|---|
| Prendre une alerte en charge (« Je m'en occupe ») | `OPERATEUR` | `POST /api/alerts/{id}/acknowledge` | 403 |
| Saisir une intervention de maintenance | `OPERATEUR` | `POST /api/interventions` | 403 |
| Consulter l'historique des interventions | `RESPONSABLE` | `GET /api/interventions` | 403 |
| Voir les indicateurs et les alertes | tout utilisateur connecté | `GET /api/kpis`, `GET /api/alerts` | 401 sans jeton |

**Scénario de démonstration**
1. Une machine tombe en panne : une alerte apparaît.
2. L'opérateur (`operateur` / `operateur`) clique sur **« Je m'en occupe »**. Tous les écrans affichent aussitôt « Pris en charge par operateur à 14:32 » (la mise à jour passe par le WebSocket).
3. Après la réparation, il clique sur **« Intervention »** : le formulaire est pré-rempli avec la machine et l'alerte. Il choisit la cause, décrit l'action et indique la durée.
4. Le responsable (`responsable` / `responsable`) voit l'**historique** et le **bilan par machine** : temps total d'intervention, nombre d'interventions, cause la plus fréquente.

## Deux niveaux de contrôle des rôles

1. **Le serveur décide** (`SecurityConfig` de l'alert-service). C'est la seule vérification qui compte, car tout ce qui tourne dans le navigateur peut être modifié par l'utilisateur.
   ```java
   .requestMatchers(HttpMethod.POST, "/api/alerts/*/acknowledge").hasRole("OPERATEUR")
   .requestMatchers(HttpMethod.POST, "/api/interventions").hasRole("OPERATEUR")
   .requestMatchers(HttpMethod.GET, "/api/interventions", "/api/interventions/**").hasRole("RESPONSABLE")
   ```
2. **Le dashboard adapte l'affichage** : il cache les boutons et les panneaux que le rôle ne permet pas d'utiliser. C'est du confort, pas de la sécurité.

**L'auteur d'une action vient du jeton**, jamais du contenu de la requête : `authentication.getName()` donne le `preferred_username` du jeton vérifié. Un utilisateur ne peut donc pas agir au nom d'un autre.

## Côté alert-service

| Couche | Nouveautés |
|---|---|
| **Domaine** | `Alert.acknowledge` (refuse une alerte résolue ou déjà prise en charge), `Intervention` (toujours valide : machine, cause, description de 1 à 500 caractères, durée de 1 min à 24 h), `InterventionCause`, exceptions métier |
| **Application** | `AcknowledgeAlertUseCase`, `RecordInterventionUseCase`, `GetInterventionsUseCase`, `MachineMonitors` (moniteurs partagés) |
| **Ports de sortie** | `InterventionRepository` ; `AlertRepository.findById` |
| **Adaptateurs** | `AlertController` (acquittement), `InterventionController`, `ApiExceptionHandler`, `MongoInterventionRepository` (collection `interventions`) |

### Les erreurs : format Problem Details

Le domaine lance des exceptions Java, et l'adaptateur web les traduit en codes HTTP au format standard *Problem Details* (RFC 9457) :

| Exception | Code | Exemple |
|---|---|---|
| `IllegalArgumentException` | 400 | durée de 0 minute, description vide |
| `NotFoundException` | 404 | alerte inconnue |
| `AlertActionRejectedException` | 409 | alerte déjà résolue ou déjà prise en charge |

```json
{ "status": 409, "title": "Conflict", "detail": "L'alerte M3-DOWNTIME-1767229140000 est déjà prise en charge par operateur" }
```

Le dashboard affiche ce message `detail` à l'utilisateur.

## Choix techniques

- **Un verrou commun entre Kafka et l'API.** Une même alerte peut être modifiée en même temps par un événement Kafka (elle s'aggrave) et par un opérateur (il la prend en charge). Les deux cas d'usage passent par `MachineMonitors` sous le même verrou : une modification à la fois, et la prise en charge est conservée quand l'alerte s'aggrave ou se résout.
- **La date de mise à jour ne recule jamais.** Le dashboard ignore une version plus ancienne que celle qu'il connaît ; `Alert` garantit donc que `updatedAt` ne diminue pas, même si un événement un peu ancien arrive juste après un acquittement.
- **Une horloge injectée** (`java.time.Clock`). Les cas d'usage ne lisent pas l'heure eux-mêmes : en production c'est l'horloge du système, dans les tests une horloge figée. Les résultats sont donc exacts et reproductibles.
- **La validation dans le domaine.** Le record `Intervention` refuse toute valeur invalide dès sa création. Le formulaire Angular vérifie les mêmes règles pour guider l'utilisateur, mais c'est le domaine qui fait foi.
- **Formulaire réactif dans une fenêtre `<dialog>`.** Angular Reactive Forms gère les règles de saisie, et l'élément HTML `<dialog>` fournit une vraie fenêtre modale (focus, touche Échap) sans bibliothèque.

## Vérifier

```bash
docker compose up -d --build
```

1. Connecte-toi avec `operateur` / `operateur` : les boutons **« Je m'en occupe »** et **« Intervention »** apparaissent sur les alertes.
2. Prends une alerte en charge, puis saisis une intervention.
3. Déconnecte-toi, puis reconnecte-toi avec `responsable` / `responsable` : plus de boutons d'action, mais le panneau **Interventions de maintenance** apparaît en bas de page.
4. Avec le jeton de l'opérateur (voir [jour-5.md](jour-5.md)), l'historique est refusé :
   ```bash
   curl -i -H "Authorization: Bearer <jeton opérateur>" http://localhost:8083/api/interventions   # HTTP/1.1 403
   ```
