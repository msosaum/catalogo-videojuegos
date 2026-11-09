#!/usr/bin/env bash
# Despliega la aplicacion en el ambiente de produccion simulado SIN pasar por
# Jenkins (util para probar la imagen a mano). http://localhost:8091
set -euo pipefail

cd "$(dirname "$0")/.."

echo "==> Compilando y empaquetando"
./mvnw -B -ntp -DskipTests clean package

echo "==> Creando la red de Docker compartida (si no existe)"
docker network inspect catalogo-red >/dev/null 2>&1 || docker network create catalogo-red

echo "==> Construyendo la imagen y levantando el contenedor"
docker compose up -d --build

echo
echo "Produccion simulada: http://localhost:8091"
