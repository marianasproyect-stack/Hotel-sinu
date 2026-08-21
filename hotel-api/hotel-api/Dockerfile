# Etapa de compilacion
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# Imagen de ejecucion
FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=build /app/target/hotel-api-0.0.1-SNAPSHOT.jar app.jar

ENV PORT=8081
ENV TZ=UTC
ENV JAVA_TOOL_OPTIONS=-Duser.timezone=UTC
EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
