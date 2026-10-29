# Roteiro de QA | Início 1.3

Projetor em 192.168.2.113 (o IP mudou depois de religar). APK: `apk/Inicio-1.3.apk`.

## Feito por ADB (29/10/2026)

| # | Cenário | Como | Esperado | Resultado |
|---|---|---|---|---|
| A1 | Aviso de pouca memória com outro app na frente | Ajustes abertos, `am send-trim-memory br.gui.inicio RUNNING_LOW` | Log "papel de parede e capas liberados"; memória do Início cai | PASS: 76 MB para 37 MB |
| A2 | Volta depois do aviso | Botão Início, captura em 0,6 s | Fundo, capas e clima de volta | PASS |
| A3 | 3 quedas em 5 minutos | `am crash br.gui.inicio` 3 vezes, Início entre elas | Tela de emergência | PASS |
| A4 | Botão Início na emergência | 3 vezes | Continua nela, uma instância só | PASS: 1 instância |
| A5 | Voltar na emergência | KEYCODE_BACK | Não sai | PASS |
| A6 | Tentar de novo | OK no botão já focado | Tela inicial normal; emergência fechada | PASS |
| A7 | Clique com menu aberto | Menu Início aberto, `input mouse tap` sobre a Netflix | Menu fecha; Netflix não abre | PASS |
| A8 | Clique sem menu | `input mouse tap` sobre a Netflix | Netflix abre | PASS |
| A9 | Clique no descanso de tela | Descanso em 2 min, `input mouse tap` sobre a Netflix | Descanso some; Netflix não abre | PASS |
| A10 | Pacotes desativados x HDMI | `dumpsys package` e `dumpsys tv_input` | Nenhum desativado fornece entrada | PASS: HDMI vem de `com.sstar.tv.inputservice` |

Não testado: queda da própria tela de emergência (cair para os Ajustes do Android). O app de release não deixa mexer nas preferências por ADB para simular.

## Com o mouse aéreo (pelo dono do projetor)

O ADB não gera "ponteiro passando por cima", só clique. Por isso estes passos são feitos à mão.

Resultado (29/10/2026): o dono executou os 11 passos e todos funcionaram (PASS).

| # | Passo | Esperado |
|---|---|---|
| M1 | Na tela inicial, passar o ponteiro devagar pelos ícones da Dock | Cada ícone cresce e mostra o nome ao receber o ponteiro |
| M2 | Parar o ponteiro sobre a Netflix e andar com as setas até o YouTube | O foco fica no YouTube; o tremor da mão não volta para a Netflix |
| M3 | Depois do M2, mexer o mouse um pouco mais | O foco volta para o item sob o ponteiro |
| M4 | Passar o ponteiro pelos cartões do "Continuar assistindo" e pela barra de cima | Cada um ganha o foco |
| M5 | Clicar num ícone, voltar com o botão Início e passar o ponteiro de novo | O foco aparece de novo no item sob o ponteiro |
| M6 | Abrir a Central de Controle e passar o ponteiro pelos blocos | Cada bloco ganha o foco; os itens atrás não |
| M7 | Com a Central aberta, clicar fora dela | Ela fecha e nada atrás abre |
| M8 | Menu Início > Papel de parede: girar a roda do mouse sobre as fotos | A fileira rola para o lado |
| M9 | Menu Início > Cidade do clima: passar o ponteiro sobre o campo de busca | O teclado não abre sozinho; abre ao clicar |
| M10 | Esperar o descanso de tela e mexer o mouse de leve | Continua no descanso |
| M11 | No descanso, mexer o mouse de verdade | Volta para a tela inicial |
