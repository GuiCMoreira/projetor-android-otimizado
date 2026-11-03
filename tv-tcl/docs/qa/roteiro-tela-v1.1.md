# Roteiro de QA | Tela da TV da avó 1.1

TV TCL em 192.168.2.120 (rede de casa). APK: `apk/TelaVo-1.1.apk`.

## Feito por ADB (02/11/2026)

| # | Cenário | Esperado | Resultado |
|---|---|---|---|
| A1 | Abrir a tela | Saudação "Boa noite" com o nome dela, data por extenso, relógio e clima | PASS ("18° · nublado") |
| A2 | Foco inicial | No primeiro app, não no "Todos os apps" | PASS (corrigido: começava no "Todos os apps") |
| A3 | "Todos os apps" | Texto inteiro no botão | PASS (corrigido: o "apps" saía cortado) |
| A4 | Seta para baixo na fileira | Vai para "Todos os apps" | PASS |
| A5 | Lista de "Todos os apps" | Só apps de verdade, sem telas internas do sistema | PASS (corrigido: vinha "Caixa de Mensagens", "copyDataToTV"...) |
| A6 | Apps fixos marcados | Etiqueta "Na tela inicial" | PASS |
| A7 | Ícones | Pluto preenchendo o quadrado; Globoplay nítido | PASS (corrigido: Globoplay borrado) |
| A8 | Tela padrão | Botão Início e reinício da TV levam à tela nova | PASS (3 reinícios) |
| A9 | Memória | Mais memória livre que a tela do Google | PASS: 297 MB (fábrica) para 447 MB; tela nova 22 MB contra 157 MB |

## Com o controle de verdade (pelo neto, 02/11/2026)

Andar pelos botões e abrir cada app, voltar com o botão Início, abrir "Todos os apps" e segurar OK para fixar e
tirar um app da tela inicial. Resultado: tudo funcionou (PASS).

## Tela 1.2 por ADB (02/11/2026)

| # | Cenário | Esperado | Resultado |
|---|---|---|---|
| B1 | Dois OKs rápidos no "Todos os apps" do Início | Abre uma vez; o segundo OK não abre as Configurações | PASS (antes do ajuste: abria as Configurações) |
| B2 | Segurar OK na Netflix em "Todos os apps" | Fixa: a tela inicial passa a ter 4 botões; a Netflix não abre | PASS |
| B3 | Segurar OK de novo | Tira: volta a 3 botões | PASS |

Durante o B1 antes do ajuste, 3 setas e um OK segurado caíram dentro das Configurações. Comparadas com as do
diagnóstico de fábrica, nenhuma configuração mudou por isso (Wi-Fi, Bluetooth, imagem e som intactos).

Pendente com o controle de verdade: segurar OK para fixar (o jeito de reconhecer mudou na 1.2).
