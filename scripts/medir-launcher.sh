#!/usr/bin/env bash
# Mede abertura a frio e memória (PSS) de um launcher, várias rodadas.
# Uso: ./medir-launcher.sh pacote/.Atividade [rodadas] [IP:PORTA]
set -euo pipefail
ALVO="${1:?Uso: $0 pacote/.Atividade [rodadas] [IP:PORTA]}"
N="${2:-3}"
DEV="${3:-192.168.2.100:5555}"
PKG="${ALVO%%/*}"
A=(adb -s "$DEV" shell)

for i in $(seq 1 "$N"); do
  "${A[@]}" am force-stop "$PKG"
  T=$("${A[@]}" am start -W -n "$ALVO" | awk -F': ' '/TotalTime/ {print $2}' | tr -d '\r')
  sleep 8
  PSS=$("${A[@]}" dumpsys meminfo "$PKG" | awk '/TOTAL PSS:/ {print $3}' | tr -d '\r')
  echo "rodada $i: abertura ${T} ms, memória $((PSS / 1024)) MB (${PSS} KB)"
done
CPU=$("${A[@]}" top -b -n 1 | awk -v p="$PKG" '$NF == p {print $9}')
echo "CPU parado: ${CPU:-0}%"
