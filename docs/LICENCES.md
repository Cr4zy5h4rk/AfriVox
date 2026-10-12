# Licences : politique, décisions et points à confirmer

Inventaire complet et à jour : [`LICENSES-THIRD-PARTY.md`](../LICENSES-THIRD-PARTY.md), **généré** par
`scripts/licenses/generate.mjs` (à relancer après tout ajout ou mise à jour de dépendance).

```bash
# Depuis la racine du dépôt AfriVox, JAVA_HOME défini (JDK 21) ; --online si le dépôt Maven local est incomplet
node scripts/licenses/generate.mjs [--frontend ../Afrivox_Frontend]
```

## Règle (règlement du concours)

Licences **permissives uniquement** : MIT, Apache 2.0, BSD. GPL, AGPL et LGPL rendent le projet irrecevable.
L'inventaire (Maven et npm) doit être tenu à jour. Les licences sont **lues dans les métadonnées** des dépendances,
pas vérifiées dans les textes : tout classement « inconnue » se contrôle à la main.

## Situation au 12 octobre 2026

| | Backend livré (Maven) | Frontend livré (npm) |
|---|---|---|
| Dépendances | 131 | 15 |
| Permissives | 127 | 15 |
| Copyleft faible (voir plus bas) | 4 | 0 |
| Copyleft fort (GPL, AGPL, LGPL) | **0** | 0 |
| Licence inconnue ou restrictive | **0** | 0 |

## Décisions prises

| Date | Décision | Raison |
|---|---|---|
| 5 oct. | Logback remplacé par **Log4j2** | Logback est en EPL/LGPL (exigé par le règlement) |
| 12 oct. | **Liquibase 5.0.3 → 4.33.0** (propriété `liquibase.version` du `pom.xml`) | La série 5.x est sous licence **FSL-1.1** (source disponible, non open source, non permissive). La série 4.x est sous Apache 2.0. Il faut au moins la 4.31 : `jhipster-framework` 9.3.0 appelle `setAnalyticsEnabled`, absent de la 4.29 (l'application ne démarrait pas). Démarrage réel validé en profil `prod` (52 changements appliqués) |
| 12 oct. | **`spring-boot-h2console` retiré** | Inutile avec PostgreSQL ; apportait H2 (MPL/EPL, copyleft faible) en `compile` |
| 12 oct. | Ne pas mettre les images Docker dans l'inventaire des dépendances | Ce sont des outils d'exécution, non distribués avec le code (voir « Outils d'exécution ») |

## Copyleft faible restant (à confirmer par écrit auprès de Systalink)

Quatre bibliothèques, **non modifiées**, en dépendance transitive de Spring Boot, Hibernate ou Liquibase :

| Bibliothèque | Licence déclarée | Rôle |
|---|---|---|
| `jakarta.annotation:jakarta.annotation-api` 3.0.0 | EPL-2.0 **ou** GPL-2.0 avec exception classpath | API standard Jakarta EE (annotations) |
| `jakarta.transaction:jakarta.transaction-api` 2.0.1 | EPL-2.0 **ou** GPL-2.0 avec exception classpath | API standard Jakarta EE (transactions) |
| `org.aspectj:aspectjweaver` 1.9.25.1 | EPL-2.0 | Aspects Spring (journalisation des appels de services) |
| `javax.xml.bind:jaxb-api` 2.3.1 | CDDL-1.1 **ou** GPL-2.0 avec exception classpath | Dépendance transitive de Liquibase 4.x |

Ces licences sont à double choix (on retient EPL-2.0 ou CDDL, pas la GPL) ; elles n'imposent rien au code de
l'application tant que les bibliothèques ne sont pas modifiées. Ce sont des briques que presque tout projet Spring
embarque : les retirer imposerait de retirer Spring Boot ou Hibernate.

**Question à envoyer à cadev@systalink.com** (déjà prévue dans les notes du projet, non envoyée à ce jour) :

> Bonjour,
>
> Notre projet (AfriVox, équipe …) n'utilise que des bibliothèques sous licences permissives (MIT, Apache 2.0, BSD),
> à l'exception de quatre bibliothèques transitives, **non modifiées**, sous licence à copyleft faible ou double
> licence : `jakarta.annotation-api` et `jakarta.transaction-api` (EPL-2.0 ou GPL-2.0 avec exception classpath),
> `aspectjweaver` (EPL-2.0) et `jaxb-api` (CDDL-1.1 ou GPL-2.0 avec exception classpath). Elles sont requises par
> Spring Boot, Hibernate et Liquibase.
>
> Ces bibliothèques non modifiées, sous licence à copyleft faible ou à double licence, sont-elles acceptées par le
> règlement ? Par ailleurs, l'environnement d'exécution Java (OpenJDK, GPL-2.0 avec exception classpath) et le
> système d'exploitation du serveur sont-ils bien hors du périmètre de cette règle ?
>
> Cordialement,

## Outils d'exécution (non distribués avec le code)

Utilisés pour construire et exécuter l'application sur le serveur, jamais redistribués dans le dépôt ni dans le
jar : `postgres:18.6` (licence PostgreSQL), `eclipse-temurin:21` (OpenJDK, GPL-2.0 avec exception classpath),
`nginx:1.27-alpine` (BSD-2-Clause), `node:22-alpine` (MIT, build seulement), `certbot/certbot` (Apache-2.0),
`alpine:3`, `httpd:2-alpine` (Apache-2.0, génération du hash d'administrateur seulement).

## Points d'attention

- **Données de test** : la source `kallama` (agriculture, wolof) et les données sereer ont des licences **inconnues**
  ou à vérifier ; elles ne sont jamais dans le dépôt ni dans un dataset publié.
- **Polices et icônes** du frontend : aucune police distante (pile système `system-ui`, `Segoe UI`, `Roboto`) et
  aucune icône Material (`mat-icon` n'est pas utilisé). Le favicon et les **icônes de la PWA** étaient ceux
  d'Angular ; ils sont remplacés (12 oct. 2026) par le logo AfriVox, généré par
  `Afrivox_Frontend/scripts/generate-brand-icons.mjs` (aucune ressource externe).
- **Texte des licences d'achat** (`DatasetPackagingService.licenseText`) : à faire relire, c'est un texte juridique
  du produit et non une licence de dépendance.
