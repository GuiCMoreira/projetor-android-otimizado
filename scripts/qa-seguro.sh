#!/usr/bin/env bash
# Funções de teste por ADB com trava de segurança: só aperta OK se a tela e o item focado forem os esperados.
# Uso: source scripts/qa-seguro.sh
DEV="${DEV:-192.168.2.100:5555}"
D="adb -s $DEV"

tecla() { $D shell input keyevent "$@"; sleep 1; }

tela() { $D shell dumpsys window | grep mCurrentFocus | sed 's/.*u0 //; s/}//'; }

# Texto do item com foco na tela atual (o próprio nó ou o primeiro texto dentro dele).
focado() {
  # Lê só o item em foco e o que está dentro dele (XML de verdade). Item sem texto devolve vazio:
  # antes, o texto do vizinho vazava e liberava o OK no item errado (o ícone do Bluetooth passou por "Controle").
  $D exec-out uiautomator dump /dev/tty 2>/dev/null | python3 -I -c '
import sys, xml.etree.ElementTree as ET
x=sys.stdin.read(); x=x[x.find("<?xml"):x.rfind(">")+1]
try: root=ET.fromstring(x)
except Exception: print(""); sys.exit()
node=next((n for n in root.iter("node") if n.get("focused")=="true"), None)
if node is None: print(""); sys.exit()
for n in node.iter("node"):
    if n.get("text"): print(n.get("text")); break
else: print(node.get("content-desc") or "")'
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
