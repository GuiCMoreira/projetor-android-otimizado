#!/usr/bin/env bash
# O "Liberar memória" ajuda de verdade? Compara, depois de usar um app, sem e com o fechamento dos apps parados:
# memória livre na hora e 2 minutos depois, e quanto o app leva para abrir de novo.
# Uso: ./avaliar-liberar-memoria.sh [pacote]   (padrão tv.pluto.android; endereço em DEV=IP:PORTA ou procura na rede)
# O "com" imita o botão: am kill em cada app instalado por você, que é o mesmo killBackgroundProcesses do launcher.
# Mexe na tela (abre o app e volta para o Início). Não usar com alguém assistindo.
set -euo pipefail
DIR="$(cd "$(dirname "$0")" && pwd)"
APP="${1:-tv.pluto.android}"
DEV="${DEV:-$(bash "$DIR/achar-projetor.sh")}"
A=(adb -s "$DEV" shell)

livre() { "${A[@]}" cat /proc/meminfo | awk '/MemAvailable/ {print int($2 / 1024)}'; }
home() { "${A[@]}" am start -a android.intent.action.MAIN -c android.intent.category.HOME >/dev/null; }
abrir() { "${A[@]}" monkey -p "$APP" -c android.intent.category.LEANBACK_LAUNCHER 1 >/dev/null 2>&1; }
tempo_abrir() {
  local t0 t1
  t0=$(date +%s%N)
  abrir
  # Espera o app estar na frente (o monkey volta antes de ele desenhar).
  until "${A[@]}" dumpsys activity activities | grep -m1 'topResumedActivity' | grep -q "$APP"; do sleep 0.2; done
  t1=$(date +%s%N)
  echo $(((t1 - t0) / 1000000))
}
fechar_parados() {
  for p in $("${A[@]}" pm list packages -3 | tr -d '\r' | sed 's/package://'); do
    [ "$p" = br.gui.inicio ] && continue
    "${A[@]}" am kill "$p"
  done
}

rodada() {
  local nome="$1" limpar="$2"
  abrir
  sleep 25
  home
  sleep 3
  local antes depois tarde t
  antes=$(livre)
  [ "$limpar" = 1 ] && fechar_parados
  sleep 2
  depois=$(livre)
  sleep 120
  tarde=$(livre)
  t=$(tempo_abrir)
  home
  printf '%-4s livre ao voltar %4d MB | logo depois %4d MB | 2 min depois %4d MB | reabrir %s: %5d ms\n' \
    "$nome" "$antes" "$depois" "$tarde" "$APP" "$t"
}

echo "Projetor $DEV, app $APP"
rodada sem 0
rodada com 1
