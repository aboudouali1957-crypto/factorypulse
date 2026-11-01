# Jour 4 : TRS sur 15 minutes et microservice d'alertes

## Ce qu'on a construit

1. **Côté kpi-service** : en plus du TRS cumulé depuis le début, un **TRS sur les 15 dernières minutes** (fenêtre glissante).
2. **Un quatrième microservice, `alert-service`** : il lit les mêmes événements Kafka, détecte les pannes prolongées et les taux de rebut anormaux, et publie des alertes.
3. **Côté tableau de bord** : la jauge, les barres et la courbe affichent le TRS sur 15 minutes, et un panneau **Alertes** liste les alertes actives et les dernières résolues.

```
                                ┌── groupe "kpi-service" ───► kpi-service ──► MongoDB (factorypulse)
simulator ──► Kafka ────────────┤                                  │
             machine-events     └── groupe "alert-service" ─► alert-service ──► MongoDB (factorypulse_alerts)
                                                                   │   └──► Kafka : machine-alerts
                                                                   ▼
                                                     dashboard (via nginx)
```

## 1. Le TRS sur 15 minutes glissantes

**Le problème.** Le TRS cumulé bouge de moins en moins avec le temps. Après 10 heures de production, une panne de 5 minutes ne change presque rien au chiffre, alors qu'en atelier, c'est justement ce qu'on veut voir. Les courbes du jour 3 étaient donc presque plates.

**La solution.** On garde les compteurs **minute par minute** (`MinuteBucket` : temps de marche, temps d'arrêt, pièces, pièces bonnes) et on ne conserve que les 15 dernières minutes. Le TRS récent se calcule sur la somme de ces 15 cases.

| Fichier | Rôle |
|---|---|
| `domain/MinuteBucket` | Les compteurs d'une minute |
| `domain/RecentWindow` | Les 15 dernières cases ; supprime les plus anciennes quand une nouvelle minute commence |
| `domain/RecentKpi` | Le résultat : disponibilité, performance, qualité et TRS sur la fenêtre |
| `domain/OeeFormulas` | Les formules du TRS, partagées par le calcul cumulé et le calcul sur 15 min |

Le JSON de `GET /api/kpis` contient maintenant un objet `recent` en plus des indicateurs cumulés. Les cases sont aussi sauvegardées dans MongoDB : après un redémarrage, la fenêtre est reprise telle quelle.

**Choix techniques**
- **Des cases par minute plutôt que la liste des événements** : au plus 15 cases par machine, quelle que soit la cadence. La mémoire et la taille du document MongoDB restent constantes.
- **Le temps vient de la date des événements**, pas de l'horloge du serveur. Le calcul donne le même résultat en direct, en relisant l'historique Kafka ou dans un test.

## 2. Le microservice `alert-service`

### Deux services sur le même topic

Le kpi-service et l'alert-service lisent tous les deux `machine-events`, mais avec **deux consumer groups différents**. Kafka garde une position de lecture (offset) **par groupe** : chaque service reçoit tous les événements, sans rien enlever à l'autre.

Ni le simulateur ni le kpi-service n'ont été modifiés pour accueillir ce nouveau service. C'est l'intérêt d'une architecture orientée événements.

### Les règles

| Règle | Déclenchement | Gravité | Résolution |
|---|---|---|---|
| **Panne** | `MACHINE_DOWN` | À surveiller | `MACHINE_UP` (le message donne la durée de la panne) |
| **Panne prolongée** | la même panne dure 30 s ou plus | Critique | `MACHINE_UP` |
| **Rebuts** | plus de 10 % de rebuts sur les 50 dernières pièces | À surveiller | taux revenu à 4 % ou moins |

Les seuils sont dans `application.yml` (clé `alert`).

**Une alerte a un cycle de vie** : elle est créée, peut s'aggraver, puis est résolue. C'est toujours **la même alerte** (même identifiant), dans une nouvelle version. MongoDB garde la dernière version, et le tableau de bord remplace l'ancienne.

### L'architecture hexagonale, encore

| Couche | Fichiers |
|---|---|
| **Domaine** | `Alert`, `AlertRules`, `MachineMonitor` (les règles), `AlertType`, `Severity`, copie de `MachineEvent` |
| **Application** | `DetectAlertsUseCase`, `GetAlertsUseCase` |
| **Ports de sortie** | `AlertRepository`, `AlertPublisher` |
| **Adaptateurs d'entrée** | `MachineEventListener` (Kafka), `AlertController` (REST) |
| **Adaptateurs de sortie** | `MongoAlertRepository`, `KafkaAlertPublisher` (topic `machine-alerts`), `StompAlertPublisher` (WebSocket `/topic/alerts`) |

**Deux adaptateurs pour un même port.** `AlertPublisher` est implémenté deux fois : vers Kafka et vers le WebSocket. Spring fournit la liste de tous les adaptateurs au cas d'usage, qui les appelle l'un après l'autre. Pour ajouter un envoi par e-mail, il suffirait d'écrire un troisième adaptateur, sans toucher au cas d'usage.

### Choix techniques

- **Identifiant stable** (`machine + type + date de début`) : Kafka garantit une livraison « au moins une fois ». Si un événement est relu, on retrouve la même alerte au lieu d'en créer une deuxième.
- **Hystérésis sur les rebuts** (alerte au-dessus de 10 %, résolution à 4 %) : sans écart entre les deux seuils, une alerte s'ouvrirait et se fermerait sans arrêt quand le taux oscille autour de 10 %.
- **`auto-offset-reset: latest`** : au tout premier démarrage, l'alert-service part des nouveaux événements. Une alerte concerne le présent ; rejouer des jours d'historique créerait des centaines d'alertes déjà dépassées. Le kpi-service, lui, lit tout depuis le début (`earliest`) pour que ses compteurs soient complets.
- **Reprise après redémarrage** : les moniteurs sont en mémoire, mais ils sont recréés à partir des alertes encore actives dans MongoDB. Une panne en cours est donc bien résolue au redémarrage suivant.
- **Une base MongoDB par service** (`factorypulse_alerts`) : chaque microservice possède ses propres données ; aucun ne lit directement la base d'un autre.
- **Topic `machine-alerts`** : les alertes sont aussi publiées dans Kafka, pour qu'un futur service (notifications, résumé des incidents…) puisse s'y abonner sans modifier l'alert-service.

## 3. Côté tableau de bord

| Fichier | Rôle |
|---|---|
| `core/alert-store.service.ts` | Charge les dernières alertes (REST), s'abonne au WebSocket `/ws-alerts`, garde l'état dans des *signals* |
| `core/alert-state.ts` | Logique pure : remplacer une alerte par sa nouvelle version, trier (critiques d'abord), limiter la liste (testée) |
| `components/alerts-panel` | Le panneau Alertes : actives avec leur durée en direct, puis les dernières résolues |

Les cartes des machines affichent un badge quand une machine a des alertes actives, et une tuile « Alertes actives » s'ajoute en haut de page.

**Le routage** : le navigateur parle toujours à une seule adresse. nginx (et `proxy.conf.json` en développement) envoie `/api/alerts` et `/ws-alerts` à l'alert-service, le reste de `/api` et `/ws` au kpi-service.

## Lancer

```bash
docker compose up -d --build
```

- **http://localhost:4300** : le tableau de bord, avec le panneau Alertes ;
- **http://localhost:8083/api/alerts** : les dernières alertes ;
- **http://localhost:8090** (Kafka UI) → *Topics* → `machine-alerts` : les alertes publiées dans Kafka ;
- Kafka UI → *Consumers* : les deux groupes `kpi-service` et `alert-service`, chacun avec sa propre position de lecture.
