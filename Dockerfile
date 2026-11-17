FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml package.json package-lock.json tsconfig.json types.d.ts vite.config.ts ./
COPY .mvn .mvn
RUN --mount=type=cache,target=/root/.m2 mvn -B -Pproduction -DskipTests dependency:go-offline

COPY src src
RUN --mount=type=cache,target=/root/.m2 mvn -B -Pproduction -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

# The application listens on an unprivileged port and writes nothing outside its own temp files,
# so there is no reason for it to run as root: a flaw in the upload or download path would then
# execute as uid 0, which on a default Docker setup is the host's root.
RUN groupadd --system --gid 1001 mish \
 && useradd --system --uid 1001 --gid mish --home /app --shell /usr/sbin/nologin mish

COPY --from=build --chown=mish:mish /build/target/*.jar app.jar
COPY --from=build --chown=mish:mish /build/src/main/webapp ./webapp

USER mish
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
