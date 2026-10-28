# FactoryPulse

Plateforme de **supervision en temps réel d'une ligne de production**.
Un simulateur imite des machines d'usine (production, pannes, rebuts, énergie). Les événements passent par **Kafka**, un service **Spring Boot** calcule les indicateurs (TRS, taux de rebut, cadence, consommation) et un tableau de bord **Angular** les affiche en direct via **WebSocket**.

## Architecture

```
Simulateur (Spring Boot) ──► Kafka ──► Service KPI (Spring Boot) ──► MongoDB
   machines, pannes,       topic            calcul TRS,                 │
   rebuts, énergie     machine-events      rebut, cadence               ▼
                                                │               WebSocket (STOMP)
                                                ▼                       │
                                     Prometheus + Grafana       Tableau de bord Angular
```

## Avancement

| Étape | Contenu | État |
|---|---|---|
| Jour 1 | Simulateur de machines + Kafka + Docker Compose | ✅ |
| Jour 2 | Service KPI : lecture Kafka, calcul du TRS, MongoDB, tests | ⏳ |
| Jour 3 | WebSocket + tableau de bord Angular temps réel | ⏳ |
| Jour 4 | Alertes, Prometheus / Grafana, CI complète | ⏳ |
| Jour 5 | Résumé des incidents par IA (API Claude) | ⏳ |

Deux niveaux d'architecture :
- **entre les services** : architecture **orientée événements**, avec des services indépendants reliés par Kafka ;
- **dans chaque service** : architecture **hexagonale** (domaine, cas d'usage, ports et adaptateurs).

Détails dans [docs/architecture.md](docs/architecture.md).

## Lancer le projet

Prérequis : **Docker Desktop**. C'est tout : Maven et Kafka tournent dans des conteneurs.

```bash
docker compose up --build
```

Puis ouvre **http://localhost:8080** (Kafka UI) → *Topics* → `machine-events` → *Messages* : les événements des machines arrivent chaque seconde.

### Lancer le simulateur depuis ton IDE (IntelliJ / VS Code)

```bash
docker compose up kafka kafka-ui    # Kafka seul dans Docker
```

Puis lance `SimulatorApplication` depuis l'IDE : il se connecte à `localhost:9092`.

### Lancer les tests

```bash
cd simulator
mvn test
```

## Exemple d'événement publié

```json
{
  "eventId": "3f6c1a2e-...",
  "machineId": "M1",
  "type": "PIECE_PRODUCED",
  "timestamp": 1767225600000,
  "good": true,
  "energyKwh": null
}
```

Types : `PIECE_PRODUCED`, `MACHINE_DOWN`, `MACHINE_UP`, `ENERGY_READING`.

## Stack

Java 21 · Spring Boot 3 · Spring Kafka · Apache Kafka (KRaft) · Docker Compose · JUnit 5 · GitHub Actions
*(à venir : MongoDB, WebSocket STOMP, Angular, Prometheus, Grafana)*

## Documentation

- [Architecture : services et architecture hexagonale](docs/architecture.md)
- [Jour 1 : le simulateur et Kafka expliqués](docs/jour-1.md)
