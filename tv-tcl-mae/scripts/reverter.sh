#!/usr/bin/env bash
# Desfaz o que foi feito na TV da mãe (ALTERACOES.md). Uso: ./reverter.sh   (endereço em DEV=IP:PORTA ou procura na rede)
set -u
DIR="$(cd "$(dirname "$0")" && pwd)"
DEV="${DEV:-$(bash "$DIR/achar-tv.sh")}" || exit 1
A=(adb -s "$DEV" shell)

for p in com.google.android.apps.mediashell com.google.android.katniss com.google.android.backdrop com.tcl.smartalexa \
         com.tcl.waterfall.overseas com.google.android.tvrecommendations com.tcl.ui_mediaCenter com.google.android.videos \
         com.google.android.tvlauncher; do
  "${A[@]}" pm enable "$p"
done
"${A[@]}" settings put secure screensaver_enabled 1
"${A[@]}" cmd appops set com.netflix.ninja RUN_ANY_IN_BACKGROUND default
"${A[@]}" cmd appops write-settings
for k in window_animation_scale transition_animation_scale animator_duration_scale; do
  "${A[@]}" settings put global "$k" 1.0
done
# Os que vieram com a TV voltam assim; HBO Max, Red Bull TV e Twitch só baixando de novo na Play Store.
for p in com.amazon.amazonvideo.livingroom com.google.android.youtube.tvmusic com.google.android.play.games; do
  "${A[@]}" cmd package install-existing "$p"
done
"${A[@]}" cmd notification disallow_listener br.gui.tvmae/br.gui.tvmae.data.PlayingListener
adb -s "$DEV" uninstall br.gui.tvmae
echo "Pronto: a tela do Google voltou e tudo foi reativado (HBO Max, Red Bull TV e Twitch: baixar na Play Store)."
