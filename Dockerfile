# Imagen de produccion (Render u otro host con Docker). Dos etapas: compila con Maven y ejecuta solo el JAR.
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src/ src/
RUN ./mvnw -B -q package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd --system --no-create-home app
COPY --from=build /app/target/*.jar app.jar
USER app
# En produccion no hay compose.yaml: la base llega por variables de entorno.
ENV SPRING_DOCKER_COMPOSE_ENABLED=false \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
EXPOSE 8080
# Render inyecta PORT; en local cae a 8080.
ENTRYPOINT ["sh", "-c", "exec java -Dserver.port=${PORT:-8080} -jar app.jar"]
