#!/usr/bin/env bash
# Mede a Pluto TV: tempo da abertura do zero até a tela inicial dela e travadas de quadro ao navegar com as setas.
# Uso: scripts/medir-pluto.sh [rodadas]   (só setas, nunca OK: não abre nem toca nada)
D="adb -s ${DEV:-192.168.2.100:5555}"
P=tv.pluto.android
N="${1:-2}"
# Tela principal: 17.x (base Paramount+) ou 5.x leanback
HOME_RE="(com.cbs.app.tv.ui.activity.HomeActivity|.ui.main.LeanbackMainHostActivity)"
for r in $(seq 1 "$N"); do
  $D shell am force-stop $P; sleep 2; $D logcat -c
  $D shell monkey -p $P -c android.intent.category.LEANBACK_LAUNCHER 1 >/dev/null 2>&1
  for i in $(seq 1 120); do $D logcat -d | grep -qE "Displayed $P/$HOME_RE" && break; sleep 0.5; done
  # horários do próprio Android: do pedido de abertura até a tela inicial da Pluto aparecer
  t0=$($D logcat -d -v epoch | grep -m1 "START u0.*$P" | awk '{print $1}')
  t1=$($D logcat -d -v epoch | grep -m1 -E "Displayed $P/$HOME_RE" | awk '{print $1}')
  sleep 8   # a tela inicial da Pluto termina de carregar as fileiras
  $D shell dumpsys gfxinfo $P reset >/dev/null
  for k in DPAD_DOWN DPAD_DOWN DPAD_RIGHT DPAD_RIGHT DPAD_RIGHT DPAD_DOWN DPAD_RIGHT DPAD_RIGHT DPAD_UP DPAD_UP; do $D shell input keyevent $k; sleep 0.8; done
  j=$($D shell dumpsys gfxinfo $P | grep -E "Total frames rendered|Janky frames|90th percentile" | tr -s ' ' | tr '\n' ' ')
  echo "rodada $r: abertura até a tela inicial da Pluto $(python3 -c "print(round(($t1-$t0)*1000))") ms | navegação: $j"
done
$D shell am start -a android.intent.action.MAIN -c android.intent.category.HOME >/dev/null
