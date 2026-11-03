#!/usr/bin/env bash
# Manda as fotos de fundo para a TV da mãe, para a pasta do app. Gerar antes no tamanho da tela (1280x720) e em JPEG
# qualidade 95 sem subamostragem de cor: em qualidade 75 os céus e degradês escuros ficam com blocos (ALTERACOES.md, item 13).
# Uso: ./enviar-fotos.sh [pasta]   (padrão: ../wallpapers; endereço em DEV=IP:PORTA ou procura na rede)
# As fotos ficam no aparelho, não dentro do app: trocar, tirar ou pôr outra é só mexer na pasta e mandar de novo.
set -euo pipefail
DIR="$(cd "$(dirname "$0")" && pwd)"
ORIGEM="${1:-$DIR/../wallpapers}"
DEV="${DEV:-$(bash "$DIR/achar-tv.sh")}"
PASTA="/sdcard/Android/data/br.gui.tvmae/files/wallpapers"
adb -s "$DEV" shell mkdir -p "$PASTA"
for f in "$ORIGEM"/*.jpg; do
  adb -s "$DEV" push -q "$f" "$PASTA/$(basename "$f")" >/dev/null
  echo "enviada: $(basename "$f")"
done
