# Alterações na TV da mãe

Cada linha diz o que mudou, o comando e como desfazer. `scripts/reverter.sh` desfaz tudo de uma vez. Mesma TV da
avó (TCL C06, Android TV 11, 940 MB), com sistema de dezembro de 2025.

## 03/11/2026, tela nova e limpeza (aprovadas pelo filho)

| # | Alteração | Comando aplicado | Como desfazer |
|---|---|---|---|
| 1 | Tela inicial `br.gui.tvmae` 1.0 (opção B do protótipo: fotos do macOS que trocam a cada hora, atalhos, Continuar assistindo e faixa de apps) e a permissão de ler o Continuar assistindo | `adb install apk/TelaMae-1.0.apk`; `pm grant br.gui.tvmae android.permission.READ_TV_LISTINGS` | `adb uninstall br.gui.tvmae` |
| 2 | 9 fotos do macOS (as mesmas do projetor) na pasta do app, em 1280x720; ficam no aparelho e fora do git | `scripts/enviar-fotos.sh` | apagar `/sdcard/Android/data/br.gui.tvmae/files/wallpapers` |
| 3 | Desativados (`scripts/desativar.sh`): Chromecast (`com.google.android.apps.mediashell`), busca por voz (`com.google.android.katniss`), descanso de tela do Google (`com.google.android.backdrop`, 61 MB), Alexa (`com.tcl.smartalexa`), TCL Channel (`com.tcl.waterfall.overseas`), recomendações (`com.google.android.tvrecommendations`), central de mídia (`com.tcl.ui_mediaCenter`); e o Play Filmes (`com.google.android.videos`) direto, porque o script recusa a entrada de prévias dele | `pm disable-user --user 0 <pacote>` | `pm enable <pacote>` |
| 4 | Descanso de tela do sistema desligado (era o do Google, desativado acima) | `settings put secure screensaver_enabled 0` | `settings put secure screensaver_enabled 1` |
| 5 | Netflix proibida de rodar em segundo plano (na TV da avó ela pré-carregava 114 MB); abre normal quando escolhida | `cmd appops set com.netflix.ninja RUN_ANY_IN_BACKGROUND ignore` e `cmd appops write-settings` | o mesmo com `default` |
| 6 | Animações em 0,5x | `settings put global window_animation_scale 0.5` (e `transition_animation_scale`, `animator_duration_scale`) | os três em `1.0` |
| 7 | Tela nova como padrão e a do Google desativada | `cmd package set-home-activity br.gui.tvmae/.home.HomeActivity`; `pm disable-user --user 0 com.google.android.tvlauncher` | `pm enable com.google.android.tvlauncher` |

Ficaram de propósito: Play Store com atualização automática (os apps que ela usa precisam estar em dia; aqui não há a Pluto 5.65 para proteger), os apps que ela usa e as peças da TCL ligadas a botões do controle e à
imagem (`com.tcl.suspension`, `com.tcl.dashboard`, `com.tcl.tv`, `com.tcl.tvinput`, `com.tcl.guard`, `com.tcl.ttvs`).

O timer de desligar não virou atalho: nesta versão do sistema não há tela de timer que dê para abrir de fora. Segue
no menu da TV.

Depois de reiniciar (03/11/2026, parada 2 minutos na tela inicial): liga direto na tela nova; **440 MB livres**
contra 300 MB de fábrica; a tela nova ocupa 33 MB (com a foto de fundo) contra 163 MB da do Google; a Netflix não
carregou sozinha. A Alexa (`com.tcl.smartalexa`) é religada a cada reinício pelo atualizador da TCL (`com.tcl.ttvs`),
como na TV da avó; não fica rodando nem aparece na tela. A Play Store estava atualizando apps logo depois de ligar
(90 MB), porque aqui a atualização automática ficou ligada.

## 03/11/2026, tela 1.1 (correções da revisão de código)

| # | Alteração | Comando aplicado | Como desfazer |
|---|---|---|---|
| 8 | Tela 1.1: a remontagem das fileiras não passa mais o foco pelo atalho "Entrada" (piscava no canto); se a fileira Continuar assistindo some, o foco vai para os apps; só capas novas trocam a imagem sem remontar; o foco só salta para o primeiro cartão na primeira abertura (antes saltava também ao voltar de um app, e um OK abria o episódio em vez do app); cada capa é baixada no máximo uma vez por volta (sem laço); a pasta das fotos é lida a cada vez (antes podia ficar sem foto até reiniciar); recargas duplicadas não remontam duas vezes; ícone atualizado aparece; cartão que não abre mostra aviso | `adb install -r apk/TelaMae-1.1.apk` | `adb install -r -d apk/TelaMae-1.0.apk` |

## 03/11/2026, tela 1.2 e apps que ela não usa (pedidos pelo filho)

| # | Alteração | Comando aplicado | Como desfazer |
|---|---|---|---|
| 9 | Tela 1.2: no máximo 2 cartões de cada app no Continuar assistindo (antes a Globoplay ocupava os 4); apps fixos de fábrica passam a ser só Globoplay, Netflix e YouTube | `adb install -r apk/TelaMae-1.2.apk` | `adb install -r -d apk/TelaMae-1.1.apk` |
| 10 | Desinstalados: HBO Max (`com.wbd.stream`), Prime Video (`com.amazon.amazonvideo.livingroom`), Red Bull TV (`com.nousguide.android.rbtv`), Twitch (`tv.twitch.android.app`), YouTube Music (`com.google.android.youtube.tvmusic`) e Play Games (`com.google.android.play.games`) | `pm uninstall --user 0 <pacote>` | Prime, YouTube Music e Play Games vêm com a TV: `cmd package install-existing <pacote>`; HBO Max, Red Bull TV e Twitch: baixar de novo na Play Store |

## 03/11/2026, tela 1.3 (Continuar assistindo só com o que foi assistido)

| # | Alteração | Comando aplicado | Como desfazer |
|---|---|---|---|
| 11 | Tela 1.3: só entram itens com progresso ou abertos pelo cartão (a Globoplay põe prévias com o ponto zerado ao abrir); o cartão aberto pela tela vai para o começo na volta (o YouTube não atualiza o horário de um vídeo revisto e a Globoplay carimba a lista toda na hora em que sincroniza); a lista é relida quando um app grava nela com a tela aberta | `adb install -r apk/TelaMae-1.3.apk` | `adb install -r -d apk/TelaMae-1.2.apk` |

A Netflix não aparece no Continuar assistindo: ela não grava nada na lista desta TV, nem com o segundo plano liberado
e o sinal de inicializar programas enviado (testado com uma série tocando, 03/11/2026). O bloqueio de segundo plano
dela voltou.

## 03/11/2026, tela 1.4 (vídeo escolhido dentro do app também vai para o começo)

| # | Alteração | Comando aplicado | Como desfazer |
|---|---|---|---|
| 12 | Tela 1.4: um serviço da tela vê o título do que toca nos apps (sessão de mídia, a mesma que o controle usa) e anota; na volta, esse item do Continuar assistindo vem primeiro, mesmo escolhido dentro do YouTube. Não lê notificações: o acesso a notificações é só o que o Android exige para ver o que toca. A TCL bloqueava o serviço (`TclAppBoot`: sem `OP_AUTO_START`) até liberar a inicialização automática | `adb install -r apk/TelaMae-1.4.apk`; `cmd notification allow_listener br.gui.tvmae/br.gui.tvmae.data.PlayingListener`; `cmd appops set br.gui.tvmae APP_AUTO_START allow` e `cmd appops write-settings` | `cmd notification disallow_listener br.gui.tvmae/br.gui.tvmae.data.PlayingListener`; `cmd appops set br.gui.tvmae APP_AUTO_START default` |

Custo medido (03/11/2026, tela reiniciada e 40 s parada, duas vezes cada): 54 MB sem o serviço e 64 MB com ele, cerca
de 10 MB a mais (o Android repassa as notificações ao serviço mesmo sem ele usar; o filtro que evitaria isso só existe
a partir do Android 12). Desligar só o serviço: `cmd notification disallow_listener br.gui.tvmae/br.gui.tvmae.data.PlayingListener`.

## 03/11/2026, fotos de fundo refeitas

| # | Alteração | Comando aplicado | Como desfazer |
|---|---|---|---|
| 13 | As 9 fotos vinham do projetor em JPEG qualidade 75 (a mais leve com 14 KB) e mostravam blocos em céus e degradês escuros. Refeitas a partir dos originais em 5K: recorte central em 16:9, redução Lanczos para 1280x720 (a resolução da tela desta TV), JPEG qualidade 95 sem subamostragem de cor (81 a 315 KB cada) | `scripts/enviar-fotos.sh` | mandar as antigas de novo com o mesmo script |
