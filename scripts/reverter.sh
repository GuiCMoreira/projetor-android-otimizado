#!/usr/bin/env bash
# Desfaz as otimizações aplicadas em 26/10/2026 no projetor Bettdow 1073.
# Uso: ./reverter.sh [IP:PORTA]   (sem endereço, procura o projetor na rede)
# Cada bloco é independente: dá para copiar só a parte que quiser desfazer.
set -u
DEV="${1:-${DEV:-$(bash "$(dirname "$0")/achar-projetor.sh")}}"
DIR="$(cd "$(dirname "$0")/.." && pwd)"
adb connect "$DEV"
A="adb -s $DEV shell"

# 1. Animações de volta ao padrão (1.0)
$A settings put global window_animation_scale 1.0
$A settings put global transition_animation_scale 1.0
$A settings put global animator_duration_scale 1.0

# 2. Reativar apps desativados
for p in zeasn.open.technical.tv.store \
         com.mobisystems.editor.office_registered \
         com.android.inputmethod.pinyin \
         com.android.printspooler \
         com.uv.droid.uota \
         com.uv.droid.uotaback; do
  $A pm enable "$p"
done

# 3. Reinstalar os apps removidos (NÃO recomendado: são adware). Descomente se precisar.
# adb -s "$DEV" install "$DIR/backup/apk/com.android.media.module.services/base.apk"
# adb -s "$DEV" install "$DIR/backup/apk/com.android.docks/base.apk"
# $A pm disable-user --user 0 com.android.docks   # estava desativado de fábrica

# 4. Remover o launcher Início (o Android volta para a tela do fabricante)
adb -s "$DEV" uninstall br.gui.inicio || true

# 5. Reativar o launcher do fabricante e devolver a tela padrão a ele
$A pm enable com.zeasn.launcher.projector
$A settings put global nl_curr_home_launcher com.zeasn.launcher.projector
$A cmd package set-home-activity com.zeasn.launcher.projector/com.zeasn.launcher.ui.WelcomeActivity

# 6. Pluto TV: voltar à compilação padrão do Android (a completa foi feita em 26/10/2026)
$A cmd package compile -m speed-profile -f tv.pluto.android

# 7. Pluto TV: voltar para a 17.2 da Play Store (em 27/10/2026 ficou a 5.65.0-leanback, mais fluida)
bash "$DIR/scripts/trocar-pluto.sh" atual
