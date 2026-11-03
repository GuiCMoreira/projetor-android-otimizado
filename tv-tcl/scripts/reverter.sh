#!/usr/bin/env bash
# Desfaz a limpeza da TV da avó (ALTERACOES.md). Uso: ./reverter.sh   (endereço em DEV=IP:PORTA ou procura na rede)
set -u
DIR="$(cd "$(dirname "$0")" && pwd)"
DEV="${DEV:-$(bash "$DIR/achar-tv.sh")}" || exit 1
A=(adb -s "$DEV" shell)

for p in com.google.android.apps.mediashell com.google.android.katniss com.google.android.tvrecommendations \
         com.google.android.backdrop com.amazon.amazonvideo.livingroom com.tcl.smartalexa com.tcl.t_solo \
         com.tcl.ui_mediaCenter com.tcl.esticker com.tcl.usercenter com.google.android.inputmethod.latin \
         com.google.android.tts com.google.android.tungsten.setupwraith com.google.android.videos \
         com.tcl.waterfall.overseas; do
  "${A[@]}" pm enable "$p"
done
"${A[@]}" settings put secure screensaver_enabled 1
"${A[@]}" settings put global window_animation_scale 1.0
"${A[@]}" settings put global transition_animation_scale 1.0
"${A[@]}" settings delete global animator_duration_scale
# Netflix volta a poder rodar em segundo plano (linha 17).
"${A[@]}" cmd appops set com.netflix.ninja RUN_ANY_IN_BACKGROUND default
"${A[@]}" cmd appops write-settings
# Tela do Google de volta e a tela nova removida (linha 11 do ALTERACOES.md).
"${A[@]}" pm enable com.google.android.tvlauncher
adb -s "$DEV" uninstall br.gui.tvavo
echo "Pronto. A atualização do Chromecast volta pela Play Store; a atualização automática da Play Store se religa"
echo "à mão (Play Store > Configurações > Atualizar apps automaticamente)."
