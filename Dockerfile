# ============================
# Stage 1: build con Maven
# ============================
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copiamos primero solo los archivos de Maven para aprovechar el cache de capas
# cuando cambia el código pero no las dependencias.
COPY pom.xml mvnw mvnw.cmd ./
COPY .mvn .mvn
RUN mvn -q -B -e -ntp dependency:go-offline

# Ahora sí copiamos el código y empacamos.
COPY src src
RUN mvn -q -B -e -ntp -DskipTests package

# ============================
# Stage 2: runtime liviano
# ============================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Crea un usuario no-root por seguridad.
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# El nombre del JAR viene del artifactId+version del pom.
COPY --from=build /workspace/target/all-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8081

# Los flags ayudan a que la JVM respete los límites del contenedor.
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-jar", "/app/app.jar"]
