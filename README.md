# AfriVox

Plateforme de données vocales africaines (concours CADEV 2026, Systalink). Des contributeurs enregistrent et valident des phrases dans des langues africaines (wolof, pulaar d'abord) ; des développeurs téléchargent des jeux de données et publient des modèles ; des organisations demandent des jeux de données sur mesure.

Ce dépôt contient le **backend** : monolithe JHipster 9.3.0 (Spring Boot 4.1.1, Java 21, Maven, PostgreSQL, Liquibase, JWT), sans frontend généré. Le frontend Angular sera une application séparée.

Le fichier `../CLAUDE.md` décrit le contexte du projet, le règlement du concours et les décisions de stack. Ce README garde la trace de ce qui a été fait dans le code et de la façon de le faire tourner.

## Sommaire

1. [Démarrage](#démarrage)
2. [Configuration et secrets](#configuration-et-secrets)
3. [Journal des modifications](#journal-des-modifications)
4. [Authentification](#authentification)
5. [Rôles](#rôles)
6. [Dépendances et licences](#dépendances-et-licences)
7. [Tests](#tests)
8. [Reste à faire](#reste-à-faire)

## Démarrage

### Prérequis

- JDK 21. Sur la machine de développement, celui d'IntelliJ convient : `C:\Program Files\JetBrains\IntelliJ IDEA 2025.1.3\jbr`.
- Docker Desktop, pour PostgreSQL et pour les tests d'intégration (Testcontainers).
- Node.js uniquement pour les outils JHipster (prettier, hooks).

### Variables d'environnement pour construire sur Windows

```bash
export JAVA_HOME="/c/Program Files/JetBrains/IntelliJ IDEA 2025.1.3/jbr"
# Sans cette option, Maven échoue avec une erreur de certificat PKIX sur Maven Central
export MAVEN_OPTS="-Djavax.net.ssl.trustStoreType=Windows-ROOT"
```

### Lancer en développement

```bash
docker compose -f src/main/docker/postgresql.yml up --wait   # base PostgreSQL
./mvnw                                                       # profil dev, port 8080
```

Le profil `dev` inclut `secret-samples` : les secrets de développement y sont des valeurs d'exemple. Voir la section suivante pour le hash du mot de passe administrateur.

### Construire

```bash
./mvnw -Pprod clean verify          # jar de production + tests
./mvnw -ntp verify -DskipTests -Pprod jib:dockerBuild   # image Docker
```

## Configuration et secrets

Aucun secret n'est versionné pour la production. Le profil `prod` refuse de démarrer si l'un d'eux manque.

| Variable d'environnement                                   | Rôle                                                                                                 |
| ---------------------------------------------------------- | ---------------------------------------------------------------------------------------------------- |
| `JHIPSTER_SECURITY_AUTHENTICATION_JWT_BASE64SECRET`        | Secret de signature des JWT (Base64, 64 octets minimum). Ne jamais réutiliser celui du profil `dev`. |
| `AFRIVOX_ADMIN_PASSWORD_HASH`                              | Hash BCrypt du mot de passe du compte `admin` créé par Liquibase.                                    |
| `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | Accès PostgreSQL.                                                                                    |

Générer le hash BCrypt de l'administrateur :

```bash
docker run --rm httpd:2.4-alpine htpasswd -nbBC 10 "" 'MotDePasse' | cut -d: -f2
# remplacer le préfixe $2y$ par $2a$
```

En local, remplacer `CHANGE_ME_BCRYPT_HASH` dans `application-secret-samples.yml` (sans commiter le résultat) ou définir `AFRIVOX_ADMIN_PASSWORD_HASH`.

### Propriétés de l'application (`application.*`)

| Propriété                                             | Défaut                                     | Effet                                                                                             |
| ----------------------------------------------------- | ------------------------------------------ | ------------------------------------------------------------------------------------------------- |
| `auto-activate-accounts`                              | `false` (code), `true` (`application.yml`) | Active les comptes à l'inscription, sans e-mail. À passer à `false` quand le SMTP sera configuré. |
| `security.refresh-token-validity-seconds`             | 604800 (7 j)                               | Durée du cookie de rafraîchissement.                                                              |
| `security.refresh-token-remember-me-validity-seconds` | 2592000 (30 j)                             | Durée avec « se souvenir de moi ».                                                                |
| `security.refresh-cookie-secure`                      | `true`                                     | `false` en développement local (HTTP).                                                            |
| `security.login-max-attempts`                         | 5                                          | Échecs par couple (IP, identifiant) avant blocage.                                                |
| `security.ip-max-attempts`                            | 30                                         | Échecs par IP avant blocage.                                                                      |
| `security.login-lock-seconds`                         | 900                                        | Durée du blocage et fenêtre de comptage.                                                          |
| `consent.current-version`                             | `2026-10-v1`                               | Version du consentement à accepter à l'inscription.                                               |

### Derrière nginx

Le profil `prod` active `server.forward-headers-strategy: native`. nginx doit donc transmettre `X-Forwarded-For` et `X-Forwarded-Proto`, sans quoi la limitation des tentatives voit toujours la même adresse.

## Journal des modifications

État au **5 octobre 2026**. Le dépôt ne contient qu'un commit (génération JHipster) ; les changements ci-dessous ne sont pas encore commités.

### 1. Nettoyage après la génération JHipster

| Sujet                  | Avant                                                            | Après                                                                                                                                                                                          |
| ---------------------- | ---------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Comptes par défaut     | `admin/admin` et `user/user` chargés par CSV                     | Supprimés. L'`admin` est créé par le changeSet Liquibase `00000000000003` à partir de `AFRIVOX_ADMIN_PASSWORD_HASH` ; le démarrage s'arrête si le hash manque. Non exécuté en contexte `test`. |
| Secret JWT             | Secret de dev partout                                            | Dev uniquement (`secret-samples`) ; production par variable d'environnement.                                                                                                                   |
| Activation des comptes | E-mail obligatoire, pas de SMTP                                  | `application.auto-activate-accounts`.                                                                                                                                                          |
| Rôles                  | `ROLE_ADMIN`, `ROLE_USER`                                        | + `ROLE_CONTRIBUTOR`, `ROLE_DEVELOPER`, `ROLE_ORGANIZATION`.                                                                                                                                   |
| Journalisation         | Logback                                                          | Log4j2 (`spring-boot-starter-log4j2`). Protection contre la falsification de journal : retours à la ligne et tabulations des messages remplacés par `_` (`%replace` dans `log4j2-spring.xml`). |
| Cache                  | Hazelcast, cache de second niveau Hibernate, `@Cacheable`        | Retirés. Décision du projet : « pas de cache ».                                                                                                                                                |
| Supervision            | Prometheus, Grafana, Control Center, nombreux endpoints actuator | Retirés. Actuator n'expose plus que `health` et `info`. La règle qui laissait `/management/prometheus` en accès libre est supprimée.                                                           |

Le changeSet initial (`00000000000001`) a été modifié au lieu d'être complété. Si une base locale existe déjà, la recréer, sinon Liquibase signale une erreur de somme de contrôle.

Fichiers supprimés : `CacheConfiguration`, `CacheKeyGeneratorConfiguration`, `LoggingConfiguration` (appenders JSON et Logstash propres à Logback), `CRLFLogConverter` et son test, `logback-spring.xml`, `logback.xml`, `user.csv`, `user_authority.csv`, et les fichiers Docker de Hazelcast, Prometheus, Grafana et du Control Center.

Le message de démarrage de l'application tient maintenant sur une ligne, car le journal remplace les retours à la ligne.

### 2. Endpoints JHipster retirés

- `GET /api/users` (`PublicUserResource`) : listait les identifiants de tous les utilisateurs à n'importe quel utilisateur connecté. Retiré, ainsi que `UserService.getAllPublicUsers` et la requête associée. Un profil public de développeur sera ajouté plus tard, avec seulement les champs voulus.
- `/api/authorities` (`AuthorityResource`) : création et suppression de rôles par l'API, alors que les rôles sont fixes dans le code. Retiré avec son test.
- Conservés : `/api/admin/users` (gestion des comptes par l'administrateur, réservée à `ROLE_ADMIN`) et `/api/account` (profil, mot de passe).

### 3. Authentification

Détaillée dans la section suivante. Code ajouté :

- `AuthenticateController` réécrit (connexion, rafraîchissement, déconnexion, limitation des tentatives).
- Services `RefreshTokenService`, `LoginAttemptService`, exception `InvalidRefreshTokenException`.
- Entités `RefreshToken` et `UserConsent`, dépôts associés, changelog `20261005000001_auth_tables.xml`.
- Inscription avec rôle et consentement (`ManagedUserVM`, `AccountResource`, `UserService`).
- Mot de passe : 8 caractères minimum (avant : 4).
- Jeton d'accès de 15 minutes (avant : 24 h), plus de jeton d'accès de 30 jours pour « se souvenir de moi ».

## Authentification

### Principe

- **Jeton d'accès** : JWT court (15 min), renvoyé dans le corps (`id_token`) et l'en-tête `Authorization`. Le frontend le garde en mémoire, pas dans `localStorage`.
- **Jeton de rafraîchissement** : valeur aléatoire de 256 bits, opaque, dans un cookie `refresh_token` (`HttpOnly`, `Secure`, `SameSite=Strict`, chemin `/api/authenticate`). Il n'est jamais dans le corps de la réponse.
- **Stockage** : seul le hash SHA-256 est en base (`refresh_token`). Une fuite de la base ne permet pas de rejouer un jeton.
- **Rotation** : chaque rafraîchissement révoque le jeton présenté et en émet un nouveau, dans la même « famille ».
- **Détection de rejeu** : présenter un jeton déjà révoqué révoque toute la famille (vol probable). L'utilisateur légitime doit se reconnecter.
- **CSRF** : le cookie est limité à `/api/authenticate` et `SameSite=Strict`, donc jamais envoyé depuis un autre site. Les autres routes utilisent l'en-tête `Authorization`, qu'un autre site ne peut pas forger.
- **Fin de session** : changement ou réinitialisation du mot de passe révoque tous les jetons de l'utilisateur. Un utilisateur désactivé ne peut plus rafraîchir. Les jetons expirés sont purgés chaque nuit à 3 h 30.

### Points d'entrée

| Méthode et chemin                | Accès           | Réponse                                                                                               |
| -------------------------------- | --------------- | ----------------------------------------------------------------------------------------------------- |
| `POST /api/authenticate`         | public          | 200 + `id_token` + cookie ; 401 si mauvais identifiants ; 429 + `Retry-After` si bloqué               |
| `POST /api/authenticate/refresh` | public (cookie) | 200 + nouveau `id_token` + nouveau cookie ; 401 + cookie effacé si absent, invalide, expiré ou rejoué |
| `POST /api/authenticate/logout`  | public (cookie) | 204, session révoquée, cookie effacé                                                                  |
| `GET /api/authenticate`          | public          | 204 si connecté, 401 sinon                                                                            |
| `POST /api/register`             | public          | 201 ; 400 si rôle absent ou interdit, ou consentement périmé                                          |
| `GET /api/consent/current`       | public          | `{ "version": "2026-10-v1" }` : version à afficher puis renvoyer dans `consentVersion`                |

Corps de `POST /api/register` (en plus des champs JHipster `login`, `email`, `password`, `firstName`, `lastName`, `langKey`) :

```json
{ "role": "CONTRIBUTOR", "consentVersion": "2026-10-v1" }
```

`role` vaut `CONTRIBUTOR`, `DEVELOPER` ou `ORGANIZATION`. `ROLE_ADMIN` ne peut jamais être demandé à l'inscription (validation du modèle et contrôle dans `UserService`).

### Limitation des tentatives de connexion

En mémoire (un seul serveur, pas de cache distribué). Deux compteurs :

- par couple (adresse IP, identifiant), 5 échecs : freine la devinette d'un mot de passe sans permettre à un tiers de verrouiller le compte d'un autre depuis une autre adresse ;
- par adresse IP, 30 échecs : freine le balayage de nombreux identifiants.

Au-delà du seuil, 15 minutes de refus, même avec le bon mot de passe. Une connexion réussie remet à zéro le compteur du couple, pas celui de l'adresse. Un redémarrage remet tout à zéro.

### Consentement

À l'inscription, la version acceptée doit être `application.consent.current-version`. Elle est enregistrée dans `user_consent` (une ligne par utilisateur et par version, avec la date), supprimée en cascade avec l'utilisateur.

### Attention côté frontend

- Faire **un seul rafraîchissement à la fois** (« single-flight »). Deux appels simultanés avec le même cookie déclenchent la détection de rejeu et coupent la session.
- Au démarrage de l'application, appeler `POST /api/authenticate/refresh` pour restaurer la session à partir du cookie.
- Rafraîchir quand le jeton d'accès expire ou sur une réponse 401.

## Rôles

| Rôle                | Attribué                          | Usage                                      |
| ------------------- | --------------------------------- | ------------------------------------------ |
| `ROLE_USER`         | à tous les comptes inscrits       | base                                       |
| `ROLE_CONTRIBUTOR`  | choix à l'inscription             | enregistrer, transcrire, valider           |
| `ROLE_DEVELOPER`    | choix à l'inscription             | datasets, modèles, clés d'API              |
| `ROLE_ORGANIZATION` | choix à l'inscription             | demandes de datasets sur mesure            |
| `ROLE_ADMIN`        | compte `admin` initial uniquement | étudier les demandes, ouvrir les campagnes |

Les rôles sont vérifiés côté serveur. Les routes `/api/admin/**` exigent `ROLE_ADMIN`.

## Dépendances et licences

Contrainte du règlement : licences permissives seulement (MIT, Apache 2.0, BSD). **L'inventaire complet n'est pas encore fait** ; il sera établi à la fin (décision du 5 octobre 2026). Mouvements de dépendances à y reporter :

| Action  | Dépendance                                                                                                                         | Licence    |
| ------- | ---------------------------------------------------------------------------------------------------------------------------------- | ---------- |
| Ajoutée | `spring-boot-starter-log4j2` (Log4j 2.25.5 : `log4j-core`, `log4j-api`, `log4j-slf4j2-impl`, `log4j-jul`)                          | Apache 2.0 |
| Retirée | `spring-boot-starter-logging` (Logback)                                                                                            | —          |
| Retirée | `spring-boot-starter-cache`, `spring-boot-starter-hazelcast`, `hazelcast-hibernate53`, `hazelcast-spring`, `javax.cache:cache-api` | —          |
| Retirée | `micrometer-registry-prometheus`                                                                                                   | —          |

Question toujours à poser par écrit à cadev@systalink.com : les bibliothèques non modifiées sous licence double ou à copyleft faible sont-elles acceptées ?

## Tests

- **Tests unitaires** (`./mvnw test`) : 33 passaient après le nettoyage (cache, Log4j2, monitoring).
- **Tests d'intégration** (`./mvnw verify`) : exigent Docker Desktop (Testcontainers PostgreSQL).
- **Non exécutés à ce jour** : tout ce qui a été écrit pour l'authentification. À lancer avant de commiter :
  - nouveaux tests unitaires `LoginAttemptServiceTest` et `RefreshTokenServiceTest` ;
  - nouveaux cas d'intégration dans `AuthenticateControllerIT` (cookie, rotation, rejeu, déconnexion, blocage) et `AccountResourceIT` (rôle, consentement) ;
  - `AccountResourceIT` existant, modifié pour fournir rôle et consentement ;
  - `AuthenticationIntegrationTest` (contexte de sécurité sans base), qui charge maintenant `AuthenticationTestBeans`.

Aucun résultat n'est affiché ici tant qu'ils n'ont pas tourné.

## Reste à faire

Dans l'ordre du planning (semaine 1, jusqu'au 11 octobre) :

1. Lancer les tests (`./mvnw verify` avec Docker) et corriger ce qui casse, puis commiter.
2. Frontend Angular (PWA, mobile d'abord, droite à gauche) : connexion, inscription.
3. Entités métier : langue, campagne, phrase, enregistrement ; envoi audio avec validation du type et de la taille.
4. Premier déploiement sur Datacloud : Docker Compose (API, PostgreSQL, nginx), HTTPS Let's Encrypt.
5. Fin du projet : inventaire des licences, schéma d'architecture, journal des décisions, déclaration de l'usage de l'IA, question licences à Systalink.
