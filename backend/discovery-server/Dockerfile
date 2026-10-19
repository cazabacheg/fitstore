# Imagen de discovery-server (infraestructura 10.6). Contexto de construcción: la raíz del monorepo.
#   docker build -f backend/discovery-server/Dockerfile -t fitstore/discovery-server .
# Etapa 1: compilación con el Maven Wrapper (JDK 21) y jar en capas
FROM eclipse-temurin:21.0.12.1_1-jdk AS build
WORKDIR /src
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY backend backend
RUN ./mvnw -B -q -pl backend/discovery-server -am -DskipTests -Djacoco.skip=true package \
 && java -Djarmode=tools -jar backend/discovery-server/target/discovery-server.jar extract --layers --launcher --destination /extraido

# Etapa 2: ejecución sobre JRE 21, usuario no root
FROM eclipse-temurin:21.0.12.1_1-jre
RUN useradd --system --uid 10001 --no-create-home fitstore
WORKDIR /app
COPY --from=build /extraido/dependencies/ ./
COPY --from=build /extraido/spring-boot-loader/ ./
COPY --from=build /extraido/snapshot-dependencies/ ./
COPY --from=build /extraido/application/ ./
USER fitstore
ENV JAVA_TOOL_OPTIONS="-Xmx160m -XX:+UseSerialGC"
EXPOSE 8761
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=10 \
  CMD curl -fsS http://localhost:8761/actuator/health || exit 1
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
