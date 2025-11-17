# ===== 1. Build Stage =====
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn -e -X -DskipTests=true clean package

# ===== 2. Runtime Stage =====
FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
COPY Mustang-CLI-2.20.0.jar /app/mustang.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
