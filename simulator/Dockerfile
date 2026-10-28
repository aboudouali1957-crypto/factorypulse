# Étape 1 : compilation avec Maven
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# Étape 2 : image légère qui ne contient que Java et le jar
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/simulator-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
