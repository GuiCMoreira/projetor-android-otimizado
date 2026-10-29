#!/usr/bin/env bash
# Auditoria de privacidade, só leitura: nada é alterado no projetor.
# Mostra assistente de voz, quem pode usar e quem usou microfone e localização, apps de telemetria conhecidos
# e acessos especiais (acessibilidade, notificações, administrador, estatísticas de uso).
# Uso: ./auditar-privacidade.sh   (endereço em DEV=IP:PORTA; sem ele, procura na rede)
# O "usou" vem do histórico do AppOps: permissão sem uso registrado pesa menos que permissão usada.
set -u
DIR="$(cd "$(dirname "$0")" && pwd)"
DEV="${DEV:-$(bash "$DIR/achar-projetor.sh")}" || exit 1
A=(adb -s "$DEV" shell)
"${A[@]}" true >/dev/null 2>&1 || { echo "sem conexão com $DEV" >&2; exit 2; }
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT

"${A[@]}" dumpsys package | tr -d '\r' > "$TMP/package"
"${A[@]}" dumpsys appops | tr -d '\r' > "$TMP/appops"
"${A[@]}" pm list packages -d | tr -d '\r' | sed 's/package://' > "$TMP/desativados"
"${A[@]}" pm list packages | tr -d '\r' | sed 's/package://' > "$TMP/instalados"
"${A[@]}" dumpsys device_policy | tr -d '\r' > "$TMP/admin"
for k in voice_interaction_service assistant voice_recognition_service enabled_accessibility_services enabled_notification_listeners; do
  echo "$k=$("${A[@]}" settings get secure "$k" | tr -d '\r')"
done > "$TMP/secure"

python3 -I - "$TMP" <<'EOF'
import re, sys, os
d = sys.argv[1]
ler = lambda n: open(os.path.join(d, n), encoding='utf-8', errors='replace').read()
package, appops = ler('package'), ler('appops')
desativados = set(ler('desativados').split())
instalados = set(ler('instalados').split())

def estado(p):
    return 'ausente' if p not in instalados else ('desativado' if p in desativados else 'ATIVO')

print('== Assistente de voz e acessos especiais')
for linha in ler('secure').splitlines():
    k, _, v = linha.partition('=')
    print(f'  {k}: {v if v not in ("null", "") else "nenhum"}')
admins = sorted(set(re.findall(r'ComponentInfo\{([^/}]+)/', ler('admin'))))
print(f'  administradores do aparelho: {", ".join(admins) or "nenhum"}')

# Permissões concedidas por pacote (o bloco do pacote e, para quem divide usuário com o sistema, o do usuário).
blocos = re.split(r'\n  Package \[', package)[1:]
concedidas = {}
for b in blocos:
    nome = b.split(']')[0]
    concedidas[nome] = set(re.findall(r'android\.permission\.([A-Z_]+): granted=true', b))

# Uso registrado no AppOps: "Package X:" seguido de linhas de operação com "time=".
usos = {}
atual = None
for linha in appops.splitlines():
    m = re.match(r'\s+Package (\S+):$', linha)
    if m:
        atual = m.group(1)
        continue
    m = re.match(r'\s+(RECORD_AUDIO|COARSE_LOCATION|FINE_LOCATION|GET_USAGE_STATS) \(([^)]*)\)', linha)
    if m and atual:
        usos.setdefault(atual, {})[m.group(1)] = m.group(2)

def usou(p, op):
    return op in usos.get(p, {})

print('\n== Microfone (RECORD_AUDIO)')
for p in sorted(n for n, s in concedidas.items() if 'RECORD_AUDIO' in s):
    print(f'  {p:45} {estado(p):10} {"usou" if usou(p, "RECORD_AUDIO") else "sem uso registrado"}')

print('\n== Localização')
for p in sorted(n for n, s in concedidas.items() if s & {'ACCESS_FINE_LOCATION', 'ACCESS_COARSE_LOCATION'}):
    uso = usou(p, 'FINE_LOCATION') or usou(p, 'COARSE_LOCATION')
    print(f'  {p:45} {estado(p):10} {"usou" if uso else "sem uso registrado"}')

print('\n== Estatísticas de uso (sabe quais apps você abre e por quanto tempo)')
for p, ops in sorted(usos.items()):
    if 'allow' in ops.get('GET_USAGE_STATS', ''):
        print(f'  {p:45} {estado(p)}')

print('\n== Telemetria e feedback conhecidos (desativados em várias listas do cleanyourtv)')
for p in ['com.google.android.feedback', 'com.android.tv.feedbackconsent', 'com.google.android.partnersetup',
          'com.google.android.tvrecommendations', 'com.google.android.backdrop', 'com.google.android.katniss',
          'com.google.android.speech.pumpkin', 'com.google.android.tts']:
    print(f'  {p:45} {estado(p)}')

print('\n== Outros nomes suspeitos ativos (heurística: report, analytics, stat, push, ad, log, track)')
suspeito = re.compile(r'(report|analytic|statis|\.stat|push|\.ads?\b|\.ad\.|logger|track|collect)', re.I)
for p in sorted(instalados - desativados):
    if suspeito.search(p):
        print(f'  {p}')
EOF
