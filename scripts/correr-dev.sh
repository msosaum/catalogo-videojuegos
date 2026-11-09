#!/usr/bin/env bash
# Levanta la aplicacion en el ambiente de DESARROLLO (http://localhost:8090).
#
# Son dos pasos y no uno: primero hay que instalar los modulos en el repositorio
# local de Maven, porque 'spring-boot:run' sobre app-web resuelve modulo-catalogo
# y modulo-clasificacion desde ahi.
set -euo pipefail

cd "$(dirname "$0")/.."

echo "==> Instalando los modulos en el repositorio local"
./mvnw -q -B -ntp -DskipTests install

echo "==> Levantando la aplicacion en http://localhost:8090 (Ctrl+C para cortar)"
./mvnw -pl app-web -B -ntp spring-boot:run
