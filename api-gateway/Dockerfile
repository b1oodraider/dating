# syntax=docker/dockerfile:1
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -q dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -q clean package -DskipTests

# --- стадия рантайма ---
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8088
# TODO(security): контейнер бежит от root — добавить непривилегированного пользователя
#  (RUN adduser -D app && USER app). Базовые образы не запинены по digest, .dockerignore нет.
# TODO(ci): jar пересобирается здесь с -DskipTests — образ собран не той сборкой, что прошла тесты.
ENTRYPOINT ["java", "-jar", "app.jar"]