#!/usr/bin/env bash
# Desativa um app com segurança (pm disable-user, reversível), recusando o que derrubaria a imagem, o controle ou a
# tela inicial. Versão da TV da mãe do scripts/desativar.sh do projetor.
# Uso: ./desativar.sh <pacote> [--forcar]   (endereço em DEV=IP:PORTA; sem ele, procura na rede)
# Recusa sempre:
#   - quem fornece entrada de vídeo (TvInputService: TV digital, HDMI) e quem roda no mesmo processo dela;
#   - a tela inicial em uso, as peças do sistema e os apps da TCL que atendem botões do controle ou a imagem.
# Pede --forcar: quem tem permissão de entrada de vídeo (TV_INPUT_HARDWARE, TV_INPUT_BUTTON, CONNECT_INPUT)
# sem fornecer entrada nenhuma.
set -u
DIR="$(cd "$(dirname "$0")" && pwd)"
PKG="${1:?Uso: $0 <pacote> [--forcar]}"
FORCAR=0
[ "${2:-}" = "--forcar" ] && FORCAR=1
DEV="${DEV:-$(bash "$DIR/achar-tv.sh")}" || exit 1
A=(adb -s "$DEV" shell)
"${A[@]}" true >/dev/null 2>&1 || { echo "sem conexão com $DEV" >&2; exit 2; }

recusa() { echo "RECUSADO: $PKG $1. Nada foi alterado." >&2; exit 1; }

"${A[@]}" pm list packages | tr -d '\r' | grep -qxF "package:$PKG" || recusa "não está instalado"
"${A[@]}" pm list packages -d | tr -d '\r' | grep -qxF "package:$PKG" && { echo "$PKG já está desativado."; exit 0; }

case "$PKG" in
  android | com.android.systemui | com.android.providers.* | com.android.vending | com.android.tv.settings | \
    com.google.android.gms | com.google.android.gsf | com.google.android.inputmethod.* | \
    com.tcl.tv | com.tcl.tvinput | com.tcl.suspension | com.tcl.dashboard | com.tcl.inputmethod.*)
    recusa "é peça do sistema, da imagem ou de um botão do controle" ;;
esac
home=$("${A[@]}" cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -1 | tr -d '\r')
[ "$PKG" = "${home%%/*}" ] && recusa "é a tela inicial em uso"

entradas=$("${A[@]}" pm query-services -a android.media.tv.TvInputService | tr -d '\r')
pacotes_entrada=$(awk -F= '/^ +packageName=/ {print $2}' <<< "$entradas" | sort -u)
processos_entrada=$(awk -F= '/^ +processName=/ {print $2}' <<< "$entradas" | sort -u)
grep -qxF "$PKG" <<< "$pacotes_entrada" && recusa "fornece entrada de vídeo (TV digital, HDMI ou canal)"
grep -qxF "$PKG" <<< "$processos_entrada" && recusa "é o processo onde a entrada de vídeo roda"

# Só o que o próprio app pede (requested permissions): os da TCL rodam com a identidade do sistema e herdam
# TV_INPUT_HARDWARE sem usar; quem pede de verdade é com.tcl.tv, que já está protegido acima.
perms=$("${A[@]}" dumpsys package "$PKG" | tr -d '\r' | sed -n '/requested permissions:/,/install permissions:/p' |
  grep -oE 'TV_INPUT_HARDWARE|TV_INPUT_BUTTON|CONNECT_INPUT' | sort -u | tr '\n' ' ')
if [ -n "$perms" ] && [ "$FORCAR" -eq 0 ]; then
  echo "ATENÇÃO: $PKG tem permissão de entrada de vídeo ($perms) mas não fornece entrada nenhuma." >&2
  echo "Se quiser mesmo: $0 $PKG --forcar" >&2
  exit 1
fi

"${A[@]}" pm disable-user --user 0 "$PKG" | tr -d '\r'
echo "Para desfazer: adb -s $DEV shell pm enable $PKG"
