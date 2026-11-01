# Mise en ligne sur AWS (EC2)

Ce guide met FactoryPulse en ligne sur un serveur AWS, avec une adresse publique en **HTTPS**.

```
navigateur ──HTTPS──► Caddy (ports 80/443, certificat automatique)
                        ├── /auth/*  → Keycloak
                        └── /*       → dashboard (nginx) ──► kpi-service, alert-service
                                     (Kafka, MongoDB : réseau Docker interne, jamais exposés)
```

Durée : environ 1 h la première fois, dont 15 à 20 minutes de construction des images.

---

## 0. Vérifier les coûts AWS (à faire en premier)

Le serveur conseillé (**t3.medium**, 4 Go de RAM) n'est **pas** dans l'offre gratuite classique : il est payé avec les **crédits** du compte, ou facturé environ **30 $/mois** sans crédits.

1. Console AWS → **Billing and Cost Management** → **Credits** : regarde s'il te reste des crédits et jusqu'à quand.
2. **Billing and Cost Management** → **Budgets** → *Create budget* → modèle **Zero spend budget** (ou *Monthly cost budget* à 5 $) avec ton e-mail. Tu seras prévenu dès la moindre dépense.
3. Pour ne plus rien payer : **arrête** l'instance (*Stop*) quand tu n'en as pas besoin, ou **supprime-la** (*Terminate*) à la fin de ta recherche de stage, ainsi que l'Elastic IP (étape 2).

## 1. Créer le serveur (instance EC2)

Console AWS → **EC2** → en haut à droite, choisis une région proche (ex. **Europe (Paris) eu-west-3**) → **Launch instance** :

| Champ | Valeur |
|---|---|
| Name | `factorypulse` |
| Image (AMI) | **Ubuntu Server 24.04 LTS**, architecture **64-bit (x86)** |
| Instance type | **t3.medium** (2 vCPU, 4 Go) |
| Key pair | *Create new key pair* → nom `factorypulse`, type RSA, format **.pem** → le fichier se télécharge : garde-le précieusement |
| Network settings → Firewall | *Create security group*, coche **Allow SSH traffic from My IP**, **Allow HTTPS traffic from the internet**, **Allow HTTP traffic from the internet** |
| Configure storage | **30 GiB**, type gp3 |

Clique **Launch instance**.

## 2. Fixer l'adresse IP (Elastic IP)

Sans cela, l'adresse change à chaque redémarrage du serveur, et le lien de ton CV ne marcherait plus.

EC2 → **Elastic IPs** → *Allocate Elastic IP address* → *Allocate*, puis *Actions* → **Associate Elastic IP address** → choisis l'instance `factorypulse` → *Associate*.

Note cette adresse, par exemple `13.38.1.25`. Ton domaine sera **`13-38-1-25.sslip.io`** : les points remplacés par des tirets. [sslip.io](https://sslip.io) est un service gratuit qui fait pointer ce nom vers ton IP, ce qui permet d'obtenir un certificat HTTPS sans acheter de nom de domaine.

## 3. Se connecter au serveur

Depuis PowerShell, dans le dossier où se trouve `factorypulse.pem` :

```powershell
ssh -i factorypulse.pem ubuntu@13.38.1.25
```

Réponds `yes` à la première connexion. Si Windows refuse la clé (« permissions too open ») :

```powershell
icacls factorypulse.pem /inheritance:r /grant:r "$($env:USERNAME):R"
```

Toutes les commandes suivantes se tapent **sur le serveur** (l'invite devient `ubuntu@...`).

## 4. Préparer le serveur

```bash
# Docker et Docker Compose
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker ubuntu

# 2 Go de mémoire d'échange (swap) : utile pendant la construction des images
sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile && sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

exit
```

Reconnecte-toi (`ssh -i factorypulse.pem ubuntu@13.38.1.25`) pour que le groupe `docker` soit pris en compte.

## 5. Récupérer le projet et le configurer

```bash
git clone https://github.com/aboudouali1957-crypto/factorypulse.git
cd factorypulse
cp .env.example .env
nano .env
```

Dans `nano`, remplace les deux valeurs :

```
FP_DOMAIN=13-38-1-25.sslip.io
KC_ADMIN_PASSWORD=<un long mot de passe à toi>
```

Enregistre avec **Ctrl + O**, Entrée, puis quitte avec **Ctrl + X**.

## 6. Lancer

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

La première fois, la construction des images prend 15 à 20 minutes. Ensuite :

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml ps
```

Tous les services doivent être `Up` (sauf `kafka-ui`, volontairement absent en production).

## 7. Vérifier

Ouvre **https://13-38-1-25.sslip.io** (avec ton adresse). Attends 1 à 2 minutes après le démarrage, le temps que Keycloak et le certificat HTTPS soient prêts.

- Connexion avec `operateur` / `operateur` ou `responsable` / `responsable`.
- Console d'administration Keycloak : **https://13-38-1-25.sslip.io/auth** (`admin` / le mot de passe du `.env`).

## Mettre à jour après un nouveau push

```bash
cd factorypulse
git pull
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

## En cas de problème

| Symptôme | Cause probable | Solution |
|---|---|---|
| La page ne s'ouvre pas du tout | Ports 80/443 fermés | EC2 → Security groups → règles entrantes HTTP (80) et HTTPS (443) depuis `0.0.0.0/0` |
| Erreur de certificat | Domaine mal écrit dans `.env` | Vérifie `FP_DOMAIN`, puis `docker compose ... up -d` ; regarde `docker compose ... logs caddy` |
| « Keycloak injoignable » | Keycloak démarre encore | Attends 1 à 2 minutes ; `docker compose ... logs keycloak` |
| Indicateurs qui ne bougent pas | Un service a manqué de mémoire | `docker compose ... ps`, puis `docker compose ... logs kpi-service` |

## Limites de cette mise en ligne

C'est une **démonstration**, pas une installation de production complète :
- les mots de passe des comptes de démonstration sont publics : n'importe qui peut saisir une intervention ;
- Keycloak tourne en mode développement (`start-dev`, base interne), et ses comptes sont recréés à chaque redémarrage du conteneur ;
- MongoDB n'a pas de mot de passe (il n'est joignable que depuis le réseau Docker interne) ;
- un seul serveur : pas de haute disponibilité.

Pour une vraie production, on ajouterait une base PostgreSQL pour Keycloak (`start` au lieu de `start-dev`), l'authentification MongoDB, des sauvegardes et une supervision.
