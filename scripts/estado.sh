#!/usr/bin/env bash
# Muestra el estado de los ambientes.
set -euo pipefail

echo "== Contenedores =="
docker ps --filter "name=jenkins-catalogo" --filter "name=catalogo-prod" \
    --format "table {{.Names}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}"

echo
echo "== Salud de produccion =="
curl -fs http://localhost:8091/actuator/health || echo "produccion no responde"
echo
echo "Jenkins:     http://localhost:8080"
echo "Produccion:  http://localhost:8091"
