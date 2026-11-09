#!/usr/bin/env bash
# Corre todas las pruebas (unitarias + integracion) igual que el pipeline.
set -euo pipefail

cd "$(dirname "$0")/.."
./mvnw -B -ntp clean verify
