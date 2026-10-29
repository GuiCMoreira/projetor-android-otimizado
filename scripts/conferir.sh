#!/usr/bin/env bash
# Confere se o projetor continua como deixamos: atualização de firmware ou reset pode desfazer a limpeza.
# Uso: ./conferir.sh [--corrigir]   (endereço em DEV=IP:PORTA; sem ele, procura na rede)
# Sai com 0 se está tudo certo e 1 se algo mudou. Com --corrigir, reaplica o que mudou e confere de novo.
set -u
DIR="$(cd "$(dirname "$0")" && pwd)"
DEV="${DEV:-$(bash "$DIR/achar-projetor.sh")}" || exit 1
CORRIGIR=0
[ "${1:-}" = "--corrigir" ] && CORRIGIR=1
A=(adb -s "$DEV" shell)

# Removidos (adware e sobras do launcher do fabricante): não podem voltar.
REMOVIDOS=(com.android.media.module.services com.android.docks)
# Desativados com pm disable-user (reversão em reverter.sh).
DESATIVADOS=(zeasn.open.technical.tv.store com.mobisystems.editor.office_registered com.android.inputmethod.pinyin
  com.android.printspooler com.uv.droid.uota com.uv.droid.uotaback com.zeasn.launcher.projector)
HOME_PKG=br.gui.inicio
ANIMACAO=0.5

problemas=0
falha() { echo "  MUDOU: $1"; problemas=$((problemas + 1)); }

conferir() {
  problemas=0
  local instalados desativados
  instalados=$("${A[@]}" pm list packages | tr -d '\r' | sed 's/package://')
  desativados=$("${A[@]}" pm list packages -d | tr -d '\r' | sed 's/package://')

  for p in "${REMOVIDOS[@]}"; do
    grep -qx "$p" <<< "$instalados" && falha "$p voltou"
  done
  for p in "${DESATIVADOS[@]}"; do
    grep -qx "$p" <<< "$instalados" && ! grep -qx "$p" <<< "$desativados" && falha "$p foi reativado"
  done
  grep -qx "$HOME_PKG" <<< "$instalados" || falha "launcher Início não está instalado"
  local home
  home=$("${A[@]}" settings get global nl_curr_home_launcher | tr -d '\r')
  [ "$home" = "$HOME_PKG" ] || falha "tela padrão é $home, não o Início"
  for k in window_animation_scale transition_animation_scale animator_duration_scale; do
    local v
    v=$("${A[@]}" settings get global "$k" | tr -d '\r')
    [ "$v" = "$ANIMACAO" ] || falha "$k é $v (esperado $ANIMACAO)"
  done
}

corrigir() {
  for p in "${REMOVIDOS[@]}"; do "${A[@]}" pm uninstall --user 0 "$p" >/dev/null 2>&1; done
  for p in "${DESATIVADOS[@]}"; do "${A[@]}" pm disable-user --user 0 "$p" >/dev/null 2>&1; done
  "${A[@]}" settings put global nl_curr_home_launcher "$HOME_PKG"
  "${A[@]}" cmd package set-home-activity "$HOME_PKG/.home.HomeActivity" >/dev/null
  for k in window_animation_scale transition_animation_scale animator_duration_scale; do
    "${A[@]}" settings put global "$k" "$ANIMACAO"
  done
}

echo "Projetor em $DEV"
conferir
if [ "$problemas" -eq 0 ]; then
  echo "Tudo como deixamos."
  exit 0
fi
if [ "$CORRIGIR" -eq 1 ]; then
  echo "Reaplicando..."
  corrigir
  conferir
  [ "$problemas" -eq 0 ] && { echo "Corrigido."; exit 0; }
fi
echo "$problemas diferença(s). Rode com --corrigir para reaplicar."
exit 1
