#!/usr/bin/env bash
# Acha a TV da mãe na rede e imprime IP:PORTA. O IP muda conforme o roteador (aqui e na casa dela).
# Uso: ./achar-tv.sh [rede]   (padrão 192.168.2)
# Primeiro tenta o que o adb já conhece; depois procura a porta 5555 na rede e confere a placa.
set -u
REDE="${1:-192.168.2}"
PLACA="C06"   # ro.product.device da TCL da mãe

e_tv() { [ "$(timeout 5 adb -s "$1" shell getprop ro.product.device 2>/dev/null | tr -d '\r')" = "$PLACA" ]; }

for d in $(adb devices | awk 'NR > 1 && $2 == "device" {print $1}'); do
  if e_tv "$d"; then echo "$d"; exit 0; fi
done

abertos=$(for i in $(seq 1 254); do
  (timeout 1 bash -c "echo > /dev/tcp/$REDE.$i/5555" 2>/dev/null && echo "$REDE.$i:5555") &
done; wait)

for d in $abertos; do
  timeout 5 adb connect "$d" >/dev/null 2>&1
  if e_tv "$d"; then echo "$d"; exit 0; fi
  adb disconnect "$d" >/dev/null 2>&1 # outro aparelho com depuração ligada: não fica conectado à toa
done
echo "TV não encontrada em $REDE.x (ligada? depuração ativa? autorizou este computador?)" >&2
exit 1
