#!/usr/bin/env bash
# Acha o projetor na rede e imprime IP:PORTA. O roteador pode dar outro IP quando ele religa.
# Uso: ./achar-projetor.sh [rede]   (padrão 192.168.2)
# Primeiro tenta o que o adb já conhece; depois procura a porta 5555 na rede e confere o modelo.
set -u
REDE="${1:-192.168.2}"
MODELO="pioneer5"   # aparece em ro.product.model do Bettdow 1073

# timeout: uma conexão antiga (o IP de antes de religar) pode ficar "device" e travar o getprop por minutos.
e_projetor() { timeout 5 adb -s "$1" shell getprop ro.product.model 2>/dev/null | grep -q "$MODELO"; }

for d in $(adb devices | awk 'NR > 1 && $2 == "device" {print $1}'); do
  if e_projetor "$d"; then echo "$d"; exit 0; fi
done

abertos=$(for i in $(seq 1 254); do
  (timeout 1 bash -c "echo > /dev/tcp/$REDE.$i/5555" 2>/dev/null && echo "$REDE.$i:5555") &
done; wait)

for d in $abertos; do
  timeout 5 adb connect "$d" >/dev/null 2>&1
  if e_projetor "$d"; then echo "$d"; exit 0; fi
  adb disconnect "$d" >/dev/null 2>&1 # outro aparelho com depuração ligada: não fica conectado à toa
done
echo "projetor não encontrado em $REDE.x (ligado? depuração USB ativa?)" >&2
exit 1
