#!/usr/bin/env bash
# Funções de teste por ADB com trava de segurança: só aperta OK se a tela e o item focado forem os esperados.
# Uso: source scripts/qa-seguro.sh
DEV="${DEV:-192.168.2.100:5555}"
D="adb -s $DEV"

tecla() { $D shell input keyevent "$@"; sleep 1; }

tela() { $D shell dumpsys window | grep mCurrentFocus | sed 's/.*u0 //; s/}//'; }

# Texto do item com foco na tela atual (o próprio nó ou o primeiro texto dentro dele).
focado() {
  $D exec-out uiautomator dump /dev/tty 2>/dev/null | python3 -I -c '
import re,sys
x=sys.stdin.read()
i=x.find("focused=\"true\"")
if i<0: print(""); sys.exit()
start=x.rfind("<node",0,i)
# primeiro texto não vazio do nó focado ou de dentro dele (linhas do launcher guardam o texto num filho)
for t in re.findall(r"text=\"([^\"]*)\"", x[start:start+4000]):
    if t: print(t); break
else:
    # cartões de imagem não têm texto: vale a descrição do próprio nó
    d=re.search(r"content-desc=\"([^\"]*)\"", x[start:i+20])
    print(d.group(1) if d else "")'
}

# ok "Texto esperado": aperta OK só se a tela for do launcher e o item focado contiver o texto.
ok() {
  local esperado="$1" t f
  t="$(tela)"; f="$(focado)"
  if [[ "$t" != br.gui.inicio/* ]]; then echo "BLOQUEADO: tela fora do launcher ($t)"; return 1; fi
  if [[ "$f" != *"$esperado"* ]]; then echo "BLOQUEADO: foco em '$f', esperado '$esperado'"; return 1; fi
  tecla DPAD_CENTER
}

# Abre pelo mesmo caminho do botão Início (FallbackHome do fabricante). Aberta direto pelo componente, logo
# depois de um force-stop, o gerenciador de memória do fabricante (com.sstar.systemcontrol) pode fechá-la.
inicio() { $D shell am start -a android.intent.action.MAIN -c android.intent.category.HOME >/dev/null; sleep 3; }
