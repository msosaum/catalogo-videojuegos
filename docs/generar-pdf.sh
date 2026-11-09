#!/usr/bin/env bash
# Genera el informe en PDF a partir de informe.md
set -euo pipefail
cd "$(dirname "$0")"

CHROME="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"

pandoc informe.md \
    --standalone \
    --from markdown \
    --to html5 \
    --css estilo.css \
    --embed-resources \
    --metadata lang=es \
    -o informe.html

"$CHROME" --headless --disable-gpu --no-pdf-header-footer \
    --print-to-pdf="Informe_Entregable4_SosaLapetina.pdf" \
    "file://$(pwd)/informe.html" 2>/dev/null

rm -f informe.html
echo "Generado: docs/Informe_Entregable4_SosaLapetina.pdf"
