# AfriVox : backend

Plateforme de données vocales africaines, réalisée pour le concours CADEV 2026 (Systalink).

- **Contributeurs** : enregistrent des phrases dans leur langue (wolof, pulaar, sereer…), valident celles des autres et gagnent des crédits.
- **Développeurs** : parcourent un catalogue de datasets versionnés, les téléchargent, publient des modèles et les comparent sur un classement public.
- **Organisations** : demandent un dataset sur mesure et suivent l'avancement de leur demande.
- **Administrateurs** : ouvrent des campagnes, qualifient les validateurs, publient des versions de datasets.

Ce dépôt contient l'**API** (Spring Boot). L'interface Angular est dans le dépôt `Afrivox_Frontend`.

## Sommaire

1. [Fonctionnalités](#fonctionnalités)
2. [Architecture](#architecture)
3. [Démarrage](#démarrage)
4. [Configuration](#configuration)
5. [API](#api)
6. [Rôles et sécurité](#rôles-et-sécurité)
7. [Données sur disque](#données-sur-disque)
8. [Déploiement](#déploiement)
9. [Client Python et scripts](#client-python-et-scripts)
10. [Structure du dépôt](#structure-du-dépôt)
11. [Licences](#licences)

## Fonctionnalités

| Domaine                      | Ce que fait la plateforme                                                                                                                                         |
| ---------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Collecte**                 | Campagnes de phrases, envoi d'enregistrements audio (WAV, WebM, Ogg, M4A, format reconnu par le contenu), consentement versionné et révocable.                    |
| **Contrôle qualité**         | Analyse automatique des WAV : durée réelle, niveau, saturation, silence, bruit estimé, note de 0 à 100. Les enregistrements inutilisables sont refusés à l'envoi. |
| **Validation par les pairs** | Chaque enregistrement est relu par plusieurs validateurs qualifiés ; consensus à 2 avis concordants ; tâches pièges pour mesurer la fiabilité des validateurs.    |
| **Langues**                  | Une langue devient active quand elle compte assez de validateurs qualifiés ; avant, liste d'attente.                                                              |
| **Période d'essai**          | Un nouveau contributeur démarre avec un plafond d'envois ; il passe actif seul si ses enregistrements sont validés.                                               |
| **Crédits**                  | Registre en ajout seul, gains versés au consensus, tarifs réglables par l'administrateur, surcharge par campagne.                                                 |
| **Datasets**                 | Versions immuables (archive zip, manifeste, découpages train/dev/test par locuteur, fiche technique), par langue et par domaine, avec plans Starter, Plus et Pro. |
| **Achat et téléchargement**  | Devis selon la licence choisie (Prototype, Recherche, Commercial), liens signés et expirants, copie personnalisée et traçable par acheteur.                       |
| **Model Hub**                | Publication de modèles (métadonnées et lien, jamais les poids), héritage de licence depuis les datasets d'entraînement, commentaires, signalement, notifications. |
| **Classement**               | Le développeur envoie ses prédictions (CSV) ; la plateforme calcule WER et CER contre des transcriptions de référence jamais publiées.                            |
| **Clés d'API**               | Clés à portées limitées (`DOWNLOAD`, `EVALUATE`, `PUBLISH`), hachées, révocables, jamais administrateur.                                                          |
| **Administration**           | Utilisateurs, campagnes, datasets, demandes, contributeurs, crédits, modération, contrôle qualité, recherche de fuite d'une copie diffusée.                       |

## Architecture

- **JHipster 9.3** : monolithe Spring Boot 4.1, Java 21, Maven.
- **PostgreSQL 18**, schéma géré par **Liquibase** (`src/main/resources/config/liquibase`).
- **JWT** : jeton d'accès de 15 minutes, jeton de rafraîchissement en cookie `HttpOnly`, rotation et détection de rejeu.
- **Log4j2** pour les journaux ; pas de cache applicatif.
- Audios et archives de datasets écrits sur disque (volumes Docker en production).
- Derrière nginx, un seul domaine : le frontend est servi en statique, `/api` est transmis à Spring.

```
src/main/java/com/afrivox/app/
├── domain/        entités JPA et énumérations
├── repository/    accès aux données (Spring Data)
├── service/       logique métier (validation, crédits, datasets, évaluation, licences…)
├── web/rest/      contrôleurs REST
├── web/filter/    authentification par clé d'API et liste blanche des routes
├── security/      JWT, rôles, limitation des tentatives
└── config/        configuration Spring
```

## Démarrage

### Prérequis

- JDK 21
- Docker (PostgreSQL de développement)
- Node.js (outils JHipster : prettier, hooks Git)

### Lancer en développement

```bash
docker compose -f src/main/docker/postgresql.yml up --wait   # PostgreSQL sur localhost:5432
./mvnw                                                       # profil dev, http://localhost:8080
```

Le profil `dev` charge des valeurs d'exemple pour les secrets (`application-secret-samples.yml`) et quelques données de départ (langue wolof, une campagne, des phrases).

Le compte `admin` est créé par Liquibase au premier démarrage à partir de `AFRIVOX_ADMIN_PASSWORD_HASH` (hash BCrypt). Pour en générer un :

```bash
docker run --rm httpd:2.4-alpine htpasswd -nbBC 10 "" 'MotDePasse' | cut -d: -f2
# remplacer le préfixe $2y$ par $2a$
```

### Construire

```bash
./mvnw -Pprod clean verify -DskipTests      # jar de production
```

Les images Docker et la pile complète (PostgreSQL, API, nginx, HTTPS) se construisent avec `deploy/` (voir [Déploiement](#déploiement)).

Sous Windows, si Maven échoue avec une erreur de certificat PKIX :

```bash
export MAVEN_OPTS="-Djavax.net.ssl.trustStoreType=Windows-ROOT"
```

## Configuration

Le profil `prod` refuse de démarrer si un secret manque. Aucun secret n'est versionné.

| Variable d'environnement                                                            | Rôle                                                                                                              |
| ----------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------- |
| `JHIPSTER_SECURITY_AUTHENTICATION_JWT_BASE64SECRET`                                 | Secret de signature des JWT (Base64, 64 octets minimum). Sert aussi à dériver la clé des liens de téléchargement. |
| `AFRIVOX_ADMIN_PASSWORD_HASH`                                                       | Hash BCrypt du mot de passe du compte `admin` initial.                                                            |
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | Accès PostgreSQL.                                                                                                 |

Propriétés de l'application (`application.*`, surchargeables par variable d'environnement) :

| Propriété                                                                | Défaut                 | Effet                                                                                            |
| ------------------------------------------------------------------------ | ---------------------- | ------------------------------------------------------------------------------------------------ |
| `auto-activate-accounts`                                                 | `true`                 | Active les comptes à l'inscription, sans e-mail. À mettre à `false` quand un SMTP est configuré. |
| `audio.storage-dir`                                                      | `data/audio`           | Dossier des enregistrements.                                                                     |
| `audio.qc.*`                                                             | voir `application.yml` | Seuils des contrôles qualité (`enabled` pour les couper).                                        |
| `datasets.storage-dir`                                                   | `data/datasets`        | Archives de référence des versions publiées.                                                     |
| `datasets.link-ttl-seconds`                                              | `600`                  | Validité d'un lien de téléchargement administrateur.                                             |
| `datasets.personalize-downloads`                                         | `true`                 | Sert une copie personnalisée (traçable) à chaque acheteur.                                       |
| `contributor-trial.enabled`, `.max-recordings`, `.min-accept-rate`       | `true`, `10`, `0.7`    | Période d'essai des contributeurs.                                                               |
| `security.login-max-attempts`, `.ip-max-attempts`, `.login-lock-seconds` | `5`, `30`, `900`       | Limitation des tentatives de connexion.                                                          |
| `consent.current-version`                                                | `2026-10-v1`           | Version du consentement demandée à l'inscription.                                                |

Derrière nginx, `X-Forwarded-For` et `X-Forwarded-Proto` doivent être transmis, sinon la limitation des tentatives et les journaux voient l'adresse du proxy.

## API

Toutes les routes sont sous `/api`. Les routes d'écriture et de lecture privée exigent un jeton `Authorization: Bearer <jwt>`.

| Préfixe                                              | Accès               | Contenu                                                                                                                       |
| ---------------------------------------------------- | ------------------- | ----------------------------------------------------------------------------------------------------------------------------- |
| `/api/authenticate`, `/api/register`, `/api/account` | public / connecté   | Connexion, rafraîchissement, déconnexion, inscription (rôle et consentement), compte.                                         |
| `/api/public/**`                                     | public              | Statistiques, langues, catalogue de datasets, modèles, classement, jeux de test, devis, téléchargements par lien signé.       |
| `/api/catalog/**`                                    | connecté            | Aperçu des datasets (texte, extraits audio limités).                                                                          |
| `/api/me/**`                                         | connecté (par rôle) | Mes campagnes, enregistrements, validation, crédits, profil contributeur, modèles, évaluations, demandes, achats, clés d'API. |
| `/api/purchases/{id}/download-link`                  | acheteur            | Lien de téléchargement signé pour un achat.                                                                                   |
| `/api/community/**`                                  | connecté            | Commentaires, signalements, notifications.                                                                                    |
| `/api/admin/**`                                      | administrateur      | Gestion des utilisateurs, campagnes, datasets, demandes, crédits, modération, qualité, traçabilité.                           |

La documentation OpenAPI est disponible en développement ; elle est fermée en production. Une clé d'API (`X-API-Key`) n'ouvre que les quelques routes de sa portée : tout le reste est refusé.

## Rôles et sécurité

| Rôle                | Attribué                  | Usage                         |
| ------------------- | ------------------------- | ----------------------------- |
| `ROLE_USER`         | tous les comptes          | base                          |
| `ROLE_CONTRIBUTOR`  | choix à l'inscription     | enregistrer, valider          |
| `ROLE_DEVELOPER`    | choix à l'inscription     | datasets, modèles, clés d'API |
| `ROLE_ORGANIZATION` | choix à l'inscription     | demandes sur mesure, achats   |
| `ROLE_ADMIN`        | compte initial uniquement | administration                |

Points de sécurité :

- mots de passe hachés (BCrypt), 8 caractères minimum ;
- jeton d'accès gardé en mémoire côté navigateur, jeton de rafraîchissement en cookie `HttpOnly`, `Secure`, `SameSite=Strict`, stocké haché ;
- limitation des tentatives de connexion par identifiant et par adresse ;
- rôles vérifiés côté serveur, refus par défaut sur les routes non déclarées ;
- fichiers audio identifiés par leur contenu, nom généré par le serveur ;
- liens de téléchargement signés (HMAC) et expirants ;
- clés d'API : seul le hash SHA-256 est stocké, affichage unique, révocation immédiate ;
- transcriptions de référence du classement jamais exposées.

## Données sur disque

| Dossier         | Contenu                                     | À sauvegarder |
| --------------- | ------------------------------------------- | ------------- |
| `data/audio`    | Enregistrements des contributeurs           | oui           |
| `data/datasets` | Archives de référence des versions publiées | oui           |

Ces dossiers sont ignorés par Git. En production, ce sont des volumes Docker nommés.

## Déploiement

Le dossier [`deploy/`](deploy/README.md) contient la pile complète pour un serveur Linux : Docker Compose (PostgreSQL, API, nginx, certbot), HTTPS Let's Encrypt, sauvegardes et instructions pas à pas.

## Client Python et scripts

- [`clients/python`](clients/python/README.md) : bibliothèque et ligne de commande `afrivox` (téléchargement, soumission au classement, publication de modèles) avec une clé d'API. Aucune dépendance.
- `scripts/dev/` : import de données de test vers un serveur **local** uniquement. Les jeux de données source ne sont pas dans le dépôt.
- `scripts/licenses/generate.mjs` : génère l'inventaire des licences.

## Structure du dépôt

```
AfriVox/
├── src/main/java/…       code de l'API
├── src/main/resources/   configuration, migrations Liquibase
├── deploy/               Docker Compose, nginx, sauvegardes
├── clients/python/       client Python et CLI
├── scripts/              import de données de test, inventaire des licences
├── docs/                 politique de licences
├── afrivox.jdl           modèle de données d'origine (JHipster)
└── LICENSES-THIRD-PARTY.md
```

Le fichier `afrivox.jdl` a servi à générer les entités. Ne pas relancer la génération JHipster complète : elle écraserait les modifications faites depuis. Pour ajouter une entité, utiliser `jhipster jdl afrivox.jdl --ignore-application --skip-git` puis relire le `git diff`.

## Licences

Le projet n'utilise que des dépendances sous licence permissive (MIT, Apache 2.0, BSD…). L'inventaire complet est dans [`LICENSES-THIRD-PARTY.md`](LICENSES-THIRD-PARTY.md), généré par `scripts/licenses/generate.mjs` ; la politique et les points restant à confirmer sont dans [`docs/LICENCES.md`](docs/LICENCES.md).
