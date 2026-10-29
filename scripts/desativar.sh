#!/usr/bin/env bash
# Desativa um app com segurança (pm disable-user, reversível), recusando o que derrubaria o HDMI ou a tela inicial.
# Uso: ./desativar.sh <pacote> [--forcar]   (endereço em DEV=IP:PORTA; sem ele, procura na rede)
# Recusa sempre:
#   - quem fornece entrada de vídeo (TvInputService: HDMI) e quem roda no mesmo processo dela;
#     neste projetor o HDMI roda dentro de com.sstar.systemcontrol;
#   - a tela inicial e as peças do sistema.
# Pede --forcar: quem tem permissão de entrada de vídeo (TV_INPUT_HARDWARE, TV_INPUT_BUTTON, CONNECT_INPUT)
# sem fornecer entrada nenhuma. Em outros projetores, desativar um desses deixou o HDMI em "Sem sinal".
set -u
DIR="$(cd "$(dirname "$0")" && pwd)"
PKG="${1:?Uso: $0 <pacote> [--forcar]}"
FORCAR=0
[ "${2:-}" = "--forcar" ] && FORCAR=1
DEV="${DEV:-$(bash "$DIR/achar-projetor.sh")}" || exit 1
A=(adb -s "$DEV" shell)
"${A[@]}" true >/dev/null 2>&1 || { echo "sem conexão com $DEV" >&2; exit 2; }

recusa() { echo "RECUSADO: $PKG $1. Nada foi alterado." >&2; exit 1; }

"${A[@]}" pm list packages | tr -d '\r' | grep -qxF "package:$PKG" || recusa "não está instalado"
"${A[@]}" pm list packages -d | tr -d '\r' | grep -qxF "package:$PKG" && { echo "$PKG já está desativado."; exit 0; }

case "$PKG" in
  android | com.android.systemui | com.android.providers.* | com.android.vending | com.google.android.gms | \
    com.google.android.gsf | com.google.android.inputmethod.* | br.gui.inicio)
    recusa "é peça do sistema ou a própria tela inicial" ;;
esac
home=$("${A[@]}" settings get global nl_curr_home_launcher | tr -d '\r')
[ "$PKG" = "$home" ] && recusa "é a tela inicial em uso"

# Quem fornece entrada de vídeo e em que processo ela roda.
entradas=$("${A[@]}" pm query-services -a android.media.tv.TvInputService | tr -d '\r')
pacotes_entrada=$(awk -F= '/^ +packageName=/ {print $2}' <<< "$entradas" | sort -u)
processos_entrada=$(awk -F= '/^ +processName=/ {print $2}' <<< "$entradas" | sort -u)
grep -qxF "$PKG" <<< "$pacotes_entrada" && recusa "fornece entrada de vídeo (HDMI ou canal)"
grep -qxF "$PKG" <<< "$processos_entrada" && recusa "é o processo onde a entrada de vídeo roda"

perms=$("${A[@]}" dumpsys package "$PKG" | tr -d '\r' | grep -oE 'TV_INPUT_HARDWARE|TV_INPUT_BUTTON|CONNECT_INPUT' | sort -u | tr '\n' ' ')
if [ -n "$perms" ] && [ "$FORCAR" -eq 0 ]; then
  echo "ATENÇÃO: $PKG tem permissão de entrada de vídeo ($perms) mas não fornece entrada nenhuma." >&2
  echo "Em outros aparelhos, desativar um desses deixou o HDMI sem sinal. Se quiser mesmo: $0 $PKG --forcar" >&2
  exit 1
fi

"${A[@]}" pm disable-user --user 0 "$PKG" | tr -d '\r'
echo "Para desfazer: adb -s $DEV shell pm enable $PKG"
echo "Anote em ALTERACOES.md e na lista DESATIVADOS do conferir.sh. Depois teste o HDMI (Central de Controle > Fontes)."
