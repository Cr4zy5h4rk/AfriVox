# Déploiement (Datacloud, Ubuntu Server + Docker Compose)

Une pile de quatre conteneurs derrière **un seul domaine** :

```
Internet ─ 80/443 ─► web (nginx) ─┬─ /          frontend Angular (fichiers statiques, PWA)
                                  └─ /api/…     ─► backend (Spring Boot, port 8080 interne)
                                                     └─► db (PostgreSQL, réseau interne)
certbot : obtient et renouvelle le certificat Let's Encrypt (HTTPS obligatoire : le micro l'exige)
```

Rien n'est publié sur Internet hormis les ports 80 et 443 de nginx. Les audios (`audio`), les archives de datasets
(`datasets`) et la base (`dbdata`) sont des **volumes Docker nommés** : ils survivent à une reconstruction des images.

> **Statut (12 oct. 2026)** : validé **en local seulement**. Les deux images se construisent (backend 368 Mo, web
> 50 Mo) ; la base et l'API démarrent en profil `prod` (Liquibase sur base vide : 52 changements, compte `admin`
> créé, API « healthy » en ~25 s, `/api/admin` refuse sans jeton, Swagger fermé). **Pas encore exécuté** : nginx,
> Let's Encrypt, le déploiement sur le serveur. Le premier déploiement réel sert de test ; voir « Vérifications ».

## 1. Préparer le serveur

Ubuntu Server 24.04 LTS (ou 22.04), accès SSH.

```bash
# Pare-feu : SSH, HTTP (défi Let's Encrypt), HTTPS
sudo ufw allow OpenSSH && sudo ufw allow 80/tcp && sudo ufw allow 443/tcp && sudo ufw enable

# Docker (script officiel) puis droit d'usage pour votre compte ; se reconnecter ensuite
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker "$USER"
```

### Serveur modeste (1 vCPU, 2 Go de RAM, 30 Go) : à faire avant tout

C'est la configuration commandée. Elle suffit pour une démonstration, à deux conditions :

```bash
# 1. Swap de 2 Go : filet de sécurité contre un arrêt de l'API ou de la base faute de mémoire
sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile && sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
sudo sysctl vm.swappiness=10

# 2. Surveiller le disque (images, caches de build, sauvegardes)
df -h / && docker system df          # nettoyer : docker builder prune -f ; docker image prune -f
```

**Construire les images sur votre PC plutôt que sur le serveur** (Maven + Angular sur 1 vCPU / 2 Go : très lent,
voire tué par manque de mémoire) :

```bash
# Sur votre PC (Docker Desktop), depuis AfriVox/deploy, avec un .env rempli :
docker compose build
docker save afrivox-backend:latest afrivox-web:latest | gzip > afrivox-images.tar.gz
scp afrivox-images.tar.gz <utilisateur>@<serveur>:~/afrivox/

# Sur le serveur :
gunzip -c ~/afrivox/afrivox-images.tar.gz | docker load
SKIP_BUILD=1 ./init-letsencrypt.sh        # étape 3 ci-dessous, sans reconstruire
```

Réserves de capacité : la copie personnalisée d'un gros dataset (archive de plusieurs centaines de Mo) occupe le
processeur pendant sa génération ; le premier démarrage de l'API prend ~1 à 2 minutes. Pour des collectes réelles
(centaines d'heures d'audio), 30 Go ne suffiront pas : prévoir un disque supplémentaire ou un autre stockage.

**Nom de domaine** : créer un enregistrement DNS `A` (et `AAAA` si IPv6) vers l'adresse IP du serveur **avant** de
lancer l'étape 3. Let's Encrypt valide le domaine par HTTP, il doit donc déjà répondre sur cette adresse.
Sans domaine à vous, un sous-domaine gratuit convient (DuckDNS par exemple) ; les services de type `x.y.z.w.nip.io`
sont souvent limités par Let's Encrypt.

## 2. Récupérer le code et les secrets

Les deux dépôts doivent être **côte à côte** (le build du frontend est lancé depuis le dépôt du backend) :

```bash
mkdir -p ~/afrivox && cd ~/afrivox
git clone <url-du-depot-backend>   AfriVox
git clone <url-du-depot-frontend>  Afrivox_Frontend
cd AfriVox/deploy
cp .env.example .env
```

Remplir `.env` (jamais commité) :

| Variable | Comment l'obtenir |
|---|---|
| `DOMAIN` | le nom de domaine, sans `https://` |
| `LETSENCRYPT_EMAIL` | une adresse qui reçoit les avis d'expiration |
| `POSTGRES_PASSWORD` | `openssl rand -base64 24 \| tr -d '/+='` |
| `JWT_BASE64_SECRET` | `openssl rand -base64 64 \| tr -d '\n'` |
| `ADMIN_PASSWORD_HASH` | voir ci-dessous (entre **apostrophes simples**) |

Hash BCrypt du mot de passe de l'administrateur (le mot de passe lui-même n'est écrit nulle part) :

```bash
docker run --rm httpd:2-alpine htpasswd -nbBC 10 admin 'VotreMotDePasseLong' | cut -d: -f2 | sed 's/^\$2y/\$2a/'
```

Le compte créé s'appelle `admin`. Il n'existe qu'à la **première** initialisation de la base : changer
`ADMIN_PASSWORD_HASH` plus tard ne modifie pas un compte existant.

## 3. Premier démarrage

```bash
chmod +x init-letsencrypt.sh backup.sh
STAGING=1 ./init-letsencrypt.sh   # essai avec un certificat de test (pas de limite de débit)
```

Si l'essai passe (le script se termine par « Termine »), refaire avec le vrai certificat :

```bash
docker compose down            # sans -v : les volumes sont conservés
docker volume rm afrivox_certbot-conf afrivox_certbot-www
./init-letsencrypt.sh
```

La première construction compile le backend (Maven) et le frontend (npm) : plusieurs minutes.

## 4. Vérifications après démarrage

- `docker compose ps` : `db`, `backend` et `web` en `healthy` / `running`.
- `https://<domaine>/` s'affiche, cadenas valide ; `http://<domaine>/` redirige vers HTTPS.
- `https://<domaine>/management/health` répond **404** (supervision non exposée).
- Connexion avec `admin` ; inscription d'un contributeur ; **enregistrement d'un audio** (le micro n'est possible
  qu'en HTTPS).
- `docker compose logs backend` : pas d'erreur Liquibase ; message de démarrage du profil `prod`.
- Vérifier que le cookie de rafraîchissement porte `HttpOnly; Secure; SameSite` (outils du navigateur).
- Un envoi depuis un second appareil : l'adresse IP vue par l'API doit être la sienne, pas celle de nginx
  (`docker compose logs backend`, ou table `download_log` après un téléchargement).

## 5. Exploitation

```bash
docker compose logs -f backend            # journaux de l'API
docker compose ps                         # état
docker compose restart backend            # redémarrer l'API seule

# Mise à jour : récupérer les deux dépôts puis reconstruire (les migrations Liquibase s'appliquent au démarrage)
git -C ../ pull && git -C ../../Afrivox_Frontend pull
docker compose build && docker compose up -d
```

### Sauvegardes

`./backup.sh` écrit dans `deploy/backups/` un dump compressé de la base (`db-<date>.sql.gz`) et une archive de chaque
volume (`audio-…`, `datasets-…`), et garde les 7 dernières. Planification quotidienne :

```bash
( crontab -l 2>/dev/null; echo "30 2 * * * cd $HOME/afrivox/AfriVox/deploy && ./backup.sh >> backup.log 2>&1" ) | crontab -
```

**Copier `backups/` hors du serveur** (poste d'un membre, autre stockage) : une sauvegarde sur le même disque ne
protège pas d'une panne du disque.

Restauration (sur une base vide) :

```bash
gunzip -c backups/db-<date>.sql.gz | docker compose exec -T db psql -U afrivox -d afrivox
docker run --rm -v afrivox_audio:/data -v "$PWD/backups:/in" alpine:3 tar xzf /in/audio-<date>.tar.gz -C /data
docker run --rm -v afrivox_datasets:/data -v "$PWD/backups:/in" alpine:3 tar xzf /in/datasets-<date>.tar.gz -C /data
```

## 6. Limites et points ouverts

- **Jamais testé** sur un vrai serveur : prévoir de la marge pour le premier déploiement (plusieurs essais possibles).
- **Pas de données au départ** : volumes vides. Les scripts d'import (`scripts/dev/`) refusent volontairement tout
  serveur non local ; une démonstration publique demande des données dont la licence est claire (les données sereer
  de test ne doivent jamais y aller).
- **SMTP absent** : les comptes sont activés à l'inscription (`application.auto-activate-accounts`).
- **Paiement simulé** : les références `SIM-…` ne correspondent à aucun vrai paiement.
- **Pas de Content-Security-Policy** dans nginx (à ajouter après essai, Angular Material demande une liste précise).
- **Un seul serveur** : pas de haute disponibilité ; la reprise passe par la sauvegarde.
- **Images de base** : `postgres:18.6`, `eclipse-temurin:21`, `nginx:1.27-alpine`, `node:22-alpine`,
  `certbot/certbot`, `alpine:3`, `httpd:2-alpine` (outils d'exécution, non distribués avec le code) ; à citer dans
  l'inventaire des licences.
