#!/usr/bin/env bash
# Mede a tela inicial como ela é usada: abrir do zero, voltar de um app, memória estável, fluidez nas setas e CPU parada.
# Uso: ./medir-launcher.sh [pacote] [rodadas]   (padrão br.gui.inicio, 3; endereço em DEV=IP:PORTA ou procura na rede)
# Mede o launcher que está como tela padrão: tudo passa pelo intent do botão Início, porque o gerenciador de memória
# do fabricante derruba a tela inicial aberta direto (am start -n) logo depois de um force-stop.
# Mexe na tela: abre os Ajustes e anda com as setas pela Dock. Não usar com alguém assistindo.
set -euo pipefail
DIR="$(cd "$(dirname "$0")" && pwd)"
PKG="${1:-br.gui.inicio}"
N="${2:-3}"
DEV="${DEV:-$(bash "$DIR/achar-projetor.sh")}"
A=(adb -s "$DEV" shell)

home() { "${A[@]}" am start -W -a android.intent.action.MAIN -c android.intent.category.HOME | awk -F': ' '/TotalTime/ {print $2}' | tr -d '\r'; }
mediana() { sort -n | awk '{v[NR] = $1} END {print v[int((NR + 1) / 2)]}'; }
pss_mb() { "${A[@]}" dumpsys meminfo "$PKG" | awk '/TOTAL PSS:/ {print int($3 / 1024)}' | tr -d '\r'; }

echo "Projetor $DEV, launcher $PKG, $N rodadas"

frio=$(for _ in $(seq 1 "$N"); do
  "${A[@]}" am force-stop "$PKG"
  sleep 2
  home
  sleep 6
done | mediana)
echo "Abertura do zero:            ${frio} ms (mediana)"

quente=$(for _ in $(seq 1 "$N"); do
  "${A[@]}" am start -a android.settings.SETTINGS >/dev/null
  sleep 3
  home
  sleep 3
done | mediana)
echo "Volta de um app (Ajustes):   ${quente} ms (mediana)"

"${A[@]}" dumpsys gfxinfo "$PKG" reset >/dev/null
for tecla in RIGHT RIGHT RIGHT RIGHT LEFT LEFT LEFT LEFT UP DOWN; do
  "${A[@]}" input keyevent "KEYCODE_DPAD_$tecla"
  sleep 0.6
done
"${A[@]}" dumpsys gfxinfo "$PKG" | tr -d '\r' | awk '
  /Total frames rendered/ {print "Setas: quadros desenhados   " $4}
  /Janky frames:/         {print "Setas: quadros travados     " $3 " " $4}
  /90th percentile/       {print "Setas: 90% dos quadros em   " $3}
  /99th percentile/       {print "Setas: 99% dos quadros em   " $3}'

sleep 3
cpu=$("${A[@]}" top -b -d 10 -n 2 -o %CPU,ARGS | awk -v p="$PKG" '$2 == p {c = $1} END {print c + 0}')
echo "CPU com a tela parada:       ${cpu}%"
echo "Memória logo depois:         $(pss_mb) MB"
# F12 a cada 40 s não faz nada na tela, mas reinicia a contagem do descanso de tela (que entraria e mudaria a medida).
for _ in 1 2 3; do sleep 40; "${A[@]}" input keyevent KEYCODE_F12; done
echo "Memória 2 minutos depois:    $(pss_mb) MB"
