# API Spring Boot : construction en deux etapes (JDK pour compiler, JRE pour executer).
# Contexte : la racine du depot AfriVox (voir deploy/docker-compose.yml).

# ---- Etape 1 : compilation ---------------------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /src
COPY . .
# Un depot extrait sous Windows peut avoir des fins de ligne CRLF : le script ne s'executerait pas sous Linux.
RUN sed -i 's/\r$//' mvnw .mvn/wrapper/maven-wrapper.properties && chmod +x mvnw
# -Pprod : profil Spring 'prod' par defaut, sans devtools ni docker-compose de developpement.
# Tests ignores ici : ils se lancent en amont (CI ou poste de dev), pas a chaque deploiement.
# Le cache Maven est conserve entre deux constructions.
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -ntp -Pprod -Dmaven.test.skip=true -Dmaven.gitcommitid.skip=true package \
    && cp "$(ls target/*.jar | grep -v '\.original$' | head -1)" /app.jar

# ---- Etape 2 : execution -----------------------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-jammy
# curl sert uniquement au controle de sante du conteneur
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 --home-dir /app app \
    && mkdir -p /app /data/audio /data/datasets \
    && chown -R app:app /app /data
WORKDIR /app
COPY --from=build --chown=app:app /app.jar /app/app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar"]
