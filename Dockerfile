# Imagen de ejecucion de la aplicacion.
# Espera que el jar ya haya sido construido por Maven (lo hace el pipeline de Jenkins
# en la etapa "Empaquetar"), de modo que construir la imagen sea cuestion de segundos.
FROM eclipse-temurin:21-jre

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

ARG JAR_FILE=app-web/target/app-web.jar
COPY ${JAR_FILE} /app/app.jar

# Directorio de datos: se monta un volumen encima para que el catalogo
# sobreviva a los despliegues.
RUN mkdir -p /app/datos

ENV SERVER_PORT=8090 \
    CATALOGO_ARCHIVO=/app/datos/juegos.json \
    APP_AMBIENTE=PRODUCCION

EXPOSE 8090

HEALTHCHECK --interval=10s --timeout=3s --start-period=20s --retries=6 \
    CMD curl -fs http://localhost:8090/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
