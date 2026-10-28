# Etapa 1: compilar el backend Java con Maven
FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /app

COPY backend/pom.xml ./pom.xml
COPY backend/src ./src

RUN mvn -B -DskipTests clean package

# Etapa 2: ejecutar la aplicación con Java 17
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /app/target/rudafer-api-1.0.0.jar app.jar

EXPOSE 10000

CMD ["sh", "-c", "java -XX:MaxRAMPercentage=75.0 -jar app.jar"]
