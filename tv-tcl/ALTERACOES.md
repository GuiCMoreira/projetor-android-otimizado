# Alterações na TV da avó

Cada linha diz o que mudou, o comando e como desfazer. `scripts/reverter.sh` desfaz tudo de uma vez.

## 02/11/2026, limpeza (aprovada pelo neto)

| # | Alteração | Comando aplicado | Como desfazer |
|---|---|---|---|
| 1 | Removida a atualização do Chromecast embutido (158 MB de armazenamento) | `pm uninstall-system-updates com.google.android.apps.mediashell` | reativar (linha 2) e atualizar pela Play Store |
| 2 | Desativados (`scripts/desativar.sh`, que recusa o que derruba imagem, controle ou tela inicial): Chromecast (`com.google.android.apps.mediashell`), busca por voz (`com.google.android.katniss`), recomendações (`com.google.android.tvrecommendations`), descanso de tela do Google (`com.google.android.backdrop`), Prime Video (`com.amazon.amazonvideo.livingroom`), Alexa (`com.tcl.smartalexa`), ouvir sozinho pelo fone (`com.tcl.t_solo`), central de mídia do pendrive (`com.tcl.ui_mediaCenter`), adesivo de loja (`com.tcl.esticker`), conta TCL (`com.tcl.usercenter`) | `pm disable-user --user 0 <pacote>` | `pm enable <pacote>` |
| 3 | Descanso de tela do sistema desligado (era o do Google, desativado acima) | `settings put secure screensaver_enabled 0` | `settings put secure screensaver_enabled 1` |
| 4 | Animações em 0,5x | `settings put global window_animation_scale 0.5` (e `transition_animation_scale`, `animator_duration_scale`) | os três em `1.0` (o terceiro era `null`) |
| 5 | Caches limpos | `pm trim-caches 2G` | não se desfaz (os apps recriam) |

Ficaram de propósito: `com.tcl.suspension` (painel rápido de entradas, botão do controle), `com.tcl.dashboard`
(pode ser o menu do botão de ajustes), `com.tcl.ttvs` (atualizador da TCL, fabricante confiável), `com.tcl.guard`,
`com.tcl.partnercustomizer`, `com.tcl.tv` e `com.tcl.tvinput` (imagem e entradas). O Play Filmes
(`com.google.android.videos`) foi recusado pelo script por registrar uma entrada de TV (prévias); não roda parado.

Medido: armazenamento livre de 515 MB para 736 MB. Memória livre de 297 MB para 304 MB depois de reiniciar, com a
Play Store atualizando apps (170 MB nela); o ganho grande depende de trocar a tela inicial do Google (122 MB).

## 02/11/2026, atualização, apps e tela inicial nova

| # | Alteração | Comando aplicado | Como desfazer |
|---|---|---|---|
| 6 | Espaço para a atualização: dados da tela do Google limpos, e removidas as atualizações e os dados dos desativados busca por voz, descanso, recomendações e conta TCL (de 733 MB para 826 MB livres). Remover a atualização reativa o app: foram desativados de novo | `pm clear com.google.android.tvlauncher`; `pm uninstall-system-updates <pacote>` e `pm clear <pacote>` | reativar (linha 2) e atualizar pela Play Store |
| 7 | Atualização oficial da TCL V8-T221T01-LF1V622 (801 MB): sistema de julho de 2022 para novembro de 2025, ainda Android 11. Ela religou a Alexa e a conta TCL; desativadas de novo | Configurações > Preferências do dispositivo > Sobre > Atualiz. de software > Atualização da Rede (confirmada na TV) | não se desfaz |
| 8 | Pluto TV 5.65.0-leanback (a mesma do projetor, feita para TV, assinatura oficial `29946c…e96c` conferida nas 5 partes) | `adb install-multiple` dos 5 APKs de `projetor-bettdow/backup/apk/tv.pluto.android-5.65.0-leanback` | `adb uninstall tv.pluto.android` |
| 9 | Globoplay 2.355 pela Play Store (plano grátis tem a TV Globo ao vivo) | Play Store, conta da TV | `adb uninstall com.globo.globotv` |
| 10 | Play Store sem atualização automática: não troca a Pluto 5.65 pela versão pesada e não ocupa memória atualizando | Play Store > Conta e configurações > Configurações > Atualizar apps automaticamente > Não atualizar | o mesmo caminho, "a qualquer hora" |
| 11 | Tela inicial nova `br.gui.tvavo` 1.0 instalada e padrão; tela do Google desativada (a recusa do `desativar.sh` é para a tela em uso; aqui a troca era o objetivo, com a nova testada e com tela de emergência) | `adb install apk/TelaVo-1.0.apk`; `cmd package set-home-activity br.gui.tvavo/.home.HomeActivity`; `pm disable-user --user 0 com.google.android.tvlauncher` | `pm enable com.google.android.tvlauncher` e `adb uninstall br.gui.tvavo` |

Depois de reiniciar: a TV liga direto na tela nova. Memória livre de 297 MB (fábrica) para 439 MB; a tela nova ocupa
22 MB contra 157 MB da do Google. A Alexa (`com.tcl.smartalexa`) é religada a cada reinício pelo atualizador da TCL
(`com.tcl.ttvs`, serviço `OverSeasAppShow`), mas não fica rodando nem aparece na tela inicial: deixada assim.

## 02/11/2026, mais memória e ícones

| # | Alteração | Comando aplicado | Como desfazer |
|---|---|---|---|
| 12 | Desativados o Gboard (`com.google.android.inputmethod.latin`; o teclado ativo e padrão é o da TCL, o Gboard nem estava ligado), o leitor de voz do Google (`com.google.android.tts`; sem voz padrão nem acessibilidade) e o assistente de configuração inicial (`com.google.android.tungsten.setupwraith`; configuração já concluída). Estimei 57 MB (o que ocupavam rodando); medido depois de reiniciar, com a TV parada na tela inicial: 439 MB livres antes, 447 MB depois (o Android já tirava boa parte deles da memória sozinho) | `pm disable-user --user 0` (o Gboard direto: o `desativar.sh` protege todo teclado do Google por precaução) | `pm enable <pacote>` |
| 13 | Tela inicial 1.1: ícones como no projetor (logo opaco até a borda ocupa o quadrado; a Pluto ficou toda amarela) e na maior resolução que o app tiver (o do Globoplay saía borrado) | `adb install -r apk/TelaVo-1.1.apk` | `adb install -r -d apk/TelaVo-1.0.apk` |
| 14 | A Play Store trocou a Pluto 5.65 pela 17.3 às 22:21, logo depois do login da conta Google e minutos antes de a atualização automática ser desligada (linha 10). Voltou para a 5.65 (desinstalar e instalar: o Android não deixa voltar de versão por cima). Com a atualização automática desligada, não se repete | `adb uninstall tv.pluto.android` e `adb install-multiple` dos 5 APKs da linha 8 | instalar pela Play Store |

Medição final (02/11/2026, reiniciada e parada 2 minutos na tela inicial): **447 MB livres**, contra 297 MB de
fábrica. A tela inicial nova ocupa 22 MB.

## 02/11/2026, tela 1.2 (correções da revisão de código)

| # | Alteração | Comando aplicado | Como desfazer |
|---|---|---|---|
| 15 | Tela inicial 1.2: clima guardado só vale 3 horas (antes mostrava clima de dias atrás sem internet); sem internet, tenta de novo a cada 10 minutos (antes a cada minuto); app fixado e depois desinstalado não ocupa mais vaga; "Todos os apps" mostra os botões na hora e os ícones chegam em blocos; não abre duas vezes e ignora o OK repetido logo depois de abrir (o segundo OK abria as Configurações, que vêm selecionadas primeiro); OK segurado reconhecido pela repetição da tecla; a tela inicial não é remontada quando nada mudou; qualquer seta devolve o foco se ele se perder; ícones pedidos em xxhdpi (bastante para 150 px, sem estourar memória) | `adb install -r apk/TelaVo-1.2.apk` | `adb install -r -d apk/TelaVo-1.1.apk` |

## 02/11/2026, Netflix sem pré-carregamento e lista de apps enxuta

| # | Alteração | Comando aplicado | Como desfazer |
|---|---|---|---|
| 16 | Desativados o Play Filmes (`com.google.android.videos`; a entrada de TV que ele registra é só de prévias, por isso o `desativar.sh` recusava) e o TCL Channel (`com.tcl.waterfall.overseas`, canais grátis com propaganda) | `pm disable-user --user 0 com.google.android.videos`; `scripts/desativar.sh com.tcl.waterfall.overseas` | `pm enable <pacote>` |
| 17 | Netflix proibida de rodar em segundo plano: ela ficava pré-carregada (114 MB) sem ninguém abrir. Continua abrindo normal quando escolhida (testado: abre em uns 2 s). Depois de reiniciar, não carregou sozinha | `cmd appops set com.netflix.ninja RUN_ANY_IN_BACKGROUND ignore` e `cmd appops write-settings` | `cmd appops set com.netflix.ninja RUN_ANY_IN_BACKGROUND default` e `cmd appops write-settings` |
| 18 | Tela inicial 1.3: "Todos os apps" esconde o Protetor de Segurança, o Quick Panel, a TCL Home, a TV e a Alexa (continuam no sistema; os botões do controle abrem cada um). Fica só Configurações, Globoplay, Netflix, Play Store, Pluto TV e YouTube | `adb install -r apk/TelaVo-1.3.apk` | `adb install -r -d apk/TelaVo-1.2.apk` |

Medido depois de reiniciar (02/11/2026): 444 MB livres; Netflix parada; 18 pacotes desativados.
| 19 | Tela inicial 1.4: o nome da saudação saiu do código para `res/values/strings.xml` (`greeting_name`), para a versão pública ir sem o nome. Mesma tela na TV | `adb install -r apk/TelaVo-1.4.apk` | `adb install -r -d apk/TelaVo-1.3.apk` |
