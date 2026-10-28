# Jour 1 : le simulateur et Kafka

## Ce qu'on a construit

Un service Spring Boot qui simule **4 machines** (presse, assemblage, conditionnement, contrôle qualité). Chaque seconde, chaque machine :

1. **tourne** : elle avance dans son cycle et produit une pièce à chaque cycle terminé. La pièce est bonne ou c'est un rebut, selon le `reject-rate` ;
2. ou **tombe en panne**, avec une probabilité `failure-probability` par seconde. Elle ne produit plus rien ;
3. ou, si elle est en panne, **est réparée**, avec une probabilité `repair-probability` par seconde.

Toutes les 5 secondes, elle publie aussi l'énergie consommée.

Tous ces événements sont publiés dans le topic Kafka `machine-events`.

## Le rôle de chaque fichier

Le simulateur suit une **architecture hexagonale** : voir [architecture.md](architecture.md).

| Fichier | Couche | Rôle |
|---|---|---|
| `domain/MachineSimulator.java` | Domaine | Le cœur : la logique d'une machine. Java pur, sans Spring. |
| `domain/MachineEvent.java` | Domaine | Le format d'un événement (un `record` Java). |
| `domain/MachineConfig.java` | Domaine | Les caractéristiques d'une machine, avec validation. |
| `domain/EventType.java` | Domaine | Les 4 types d'événements. |
| `application/SimulateProductionLineUseCase.java` | Application | Cas d'usage : fait avancer toute la ligne d'un tick et publie les événements. |
| `application/port/out/MachineEventPublisher.java` | Port de sortie | Interface « publier un événement », sans savoir où. |
| `adapter/out/kafka/KafkaMachineEventPublisher.java` | Adaptateur de sortie | Implémente le port avec Kafka (clé = `machineId`). |
| `adapter/in/scheduler/SimulationScheduler.java` | Adaptateur d'entrée | Déclenche le cas d'usage chaque seconde (`@Scheduled`). |
| `config/SimulatorProperties.java` | Configuration | Lit la section `simulator:` de `application.yml`. |
| `config/KafkaTopicConfig.java` | Configuration | Crée le topic `machine-events` avec 3 partitions. |
| `config/UseCaseConfig.java` | Configuration | Assemble le cas d'usage et l'adaptateur Kafka. |
| `MachineSimulatorTest.java` | Test | Teste une machine avec un aléatoire fixé (seed). |
| `SimulateProductionLineUseCaseTest.java` | Test | Teste le cas d'usage avec un faux publisher en mémoire, sans Kafka. |

## Les choix techniques, et comment les expliquer en entretien

**Pourquoi Kafka plutôt qu'un appel REST direct ?**
Le simulateur et le service de calcul sont **découplés** : si le service KPI s'arrête, les événements restent stockés dans Kafka et seront lus au redémarrage. On peut aussi ajouter d'autres consommateurs (alertes, archivage) sans toucher au simulateur.

**Pourquoi la clé du message est le `machineId` ?**
Kafka envoie tous les messages qui ont la même clé dans la **même partition**. L'ordre est garanti à l'intérieur d'une partition, donc les événements d'une machine sont lus dans l'ordre. C'est indispensable : « panne » puis « réparation » ne doit pas devenir « réparation » puis « panne ».

**Pourquoi 3 partitions ?**
Pour pouvoir lire en parallèle : jusqu'à 3 instances du service KPI pourront se partager le travail (jour 2).

**Pourquoi le simulateur est en Java pur, séparé de Spring ?**
Pour le tester sans démarrer Kafka ni Spring. Avec un `Random` initialisé avec une seed, le test donne toujours le même résultat. C'est ce qu'on appelle un test **déterministe**.

**Pourquoi un `timestamp` en millisecondes (epoch) ?**
C'est un nombre simple, sans fuseau horaire ni format de date à interpréter. C'est le format standard pour des événements.

**Qu'est-ce que KRaft ?**
Le mode de Kafka sans ZooKeeper : Kafka gère lui-même ses métadonnées. Un seul conteneur suffit.

**Comment la vitesse réelle varie-t-elle ?**
Elle est tirée entre 80 % et 100 % de la vitesse idéale à chaque seconde. Ça servira au calcul de la **performance** du TRS au jour 2.

## Rappel : le TRS (calculé au jour 2)

```
TRS = Disponibilité × Performance × Qualité

Disponibilité = temps de marche / temps d'ouverture
Performance   = (pièces produites × temps de cycle idéal) / temps de marche
Qualité       = pièces bonnes / pièces produites
```

## Questions à te poser avant le jour 2

1. Que se passe-t-il si le simulateur envoie un événement alors que Kafka est arrêté ?
2. Pourquoi la presse (M1) produit-elle moins de 1 800 pièces par heure alors que son cycle idéal est de 2 secondes ?
3. Si on voulait 10 machines au lieu de 4, quel fichier faudrait-il modifier ?
