#!/usr/bin/env bash
# Detiene Jenkins y el ambiente de produccion simulado.
set -euo pipefail

cd "$(dirname "$0")/.."
docker rm -f catalogo-prod >/dev/null 2>&1 || true
(cd jenkins && docker compose down)
echo "Ambientes detenidos (los volumenes de datos se conservan)."
