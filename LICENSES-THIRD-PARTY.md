# Licences des dependances tierces

Genere par `scripts/licenses/generate.mjs` le 2026-10-12. **Ne pas editer a la main** : relancer le script apres tout ajout ou mise a jour de dependance.

Regle du reglement : licences permissives uniquement (MIT, Apache 2.0, BSD). GPL, AGPL et LGPL rendent le projet irrecevable ; le copyleft faible (EPL, MPL, CDDL) fait l'objet d'une question ecrite a cadev@systalink.com.

Les licences sont **lues** dans les metadonnees (poms Maven, `package-lock.json`, `node_modules`), pas verifiees dans les textes. Une licence « inconnue » doit etre controlee a la main avant le rendu.

## Resume

- **Backend livre** (Maven, portees compile et runtime, profil `prod`) : 131 dependances — permissive : 127 ; copyleft faible : 4
- **Frontend livre** (npm, hors outillage de build) : 15 paquets — permissive : 15
- Outillage de build et de test (non livre) : Maven 59 ; npm frontend 401 ; npm backend 523

## A examiner (livre, hors licence permissive)

| Ecosysteme    | Dependance                                  | Version  | Portee  | Licence                                | Classement      | Remarque |
| ------------- | ------------------------------------------- | -------- | ------- | -------------------------------------- | --------------- | -------- |
| Maven (livre) | jakarta.annotation:jakarta.annotation-api   | 3.0.0    | compile | EPL-2.0 / GPL avec exception classpath | copyleft faible |          |
| Maven (livre) | jakarta.transaction:jakarta.transaction-api | 2.0.1    | compile | EPL-2.0 / GPL avec exception classpath | copyleft faible |          |
| Maven (livre) | javax.xml.bind:jaxb-api                     | 2.3.1    | compile | CDDL / GPL avec exception classpath    | copyleft faible |          |
| Maven (livre) | org.aspectj:aspectjweaver                   | 1.9.25.1 | compile | EPL-2.0                                | copyleft faible |          |

## Backend livre (Maven, compile et runtime)

| Dependance                                                           | Version       | Portee  | Licence                                                     | Classement      | Remarque |
| -------------------------------------------------------------------- | ------------- | ------- | ----------------------------------------------------------- | --------------- | -------- |
| jakarta.annotation:jakarta.annotation-api                            | 3.0.0         | compile | EPL-2.0 / GPL avec exception classpath                      | copyleft faible |          |
| jakarta.transaction:jakarta.transaction-api                          | 2.0.1         | compile | EPL-2.0 / GPL avec exception classpath                      | copyleft faible |          |
| javax.xml.bind:jaxb-api                                              | 2.3.1         | compile | CDDL / GPL avec exception classpath                         | copyleft faible |          |
| org.aspectj:aspectjweaver                                            | 1.9.25.1      | compile | EPL-2.0                                                     | copyleft faible |          |
| com.fasterxml:classmate                                              | 1.7.3         | compile | Apache-2.0                                                  | permissive      |          |
| com.fasterxml.jackson.core:jackson-annotations                       | 2.21          | compile | Apache-2.0                                                  | permissive      |          |
| com.fasterxml.jackson.core:jackson-core                              | 2.21.5        | compile | Apache-2.0                                                  | permissive      |          |
| com.fasterxml.jackson.core:jackson-databind                          | 2.21.5        | compile | Apache-2.0                                                  | permissive      |          |
| com.fasterxml.jackson.dataformat:jackson-dataformat-yaml             | 2.21.5        | compile | Apache-2.0                                                  | permissive      |          |
| com.fasterxml.jackson.datatype:jackson-datatype-jsr310               | 2.21.5        | compile | Apache-2.0                                                  | permissive      |          |
| com.nimbusds:nimbus-jose-jwt                                         | 10.9.1        | compile | Apache-2.0                                                  | permissive      |          |
| com.opencsv:opencsv                                                  | 5.11.2        | compile | Apache-2.0                                                  | permissive      |          |
| com.zaxxer:HikariCP                                                  | 7.0.2         | compile | Apache-2.0                                                  | permissive      |          |
| commons-io:commons-io                                                | 2.19.0        | compile | Apache-2.0                                                  | permissive      |          |
| commons-logging:commons-logging                                      | 1.3.6         | compile | Apache-2.0                                                  | permissive      |          |
| io.micrometer:micrometer-commons                                     | 1.17.1        | compile | Apache-2.0                                                  | permissive      |          |
| io.micrometer:micrometer-core                                        | 1.17.1        | compile | Apache-2.0                                                  | permissive      |          |
| io.micrometer:micrometer-jakarta9                                    | 1.17.1        | compile | Apache-2.0                                                  | permissive      |          |
| io.micrometer:micrometer-observation                                 | 1.17.1        | compile | Apache-2.0                                                  | permissive      |          |
| io.swagger.core.v3:swagger-annotations-jakarta                       | 2.2.52        | compile | Apache-2.0                                                  | permissive      |          |
| io.swagger.core.v3:swagger-core-jakarta                              | 2.2.52        | compile | Apache-2.0                                                  | permissive      |          |
| io.swagger.core.v3:swagger-models-jakarta                            | 2.2.52        | compile | Apache-2.0                                                  | permissive      |          |
| jakarta.activation:jakarta.activation-api                            | 2.1.4         | compile | BSD-3-Clause (EDL)                                          | permissive      |          |
| jakarta.inject:jakarta.inject-api                                    | 2.0.1         | runtime | Apache-2.0                                                  | permissive      |          |
| jakarta.mail:jakarta.mail-api                                        | 2.1.5         | compile | EPL-2.0 / GPL avec exception classpath / BSD-3-Clause (EDL) | permissive      |          |
| jakarta.persistence:jakarta.persistence-api                          | 3.2.0         | compile | EPL-2.0 / BSD-3-Clause (EDL)                                | permissive      |          |
| jakarta.validation:jakarta.validation-api                            | 3.1.1         | compile | Apache-2.0                                                  | permissive      |          |
| jakarta.xml.bind:jakarta.xml.bind-api                                | 4.0.5         | compile | BSD-3-Clause (EDL)                                          | permissive      |          |
| net.bytebuddy:byte-buddy                                             | 1.18.11       | runtime | Apache-2.0                                                  | permissive      |          |
| org.antlr:antlr4-runtime                                             | 4.13.2        | compile | BSD-3-Clause                                                | permissive      |          |
| org.apache.commons:commons-collections4                              | 4.5.0         | compile | Apache-2.0                                                  | permissive      |          |
| org.apache.commons:commons-lang3                                     | 3.20.0        | compile | Apache-2.0                                                  | permissive      |          |
| org.apache.commons:commons-text                                      | 1.13.1        | compile | Apache-2.0                                                  | permissive      |          |
| org.apache.logging.log4j:log4j-api                                   | 2.25.5        | compile | Apache-2.0                                                  | permissive      |          |
| org.apache.logging.log4j:log4j-core                                  | 2.25.5        | compile | Apache-2.0                                                  | permissive      |          |
| org.apache.logging.log4j:log4j-jul                                   | 2.25.5        | compile | Apache-2.0                                                  | permissive      |          |
| org.apache.logging.log4j:log4j-slf4j2-impl                           | 2.25.5        | compile | Apache-2.0                                                  | permissive      |          |
| org.apache.tomcat.embed:tomcat-embed-core                            | 11.0.24       | compile | Apache-2.0                                                  | permissive      |          |
| org.apache.tomcat.embed:tomcat-embed-el                              | 11.0.24       | compile | Apache-2.0                                                  | permissive      |          |
| org.apache.tomcat.embed:tomcat-embed-websocket                       | 11.0.24       | compile | Apache-2.0                                                  | permissive      |          |
| org.attoparser:attoparser                                            | 2.0.7.RELEASE | compile | Apache-2.0                                                  | permissive      |          |
| org.checkerframework:checker-qual                                    | 3.55.1        | runtime | MIT                                                         | permissive      |          |
| org.eclipse.angus:angus-activation                                   | 2.0.3         | runtime | BSD-3-Clause (EDL)                                          | permissive      |          |
| org.eclipse.angus:angus-mail                                         | 2.0.5         | runtime | EPL-2.0 / GPL avec exception classpath / BSD-3-Clause (EDL) | permissive      |          |
| org.hdrhistogram:HdrHistogram                                        | 2.2.2         | runtime | Public Domain, per Creative Commons CC0 / BSD-2-Clause      | permissive      |          |
| org.hibernate.models:hibernate-models                                | 1.1.1         | runtime | Apache-2.0                                                  | permissive      |          |
| org.hibernate.orm:hibernate-core                                     | 7.4.5.Final   | compile | Apache-2.0                                                  | permissive      |          |
| org.hibernate.validator:hibernate-validator                          | 9.1.3.Final   | compile | Apache-2.0                                                  | permissive      |          |
| org.jboss.logging:jboss-logging                                      | 3.6.3.Final   | compile | Apache-2.0                                                  | permissive      |          |
| org.jspecify:jspecify                                                | 1.0.1         | compile | Apache-2.0                                                  | permissive      |          |
| org.liquibase:liquibase-core                                         | 4.33.0        | compile | Apache-2.0                                                  | permissive      |          |
| org.mapstruct:mapstruct                                              | 1.6.3         | compile | Apache-2.0                                                  | permissive      |          |
| org.postgresql:postgresql                                            | 42.7.13       | compile | BSD-2-Clause                                                | permissive      |          |
| org.slf4j:slf4j-api                                                  | 2.0.18        | compile | MIT                                                         | permissive      |          |
| org.springdoc:springdoc-openapi-starter-common                       | 3.1.0         | compile | Apache-2.0                                                  | permissive      |          |
| org.springdoc:springdoc-openapi-starter-webmvc-api                   | 3.1.0         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework:spring-aop                                       | 7.0.9         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework:spring-aspects                                   | 7.0.9         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework:spring-beans                                     | 7.0.9         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework:spring-context                                   | 7.0.9         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework:spring-context-support                           | 7.0.9         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework:spring-core                                      | 7.0.9         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework:spring-expression                                | 7.0.9         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework:spring-jdbc                                      | 7.0.9         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework:spring-orm                                       | 7.0.9         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework:spring-tx                                        | 7.0.9         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework:spring-web                                       | 7.0.9         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework:spring-webmvc                                    | 7.0.9         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework.boot:spring-boot                                 | 4.1.1         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework.boot:spring-boot-actuator                        | 4.1.1         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework.boot:spring-boot-actuator-autoconfigure          | 4.1.1         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework.boot:spring-boot-autoconfigure                   | 4.1.1         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework.boot:spring-boot-data-commons                    | 4.1.1         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework.boot:spring-boot-data-jpa                        | 4.1.1         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework.boot:spring-boot-health                          | 4.1.1         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework.boot:spring-boot-hibernate                       | 4.1.1         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework.boot:spring-boot-http-converter                  | 4.1.1         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework.boot:spring-boot-jackson                         | 4.1.1         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework.boot:spring-boot-jdbc                            | 4.1.1         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework.boot:spring-boot-jpa                             | 4.1.1         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework.boot:spring-boot-liquibase                       | 4.1.1         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework.boot:spring-boot-mail                            | 4.1.1         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework.boot:spring-boot-micrometer-metrics              | 4.1.1         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework.boot:spring-boot-micrometer-observation          | 4.1.1         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework.boot:spring-boot-persistence                     | 4.1.1         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework.boot:spring-boot-security                        | 4.1.1         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework.boot:spring-boot-security-oauth2-resource-server | 4.1.1         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework.boot:spring-boot-servlet                         | 4.1.1         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework.boot:spring-boot-sql                             | 4.1.1         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework.boot:spring-boot-starter                         | 4.1.1         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework.boot:spring-boot-starter-actuator                | 4.1.1         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework.boot:spring-boot-starter-aspectj                 | 4.1.1         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework.boot:spring-boot-starter-data-jpa                | 4.1.1         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework.boot:spring-boot-starter-jackson                 | 4.1.1         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework.boot:spring-boot-starter-jdbc                    | 4.1.1         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework.boot:spring-boot-starter-liquibase               | 4.1.1         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework.boot:spring-boot-starter-log4j2                  | 4.1.1         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework.boot:spring-boot-starter-mail                    | 4.1.1         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework.boot:spring-boot-starter-micrometer-metrics      | 4.1.1         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework.boot:spring-boot-starter-oauth2-resource-server  | 4.1.1         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework.boot:spring-boot-starter-security                | 4.1.1         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework.boot:spring-boot-starter-thymeleaf               | 4.1.1         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework.boot:spring-boot-starter-tomcat                  | 4.1.1         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework.boot:spring-boot-starter-tomcat-runtime          | 4.1.1         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework.boot:spring-boot-starter-validation              | 4.1.1         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework.boot:spring-boot-starter-web                     | 4.1.1         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework.boot:spring-boot-thymeleaf                       | 4.1.1         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework.boot:spring-boot-tomcat                          | 4.1.1         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework.boot:spring-boot-transaction                     | 4.1.1         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework.boot:spring-boot-validation                      | 4.1.1         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework.boot:spring-boot-web-server                      | 4.1.1         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework.boot:spring-boot-webmvc                          | 4.1.1         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework.data:spring-data-commons                         | 4.1.1         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework.data:spring-data-jpa                             | 4.1.1         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework.security:spring-security-config                  | 7.1.1         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework.security:spring-security-core                    | 7.1.1         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework.security:spring-security-crypto                  | 7.1.1         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework.security:spring-security-data                    | 7.1.1         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework.security:spring-security-oauth2-core             | 7.1.1         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework.security:spring-security-oauth2-jose             | 7.1.1         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework.security:spring-security-oauth2-resource-server  | 7.1.1         | compile | Apache-2.0                                                  | permissive      |          |
| org.springframework.security:spring-security-web                     | 7.1.1         | compile | Apache-2.0                                                  | permissive      |          |
| org.thymeleaf:thymeleaf                                              | 3.1.5.RELEASE | compile | Apache-2.0                                                  | permissive      |          |
| org.thymeleaf:thymeleaf-spring6                                      | 3.1.5.RELEASE | compile | Apache-2.0                                                  | permissive      |          |
| org.unbescape:unbescape                                              | 1.1.6.RELEASE | compile | Apache-2.0                                                  | permissive      |          |
| org.yaml:snakeyaml                                                   | 2.6           | compile | Apache-2.0                                                  | permissive      |          |
| tech.jhipster:jhipster-framework                                     | 9.3.0         | compile | Apache-2.0                                                  | permissive      |          |
| tools.jackson.core:jackson-core                                      | 3.1.5         | compile | Apache-2.0                                                  | permissive      |          |
| tools.jackson.core:jackson-databind                                  | 3.1.5         | compile | Apache-2.0                                                  | permissive      |          |
| tools.jackson.datatype:jackson-datatype-hibernate7                   | 3.1.5         | compile | Apache-2.0                                                  | permissive      |          |
| tools.jackson.module:jackson-module-jaxb-annotations                 | 3.1.5         | compile | Apache-2.0                                                  | permissive      |          |

## Frontend livre (npm)

| Paquet                    | Version | Portee     | Licence      | Classement |
| ------------------------- | ------- | ---------- | ------------ | ---------- |
| @angular/cdk              | 22.2.1  | production | MIT          | permissive |
| @angular/common           | 22.2.1  | production | MIT          | permissive |
| @angular/compiler         | 22.2.1  | production | MIT          | permissive |
| @angular/core             | 22.2.1  | production | MIT          | permissive |
| @angular/forms            | 22.2.1  | production | MIT          | permissive |
| @angular/material         | 22.2.1  | production | MIT          | permissive |
| @angular/platform-browser | 22.2.1  | production | MIT          | permissive |
| @angular/router           | 22.2.1  | production | MIT          | permissive |
| @angular/service-worker   | 22.2.1  | production | MIT          | permissive |
| @standard-schema/spec     | 1.1.0   | production | MIT          | permissive |
| entities                  | 8.1.0   | production | BSD-2-Clause | permissive |
| parse5                    | 8.0.1   | production | MIT          | permissive |
| rxjs                      | 7.8.2   | production | Apache-2.0   | permissive |
| tslib                     | 2.8.1   | production | 0BSD         | permissive |
| zod                       | 4.6.5   | production | MIT          | permissive |

## Outillage de build et de test (non livre)

Ces composants servent a compiler, tester et formater ; ils ne sont pas inclus dans le jar ni dans les fichiers servis. Les plugins Maven (checkstyle, modernizer, etc.) ne sont pas listes : ils ne sont pas distribues.

### Maven (portees test et provided)

| Dependance                                                   | Version              | Portee   | Licence                   | Classement      | Remarque |
| ------------------------------------------------------------ | -------------------- | -------- | ------------------------- | --------------- | -------- |
| org.junit.jupiter:junit-jupiter                              | 6.0.3                | test     | EPL-2.0                   | copyleft faible |          |
| org.junit.jupiter:junit-jupiter-api                          | 6.0.3                | test     | EPL-2.0                   | copyleft faible |          |
| org.junit.jupiter:junit-jupiter-engine                       | 6.0.3                | test     | EPL-2.0                   | copyleft faible |          |
| org.junit.jupiter:junit-jupiter-params                       | 6.0.3                | test     | EPL-2.0                   | copyleft faible |          |
| org.junit.platform:junit-platform-commons                    | 6.0.3                | test     | EPL-2.0                   | copyleft faible |          |
| org.junit.platform:junit-platform-engine                     | 6.0.3                | test     | EPL-2.0                   | copyleft faible |          |
| com.github.docker-java:docker-java-api                       | 3.7.1                | test     | Apache-2.0                | permissive      |          |
| com.github.docker-java:docker-java-transport                 | 3.7.1                | test     | Apache-2.0                | permissive      |          |
| com.github.docker-java:docker-java-transport-zerodep         | 3.7.1                | test     | Apache-2.0                | permissive      |          |
| com.jayway.jsonpath:json-path                                | 2.10.0               | test     | Apache-2.0                | permissive      |          |
| com.sun.istack:istack-commons-runtime                        | 4.1.2                | provided | BSD-3-Clause (EDL)        | permissive      |          |
| com.tngtech.archunit:archunit                                | 1.5.0                | test     | Apache-2.0 / BSD-3-Clause | permissive      |          |
| com.tngtech.archunit:archunit-junit5-api                     | 1.5.0                | test     | Apache-2.0                | permissive      |          |
| com.tngtech.archunit:archunit-junit5-engine                  | 1.5.0                | test     | Apache-2.0                | permissive      |          |
| com.tngtech.archunit:archunit-junit5-engine-api              | 1.5.0                | test     | Apache-2.0                | permissive      |          |
| com.vaadin.external.google:android-json                      | 0.0.20131108.vaadin1 | test     | Apache-2.0                | permissive      |          |
| commons-codec:commons-codec                                  | 1.21.0               | test     | Apache-2.0                | permissive      |          |
| io.smallrye:jandex                                           | 3.3.2                | provided | Apache-2.0                | permissive      |          |
| net.bytebuddy:byte-buddy-agent                               | 1.18.11              | test     | Apache-2.0                | permissive      |          |
| net.java.dev.jna:jna                                         | 5.18.1               | test     | LGPL / Apache-2.0         | permissive      |          |
| net.minidev:accessors-smart                                  | 2.6.0                | test     | Apache-2.0                | permissive      |          |
| net.minidev:json-smart                                       | 2.6.0                | test     | Apache-2.0                | permissive      |          |
| org.apache.commons:commons-compress                          | 1.28.0               | test     | Apache-2.0                | permissive      |          |
| org.apiguardian:apiguardian-api                              | 1.1.2                | test     | Apache-2.0                | permissive      |          |
| org.assertj:assertj-core                                     | 3.27.7               | test     | Apache-2.0                | permissive      |          |
| org.awaitility:awaitility                                    | 4.3.0                | test     | Apache-2.0                | permissive      |          |
| org.glassfish.jaxb:jaxb-core                                 | 4.0.9                | provided | BSD-3-Clause (EDL)        | permissive      |          |
| org.glassfish.jaxb:jaxb-runtime                              | 4.0.9                | provided | BSD-3-Clause (EDL)        | permissive      |          |
| org.glassfish.jaxb:txw2                                      | 4.0.9                | provided | BSD-3-Clause (EDL)        | permissive      |          |
| org.hamcrest:hamcrest                                        | 3.0                  | test     | BSD-3-Clause              | permissive      |          |
| org.hibernate.orm:hibernate-processor                        | 7.4.5.Final          | provided | Apache-2.0                | permissive      |          |
| org.jetbrains:annotations                                    | 17.0.0               | test     | Apache-2.0                | permissive      |          |
| org.mockito:mockito-core                                     | 5.23.0               | test     | MIT                       | permissive      |          |
| org.mockito:mockito-junit-jupiter                            | 5.23.0               | test     | MIT                       | permissive      |          |
| org.objenesis:objenesis                                      | 3.3                  | test     | Apache-2.0                | permissive      |          |
| org.opentest4j:opentest4j                                    | 1.3.0                | test     | Apache-2.0                | permissive      |          |
| org.ow2.asm:asm                                              | 9.7.1                | test     | BSD-3-Clause              | permissive      |          |
| org.rnorth.duct-tape:duct-tape                               | 1.0.8                | test     | MIT                       | permissive      |          |
| org.skyscreamer:jsonassert                                   | 1.5.3                | test     | Apache-2.0                | permissive      |          |
| org.springframework:spring-test                              | 7.0.9                | test     | Apache-2.0                | permissive      |          |
| org.springframework.boot:spring-boot-configuration-processor | 4.1.1                | provided | Apache-2.0                | permissive      |          |
| org.springframework.boot:spring-boot-resttestclient          | 4.1.1                | test     | Apache-2.0                | permissive      |          |
| org.springframework.boot:spring-boot-security-test           | 4.1.1                | test     | Apache-2.0                | permissive      |          |
| org.springframework.boot:spring-boot-starter-jackson-test    | 4.1.1                | test     | Apache-2.0                | permissive      |          |
| org.springframework.boot:spring-boot-starter-security-test   | 4.1.1                | test     | Apache-2.0                | permissive      |          |
| org.springframework.boot:spring-boot-starter-test            | 4.1.1                | test     | Apache-2.0                | permissive      |          |
| org.springframework.boot:spring-boot-starter-webmvc          | 4.1.1                | test     | Apache-2.0                | permissive      |          |
| org.springframework.boot:spring-boot-starter-webmvc-test     | 4.1.1                | test     | Apache-2.0                | permissive      |          |
| org.springframework.boot:spring-boot-test                    | 4.1.1                | test     | Apache-2.0                | permissive      |          |
| org.springframework.boot:spring-boot-test-autoconfigure      | 4.1.1                | test     | Apache-2.0                | permissive      |          |
| org.springframework.boot:spring-boot-testcontainers          | 4.1.1                | test     | Apache-2.0                | permissive      |          |
| org.springframework.boot:spring-boot-webmvc-test             | 4.1.1                | test     | Apache-2.0                | permissive      |          |
| org.springframework.security:spring-security-test            | 7.1.1                | test     | Apache-2.0                | permissive      |          |
| org.testcontainers:testcontainers                            | 2.0.5                | test     | MIT                       | permissive      |          |
| org.testcontainers:testcontainers-database-commons           | 2.0.5                | test     | MIT                       | permissive      |          |
| org.testcontainers:testcontainers-jdbc                       | 2.0.5                | test     | MIT                       | permissive      |          |
| org.testcontainers:testcontainers-junit-jupiter              | 2.0.5                | test     | MIT                       | permissive      |          |
| org.testcontainers:testcontainers-postgresql                 | 2.0.5                | test     | MIT                       | permissive      |          |
| org.xmlunit:xmlunit-core                                     | 2.11.0               | test     | Apache-2.0                | permissive      |          |

### npm frontend (devDependencies)

| Paquet                                          | Version      | Portee | Licence                       | Classement      |
| ----------------------------------------------- | ------------ | ------ | ----------------------------- | --------------- |
| lru-cache                                       | 11.5.3       | dev    | BlueOak-1.0.0                 | inconnue        |
| lightningcss                                    | 1.33.0       | dev    | MPL-2.0                       | copyleft faible |
| lightningcss-android-arm64                      | 1.33.0       | dev    | MPL-2.0                       | copyleft faible |
| lightningcss-darwin-arm64                       | 1.33.0       | dev    | MPL-2.0                       | copyleft faible |
| lightningcss-darwin-x64                         | 1.33.0       | dev    | MPL-2.0                       | copyleft faible |
| lightningcss-freebsd-x64                        | 1.33.0       | dev    | MPL-2.0                       | copyleft faible |
| lightningcss-linux-arm-gnueabihf                | 1.33.0       | dev    | MPL-2.0                       | copyleft faible |
| lightningcss-linux-arm64-gnu                    | 1.33.0       | dev    | MPL-2.0                       | copyleft faible |
| lightningcss-linux-arm64-musl                   | 1.33.0       | dev    | MPL-2.0                       | copyleft faible |
| lightningcss-linux-x64-gnu                      | 1.33.0       | dev    | MPL-2.0                       | copyleft faible |
| lightningcss-linux-x64-musl                     | 1.33.0       | dev    | MPL-2.0                       | copyleft faible |
| lightningcss-win32-arm64-msvc                   | 1.33.0       | dev    | MPL-2.0                       | copyleft faible |
| lightningcss-win32-x64-msvc                     | 1.33.0       | dev    | MPL-2.0                       | copyleft faible |
| @ampproject/remapping                           | 2.3.0        | dev    | Apache-2.0                    | permissive      |
| @angular-devkit/architect                       | 0.2202.1     | dev    | MIT                           | permissive      |
| @angular-devkit/core                            | 22.2.1       | dev    | MIT                           | permissive      |
| @angular-devkit/schematics                      | 22.2.1       | dev    | MIT                           | permissive      |
| @angular/build                                  | 22.2.1       | dev    | MIT                           | permissive      |
| @angular/cli                                    | 22.2.1       | dev    | MIT                           | permissive      |
| @angular/compiler-cli                           | 22.2.1       | dev    | MIT                           | permissive      |
| @asamuzakjp/css-color                           | 7.1.3        | dev    | MIT                           | permissive      |
| @asamuzakjp/dom-selector                        | 9.2.4        | dev    | MIT                           | permissive      |
| @babel/code-frame                               | 8.0.6        | dev    | MIT                           | permissive      |
| @babel/compat-data                              | 8.0.5        | dev    | MIT                           | permissive      |
| @babel/core                                     | 8.0.1        | dev    | MIT                           | permissive      |
| @babel/core                                     | 8.0.5        | dev    | MIT                           | permissive      |
| @babel/generator                                | 8.0.6        | dev    | MIT                           | permissive      |
| @babel/helper-compilation-targets               | 8.0.6        | dev    | MIT                           | permissive      |
| @babel/helper-globals                           | 8.0.6        | dev    | MIT                           | permissive      |
| @babel/helper-string-parser                     | 8.0.6        | dev    | MIT                           | permissive      |
| @babel/helper-validator-identifier              | 8.0.6        | dev    | MIT                           | permissive      |
| @babel/helper-validator-option                  | 8.0.0        | dev    | MIT                           | permissive      |
| @babel/helpers                                  | 8.0.5        | dev    | MIT                           | permissive      |
| @babel/parser                                   | 8.0.6        | dev    | MIT                           | permissive      |
| @babel/template                                 | 8.0.0        | dev    | MIT                           | permissive      |
| @babel/traverse                                 | 8.0.6        | dev    | MIT                           | permissive      |
| @babel/types                                    | 8.0.6        | dev    | MIT                           | permissive      |
| @bramus/specificity                             | 2.4.2        | dev    | MIT                           | permissive      |
| @bufbuild/protobuf                              | 2.16.0       | dev    | (Apache-2.0 AND BSD-3-Clause) | permissive      |
| @csstools/color-helpers                         | 6.1.2        | dev    | MIT-0                         | permissive      |
| @csstools/css-calc                              | 3.4.3        | dev    | MIT                           | permissive      |
| @csstools/css-color-parser                      | 4.2.6        | dev    | MIT                           | permissive      |
| @csstools/css-parser-algorithms                 | 4.0.2        | dev    | MIT                           | permissive      |
| @csstools/css-syntax-patches-for-csstree        | 1.1.15       | dev    | MIT-0                         | permissive      |
| @csstools/css-tokenizer                         | 4.0.2        | dev    | MIT                           | permissive      |
| @esbuild/aix-ppc64                              | 0.28.2       | dev    | MIT                           | permissive      |
| @esbuild/android-arm                            | 0.28.2       | dev    | MIT                           | permissive      |
| @esbuild/android-arm64                          | 0.28.2       | dev    | MIT                           | permissive      |
| @esbuild/android-x64                            | 0.28.2       | dev    | MIT                           | permissive      |
| @esbuild/darwin-arm64                           | 0.28.2       | dev    | MIT                           | permissive      |
| @esbuild/darwin-x64                             | 0.28.2       | dev    | MIT                           | permissive      |
| @esbuild/freebsd-arm64                          | 0.28.2       | dev    | MIT                           | permissive      |
| @esbuild/freebsd-x64                            | 0.28.2       | dev    | MIT                           | permissive      |
| @esbuild/linux-arm                              | 0.28.2       | dev    | MIT                           | permissive      |
| @esbuild/linux-arm64                            | 0.28.2       | dev    | MIT                           | permissive      |
| @esbuild/linux-ia32                             | 0.28.2       | dev    | MIT                           | permissive      |
| @esbuild/linux-loong64                          | 0.28.2       | dev    | MIT                           | permissive      |
| @esbuild/linux-mips64el                         | 0.28.2       | dev    | MIT                           | permissive      |
| @esbuild/linux-ppc64                            | 0.28.2       | dev    | MIT                           | permissive      |
| @esbuild/linux-riscv64                          | 0.28.2       | dev    | MIT                           | permissive      |
| @esbuild/linux-s390x                            | 0.28.2       | dev    | MIT                           | permissive      |
| @esbuild/linux-x64                              | 0.28.2       | dev    | MIT                           | permissive      |
| @esbuild/netbsd-arm64                           | 0.28.2       | dev    | MIT                           | permissive      |
| @esbuild/netbsd-x64                             | 0.28.2       | dev    | MIT                           | permissive      |
| @esbuild/openbsd-arm64                          | 0.28.2       | dev    | MIT                           | permissive      |
| @esbuild/openbsd-x64                            | 0.28.2       | dev    | MIT                           | permissive      |
| @esbuild/openharmony-arm64                      | 0.28.2       | dev    | MIT                           | permissive      |
| @esbuild/sunos-x64                              | 0.28.2       | dev    | MIT                           | permissive      |
| @esbuild/win32-arm64                            | 0.28.2       | dev    | MIT                           | permissive      |
| @esbuild/win32-ia32                             | 0.28.2       | dev    | MIT                           | permissive      |
| @esbuild/win32-x64                              | 0.28.2       | dev    | MIT                           | permissive      |
| @exodus/bytes                                   | 1.16.0       | dev    | MIT                           | permissive      |
| @harperfast/extended-iterable                   | 1.0.3        | dev    | Apache-2.0                    | permissive      |
| @inquirer/ansi                                  | 2.0.9        | dev    | MIT                           | permissive      |
| @inquirer/checkbox                              | 5.2.6        | dev    | MIT                           | permissive      |
| @inquirer/confirm                               | 6.3.2        | dev    | MIT                           | permissive      |
| @inquirer/core                                  | 12.0.4       | dev    | MIT                           | permissive      |
| @inquirer/editor                                | 5.3.4        | dev    | MIT                           | permissive      |
| @inquirer/expand                                | 5.1.6        | dev    | MIT                           | permissive      |
| @inquirer/external-editor                       | 3.0.6        | dev    | MIT                           | permissive      |
| @inquirer/figures                               | 2.0.9        | dev    | MIT                           | permissive      |
| @inquirer/input                                 | 5.1.7        | dev    | MIT                           | permissive      |
| @inquirer/number                                | 4.2.4        | dev    | MIT                           | permissive      |
| @inquirer/password                              | 5.2.3        | dev    | MIT                           | permissive      |
| @inquirer/prompts                               | 8.7.2        | dev    | MIT                           | permissive      |
| @inquirer/rawlist                               | 5.3.6        | dev    | MIT                           | permissive      |
| @inquirer/search                                | 4.3.4        | dev    | MIT                           | permissive      |
| @inquirer/select                                | 5.2.6        | dev    | MIT                           | permissive      |
| @inquirer/type                                  | 4.2.0        | dev    | MIT                           | permissive      |
| @inquirer/type                                  | 4.2.0        | dev    | MIT                           | permissive      |
| @inquirer/type                                  | 4.2.0        | dev    | MIT                           | permissive      |
| @inquirer/type                                  | 4.2.0        | dev    | MIT                           | permissive      |
| @inquirer/type                                  | 4.2.0        | dev    | MIT                           | permissive      |
| @inquirer/type                                  | 4.2.0        | dev    | MIT                           | permissive      |
| @inquirer/type                                  | 4.2.0        | dev    | MIT                           | permissive      |
| @inquirer/type                                  | 4.2.0        | dev    | MIT                           | permissive      |
| @inquirer/type                                  | 4.2.0        | dev    | MIT                           | permissive      |
| @inquirer/type                                  | 4.2.0        | dev    | MIT                           | permissive      |
| @inquirer/type                                  | 4.1.1        | dev    | MIT                           | permissive      |
| @jridgewell/gen-mapping                         | 0.4.0-beta.0 | dev    | MIT                           | permissive      |
| @jridgewell/gen-mapping                         | 0.3.13       | dev    | MIT                           | permissive      |
| @jridgewell/resolve-uri                         | 3.1.2        | dev    | MIT                           | permissive      |
| @jridgewell/sourcemap-codec                     | 1.6.0        | dev    | MIT                           | permissive      |
| @jridgewell/trace-mapping                       | 0.3.31       | dev    | MIT                           | permissive      |
| @listr2/prompt-adapter-inquirer                 | 4.2.7        | dev    | MIT                           | permissive      |
| @lmdb/lmdb-darwin-arm64                         | 3.5.6        | dev    | MIT                           | permissive      |
| @lmdb/lmdb-darwin-x64                           | 3.5.6        | dev    | MIT                           | permissive      |
| @lmdb/lmdb-linux-arm                            | 3.5.6        | dev    | MIT                           | permissive      |
| @lmdb/lmdb-linux-arm64                          | 3.5.6        | dev    | MIT                           | permissive      |
| @lmdb/lmdb-linux-x64                            | 3.5.6        | dev    | MIT                           | permissive      |
| @lmdb/lmdb-win32-arm64                          | 3.5.6        | dev    | MIT                           | permissive      |
| @lmdb/lmdb-win32-x64                            | 3.5.6        | dev    | MIT                           | permissive      |
| @modelcontextprotocol/core                      | 2.0.0        | dev    | MIT                           | permissive      |
| @modelcontextprotocol/server                    | 2.0.0        | dev    | MIT                           | permissive      |
| @msgpackr-extract/msgpackr-extract-darwin-arm64 | 3.0.4        | dev    | MIT                           | permissive      |
| @msgpackr-extract/msgpackr-extract-darwin-x64   | 3.0.4        | dev    | MIT                           | permissive      |
| @msgpackr-extract/msgpackr-extract-linux-arm    | 3.0.4        | dev    | MIT                           | permissive      |
| @msgpackr-extract/msgpackr-extract-linux-arm64  | 3.0.4        | dev    | MIT                           | permissive      |
| @msgpackr-extract/msgpackr-extract-linux-x64    | 3.0.4        | dev    | MIT                           | permissive      |
| @msgpackr-extract/msgpackr-extract-win32-x64    | 3.0.4        | dev    | MIT                           | permissive      |
| @napi-rs/nice                                   | 1.1.1        | dev    | MIT                           | permissive      |
| @napi-rs/nice-android-arm-eabi                  | 1.1.1        | dev    | MIT                           | permissive      |
| @napi-rs/nice-android-arm64                     | 1.1.1        | dev    | MIT                           | permissive      |
| @napi-rs/nice-darwin-arm64                      | 1.1.1        | dev    | MIT                           | permissive      |
| @napi-rs/nice-darwin-x64                        | 1.1.1        | dev    | MIT                           | permissive      |
| @napi-rs/nice-freebsd-x64                       | 1.1.1        | dev    | MIT                           | permissive      |
| @napi-rs/nice-linux-arm-gnueabihf               | 1.1.1        | dev    | MIT                           | permissive      |
| @napi-rs/nice-linux-arm64-gnu                   | 1.1.1        | dev    | MIT                           | permissive      |
| @napi-rs/nice-linux-arm64-musl                  | 1.1.1        | dev    | MIT                           | permissive      |
| @napi-rs/nice-linux-ppc64-gnu                   | 1.1.1        | dev    | MIT                           | permissive      |
| @napi-rs/nice-linux-riscv64-gnu                 | 1.1.1        | dev    | MIT                           | permissive      |
| @napi-rs/nice-linux-s390x-gnu                   | 1.1.1        | dev    | MIT                           | permissive      |
| @napi-rs/nice-linux-x64-gnu                     | 1.1.1        | dev    | MIT                           | permissive      |
| @napi-rs/nice-linux-x64-musl                    | 1.1.1        | dev    | MIT                           | permissive      |
| @napi-rs/nice-openharmony-arm64                 | 1.1.1        | dev    | MIT                           | permissive      |
| @napi-rs/nice-win32-arm64-msvc                  | 1.1.1        | dev    | MIT                           | permissive      |
| @napi-rs/nice-win32-ia32-msvc                   | 1.1.1        | dev    | MIT                           | permissive      |
| @napi-rs/nice-win32-x64-msvc                    | 1.1.1        | dev    | MIT                           | permissive      |
| @oxc-parser/binding-android-arm-eabi            | 0.150.0      | dev    | MIT                           | permissive      |
| @oxc-parser/binding-android-arm64               | 0.150.0      | dev    | MIT                           | permissive      |
| @oxc-parser/binding-darwin-arm64                | 0.150.0      | dev    | MIT                           | permissive      |
| @oxc-parser/binding-darwin-x64                  | 0.150.0      | dev    | MIT                           | permissive      |
| @oxc-parser/binding-freebsd-x64                 | 0.150.0      | dev    | MIT                           | permissive      |
| @oxc-parser/binding-linux-arm-gnueabihf         | 0.150.0      | dev    | MIT                           | permissive      |
| @oxc-parser/binding-linux-arm-musleabihf        | 0.150.0      | dev    | MIT                           | permissive      |
| @oxc-parser/binding-linux-arm64-gnu             | 0.150.0      | dev    | MIT                           | permissive      |
| @oxc-parser/binding-linux-arm64-musl            | 0.150.0      | dev    | MIT                           | permissive      |
| @oxc-parser/binding-linux-ppc64-gnu             | 0.150.0      | dev    | MIT                           | permissive      |
| @oxc-parser/binding-linux-riscv64-gnu           | 0.150.0      | dev    | MIT                           | permissive      |
| @oxc-parser/binding-linux-riscv64-musl          | 0.150.0      | dev    | MIT                           | permissive      |
| @oxc-parser/binding-linux-s390x-gnu             | 0.150.0      | dev    | MIT                           | permissive      |
| @oxc-parser/binding-linux-x64-gnu               | 0.150.0      | dev    | MIT                           | permissive      |
| @oxc-parser/binding-linux-x64-musl              | 0.150.0      | dev    | MIT                           | permissive      |
| @oxc-parser/binding-openharmony-arm64           | 0.150.0      | dev    | MIT                           | permissive      |
| @oxc-parser/binding-win32-arm64-msvc            | 0.150.0      | dev    | MIT                           | permissive      |
| @oxc-parser/binding-win32-ia32-msvc             | 0.150.0      | dev    | MIT                           | permissive      |
| @oxc-parser/binding-win32-x64-msvc              | 0.150.0      | dev    | MIT                           | permissive      |
| @oxc-project/types                              | 0.150.0      | dev    | MIT                           | permissive      |
| @oxc-project/types                              | 0.149.0      | dev    | MIT                           | permissive      |
| @oxc-project/types                              | 0.152.0      | dev    | MIT                           | permissive      |
| @parcel/watcher                                 | 2.6.0        | dev    | MIT                           | permissive      |
| @parcel/watcher-android-arm64                   | 2.6.0        | dev    | MIT                           | permissive      |
| @parcel/watcher-darwin-arm64                    | 2.6.0        | dev    | MIT                           | permissive      |
| @parcel/watcher-darwin-x64                      | 2.6.0        | dev    | MIT                           | permissive      |
| @parcel/watcher-freebsd-x64                     | 2.6.0        | dev    | MIT                           | permissive      |
| @parcel/watcher-linux-arm-glibc                 | 2.6.0        | dev    | MIT                           | permissive      |
| @parcel/watcher-linux-arm-musl                  | 2.6.0        | dev    | MIT                           | permissive      |
| @parcel/watcher-linux-arm64-glibc               | 2.6.0        | dev    | MIT                           | permissive      |
| @parcel/watcher-linux-arm64-musl                | 2.6.0        | dev    | MIT                           | permissive      |
| @parcel/watcher-linux-x64-glibc                 | 2.6.0        | dev    | MIT                           | permissive      |
| @parcel/watcher-linux-x64-musl                  | 2.6.0        | dev    | MIT                           | permissive      |
| @parcel/watcher-win32-arm64                     | 2.6.0        | dev    | MIT                           | permissive      |
| @parcel/watcher-win32-x64                       | 2.6.0        | dev    | MIT                           | permissive      |
| @rolldown/binding-android-arm-eabi              | 1.2.8        | dev    | MIT                           | permissive      |
| @rolldown/binding-android-arm-eabi              | 1.2.12       | dev    | MIT                           | permissive      |
| @rolldown/binding-android-arm64                 | 1.2.8        | dev    | MIT                           | permissive      |
| @rolldown/binding-android-arm64                 | 1.2.12       | dev    | MIT                           | permissive      |
| @rolldown/binding-darwin-arm64                  | 1.2.8        | dev    | MIT                           | permissive      |
| @rolldown/binding-darwin-arm64                  | 1.2.12       | dev    | MIT                           | permissive      |
| @rolldown/binding-darwin-x64                    | 1.2.8        | dev    | MIT                           | permissive      |
| @rolldown/binding-darwin-x64                    | 1.2.12       | dev    | MIT                           | permissive      |
| @rolldown/binding-freebsd-x64                   | 1.2.8        | dev    | MIT                           | permissive      |
| @rolldown/binding-freebsd-x64                   | 1.2.12       | dev    | MIT                           | permissive      |
| @rolldown/binding-linux-arm-gnueabihf           | 1.2.8        | dev    | MIT                           | permissive      |
| @rolldown/binding-linux-arm-gnueabihf           | 1.2.12       | dev    | MIT                           | permissive      |
| @rolldown/binding-linux-arm64-gnu               | 1.2.8        | dev    | MIT                           | permissive      |
| @rolldown/binding-linux-arm64-gnu               | 1.2.12       | dev    | MIT                           | permissive      |
| @rolldown/binding-linux-arm64-musl              | 1.2.8        | dev    | MIT                           | permissive      |
| @rolldown/binding-linux-arm64-musl              | 1.2.12       | dev    | MIT                           | permissive      |
| @rolldown/binding-linux-ppc64-gnu               | 1.2.8        | dev    | MIT                           | permissive      |
| @rolldown/binding-linux-ppc64-gnu               | 1.2.12       | dev    | MIT                           | permissive      |
| @rolldown/binding-linux-s390x-gnu               | 1.2.8        | dev    | MIT                           | permissive      |
| @rolldown/binding-linux-s390x-gnu               | 1.2.12       | dev    | MIT                           | permissive      |
| @rolldown/binding-linux-x64-gnu                 | 1.2.8        | dev    | MIT                           | permissive      |
| @rolldown/binding-linux-x64-gnu                 | 1.2.12       | dev    | MIT                           | permissive      |
| @rolldown/binding-linux-x64-musl                | 1.2.8        | dev    | MIT                           | permissive      |
| @rolldown/binding-linux-x64-musl                | 1.2.12       | dev    | MIT                           | permissive      |
| @rolldown/binding-openharmony-arm64             | 1.2.8        | dev    | MIT                           | permissive      |
| @rolldown/binding-openharmony-arm64             | 1.2.12       | dev    | MIT                           | permissive      |
| @rolldown/binding-win32-arm64-msvc              | 1.2.8        | dev    | MIT                           | permissive      |
| @rolldown/binding-win32-arm64-msvc              | 1.2.12       | dev    | MIT                           | permissive      |
| @rolldown/binding-win32-x64-msvc                | 1.2.8        | dev    | MIT                           | permissive      |
| @rolldown/binding-win32-x64-msvc                | 1.2.12       | dev    | MIT                           | permissive      |
| @rolldown/pluginutils                           | 1.0.1        | dev    | MIT                           | permissive      |
| @schematics/angular                             | 22.2.1       | dev    | MIT                           | permissive      |
| @types/chai                                     | 5.2.3        | dev    | MIT                           | permissive      |
| @types/deep-eql                                 | 4.0.2        | dev    | MIT                           | permissive      |
| @types/estree                                   | 1.0.9        | dev    | MIT                           | permissive      |
| @types/gensync                                  | 1.0.5        | dev    | MIT                           | permissive      |
| @types/jsesc                                    | 2.5.1        | dev    | MIT                           | permissive      |
| @vitejs/plugin-basic-ssl                        | 2.3.0        | dev    | MIT                           | permissive      |
| @vitest/mocker                                  | 5.0.3        | dev    | MIT                           | permissive      |
| @vitest/spy                                     | 5.0.3        | dev    | MIT                           | permissive      |
| agent-base                                      | 9.0.0        | dev    | MIT                           | permissive      |
| ajv                                             | 8.20.0       | dev    | MIT                           | permissive      |
| ajv-formats                                     | 3.0.1        | dev    | MIT                           | permissive      |
| ansi-escapes                                    | 7.3.0        | dev    | MIT                           | permissive      |
| ansi-regex                                      | 6.4.0        | dev    | MIT                           | permissive      |
| ansi-styles                                     | 6.2.3        | dev    | MIT                           | permissive      |
| assertion-error                                 | 2.0.1        | dev    | MIT                           | permissive      |
| baseline-browser-mapping                        | 2.11.27      | dev    | Apache-2.0                    | permissive      |
| beasties                                        | 0.5.4        | dev    | Apache-2.0                    | permissive      |
| bidi-js                                         | 1.1.0        | dev    | MIT                           | permissive      |
| boolbase                                        | 2.0.0        | dev    | ISC                           | permissive      |
| browserslist                                    | 4.29.3       | dev    | MIT                           | permissive      |
| buffer-from                                     | 1.1.2        | dev    | MIT                           | permissive      |
| caniuse-lite                                    | 1.0.30001814 | dev    | CC-BY-4.0                     | permissive      |
| chai                                            | 6.3.0        | dev    | MIT                           | permissive      |
| chalk                                           | 5.6.2        | dev    | MIT                           | permissive      |
| chardet                                         | 2.2.0        | dev    | MIT                           | permissive      |
| chokidar                                        | 5.0.0        | dev    | MIT                           | permissive      |
| cli-cursor                                      | 5.0.0        | dev    | MIT                           | permissive      |
| cli-spinners                                    | 3.4.0        | dev    | MIT                           | permissive      |
| cli-truncate                                    | 6.1.1        | dev    | MIT                           | permissive      |
| cli-width                                       | 4.1.0        | dev    | ISC                           | permissive      |
| cliui                                           | 9.0.1        | dev    | ISC                           | permissive      |
| colorjs.io                                      | 0.7.1        | dev    | MIT                           | permissive      |
| convert-source-map                              | 2.0.0        | dev    | MIT                           | permissive      |
| convert-source-map                              | 2.0.0        | dev    | MIT                           | permissive      |
| convert-source-map                              | 1.9.0        | dev    | MIT                           | permissive      |
| css-select                                      | 7.0.0        | dev    | BSD-2-Clause                  | permissive      |
| css-tree                                        | 3.2.1        | dev    | MIT                           | permissive      |
| css-what                                        | 8.0.0        | dev    | BSD-2-Clause                  | permissive      |
| data-urls                                       | 8.0.0        | dev    | MIT                           | permissive      |
| debug                                           | 4.4.3        | dev    | MIT                           | permissive      |
| decimal.js                                      | 10.6.0       | dev    | MIT                           | permissive      |
| detect-libc                                     | 2.1.2        | dev    | Apache-2.0                    | permissive      |
| dom-serializer                                  | 3.1.1        | dev    | MIT                           | permissive      |
| domelementtype                                  | 3.0.0        | dev    | BSD-2-Clause                  | permissive      |
| domhandler                                      | 6.0.1        | dev    | BSD-2-Clause                  | permissive      |
| domutils                                        | 4.0.2        | dev    | BSD-2-Clause                  | permissive      |
| electron-to-chromium                            | 1.5.444      | dev    | ISC                           | permissive      |
| emoji-regex                                     | 10.6.0       | dev    | MIT                           | permissive      |
| empathic                                        | 2.1.0        | dev    | MIT                           | permissive      |
| environment                                     | 1.1.0        | dev    | MIT                           | permissive      |
| es-module-lexer                                 | 2.3.2        | dev    | MIT                           | permissive      |
| esbuild                                         | 0.28.2       | dev    | MIT                           | permissive      |
| escalade                                        | 3.2.0        | dev    | MIT                           | permissive      |
| estree-walker                                   | 3.0.3        | dev    | MIT                           | permissive      |
| expect-type                                     | 1.4.0        | dev    | Apache-2.0                    | permissive      |
| fast-deep-equal                                 | 3.1.3        | dev    | MIT                           | permissive      |
| fast-string-truncated-width                     | 3.0.3        | dev    | MIT                           | permissive      |
| fast-string-width                               | 3.0.2        | dev    | MIT                           | permissive      |
| fast-uri                                        | 3.1.8        | dev    | BSD-3-Clause                  | permissive      |
| fast-wrap-ansi                                  | 0.2.2        | dev    | MIT                           | permissive      |
| fdir                                            | 6.5.0        | dev    | MIT                           | permissive      |
| flru                                            | 1.0.2        | dev    | MIT                           | permissive      |
| fsevents                                        | 2.3.3        | dev    | MIT                           | permissive      |
| gensync                                         | 1.0.0-beta.2 | dev    | MIT                           | permissive      |
| get-caller-file                                 | 2.0.5        | dev    | ISC                           | permissive      |
| get-east-asian-width                            | 1.7.0        | dev    | MIT                           | permissive      |
| has-flag                                        | 4.0.0        | dev    | MIT                           | permissive      |
| hosted-git-info                                 | 10.1.1       | dev    | ISC                           | permissive      |
| html-encoding-sniffer                           | 7.0.0        | dev    | MIT                           | permissive      |
| htmlparser2                                     | 12.0.0       | dev    | MIT                           | permissive      |
| https-proxy-agent                               | 9.1.0        | dev    | MIT                           | permissive      |
| iconv-lite                                      | 0.7.3        | dev    | MIT                           | permissive      |
| immutable                                       | 5.1.9        | dev    | MIT                           | permissive      |
| import-meta-resolve                             | 4.2.0        | dev    | MIT                           | permissive      |
| is-extglob                                      | 2.1.1        | dev    | MIT                           | permissive      |
| is-fullwidth-code-point                         | 5.1.0        | dev    | MIT                           | permissive      |
| is-glob                                         | 4.0.3        | dev    | MIT                           | permissive      |
| is-interactive                                  | 2.0.0        | dev    | MIT                           | permissive      |
| is-potential-custom-element-name                | 1.0.1        | dev    | MIT                           | permissive      |
| is-unicode-supported                            | 2.1.0        | dev    | MIT                           | permissive      |
| js-tokens                                       | 10.0.0       | dev    | MIT                           | permissive      |
| jsdom                                           | 30.1.2       | dev    | MIT                           | permissive      |
| jsesc                                           | 3.1.0        | dev    | MIT                           | permissive      |
| json-schema-traverse                            | 1.0.0        | dev    | MIT                           | permissive      |
| json5                                           | 2.2.3        | dev    | MIT                           | permissive      |
| jsonc-parser                                    | 3.3.1        | dev    | MIT                           | permissive      |
| listr2                                          | 11.1.0       | dev    | MIT                           | permissive      |
| lmdb                                            | 3.5.6        | dev    | MIT                           | permissive      |
| log-symbols                                     | 7.0.1        | dev    | MIT                           | permissive      |
| log-update                                      | 8.0.0        | dev    | MIT                           | permissive      |
| magic-string                                    | 1.4.1        | dev    | MIT                           | permissive      |
| mdn-data                                        | 2.27.1       | dev    | CC0-1.0                       | permissive      |
| mimic-function                                  | 5.0.1        | dev    | MIT                           | permissive      |
| mrmime                                          | 2.0.1        | dev    | MIT                           | permissive      |
| ms                                              | 2.1.3        | dev    | MIT                           | permissive      |
| msgpackr                                        | 1.12.1       | dev    | MIT                           | permissive      |
| msgpackr-extract                                | 3.0.4        | dev    | MIT                           | permissive      |
| mute-stream                                     | 3.0.0        | dev    | ISC                           | permissive      |
| nanoid                                          | 3.3.20       | dev    | MIT                           | permissive      |
| node-addon-api                                  | 6.1.0        | dev    | MIT                           | permissive      |
| node-addon-api                                  | 7.1.1        | dev    | MIT                           | permissive      |
| node-gyp-build-optional-packages                | 5.2.2        | dev    | MIT                           | permissive      |
| node-releases                                   | 2.0.57       | dev    | MIT                           | permissive      |
| npm-package-arg                                 | 14.0.0       | dev    | ISC                           | permissive      |
| nth-check                                       | 3.0.1        | dev    | BSD-2-Clause                  | permissive      |
| obug                                            | 2.2.1        | dev    | MIT                           | permissive      |
| onetime                                         | 7.0.0        | dev    | MIT                           | permissive      |
| ora                                             | 9.4.1        | dev    | MIT                           | permissive      |
| ordered-binary                                  | 1.6.2        | dev    | MIT                           | permissive      |
| oxc-parser                                      | 0.150.0      | dev    | MIT                           | permissive      |
| parse5-html-rewriting-stream                    | 8.0.1        | dev    | MIT                           | permissive      |
| parse5-sax-parser                               | 8.0.0        | dev    | MIT                           | permissive      |
| picocolors                                      | 1.1.1        | dev    | ISC                           | permissive      |
| picomatch                                       | 4.0.7        | dev    | MIT                           | permissive      |
| piscina                                         | 5.3.2        | dev    | MIT                           | permissive      |
| postcss                                         | 8.5.29       | dev    | MIT                           | permissive      |
| postcss-media-query-parser                      | 0.2.3        | dev    | MIT                           | permissive      |
| postcss-safe-parser                             | 7.1.0        | dev    | MIT                           | permissive      |
| prettier                                        | 3.9.9        | dev    | MIT                           | permissive      |
| proc-log                                        | 7.0.0        | dev    | ISC                           | permissive      |
| proxy-agent-negotiate                           | 1.1.0        | dev    | MIT                           | permissive      |
| punycode                                        | 2.3.1        | dev    | MIT                           | permissive      |
| readdirp                                        | 5.1.1        | dev    | MIT                           | permissive      |
| reflect-metadata                                | 0.2.2        | dev    | Apache-2.0                    | permissive      |
| require-from-string                             | 2.0.2        | dev    | MIT                           | permissive      |
| restore-cursor                                  | 5.1.0        | dev    | MIT                           | permissive      |
| rolldown                                        | 1.2.8        | dev    | MIT                           | permissive      |
| rolldown                                        | 1.2.12       | dev    | MIT                           | permissive      |
| safer-buffer                                    | 2.1.2        | dev    | MIT                           | permissive      |
| sass                                            | 1.104.1      | dev    | MIT                           | permissive      |
| sass-embedded                                   | 1.104.1      | dev    | MIT                           | permissive      |
| sass-embedded-all-unknown                       | 1.104.1      | dev    | MIT                           | permissive      |
| sass-embedded-android-arm                       | 1.104.1      | dev    | MIT                           | permissive      |
| sass-embedded-android-arm64                     | 1.104.1      | dev    | MIT                           | permissive      |
| sass-embedded-android-riscv64                   | 1.104.1      | dev    | MIT                           | permissive      |
| sass-embedded-android-x64                       | 1.104.1      | dev    | MIT                           | permissive      |
| sass-embedded-darwin-arm64                      | 1.104.1      | dev    | MIT                           | permissive      |
| sass-embedded-darwin-x64                        | 1.104.1      | dev    | MIT                           | permissive      |
| sass-embedded-linux-arm                         | 1.104.1      | dev    | MIT                           | permissive      |
| sass-embedded-linux-arm64                       | 1.104.1      | dev    | MIT                           | permissive      |
| sass-embedded-linux-musl-arm                    | 1.104.1      | dev    | MIT                           | permissive      |
| sass-embedded-linux-musl-arm64                  | 1.104.1      | dev    | MIT                           | permissive      |
| sass-embedded-linux-musl-riscv64                | 1.104.1      | dev    | MIT                           | permissive      |
| sass-embedded-linux-musl-x64                    | 1.104.1      | dev    | MIT                           | permissive      |
| sass-embedded-linux-riscv64                     | 1.104.1      | dev    | MIT                           | permissive      |
| sass-embedded-linux-x64                         | 1.104.1      | dev    | MIT                           | permissive      |
| sass-embedded-unknown-all                       | 1.104.1      | dev    | MIT                           | permissive      |
| sass-embedded-win32-arm64                       | 1.104.1      | dev    | MIT                           | permissive      |
| sass-embedded-win32-x64                         | 1.104.1      | dev    | MIT                           | permissive      |
| saxes                                           | 6.0.0        | dev    | ISC                           | permissive      |
| semver                                          | 7.8.5        | dev    | ISC                           | permissive      |
| signal-exit                                     | 4.1.0        | dev    | ISC                           | permissive      |
| slice-ansi                                      | 9.0.1        | dev    | MIT                           | permissive      |
| source-map                                      | 0.8.0        | dev    | BSD-3-Clause                  | permissive      |
| source-map                                      | 0.6.1        | dev    | BSD-3-Clause                  | permissive      |
| source-map-js                                   | 1.2.2        | dev    | BSD-3-Clause                  | permissive      |
| source-map-support                              | 0.5.21       | dev    | MIT                           | permissive      |
| std-env                                         | 4.3.0        | dev    | MIT                           | permissive      |
| stdin-discarder                                 | 0.3.2        | dev    | MIT                           | permissive      |
| string-width                                    | 7.2.0        | dev    | MIT                           | permissive      |
| string-width                                    | 8.3.0        | dev    | MIT                           | permissive      |
| strip-ansi                                      | 7.2.0        | dev    | MIT                           | permissive      |
| supports-color                                  | 8.1.1        | dev    | MIT                           | permissive      |
| sync-child-process                              | 1.0.2        | dev    | MIT                           | permissive      |
| sync-message-port                               | 1.2.0        | dev    | MIT                           | permissive      |
| tinybench                                       | 6.2.0        | dev    | MIT                           | permissive      |
| tinyexec                                        | 1.3.1        | dev    | MIT                           | permissive      |
| tinyglobby                                      | 0.2.17       | dev    | MIT                           | permissive      |
| tldts                                           | 7.4.16       | dev    | MIT                           | permissive      |
| tldts-core                                      | 7.4.16       | dev    | MIT                           | permissive      |
| tough-cookie                                    | 6.0.2        | dev    | BSD-3-Clause                  | permissive      |
| tr46                                            | 7.0.0        | dev    | MIT                           | permissive      |
| typescript                                      | 6.0.3        | dev    | Apache-2.0                    | permissive      |
| undici                                          | 8.11.2       | dev    | MIT                           | permissive      |
| update-browserslist-db                          | 1.3.3        | dev    | MIT                           | permissive      |
| validate-npm-package-name                       | 8.0.0        | dev    | ISC                           | permissive      |
| varint                                          | 6.0.0        | dev    | MIT                           | permissive      |
| verkit                                          | 0.3.2        | dev    | MIT                           | permissive      |
| vite                                            | 8.3.0        | dev    | MIT                           | permissive      |
| vite                                            | 8.3.2        | dev    | MIT                           | permissive      |
| vitest                                          | 5.0.3        | dev    | MIT                           | permissive      |
| w3c-xmlserializer                               | 6.0.0        | dev    | MIT                           | permissive      |
| weak-lru-cache                                  | 1.2.2        | dev    | MIT                           | permissive      |
| webidl-conversions                              | 8.0.1        | dev    | BSD-2-Clause                  | permissive      |
| whatwg-mimetype                                 | 5.0.0        | dev    | MIT                           | permissive      |
| whatwg-url                                      | 17.2.0       | dev    | MIT                           | permissive      |
| why-is-node-running                             | 3.2.1        | dev    | MIT                           | permissive      |
| wrap-ansi                                       | 9.0.2        | dev    | MIT                           | permissive      |
| wrap-ansi                                       | 10.0.2       | dev    | MIT                           | permissive      |
| xml-name-validator                              | 5.0.0        | dev    | Apache-2.0                    | permissive      |
| xmlchars                                        | 2.2.0        | dev    | MIT                           | permissive      |
| xxhash-wasm                                     | 1.1.0        | dev    | MIT                           | permissive      |
| y18n                                            | 5.0.8        | dev    | ISC                           | permissive      |
| yargs                                           | 18.1.0       | dev    | MIT                           | permissive      |
| yargs-parser                                    | 22.0.0       | dev    | ISC                           | permissive      |
| yoctocolors                                     | 2.2.0        | dev    | MIT                           | permissive      |

### npm backend (outillage JHipster, Prettier, Husky)

| Paquet                                      | Version | Portee | Licence                             | Classement |
| ------------------------------------------- | ------- | ------ | ----------------------------------- | ---------- |
| binaryextensions                            | 6.11.0  | dev    | Artistic-2.0                        | inconnue   |
| chownr                                      | 3.0.0   | dev    | BlueOak-1.0.0                       | inconnue   |
| cli-table                                   | 0.3.11  | dev    | inconnue                            | inconnue   |
| common-ancestor-path                        | 2.0.0   | dev    | BlueOak-1.0.0                       | inconnue   |
| editions                                    | 6.22.0  | dev    | Artistic-2.0                        | inconnue   |
| glob                                        | 13.0.6  | dev    | BlueOak-1.0.0                       | inconnue   |
| isexe                                       | 4.0.0   | dev    | BlueOak-1.0.0                       | inconnue   |
| isexe                                       | 4.0.0   | dev    | BlueOak-1.0.0                       | inconnue   |
| isexe                                       | 4.0.0   | dev    | BlueOak-1.0.0                       | inconnue   |
| lru-cache                                   | 11.5.3  | dev    | BlueOak-1.0.0                       | inconnue   |
| minimatch                                   | 10.2.6  | dev    | BlueOak-1.0.0                       | inconnue   |
| minipass                                    | 7.1.3   | dev    | BlueOak-1.0.0                       | inconnue   |
| minipass-flush                              | 1.0.7   | dev    | BlueOak-1.0.0                       | inconnue   |
| path-scurry                                 | 2.0.2   | dev    | BlueOak-1.0.0                       | inconnue   |
| tar                                         | 7.5.22  | dev    | BlueOak-1.0.0                       | inconnue   |
| textextensions                              | 6.11.0  | dev    | Artistic-2.0                        | inconnue   |
| version-range                               | 4.15.0  | dev    | Artistic-2.0                        | inconnue   |
| yallist                                     | 5.0.0   | dev    | BlueOak-1.0.0                       | inconnue   |
| @babel/code-frame                           | 7.29.7  | dev    | MIT                                 | permissive |
| @babel/helper-validator-identifier          | 7.29.7  | dev    | MIT                                 | permissive |
| @chevrotain/cst-dts-gen                     | 13.2.0  | dev    | Apache-2.0                          | permissive |
| @chevrotain/gast                            | 13.2.0  | dev    | Apache-2.0                          | permissive |
| @chevrotain/regexp-to-ast                   | 13.2.0  | dev    | Apache-2.0                          | permissive |
| @chevrotain/types                           | 13.2.0  | dev    | Apache-2.0                          | permissive |
| @chevrotain/utils                           | 13.2.0  | dev    | Apache-2.0                          | permissive |
| @emnapi/core                                | 1.10.0  | dev    | MIT                                 | permissive |
| @emnapi/runtime                             | 1.10.0  | dev    | MIT                                 | permissive |
| @emnapi/wasi-threads                        | 1.2.1   | dev    | MIT                                 | permissive |
| @eslint-community/eslint-utils              | 4.10.1  | dev    | MIT                                 | permissive |
| @eslint-community/regexpp                   | 4.12.2  | dev    | MIT                                 | permissive |
| @eslint/config-array                        | 0.23.5  | dev    | Apache-2.0                          | permissive |
| @eslint/config-helpers                      | 0.7.0   | dev    | Apache-2.0                          | permissive |
| @eslint/core                                | 1.2.1   | dev    | Apache-2.0                          | permissive |
| @eslint/js                                  | 10.0.1  | dev    | MIT                                 | permissive |
| @eslint/object-schema                       | 3.0.5   | dev    | Apache-2.0                          | permissive |
| @eslint/plugin-kit                          | 0.7.3   | dev    | Apache-2.0                          | permissive |
| @faker-js/faker                             | 10.6.0  | dev    | MIT                                 | permissive |
| @gar/promise-retry                          | 1.0.3   | dev    | MIT                                 | permissive |
| @humanfs/core                               | 0.19.2  | dev    | Apache-2.0                          | permissive |
| @humanfs/node                               | 0.16.8  | dev    | Apache-2.0                          | permissive |
| @humanfs/types                              | 0.15.0  | dev    | Apache-2.0                          | permissive |
| @humanwhocodes/module-importer              | 1.0.1   | dev    | Apache-2.0                          | permissive |
| @humanwhocodes/retry                        | 0.4.3   | dev    | Apache-2.0                          | permissive |
| @iarna/toml                                 | 3.0.0   | dev    | ISC                                 | permissive |
| @inquirer/ansi                              | 2.0.9   | dev    | MIT                                 | permissive |
| @inquirer/checkbox                          | 5.2.6   | dev    | MIT                                 | permissive |
| @inquirer/confirm                           | 6.3.3   | dev    | MIT                                 | permissive |
| @inquirer/core                              | 12.0.4  | dev    | MIT                                 | permissive |
| @inquirer/core                              | 12.0.4  | dev    | MIT                                 | permissive |
| @inquirer/core                              | 11.2.1  | dev    | MIT                                 | permissive |
| @inquirer/core                              | 12.0.4  | dev    | MIT                                 | permissive |
| @inquirer/core                              | 12.0.4  | dev    | MIT                                 | permissive |
| @inquirer/core                              | 12.0.4  | dev    | MIT                                 | permissive |
| @inquirer/core                              | 12.0.4  | dev    | MIT                                 | permissive |
| @inquirer/core                              | 12.0.4  | dev    | MIT                                 | permissive |
| @inquirer/core                              | 12.0.4  | dev    | MIT                                 | permissive |
| @inquirer/core                              | 12.0.4  | dev    | MIT                                 | permissive |
| @inquirer/core                              | 12.0.4  | dev    | MIT                                 | permissive |
| @inquirer/editor                            | 5.3.4   | dev    | MIT                                 | permissive |
| @inquirer/expand                            | 5.1.6   | dev    | MIT                                 | permissive |
| @inquirer/external-editor                   | 3.0.6   | dev    | MIT                                 | permissive |
| @inquirer/figures                           | 2.0.9   | dev    | MIT                                 | permissive |
| @inquirer/input                             | 5.1.7   | dev    | MIT                                 | permissive |
| @inquirer/number                            | 4.2.4   | dev    | MIT                                 | permissive |
| @inquirer/password                          | 5.2.3   | dev    | MIT                                 | permissive |
| @inquirer/prompts                           | 8.7.3   | dev    | MIT                                 | permissive |
| @inquirer/rawlist                           | 5.3.6   | dev    | MIT                                 | permissive |
| @inquirer/search                            | 4.3.4   | dev    | MIT                                 | permissive |
| @inquirer/select                            | 5.2.6   | dev    | MIT                                 | permissive |
| @inquirer/type                              | 4.2.0   | dev    | MIT                                 | permissive |
| @isaacs/fs-minipass                         | 4.0.1   | dev    | ISC                                 | permissive |
| @isaacs/string-locale-compare               | 1.1.0   | dev    | ISC                                 | permissive |
| @kwsites/file-exists                        | 1.1.1   | dev    | MIT                                 | permissive |
| @kwsites/promise-deferred                   | 1.1.1   | dev    | MIT                                 | permissive |
| @napi-rs/nice                               | 1.1.1   | dev    | MIT                                 | permissive |
| @napi-rs/nice-android-arm-eabi              | 1.1.1   | dev    | MIT                                 | permissive |
| @napi-rs/nice-android-arm64                 | 1.1.1   | dev    | MIT                                 | permissive |
| @napi-rs/nice-darwin-arm64                  | 1.1.1   | dev    | MIT                                 | permissive |
| @napi-rs/nice-darwin-x64                    | 1.1.1   | dev    | MIT                                 | permissive |
| @napi-rs/nice-freebsd-x64                   | 1.1.1   | dev    | MIT                                 | permissive |
| @napi-rs/nice-linux-arm-gnueabihf           | 1.1.1   | dev    | MIT                                 | permissive |
| @napi-rs/nice-linux-arm64-gnu               | 1.1.1   | dev    | MIT                                 | permissive |
| @napi-rs/nice-linux-arm64-musl              | 1.1.1   | dev    | MIT                                 | permissive |
| @napi-rs/nice-linux-ppc64-gnu               | 1.1.1   | dev    | MIT                                 | permissive |
| @napi-rs/nice-linux-riscv64-gnu             | 1.1.1   | dev    | MIT                                 | permissive |
| @napi-rs/nice-linux-s390x-gnu               | 1.1.1   | dev    | MIT                                 | permissive |
| @napi-rs/nice-linux-x64-gnu                 | 1.1.1   | dev    | MIT                                 | permissive |
| @napi-rs/nice-linux-x64-musl                | 1.1.1   | dev    | MIT                                 | permissive |
| @napi-rs/nice-openharmony-arm64             | 1.1.1   | dev    | MIT                                 | permissive |
| @napi-rs/nice-win32-arm64-msvc              | 1.1.1   | dev    | MIT                                 | permissive |
| @napi-rs/nice-win32-ia32-msvc               | 1.1.1   | dev    | MIT                                 | permissive |
| @napi-rs/nice-win32-x64-msvc                | 1.1.1   | dev    | MIT                                 | permissive |
| @napi-rs/wasm-runtime                       | 1.2.5   | dev    | MIT                                 | permissive |
| @nodable/entities                           | 3.1.0   | dev    | MIT                                 | permissive |
| @nodelib/fs.scandir                         | 2.1.5   | dev    | MIT                                 | permissive |
| @nodelib/fs.stat                            | 2.0.5   | dev    | MIT                                 | permissive |
| @nodelib/fs.walk                            | 1.2.8   | dev    | MIT                                 | permissive |
| @npmcli/agent                               | 4.0.2   | dev    | ISC                                 | permissive |
| @npmcli/arborist                            | 9.9.2   | dev    | ISC                                 | permissive |
| @npmcli/fs                                  | 5.0.0   | dev    | ISC                                 | permissive |
| @npmcli/git                                 | 7.0.2   | dev    | ISC                                 | permissive |
| @npmcli/installed-package-contents          | 4.0.0   | dev    | ISC                                 | permissive |
| @npmcli/map-workspaces                      | 5.0.3   | dev    | ISC                                 | permissive |
| @npmcli/metavuln-calculator                 | 9.0.3   | dev    | ISC                                 | permissive |
| @npmcli/name-from-folder                    | 4.0.0   | dev    | ISC                                 | permissive |
| @npmcli/node-gyp                            | 5.0.0   | dev    | ISC                                 | permissive |
| @npmcli/package-json                        | 7.0.5   | dev    | ISC                                 | permissive |
| @npmcli/promise-spawn                       | 9.0.1   | dev    | ISC                                 | permissive |
| @npmcli/query                               | 5.0.0   | dev    | ISC                                 | permissive |
| @npmcli/redact                              | 4.0.0   | dev    | ISC                                 | permissive |
| @npmcli/run-script                          | 10.0.4  | dev    | ISC                                 | permissive |
| @pnpm/config.env-replace                    | 1.1.0   | dev    | MIT                                 | permissive |
| @pnpm/network.ca-file                       | 1.0.2   | dev    | MIT                                 | permissive |
| @pnpm/npm-conf                              | 3.0.3   | dev    | MIT                                 | permissive |
| @sec-ant/readable-stream                    | 0.4.1   | dev    | MIT                                 | permissive |
| @sigstore/bundle                            | 4.0.0   | dev    | Apache-2.0                          | permissive |
| @sigstore/core                              | 3.2.1   | dev    | Apache-2.0                          | permissive |
| @sigstore/protobuf-specs                    | 0.5.2   | dev    | Apache-2.0                          | permissive |
| @sigstore/sign                              | 4.1.1   | dev    | Apache-2.0                          | permissive |
| @sigstore/tuf                               | 4.0.2   | dev    | Apache-2.0                          | permissive |
| @sigstore/verify                            | 3.1.1   | dev    | Apache-2.0                          | permissive |
| @simple-git/args-pathspec                   | 1.0.4   | dev    | MIT                                 | permissive |
| @simple-git/argv-parser                     | 1.1.1   | dev    | MIT                                 | permissive |
| @sindresorhus/merge-streams                 | 4.0.0   | dev    | MIT                                 | permissive |
| @tufjs/canonical-json                       | 2.0.0   | dev    | MIT                                 | permissive |
| @tufjs/models                               | 4.1.0   | dev    | MIT                                 | permissive |
| @tybys/wasm-util                            | 0.10.4  | dev    | MIT                                 | permissive |
| @types/debug                                | 4.1.13  | dev    | MIT                                 | permissive |
| @types/ejs                                  | 3.1.5   | dev    | MIT                                 | permissive |
| @types/esrecurse                            | 4.3.1   | dev    | MIT                                 | permissive |
| @types/estree                               | 1.0.9   | dev    | MIT                                 | permissive |
| @types/expect                               | 1.20.4  | dev    | MIT                                 | permissive |
| @types/json-schema                          | 7.0.15  | dev    | MIT                                 | permissive |
| @types/lodash                               | 4.17.25 | dev    | MIT                                 | permissive |
| @types/lodash-es                            | 4.17.12 | dev    | MIT                                 | permissive |
| @types/ms                                   | 2.1.0   | dev    | MIT                                 | permissive |
| @types/node                                 | 26.6.4  | dev    | MIT                                 | permissive |
| @types/normalize-package-data               | 2.4.4   | dev    | MIT                                 | permissive |
| @types/picomatch                            | 4.0.3   | dev    | MIT                                 | permissive |
| @types/vinyl                                | 2.0.12  | dev    | MIT                                 | permissive |
| @typescript-eslint/eslint-plugin            | 8.68.0  | dev    | MIT                                 | permissive |
| @typescript-eslint/parser                   | 8.68.0  | dev    | MIT                                 | permissive |
| @typescript-eslint/project-service          | 8.68.0  | dev    | MIT                                 | permissive |
| @typescript-eslint/scope-manager            | 8.68.0  | dev    | MIT                                 | permissive |
| @typescript-eslint/tsconfig-utils           | 8.68.0  | dev    | MIT                                 | permissive |
| @typescript-eslint/type-utils               | 8.68.0  | dev    | MIT                                 | permissive |
| @typescript-eslint/types                    | 8.68.0  | dev    | MIT                                 | permissive |
| @typescript-eslint/types                    | 8.68.0  | dev    | MIT                                 | permissive |
| @typescript-eslint/types                    | 8.68.0  | dev    | MIT                                 | permissive |
| @typescript-eslint/types                    | 8.71.0  | dev    | MIT                                 | permissive |
| @typescript-eslint/types                    | 8.68.0  | dev    | MIT                                 | permissive |
| @typescript-eslint/types                    | 8.68.0  | dev    | MIT                                 | permissive |
| @typescript-eslint/types                    | 8.68.0  | dev    | MIT                                 | permissive |
| @typescript-eslint/typescript-estree        | 8.68.0  | dev    | MIT                                 | permissive |
| @typescript-eslint/utils                    | 8.68.0  | dev    | MIT                                 | permissive |
| @typescript-eslint/visitor-keys             | 8.68.0  | dev    | MIT                                 | permissive |
| @unrs/resolver-binding-android-arm-eabi     | 1.12.2  | dev    | MIT                                 | permissive |
| @unrs/resolver-binding-android-arm64        | 1.12.2  | dev    | MIT                                 | permissive |
| @unrs/resolver-binding-darwin-arm64         | 1.12.2  | dev    | MIT                                 | permissive |
| @unrs/resolver-binding-darwin-x64           | 1.12.2  | dev    | MIT                                 | permissive |
| @unrs/resolver-binding-freebsd-x64          | 1.12.2  | dev    | MIT                                 | permissive |
| @unrs/resolver-binding-linux-arm-gnueabihf  | 1.12.2  | dev    | MIT                                 | permissive |
| @unrs/resolver-binding-linux-arm-musleabihf | 1.12.2  | dev    | MIT                                 | permissive |
| @unrs/resolver-binding-linux-arm64-gnu      | 1.12.2  | dev    | MIT                                 | permissive |
| @unrs/resolver-binding-linux-arm64-musl     | 1.12.2  | dev    | MIT                                 | permissive |
| @unrs/resolver-binding-linux-loong64-gnu    | 1.12.2  | dev    | MIT                                 | permissive |
| @unrs/resolver-binding-linux-loong64-musl   | 1.12.2  | dev    | MIT                                 | permissive |
| @unrs/resolver-binding-linux-ppc64-gnu      | 1.12.2  | dev    | MIT                                 | permissive |
| @unrs/resolver-binding-linux-riscv64-gnu    | 1.12.2  | dev    | MIT                                 | permissive |
| @unrs/resolver-binding-linux-riscv64-musl   | 1.12.2  | dev    | MIT                                 | permissive |
| @unrs/resolver-binding-linux-s390x-gnu      | 1.12.2  | dev    | MIT                                 | permissive |
| @unrs/resolver-binding-linux-x64-gnu        | 1.12.2  | dev    | MIT                                 | permissive |
| @unrs/resolver-binding-linux-x64-musl       | 1.12.2  | dev    | MIT                                 | permissive |
| @unrs/resolver-binding-openharmony-arm64    | 1.12.2  | dev    | MIT                                 | permissive |
| @unrs/resolver-binding-wasm32-wasi          | 1.12.2  | dev    | MIT                                 | permissive |
| @unrs/resolver-binding-win32-arm64-msvc     | 1.12.2  | dev    | MIT                                 | permissive |
| @unrs/resolver-binding-win32-ia32-msvc      | 1.12.2  | dev    | MIT                                 | permissive |
| @unrs/resolver-binding-win32-x64-msvc       | 1.12.2  | dev    | MIT                                 | permissive |
| @yeoman/adapter                             | 4.0.2   | dev    | MIT                                 | permissive |
| @yeoman/conflicter                          | 4.1.0   | dev    | MIT                                 | permissive |
| @yeoman/namespace                           | 2.1.0   | dev    | MIT                                 | permissive |
| @yeoman/transform                           | 2.1.2   | dev    | MIT                                 | permissive |
| @yeoman/types                               | 1.13.0  | dev    | MIT                                 | permissive |
| abbrev                                      | 4.0.0   | dev    | ISC                                 | permissive |
| acorn                                       | 8.19.0  | dev    | MIT                                 | permissive |
| acorn-jsx                                   | 5.3.2   | dev    | MIT                                 | permissive |
| agent-base                                  | 7.1.4   | dev    | MIT                                 | permissive |
| ajv                                         | 6.15.0  | dev    | MIT                                 | permissive |
| ansi-regex                                  | 6.4.0   | dev    | MIT                                 | permissive |
| anynum                                      | 1.0.1   | dev    | MIT                                 | permissive |
| array-differ                                | 4.0.0   | dev    | MIT                                 | permissive |
| array-union                                 | 3.0.1   | dev    | MIT                                 | permissive |
| arrify                                      | 3.0.0   | dev    | MIT                                 | permissive |
| b4a                                         | 1.9.0   | dev    | Apache-2.0                          | permissive |
| balanced-match                              | 4.0.4   | dev    | MIT                                 | permissive |
| bare-events                                 | 2.9.2   | dev    | Apache-2.0                          | permissive |
| bin-links                                   | 6.0.2   | dev    | ISC                                 | permissive |
| binary-extensions                           | 3.2.0   | dev    | MIT                                 | permissive |
| brace-expansion                             | 5.0.12  | dev    | MIT                                 | permissive |
| braces                                      | 3.0.3   | dev    | MIT                                 | permissive |
| cacache                                     | 20.0.4  | dev    | ISC                                 | permissive |
| chalk                                       | 5.6.2   | dev    | MIT                                 | permissive |
| chalk                                       | 6.0.0   | dev    | MIT                                 | permissive |
| chalk                                       | 5.6.2   | dev    | MIT                                 | permissive |
| chalk                                       | 5.6.2   | dev    | MIT                                 | permissive |
| chalk                                       | 5.6.2   | dev    | MIT                                 | permissive |
| chardet                                     | 2.2.0   | dev    | MIT                                 | permissive |
| chevrotain                                  | 13.2.0  | dev    | Apache-2.0                          | permissive |
| cli-cursor                                  | 5.0.0   | dev    | MIT                                 | permissive |
| cli-spinners                                | 3.4.0   | dev    | MIT                                 | permissive |
| cli-width                                   | 4.1.0   | dev    | ISC                                 | permissive |
| clone                                       | 2.1.2   | dev    | MIT                                 | permissive |
| cmd-shim                                    | 8.0.0   | dev    | ISC                                 | permissive |
| colors                                      | 1.0.3   | dev    | MIT                                 | permissive |
| commander                                   | 15.0.0  | dev    | MIT                                 | permissive |
| commander                                   | 14.0.3  | dev    | MIT                                 | permissive |
| comment-parser                              | 1.4.9   | dev    | MIT                                 | permissive |
| commondir                                   | 1.0.1   | dev    | MIT                                 | permissive |
| config-chain                                | 1.1.13  | dev    | MIT                                 | permissive |
| content-type                                | 2.1.0   | dev    | MIT                                 | permissive |
| cross-spawn                                 | 7.0.6   | dev    | MIT                                 | permissive |
| cssesc                                      | 3.0.0   | dev    | MIT                                 | permissive |
| dateformat                                  | 5.0.3   | dev    | MIT                                 | permissive |
| debug                                       | 4.4.3   | dev    | MIT                                 | permissive |
| deep-extend                                 | 0.6.0   | dev    | MIT                                 | permissive |
| deep-is                                     | 0.1.4   | dev    | MIT                                 | permissive |
| detect-indent                               | 7.0.2   | dev    | MIT                                 | permissive |
| detect-newline                              | 4.0.1   | dev    | MIT                                 | permissive |
| didyoumean                                  | 1.2.2   | dev    | Apache-2.0                          | permissive |
| diff                                        | 9.0.0   | dev    | BSD-3-Clause                        | permissive |
| dockerfile-ast                              | 0.7.1   | dev    | MIT                                 | permissive |
| dot-properties                              | 1.1.1   | dev    | MIT                                 | permissive |
| drange                                      | 1.1.1   | dev    | MIT                                 | permissive |
| ejs                                         | 6.0.1   | dev    | Apache-2.0                          | permissive |
| env-paths                                   | 4.0.0   | dev    | MIT                                 | permissive |
| env-paths                                   | 2.2.1   | dev    | MIT                                 | permissive |
| escape-string-regexp                        | 4.0.0   | dev    | MIT                                 | permissive |
| eslint                                      | 10.9.1  | dev    | MIT                                 | permissive |
| eslint-import-context                       | 0.1.9   | dev    | MIT                                 | permissive |
| eslint-plugin-import-x                      | 4.17.1  | dev    | MIT                                 | permissive |
| eslint-plugin-java-lang                     | 0.6.0   | dev    | MIT                                 | permissive |
| eslint-plugin-unused-imports                | 4.4.1   | dev    | MIT                                 | permissive |
| eslint-scope                                | 9.1.2   | dev    | BSD-2-Clause                        | permissive |
| eslint-visitor-keys                         | 3.4.3   | dev    | Apache-2.0                          | permissive |
| eslint-visitor-keys                         | 5.0.1   | dev    | Apache-2.0                          | permissive |
| espree                                      | 11.2.0  | dev    | BSD-2-Clause                        | permissive |
| esquery                                     | 1.7.0   | dev    | BSD-3-Clause                        | permissive |
| esrecurse                                   | 4.3.0   | dev    | BSD-2-Clause                        | permissive |
| estraverse                                  | 5.3.0   | dev    | BSD-2-Clause                        | permissive |
| esutils                                     | 2.0.3   | dev    | BSD-2-Clause                        | permissive |
| eventemitter3                               | 5.0.4   | dev    | MIT                                 | permissive |
| events-universal                            | 1.0.1   | dev    | Apache-2.0                          | permissive |
| execa                                       | 10.0.1  | dev    | MIT                                 | permissive |
| execa                                       | 9.6.1   | dev    | MIT                                 | permissive |
| execa                                       | 9.6.1   | dev    | MIT                                 | permissive |
| exponential-backoff                         | 3.1.3   | dev    | Apache-2.0                          | permissive |
| fast-deep-equal                             | 3.1.3   | dev    | MIT                                 | permissive |
| fast-fifo                                   | 1.3.2   | dev    | MIT                                 | permissive |
| fast-glob                                   | 3.3.3   | dev    | MIT                                 | permissive |
| fast-json-stable-stringify                  | 2.1.0   | dev    | MIT                                 | permissive |
| fast-levenshtein                            | 2.0.6   | dev    | MIT                                 | permissive |
| fast-string-truncated-width                 | 3.0.3   | dev    | MIT                                 | permissive |
| fast-string-width                           | 3.0.2   | dev    | MIT                                 | permissive |
| fast-wrap-ansi                              | 0.2.2   | dev    | MIT                                 | permissive |
| fast-xml-builder                            | 1.3.1   | dev    | MIT                                 | permissive |
| fast-xml-parser                             | 5.11.0  | dev    | MIT                                 | permissive |
| fastq                                       | 1.20.3  | dev    | ISC                                 | permissive |
| fdir                                        | 6.5.0   | dev    | MIT                                 | permissive |
| figures                                     | 6.1.0   | dev    | MIT                                 | permissive |
| file-entry-cache                            | 8.0.0   | dev    | MIT                                 | permissive |
| fill-range                                  | 7.1.1   | dev    | MIT                                 | permissive |
| find-up                                     | 5.0.0   | dev    | MIT                                 | permissive |
| find-up                                     | 7.0.0   | dev    | MIT                                 | permissive |
| find-up-simple                              | 1.0.1   | dev    | MIT                                 | permissive |
| first-chunk-stream                          | 5.0.0   | dev    | MIT                                 | permissive |
| flat-cache                                  | 4.0.1   | dev    | MIT                                 | permissive |
| flatted                                     | 3.4.4   | dev    | ISC                                 | permissive |
| fly-import                                  | 1.2.0   | dev    | MIT                                 | permissive |
| fs-minipass                                 | 3.0.3   | dev    | ISC                                 | permissive |
| generator-jhipster                          | 9.3.0   | dev    | Apache-2.0                          | permissive |
| get-east-asian-width                        | 1.7.0   | dev    | MIT                                 | permissive |
| get-stream                                  | 9.0.1   | dev    | MIT                                 | permissive |
| get-tsconfig                                | 4.14.3  | dev    | MIT                                 | permissive |
| git-hooks-list                              | 4.2.1   | dev    | MIT                                 | permissive |
| glob-parent                                 | 5.1.2   | dev    | ISC                                 | permissive |
| glob-parent                                 | 6.0.2   | dev    | ISC                                 | permissive |
| globals                                     | 17.11.0 | dev    | MIT                                 | permissive |
| globby                                      | 16.2.4  | dev    | MIT                                 | permissive |
| graceful-fs                                 | 4.2.10  | dev    | ISC                                 | permissive |
| grouped-queue                               | 2.1.0   | dev    | MIT                                 | permissive |
| hosted-git-info                             | 9.0.3   | dev    | ISC                                 | permissive |
| http-cache-semantics                        | 4.3.0   | dev    | BSD-2-Clause                        | permissive |
| http-proxy-agent                            | 7.0.2   | dev    | MIT                                 | permissive |
| https-proxy-agent                           | 7.0.6   | dev    | MIT                                 | permissive |
| human-signals                               | 8.0.1   | dev    | Apache-2.0                          | permissive |
| husky                                       | 9.1.7   | dev    | MIT                                 | permissive |
| iconv-lite                                  | 0.7.3   | dev    | MIT                                 | permissive |
| ignore                                      | 7.0.12  | dev    | MIT                                 | permissive |
| ignore                                      | 7.0.12  | dev    | MIT                                 | permissive |
| ignore                                      | 5.3.2   | dev    | MIT                                 | permissive |
| ignore-walk                                 | 8.0.0   | dev    | ISC                                 | permissive |
| imurmurhash                                 | 0.1.4   | dev    | MIT                                 | permissive |
| index-to-position                           | 1.2.0   | dev    | MIT                                 | permissive |
| ini                                         | 6.0.0   | dev    | ISC                                 | permissive |
| ini                                         | 5.0.0   | dev    | ISC                                 | permissive |
| ini                                         | 1.3.8   | dev    | ISC                                 | permissive |
| inquirer                                    | 13.4.3  | dev    | MIT                                 | permissive |
| ip-address                                  | 10.7.3  | dev    | MIT                                 | permissive |
| is-extglob                                  | 2.1.1   | dev    | MIT                                 | permissive |
| is-glob                                     | 4.0.3   | dev    | MIT                                 | permissive |
| is-interactive                              | 2.0.0   | dev    | MIT                                 | permissive |
| is-number                                   | 7.0.0   | dev    | MIT                                 | permissive |
| is-path-inside                              | 4.0.0   | dev    | MIT                                 | permissive |
| is-plain-obj                                | 4.1.0   | dev    | MIT                                 | permissive |
| is-safe-filename                            | 0.1.1   | dev    | MIT                                 | permissive |
| is-stream                                   | 4.0.1   | dev    | MIT                                 | permissive |
| is-unicode-supported                        | 2.1.0   | dev    | MIT                                 | permissive |
| is-unsafe                                   | 2.0.2   | dev    | MIT                                 | permissive |
| is-utf8                                     | 0.2.1   | dev    | MIT                                 | permissive |
| isbinaryfile                                | 5.0.7   | dev    | MIT                                 | permissive |
| isbinaryfile                                | 5.0.0   | dev    | MIT                                 | permissive |
| isbinaryfile                                | 5.0.7   | dev    | MIT                                 | permissive |
| isexe                                       | 2.0.0   | dev    | ISC                                 | permissive |
| js-tokens                                   | 4.0.0   | dev    | MIT                                 | permissive |
| json-buffer                                 | 3.0.1   | dev    | MIT                                 | permissive |
| json-parse-even-better-errors               | 5.0.0   | dev    | MIT                                 | permissive |
| json-schema-traverse                        | 0.4.1   | dev    | MIT                                 | permissive |
| json-stable-stringify-without-jsonify       | 1.0.1   | dev    | MIT                                 | permissive |
| json-stringify-nice                         | 1.1.4   | dev    | ISC                                 | permissive |
| jsonparse                                   | 1.3.1   | dev    | MIT                                 | permissive |
| just-diff                                   | 6.0.2   | dev    | MIT                                 | permissive |
| just-diff-apply                             | 5.5.0   | dev    | MIT                                 | permissive |
| keyv                                        | 4.5.4   | dev    | MIT                                 | permissive |
| ky                                          | 1.14.3  | dev    | MIT                                 | permissive |
| latest-version                              | 9.0.0   | dev    | MIT                                 | permissive |
| levn                                        | 0.4.1   | dev    | MIT                                 | permissive |
| lint-staged                                 | 17.3.0  | dev    | MIT                                 | permissive |
| locate-path                                 | 6.0.0   | dev    | MIT                                 | permissive |
| locate-path                                 | 7.2.0   | dev    | MIT                                 | permissive |
| locate-path                                 | 8.0.0   | dev    | MIT                                 | permissive |
| lodash-es                                   | 4.18.1  | dev    | MIT                                 | permissive |
| log-symbols                                 | 7.0.1   | dev    | MIT                                 | permissive |
| make-fetch-happen                           | 15.0.6  | dev    | ISC                                 | permissive |
| mem-fs                                      | 4.1.5   | dev    | MIT                                 | permissive |
| mem-fs-editor                               | 12.0.8  | dev    | MIT                                 | permissive |
| merge2                                      | 1.4.1   | dev    | MIT                                 | permissive |
| micromatch                                  | 4.0.8   | dev    | MIT                                 | permissive |
| mimic-function                              | 5.0.1   | dev    | MIT                                 | permissive |
| minimist                                    | 1.2.8   | dev    | MIT                                 | permissive |
| minipass                                    | 3.3.6   | dev    | ISC                                 | permissive |
| minipass                                    | 3.3.6   | dev    | ISC                                 | permissive |
| minipass-collect                            | 2.0.1   | dev    | ISC                                 | permissive |
| minipass-fetch                              | 5.0.2   | dev    | MIT                                 | permissive |
| minipass-pipeline                           | 1.2.4   | dev    | ISC                                 | permissive |
| minipass-sized                              | 2.0.0   | dev    | ISC                                 | permissive |
| minizlib                                    | 3.1.0   | dev    | MIT                                 | permissive |
| ms                                          | 2.1.3   | dev    | MIT                                 | permissive |
| multimatch                                  | 8.0.0   | dev    | MIT                                 | permissive |
| mute-stream                                 | 3.0.0   | dev    | ISC                                 | permissive |
| napi-postinstall                            | 0.3.4   | dev    | MIT                                 | permissive |
| natural-compare                             | 1.4.0   | dev    | MIT                                 | permissive |
| negotiator                                  | 1.1.0   | dev    | MIT                                 | permissive |
| node-gyp                                    | 12.4.0  | dev    | MIT                                 | permissive |
| nopt                                        | 9.0.0   | dev    | ISC                                 | permissive |
| normalize-package-data                      | 8.0.0   | dev    | BSD-2-Clause                        | permissive |
| normalize-path                              | 3.0.0   | dev    | MIT                                 | permissive |
| npm-bundled                                 | 5.0.0   | dev    | ISC                                 | permissive |
| npm-install-checks                          | 8.0.0   | dev    | BSD-2-Clause                        | permissive |
| npm-normalize-package-bin                   | 5.0.0   | dev    | ISC                                 | permissive |
| npm-package-arg                             | 13.0.2  | dev    | ISC                                 | permissive |
| npm-packlist                                | 10.0.4  | dev    | ISC                                 | permissive |
| npm-pick-manifest                           | 11.0.3  | dev    | ISC                                 | permissive |
| npm-registry-fetch                          | 19.1.1  | dev    | ISC                                 | permissive |
| npm-run-path                                | 6.0.0   | dev    | MIT                                 | permissive |
| onetime                                     | 7.0.0   | dev    | MIT                                 | permissive |
| optionator                                  | 0.9.4   | dev    | MIT                                 | permissive |
| ora                                         | 9.4.1   | dev    | MIT                                 | permissive |
| os-locale                                   | 8.0.0   | dev    | MIT                                 | permissive |
| p-limit                                     | 3.1.0   | dev    | MIT                                 | permissive |
| p-limit                                     | 4.0.0   | dev    | MIT                                 | permissive |
| p-limit                                     | 4.0.0   | dev    | MIT                                 | permissive |
| p-locate                                    | 5.0.0   | dev    | MIT                                 | permissive |
| p-locate                                    | 6.0.0   | dev    | MIT                                 | permissive |
| p-locate                                    | 6.0.0   | dev    | MIT                                 | permissive |
| p-map                                       | 7.0.8   | dev    | MIT                                 | permissive |
| p-queue                                     | 9.3.3   | dev    | MIT                                 | permissive |
| p-queue                                     | 8.1.1   | dev    | MIT                                 | permissive |
| p-timeout                                   | 7.0.2   | dev    | MIT                                 | permissive |
| p-timeout                                   | 6.1.4   | dev    | MIT                                 | permissive |
| p-transform                                 | 5.0.1   | dev    | Apache-2.0                          | permissive |
| package-json                                | 10.0.1  | dev    | MIT                                 | permissive |
| pacote                                      | 21.5.1  | dev    | ISC                                 | permissive |
| parse-conflict-json                         | 5.0.1   | dev    | ISC                                 | permissive |
| parse-gitignore                             | 2.0.0   | dev    | MIT                                 | permissive |
| parse-json                                  | 8.3.0   | dev    | MIT                                 | permissive |
| parse-ms                                    | 4.0.0   | dev    | MIT                                 | permissive |
| path-exists                                 | 4.0.0   | dev    | MIT                                 | permissive |
| path-exists                                 | 5.0.0   | dev    | MIT                                 | permissive |
| path-expression-matcher                     | 1.6.2   | dev    | MIT                                 | permissive |
| path-key                                    | 4.0.0   | dev    | MIT                                 | permissive |
| path-key                                    | 3.1.1   | dev    | MIT                                 | permissive |
| picocolors                                  | 1.1.1   | dev    | ISC                                 | permissive |
| picomatch                                   | 2.3.2   | dev    | MIT                                 | permissive |
| picomatch                                   | 4.0.7   | dev    | MIT                                 | permissive |
| piscina                                     | 5.3.1   | dev    | MIT                                 | permissive |
| pluralize                                   | 8.0.0   | dev    | MIT                                 | permissive |
| postcss-selector-parser                     | 7.1.6   | dev    | MIT                                 | permissive |
| prelude-ls                                  | 1.2.1   | dev    | MIT                                 | permissive |
| prettier                                    | 3.9.6   | dev    | MIT                                 | permissive |
| prettier-plugin-java                        | 2.10.3  | dev    | Apache-2.0                          | permissive |
| prettier-plugin-packagejson                 | 3.0.2   | dev    | MIT                                 | permissive |
| prettier-plugin-properties                  | 0.3.1   | dev    | MIT                                 | permissive |
| pretty-bytes                                | 7.2.0   | dev    | MIT                                 | permissive |
| pretty-ms                                   | 9.3.1   | dev    | MIT                                 | permissive |
| proc-log                                    | 6.1.0   | dev    | ISC                                 | permissive |
| proggy                                      | 4.0.0   | dev    | ISC                                 | permissive |
| promise-all-reject-late                     | 1.0.1   | dev    | ISC                                 | permissive |
| promise-call-limit                          | 3.0.2   | dev    | ISC                                 | permissive |
| proto-list                                  | 1.2.4   | dev    | ISC                                 | permissive |
| punycode                                    | 2.3.1   | dev    | MIT                                 | permissive |
| queue-microtask                             | 1.2.3   | dev    | MIT                                 | permissive |
| randexp                                     | 0.5.3   | dev    | MIT                                 | permissive |
| rc                                          | 1.2.8   | dev    | (BSD-2-Clause OR MIT OR Apache-2.0) | permissive |
| read-cmd-shim                               | 6.0.0   | dev    | ISC                                 | permissive |
| read-package-up                             | 12.0.0  | dev    | MIT                                 | permissive |
| read-pkg                                    | 10.1.0  | dev    | MIT                                 | permissive |
| registry-auth-token                         | 5.1.1   | dev    | MIT                                 | permissive |
| registry-url                                | 7.2.0   | dev    | MIT                                 | permissive |
| registry-url                                | 6.0.1   | dev    | MIT                                 | permissive |
| remove-trailing-separator                   | 1.1.0   | dev    | ISC                                 | permissive |
| replace-ext                                 | 2.0.0   | dev    | MIT                                 | permissive |
| resolve-pkg-maps                            | 1.0.0   | dev    | MIT                                 | permissive |
| restore-cursor                              | 5.1.0   | dev    | MIT                                 | permissive |
| ret                                         | 0.2.2   | dev    | MIT                                 | permissive |
| reusify                                     | 1.1.0   | dev    | MIT                                 | permissive |
| run-async                                   | 4.0.6   | dev    | MIT                                 | permissive |
| run-parallel                                | 1.2.0   | dev    | MIT                                 | permissive |
| rxjs                                        | 7.8.2   | dev    | Apache-2.0                          | permissive |
| safer-buffer                                | 2.1.2   | dev    | MIT                                 | permissive |
| semver                                      | 7.8.5   | dev    | ISC                                 | permissive |
| shebang-command                             | 2.0.0   | dev    | MIT                                 | permissive |
| shebang-regex                               | 3.0.0   | dev    | MIT                                 | permissive |
| signal-exit                                 | 4.1.0   | dev    | ISC                                 | permissive |
| sigstore                                    | 4.1.1   | dev    | Apache-2.0                          | permissive |
| simple-git                                  | 3.36.0  | dev    | MIT                                 | permissive |
| slash                                       | 5.1.0   | dev    | MIT                                 | permissive |
| smart-buffer                                | 4.2.0   | dev    | MIT                                 | permissive |
| socks                                       | 2.8.10  | dev    | MIT                                 | permissive |
| socks-proxy-agent                           | 8.0.5   | dev    | MIT                                 | permissive |
| sort-keys                                   | 6.0.1   | dev    | MIT                                 | permissive |
| sort-object-keys                            | 2.1.0   | dev    | MIT                                 | permissive |
| sort-package-json                           | 3.7.1   | dev    | MIT                                 | permissive |
| spdx-correct                                | 3.2.0   | dev    | Apache-2.0                          | permissive |
| spdx-exceptions                             | 2.5.0   | dev    | CC-BY-3.0                           | permissive |
| spdx-expression-parse                       | 3.0.1   | dev    | MIT                                 | permissive |
| spdx-expression-parse                       | 4.0.0   | dev    | MIT                                 | permissive |
| spdx-expression-parse                       | 3.0.1   | dev    | MIT                                 | permissive |
| spdx-license-ids                            | 3.0.24  | dev    | CC0-1.0                             | permissive |
| ssri                                        | 13.0.1  | dev    | ISC                                 | permissive |
| stable-hash-x                               | 0.2.0   | dev    | MIT                                 | permissive |
| stdin-discarder                             | 0.3.2   | dev    | MIT                                 | permissive |
| streamx                                     | 2.28.1  | dev    | MIT                                 | permissive |
| string-argv                                 | 0.3.2   | dev    | MIT                                 | permissive |
| string-width                                | 8.3.0   | dev    | MIT                                 | permissive |
| strip-ansi                                  | 7.2.0   | dev    | MIT                                 | permissive |
| strip-bom-buf                               | 3.0.1   | dev    | MIT                                 | permissive |
| strip-bom-stream                            | 5.0.0   | dev    | MIT                                 | permissive |
| strip-final-newline                         | 4.0.0   | dev    | MIT                                 | permissive |
| strip-json-comments                         | 2.0.1   | dev    | MIT                                 | permissive |
| strnum                                      | 2.4.2   | dev    | MIT                                 | permissive |
| tagged-tag                                  | 1.0.0   | dev    | MIT                                 | permissive |
| teex                                        | 1.0.1   | dev    | MIT                                 | permissive |
| text-decoder                                | 1.2.7   | dev    | Apache-2.0                          | permissive |
| text-table                                  | 0.2.0   | dev    | MIT                                 | permissive |
| tinyexec                                    | 1.3.1   | dev    | MIT                                 | permissive |
| tinyglobby                                  | 0.2.17  | dev    | MIT                                 | permissive |
| to-regex-range                              | 5.0.1   | dev    | MIT                                 | permissive |
| treeverse                                   | 3.0.0   | dev    | ISC                                 | permissive |
| ts-api-utils                                | 2.5.0   | dev    | MIT                                 | permissive |
| tslib                                       | 2.8.1   | dev    | 0BSD                                | permissive |
| tuf-js                                      | 4.1.0   | dev    | MIT                                 | permissive |
| type-check                                  | 0.4.0   | dev    | MIT                                 | permissive |
| type-fest                                   | 4.41.0  | dev    | (MIT OR CC0-1.0)                    | permissive |
| type-fest                                   | 5.8.0   | dev    | (MIT OR CC0-1.0)                    | permissive |
| typescript                                  | 6.0.3   | dev    | Apache-2.0                          | permissive |
| typescript-eslint                           | 8.68.0  | dev    | MIT                                 | permissive |
| undici                                      | 6.29.0  | dev    | MIT                                 | permissive |
| undici-types                                | 8.9.0   | dev    | MIT                                 | permissive |
| unicorn-magic                               | 0.4.1   | dev    | MIT                                 | permissive |
| unicorn-magic                               | 0.4.1   | dev    | MIT                                 | permissive |
| unicorn-magic                               | 0.3.0   | dev    | MIT                                 | permissive |
| unicorn-magic                               | 0.1.0   | dev    | MIT                                 | permissive |
| unrs-resolver                               | 1.12.2  | dev    | MIT                                 | permissive |
| untildify                                   | 6.0.0   | dev    | MIT                                 | permissive |
| uri-js                                      | 4.4.1   | dev    | BSD-2-Clause                        | permissive |
| util-deprecate                              | 1.0.2   | dev    | MIT                                 | permissive |
| validate-npm-package-license                | 3.0.4   | dev    | Apache-2.0                          | permissive |
| validate-npm-package-name                   | 7.0.2   | dev    | ISC                                 | permissive |
| vinyl                                       | 3.0.1   | dev    | MIT                                 | permissive |
| vinyl-file                                  | 5.0.0   | dev    | MIT                                 | permissive |
| vscode-languageserver-textdocument          | 1.0.15  | dev    | MIT                                 | permissive |
| vscode-languageserver-types                 | 3.18.4  | dev    | MIT                                 | permissive |
| walk-up-path                                | 4.0.0   | dev    | ISC                                 | permissive |
| web-tree-sitter                             | 0.26.11 | dev    | MIT                                 | permissive |
| web-tree-sitter                             | 0.26.13 | dev    | MIT                                 | permissive |
| which                                       | 6.0.1   | dev    | ISC                                 | permissive |
| which                                       | 6.0.1   | dev    | ISC                                 | permissive |
| which                                       | 6.0.1   | dev    | ISC                                 | permissive |
| which                                       | 2.0.2   | dev    | ISC                                 | permissive |
| which-command                               | 0.1.0   | dev    | MIT                                 | permissive |
| which-package-manager                       | 1.0.1   | dev    | MIT                                 | permissive |
| word-wrap                                   | 1.2.5   | dev    | MIT                                 | permissive |
| write-file-atomic                           | 7.0.1   | dev    | ISC                                 | permissive |
| xml-naming                                  | 0.3.0   | dev    | MIT                                 | permissive |
| yallist                                     | 4.0.0   | dev    | ISC                                 | permissive |
| yallist                                     | 4.0.0   | dev    | ISC                                 | permissive |
| yaml                                        | 2.9.0   | dev    | ISC                                 | permissive |
| yeoman-environment                          | 6.2.0   | dev    | BSD-2-Clause                        | permissive |
| yeoman-generator                            | 8.3.0   | dev    | BSD-2-Clause                        | permissive |
| yocto-queue                                 | 1.2.2   | dev    | MIT                                 | permissive |
| yocto-queue                                 | 1.2.2   | dev    | MIT                                 | permissive |
| yocto-queue                                 | 0.1.0   | dev    | MIT                                 | permissive |
| yoctocolors                                 | 2.2.0   | dev    | MIT                                 | permissive |
