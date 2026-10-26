# Roteiro de QA | Launcher Início 1.0

Aparelho: Bettdow 1073, ADB 192.168.2.100:5555. Versão testada: release `apk/Inicio-1.0.apk`.
Execução por `adb shell input keyevent` + captura em `launcher/build/qa/qa-NN.png`, conferida imagem por imagem.
PASS só com captura conferida. FAIL é FAIL.

| # | Cenário | Passos | Esperado |
|---|---|---|---|
| 1 | Abertura | `am start -W -n br.gui.inicio/.home.HomeActivity` | Tela inicial completa; TotalTime registrado |
| 2 | Foco inicial | Abrir | Primeiro ícone do Dock maior, balão com o nome |
| 3 | Limites do Dock | ← no primeiro ícone; → até passar do último | Foco não sai do Dock |
| 4 | Subir e descer | ↑ no Dock; ↓ na barra | Vai para "Início"; volta ao último ícone focado |
| 5 | Limites da barra | ← em "Início"; → até passar da data/hora; ↑ | Foco não sai da barra |
| 6 | Menu Início | OK em "Início"; Voltar | 5 itens e 2 divisórias; Voltar fecha e o foco volta para "Início" |
| 7 | Botão Menu | Tecla MENU com foco no Dock | Abre o menu "Início" |
| 8 | Central de Controle | OK em "Controle"; → (Keystone); OK | 8 blocos; abre a tela de keystone do fabricante |
| 9 | Calendário | OK na data; → duas vezes; ↓; OK | Mês avança 2 vezes (dezembro); "Ajustar data e hora…" abre a tela do sistema |
| 10 | Wi-Fi e Bluetooth | OK em cada um | Abre a tela de redes / de Bluetooth do fabricante |
| 11 | Abrir app do Dock | OK em YouTube; voltar ao launcher | YouTube abre |
| 12 | Todos os apps | OK no último ícone; → até o fim da 1ª linha; → | Grade 6 x 3; com mais de 18 apps passa para a página 2 (com 18 ou menos: não se aplica, registrar) |
| 13 | Ponto azul | Com o dono: reinstalar o Downloader pela Play Store; abrir Todos os apps; abrir o Downloader; voltar | Ponto azul aparece; some depois de abrir (teste na tela) |
| 14 | Adicionar ao Dock | Todos os apps: MENU sobre o Miracast; "Adicionar ao Dock"; voltar à Home | Miracast aparece no fim dos favoritos |
| 15 | Dock cheio | Organizar Dock: marcar até 8; tentar o 9º | Aviso "Dock cheio"; o 9º não entra |
| 16 | Remover do Dock | Organizar Dock: OK nos apps marcados no cenário 15 e no Miracast | Saem do Dock; Home volta aos 4 favoritos |
| 17 | Mover no Dock | Organizar Dock: → na Netflix; depois ← | Netflix vai para a posição 3 e volta para a 2 |
| 18 | Papel de parede | Trocar para "vale"; `am force-stop`; reabrir | Fundo e vidro mudam; escolha persiste |
| 19 | Foto do computador | `scripts/enviar-wallpaper.sh` com uma foto retrato 3000x4000 | Aparece em Fotos, recortada sem distorção |
| 20 | Foto corrompida | `adb push` de um texto renomeado para `ruim.jpg`; escolher | Não trava; fundo cai no degradê padrão |
| 21 | Voltar não sai | Voltar 3 vezes na tela inicial | Continua na tela inicial |
| 22 | Launcher original | Menu Início > "Launcher original do fabricante" | Abre a tela do fabricante |

Ao final: tirar do Dock tudo o que foi adicionado, voltar ao degradê 1, apagar as fotos de teste da pasta do app.
