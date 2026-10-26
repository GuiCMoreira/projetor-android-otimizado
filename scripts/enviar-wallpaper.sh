#!/usr/bin/env bash
# Envia uma foto para o launcher Início, já recortada e reduzida para a tela do projetor (1920x1080).
# Uso: ./enviar-wallpaper.sh foto.jpg [IP:PORTA]
set -euo pipefail
FOTO="${1:?Uso: $0 foto.jpg [IP:PORTA]}"
DEV="${2:-192.168.2.100:5555}"
PASTA="/sdcard/Android/data/br.gui.inicio/files/wallpapers"

# Nome seguro: só letras, números, ponto, hífen e sublinhado (o launcher ignora outros nomes).
BASE="$(basename "${FOTO%.*}")"
NOME="$(printf '%s' "$BASE" | tr -c 'A-Za-z0-9._-' '_' | sed 's/^[^A-Za-z0-9]*//' | cut -c1-60)"
[ -n "$NOME" ] || NOME="foto"

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
SAIDA="$TMP/${NOME}.jpg"
convert "$FOTO" -auto-orient -resize '1920x1080^' -gravity center -extent 1920x1080 -strip -quality 85 "$SAIDA"

adb connect "$DEV" >/dev/null
adb -s "$DEV" shell mkdir -p "$PASTA"
adb -s "$DEV" push "$SAIDA" "$PASTA/${NOME}.jpg"
echo "Enviada: ${NOME}.jpg. No projetor: Início > Papel de parede > Fotos."
