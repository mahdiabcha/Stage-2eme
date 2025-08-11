# ---- Build stage: utilise Maven (pas besoin de mvnw) ----
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copie le pom d'abord pour mettre en cache les dépendances
COPY pom.xml .
RUN mvn -q -B -DskipTests dependency:go-offline

# Puis le code source
COPY src ./src

# Compile et package
RUN mvn -q -B -DskipTests package

# ---- Runtime stage: JRE léger ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","/app/app.jar"]
