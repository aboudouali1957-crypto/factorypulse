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
| Jour 2 | Service KPI : lecture Kafka, calcul du TRS, MongoDB, API REST, tests | ✅ |
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

Puis ouvre :
- **http://localhost:8090** (Kafka UI) → *Topics* → `machine-events` → *Messages* : les événements des machines arrivent chaque seconde ;
- **http://localhost:8082/api/kpis** : les indicateurs calculés de chaque machine (TRS, disponibilité, performance, qualité…).

### Lancer le simulateur depuis ton IDE (IntelliJ / VS Code)

```bash
docker compose up kafka kafka-ui    # Kafka seul dans Docker
```

Puis lance `SimulatorApplication` depuis l'IDE : il se connecte à `localhost:9092`.

### Lancer les tests

```bash
cd simulator   && mvn test && cd ..
cd kpi-service && mvn test && cd ..
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

## Exemple d'indicateurs (`GET /api/kpis/M1`)

```json
{
  "machineId": "M1",
  "running": true,
  "availability": 0.925,
  "performance": 0.898,
  "quality": 0.971,
  "oee": 0.807,
  "piecesProduced": 1495,
  "goodPieces": 1452,
  "rejectedPieces": 43,
  "rejectRate": 0.029,
  "throughputPerHour": 1495.0,
  "energyKwh": 13.99,
  "downCount": 13,
  "updatedAt": 1767229200000
}
```
Ici, après une heure de simulation : **TRS de 80,7 %** (disponibilité 92,5 % × performance 89,8 % × qualité 97,1 %).

## Stack

Java 21 · Spring Boot 3 · Spring Kafka · Apache Kafka (KRaft) · MongoDB · Spring Data · API REST · Docker Compose · JUnit 5 · GitHub Actions
*(à venir : WebSocket STOMP, Angular, Prometheus, Grafana)*

## Documentation

- [Architecture : services et architecture hexagonale](docs/architecture.md)
- [Jour 1 : le simulateur et Kafka expliqués](docs/jour-1.md)
- [Jour 2 : le kpi-service et le calcul du TRS](docs/jour-2.md)
