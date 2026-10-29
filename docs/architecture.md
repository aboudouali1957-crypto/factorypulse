# Architecture de FactoryPulse

FactoryPulse a deux niveaux d'architecture :

1. **Entre les services** : une **architecture orientée événements** (event-driven). Plusieurs services indépendants communiquent par Kafka.
2. **À l'intérieur de chaque service** : une **architecture hexagonale** (ports et adaptateurs).

---

## 1. Vue d'ensemble : des services qui communiquent par événements

```
┌──────────────┐   événements   ┌─────────┐   événements   ┌──────────────┐   WebSocket   ┌─────────────┐
│  simulator   │ ─────────────► │  Kafka  │ ─────────────► │ kpi-service  │ ────────────► │  dashboard  │
│ (Spring Boot)│ machine-events │ (KRaft) │                │ (Spring Boot)│               │  (Angular)  │
└──────────────┘                └─────────┘                └──────┬───────┘               └─────────────┘
     JOUR 1                       JOUR 1                          │ MongoDB                    JOUR 3
                                                                  ▼
                                                               JOUR 2
```

| Service | Rôle | Communique avec |
|---|---|---|
| `simulator` | Simule les machines, publie les événements | Kafka (producteur) |
| `kpi-service` | Lit les événements, calcule TRS, rebut, cadence, énergie ; les expose en REST | Kafka (consommateur), MongoDB, API REST (WebSocket au jour 3) |
| `dashboard` | Affiche les indicateurs en temps réel | kpi-service (WebSocket) |

**Pourquoi ce découpage ?**
- **Découplage** : le simulateur ne connaît pas le kpi-service. Chacun peut être arrêté, redéployé ou remplacé sans casser l'autre.
- **Résilience** : si le kpi-service tombe, les événements restent dans Kafka et sont lus au redémarrage.
- **Évolutivité** : on peut ajouter un nouveau consommateur (alertes, archivage, IA) sans modifier les autres services. Grâce aux 3 partitions, on peut aussi lancer plusieurs instances du kpi-service pour lire en parallèle.

---

## 2. À l'intérieur d'un service : l'architecture hexagonale

Le principe : **le métier est au centre et ne dépend de rien**. Les technologies (Kafka, Spring, base de données) sont à l'extérieur et se branchent par des **ports** (interfaces).

```
                     ┌───────────────────────────────────────────────┐
                     │                 APPLICATION                   │
  ADAPTATEUR         │                                               │        ADAPTATEUR
  D'ENTRÉE           │   SimulateProductionLineUseCase (cas d'usage) │        DE SORTIE
                     │                    │                          │
┌──────────────────┐ │                    ▼                          │ ┌──────────────────────────┐
│SimulationScheduler│─┼──► runTick() ─► DOMAINE                      │ │KafkaMachineEventPublisher│
│ (@Scheduled)     │ │                MachineSimulator               │ │   (implémente le port)   │
└──────────────────┘ │                MachineEvent, MachineConfig    │ └────────────▲─────────────┘
                     │                    │                          │              │
                     │                    ▼                          │              │
                     │      PORT DE SORTIE : MachineEventPublisher ───┼──────────────┘
                     │                  (interface)                  │
                     └───────────────────────────────────────────────┘
```

Les deux services suivent ce même découpage : le simulateur (ci-dessous) et le kpi-service (détaillé dans [jour-2.md](jour-2.md)).

### Les couches du simulateur

| Couche | Package | Contenu | Dépend de |
|---|---|---|---|
| **Domaine** | `domain` | `MachineSimulator`, `MachineEvent`, `MachineConfig`, `EventType` | rien (Java pur) |
| **Application** | `application` | `SimulateProductionLineUseCase` (orchestre une ligne) | domaine + port |
| **Port de sortie** | `application.port.out` | `MachineEventPublisher` (interface) | domaine |
| **Adaptateur d'entrée** | `adapter.in.scheduler` | `SimulationScheduler` : déclenche le cas d'usage chaque seconde | application, Spring |
| **Adaptateur de sortie** | `adapter.out.kafka` | `KafkaMachineEventPublisher` : implémente le port avec Kafka | port, Spring Kafka |
| **Configuration** | `config` | `SimulatorProperties`, `KafkaTopicConfig`, `UseCaseConfig` (assemblage) | tout |

**La règle d'or : les dépendances pointent vers le centre.**
Le domaine ne connaît ni l'application ni Kafka. L'application ne connaît pas Kafka, seulement l'interface `MachineEventPublisher`. Seuls les adaptateurs connaissent les technologies.

### Pourquoi c'est utile, avec des exemples concrets

- **Tester sans infrastructure** : `SimulateProductionLineUseCaseTest` remplace Kafka par un `InMemoryPublisher` de 5 lignes. Le test s'exécute en quelques millisecondes, sans Docker.
- **Changer de technologie** : pour publier dans RabbitMQ au lieu de Kafka, on écrit un nouvel adaptateur qui implémente `MachineEventPublisher`. Le domaine et le cas d'usage ne changent pas.
- **Ajouter un déclencheur** : un endpoint REST « avancer d'un tick » serait un nouvel adaptateur d'entrée qui appelle le même `runTick()`.

---

## 3. Les questions d'architecture en entretien

**« Pourquoi pas un monolithe ? »**
Un monolithe serait plus simple au début. Mais ici, la production d'événements et leur analyse ont des rythmes et des besoins différents. Séparer les services par Kafka permet de les faire évoluer, de les redémarrer et de les mettre à l'échelle indépendamment.

**« Pourquoi l'architecture hexagonale ? »**
Pour isoler la logique métier des technologies. Elle reste testable seule et ne dépend pas d'un choix technique qui peut changer.

**« Où est l'inversion de dépendance ? »**
Le cas d'usage dépend de l'interface `MachineEventPublisher`, pas de Kafka. C'est l'adaptateur Kafka qui dépend de l'interface et l'implémente. Spring injecte l'implémentation au démarrage, dans `UseCaseConfig`.

**« Pourquoi le cas d'usage n'a pas d'annotation `@Service` ? »**
Pour qu'il ne dépende pas de Spring. Il est créé dans `UseCaseConfig` avec `@Bean`. Le framework reste en périphérie.
