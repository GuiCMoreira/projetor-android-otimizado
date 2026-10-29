#!/usr/bin/env bash
# Impede um app de rodar em segundo plano sem desinstalar: ele continua abrindo normal quando você escolhe.
# Uso:
#   ./sem-segundo-plano.sh listar             apps instalados rodando sem estar na tela (e quanto ocupam)
#   ./sem-segundo-plano.sh bloquear <pacote>  não roda mais em segundo plano
#   ./sem-segundo-plano.sh liberar <pacote>   desfaz
# Endereço em DEV=IP:PORTA; sem ele, procura na rede.
# O write-settings grava a regra em disco: sem ele, ela volta ao padrão quando o projetor reinicia.
# Nunca usar "deny" aqui: alguns apps fecham com erro; "ignore" só nega em silêncio.
set -u
DIR="$(cd "$(dirname "$0")" && pwd)"
DEV="${DEV:-$(bash "$DIR/achar-projetor.sh")}" || exit 1
A=(adb -s "$DEV" shell)

case "${1:-}" in
  listar)
    frente=$("${A[@]}" dumpsys activity activities | grep -m1 -o 'topResumedActivity=[^ ]* [^ ]* [^ /]*' | awk '{print $NF}')
    echo "Na tela: $frente"
    echo "Rodando sem estar na tela (apps instalados por você; memória em MB):"
    for p in $("${A[@]}" pm list packages -3 | tr -d '\r' | sed 's/package://'); do
      [ "$p" = "$frente" ] && continue
      kb=$("${A[@]}" dumpsys meminfo "$p" 2>/dev/null | awk '/TOTAL PSS:/ {print $3}' | tr -d '\r')
      [ -n "$kb" ] || continue
      # "ignore" = bloqueado por este script; "allow" = padrão do Android.
      regra=$("${A[@]}" cmd appops get "$p" RUN_ANY_IN_BACKGROUND | tr -d '\r' | grep -o 'ignore\|allow' | head -1)
      printf '  %-40s %4d MB  %s\n' "$p" $((kb / 1024)) "${regra:-allow}"
    done
    ;;
  bloquear)
    p="${2:?informe o pacote}"
    "${A[@]}" cmd appops set "$p" RUN_ANY_IN_BACKGROUND ignore
    "${A[@]}" cmd appops write-settings
    "${A[@]}" am force-stop "$p"
    echo "$p: $("${A[@]}" cmd appops get "$p" RUN_ANY_IN_BACKGROUND | tr -d '\r')"
    ;;
  liberar)
    p="${2:?informe o pacote}"
    "${A[@]}" cmd appops set "$p" RUN_ANY_IN_BACKGROUND default
    "${A[@]}" cmd appops write-settings
    echo "$p: liberado"
    ;;
  *)
    sed -n '2,9p' "$0"
    exit 2
    ;;
esac
