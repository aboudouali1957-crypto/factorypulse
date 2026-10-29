# Jour 2 : le kpi-service (calcul du TRS)

## Ce qu'on a construit

Un deuxième microservice, **kpi-service**, qui :

1. **lit** les tickets des machines dans Kafka (topic `machine-events`) ;
2. **calcule** les indicateurs de chaque machine : TRS, disponibilité, performance, qualité, taux de rebut, cadence, énergie ;
3. **stocke** le résultat dans **MongoDB** (un document par machine) ;
4. **expose** les indicateurs en **API REST** : `GET http://localhost:8082/api/kpis`.

```
simulator ──► Kafka ──► kpi-service ──► MongoDB
 (jour 1)               │  (jour 2)
                        └──► API REST /api/kpis
```

## Le TRS, simplement

Le **TRS** (Taux de Rendement Synthétique, en anglais **OEE**) répond à la question : « sur le temps où la machine aurait pu produire, quelle part a vraiment servi à faire des pièces bonnes ? »

```
TRS = Disponibilité × Performance × Qualité
```

| Indicateur | Question | Calcul |
|---|---|---|
| Disponibilité | A-t-elle tourné ou été en panne ? | temps de marche / temps total |
| Performance | A-t-elle tourné à la bonne vitesse ? | (pièces × cycle idéal) / temps de marche |
| Qualité | Les pièces sont-elles bonnes ? | pièces bonnes / pièces produites |

**Exemple** (c'est le scénario du test `MachineKpiTest`) : sur 10 secondes, la machine tourne 8 s puis tombe en panne 2 s. Elle produit 4 pièces (cycle idéal de 2 s), dont 1 rebut.
- Disponibilité = 8 / 10 = **80 %**
- Performance = (4 × 2 s) / 8 s = **100 %**
- Qualité = 3 / 4 = **75 %**
- **TRS = 0,8 × 1 × 0,75 = 60 %**

Avec le simulateur, on obtient environ **80 à 85 %** de TRS sur une heure. Dans l'industrie, 85 % est considéré comme excellent.

## Comment on calcule le temps de marche

Le service ne reçoit pas « la machine a tourné 8 secondes ». Il reçoit seulement des tickets datés. Il fait donc comme un chronomètre :

1. il retient l'**état** de la machine (en marche ou en panne) ;
2. à chaque ticket, il calcule le temps écoulé depuis le ticket précédent ;
3. il ajoute ce temps au compteur « marche » ou « arrêt », selon l'état ;
4. puis il applique le ticket : `MACHINE_DOWN` → état « panne », `MACHINE_UP` → état « marche ».

**Cas particulier** : si plus de 10 secondes séparent deux tickets, c'est que le simulateur était arrêté (par exemple entre hier soir et aujourd'hui). Ce temps n'est compté nulle part, sinon il fausserait la disponibilité.

## L'architecture (la même que le simulateur)

| Couche | Fichier | Rôle |
|---|---|---|
| Domaine | `MachineKpi` | Le calcul des indicateurs d'une machine, Java pur |
| Domaine | `MachineEvent`, `EventType` | Le format d'un ticket reçu |
| Domaine | `KpiSnapshot` | Les indicateurs à un instant donné (ce qu'on affiche) |
| Domaine | `MachineKpiState` | Les compteurs bruts à sauvegarder |
| Application | `ProcessMachineEventUseCase` | « Un ticket arrive : mettre à jour la machine » |
| Application | `GetKpisUseCase` | « Consulter les indicateurs » |
| Port de sortie | `MachineKpiRepository` | Interface « sauvegarder / relire », sans savoir où |
| Adaptateur d'entrée | `MachineEventListener` | Lit Kafka (`@KafkaListener`) |
| Adaptateur d'entrée | `KpiController` | API REST (`@RestController`) |
| Adaptateur de sortie | `MongoMachineKpiRepository` | Implémente le port avec MongoDB |
| Configuration | `KpiProperties`, `UseCaseConfig` | Lit `application.yml` et assemble le tout |

## Choix techniques

**Pourquoi le kpi-service a sa propre copie de `MachineEvent` ?**
Deux microservices ne partagent pas de code : sinon, modifier l'un obligerait à redéployer l'autre. Leur seul contrat commun est le **format JSON** du message Kafka.

**Qu'est-ce qu'un consumer group ?**
Toutes les instances du kpi-service ont le même `group-id`. Kafka leur répartit les 3 partitions : avec 3 instances, chacune lit une partition, donc on traite 3 fois plus vite. Comme une machine est toujours dans la même partition, une seule instance s'occupe d'elle : pas de conflit.

**Que se passe-t-il si le kpi-service redémarre ?**
Kafka retient l'**offset**, c'est-à-dire le dernier message traité par le groupe. Au redémarrage, le service reprend là où il s'était arrêté. Les compteurs de chaque machine sont relus dans MongoDB, donc le calcul continue sans repartir de zéro.

**Et si un message est traité deux fois ?**
C'est possible : Kafka garantit « au moins une fois » (*at-least-once*). Si le service plante juste après avoir sauvegardé mais avant d'enregistrer l'offset, le message est relu. Pour l'éviter, on pourrait mémoriser les `eventId` déjà traités (**idempotence**) : c'est une amélioration possible.

**Pourquoi MongoDB ?**
Un document par machine, avec des champs qui peuvent évoluer, et des lectures et écritures simples par identifiant. Une base relationnelle (PostgreSQL) marcherait aussi : grâce au port `MachineKpiRepository`, on pourrait changer sans toucher au calcul.

**Pourquoi `ErrorHandlingDeserializer` ?**
Si un message illisible arrive dans le topic (un « poison pill »), le service le signale et passe au suivant, au lieu de rester bloqué à essayer de le lire en boucle.
