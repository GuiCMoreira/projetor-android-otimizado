#!/usr/bin/env bash
# Troca a versão da Pluto TV no projetor. As duas cópias estão em backup/apk (assinatura oficial conferida).
# Uso: scripts/trocar-pluto.sh leve    -> 5.65.0-leanback (feita para TV, mais fluida; pede conta grátis)
#      scripts/trocar-pluto.sh atual   -> 17.2.0 (Play Store, base Paramount+; funciona sem conta)
# Trocar apaga o "Continuar assistindo" e o login da Pluto (o Android exige desinstalar para voltar de versão).
set -e
DIR="$(cd "$(dirname "$0")/.." && pwd)"
D="adb -s ${DEV:-$(bash "$(dirname "$0")/achar-projetor.sh")}"
case "${1:-}" in
  leve) V="$DIR/backup/apk/tv.pluto.android-5.65.0-leanback" ;;
  atual) V="$DIR/backup/apk/tv.pluto.android-17.2.0" ;;
  *) echo "Uso: $0 leve|atual"; exit 1 ;;
esac
$D uninstall tv.pluto.android || true
$D install-multiple "$V"/*.apk
# Compilação completa: navegação com menos travadas neste processador
$D shell cmd package compile -m speed -f tv.pluto.android
$D shell dumpsys package tv.pluto.android | grep -m1 versionName
