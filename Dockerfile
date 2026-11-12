# Imagen del servidor de Archipiélago: compila con Maven Wrapper y ejecuta el jar de Spring Boot.
# El host de contenedores (Railway en la versión pública) inyecta el puerto en la variable PORT.

# ---- build ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /fuente
COPY mvnw pom.xml ./
COPY .mvn .mvn
COPY motor/pom.xml motor/pom.xml
COPY servidor/pom.xml servidor/pom.xml
RUN chmod +x mvnw
COPY motor/src motor/src
COPY servidor/src servidor/src
# Los tests se ejecutan antes del push (./mvnw verify); aquí solo se empaqueta.
RUN ./mvnw -B -q -pl servidor -am package -DskipTests

# ---- ejecución ----
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 archipielago
COPY --from=build /fuente/servidor/target/archipielago-servidor.jar archipielago.jar
USER archipielago
ENV PORT=8080
EXPOSE 8080
# Ajustes para instancias pequeñas (p. ej. 512 MB): poca memoria y arranque rápido.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-XX:+UseSerialGC", "-XX:TieredStopAtLevel=1", "-jar", "archipielago.jar"]
