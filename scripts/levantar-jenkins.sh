#!/usr/bin/env bash
# Levanta el servidor Jenkins (http://localhost:8080, usuario admin / admin).
# Uso:  REPO_URL=https://github.com/usuario/repo.git ./scripts/levantar-jenkins.sh
set -euo pipefail

cd "$(dirname "$0")/.."

echo "==> Creando la red de Docker compartida (si no existe)"
docker network inspect catalogo-red >/dev/null 2>&1 || docker network create catalogo-red

echo "==> Construyendo y levantando Jenkins"
(cd jenkins && docker compose up -d --build)

echo
echo "Jenkins: http://localhost:8080   (admin / admin)"
echo "Puede tardar un minuto en arrancar la primera vez."
