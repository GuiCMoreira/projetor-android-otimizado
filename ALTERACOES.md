# Registro de alterações | Projetor Bettdow 1073

Para desfazer tudo: `scripts/reverter.sh` (ou os comandos individuais abaixo).

## 26/10/2026, etapa 2 (aprovada pelo dono do aparelho)

| # | Alteração | Comando aplicado | Como desfazer |
|---|---|---|---|
| 1 | Removido adware `com.android.media.module.services` | `adb uninstall com.android.media.module.services` | `adb install backup/apk/com.android.media.module.services/base.apk` |
| 2 | Removido `com.android.docks` (já vinha desativado) | `adb uninstall com.android.docks` | `adb install backup/apk/com.android.docks/base.apk` |
| 3 | Desativada loja do fabricante | `pm disable-user --user 0 zeasn.open.technical.tv.store` | `pm enable zeasn.open.technical.tv.store` |
| 4 | Desativado OfficeSuite | `pm disable-user --user 0 com.mobisystems.editor.office_registered` | `pm enable com.mobisystems.editor.office_registered` |
| 5 | Desativado teclado chinês | `pm disable-user --user 0 com.android.inputmethod.pinyin` | `pm enable com.android.inputmethod.pinyin` |
| 6 | Desativado serviço de impressão | `pm disable-user --user 0 com.android.printspooler` | `pm enable com.android.printspooler` |
| 7 | Desativado atualizador de firmware | `pm disable-user --user 0 com.uv.droid.uota` e `com.uv.droid.uotaback` | `pm enable` nos dois |
| 8 | Animações em 0.5x | `settings put global {window,transition}_animation_scale 0.5` e `animator_duration_scale 0.5` | mesmos comandos com `1.0` |

Hashes SHA-256 dos APKs salvos:
- `com.android.media.module.services`: `146cf9897933ce6c2771cf5a1363387c30930f2a02fcd043b14507b384d95638`
- `com.android.docks`: `2a0cf5bbc68318925e68dedaf6b5b30e7ad58ab59dbe75c56eb4225859e2434b`

## Verificação logo após aplicar

- Os dois apps removidos não aparecem mais em `pm list packages`.
- Os 6 desativados aparecem em `pm list packages -d`. Nenhum processo `uota` rodando.
- YouTube, Netflix, Crunchyroll, Pluto TV, Projectivy, Play Store e Gboard continuam ativos. Gboard segue como teclado padrão.
- `MemAvailable`: 140 MB antes (com 3 apps abertos), 233 MB depois. Não é uma comparação justa; a medição definitiva será feita após reiniciar.

## 26/10/2026, launcher Início

| # | Alteração | Comando aplicado | Como desfazer |
|---|---|---|---|
| 9 | Instalado o launcher Início 1.0 (`br.gui.inicio`) | `adb install apk/Inicio-1.0.apk` | `adb uninstall br.gui.inicio` |
| 10 | Início como tela padrão (feito pelo ADB). Neste firmware o botão Início abre `com.newlink.provision/.FallbackHome`, que lê `settings global nl_curr_home_launcher` e abre MAIN+LAUNCHER do pacote | `cmd package set-home-activity br.gui.inicio/.home.HomeActivity` e `settings put global nl_curr_home_launcher br.gui.inicio` (o launcher do fabricante regrava essa chave quando roda, por isso foi encerrado antes com `am force-stop`) | `settings put global nl_curr_home_launcher com.zeasn.launcher.projector` e `cmd package set-home-activity com.zeasn.launcher.projector/com.zeasn.launcher.ui.WelcomeActivity` |
| 11 | Downloader (`com.esaba.downloader`) removido por engano durante os testes | `pm uninstall --user 0 com.esaba.downloader` | reinstalar pela Play Store |
| 12 | Launcher do fabricante desativado (foi quem instalou o adware; com ele ativo, regrava `nl_curr_home_launcher`) | `pm disable-user --user 0 com.zeasn.launcher.projector` | `pm enable com.zeasn.launcher.projector` (e os comandos da linha 10) |

| 13 | Launcher Início atualizado para a 1.1 (clima, descanso de tela, troca de fundo, volume, timer). Nova permissão: internet | `adb install -r apk/Inicio-1.1.apk` | `adb install -r -d apk/Inicio-1.0.apk` (volta para a 1.0) |
| 14 | Launcher Início atualizado para a 1.2 (Continuar assistindo, ícones, relógio à esquerda, descanso estilo macOS). Novas permissões: `READ_EPG_DATA` e `READ_TV_LISTINGS` (concedida pelo próprio firmware na instalação) | `adb install -r apk/Inicio-1.2.apk` | `adb install -r -d apk/Inicio-1.1.apk` (volta para a 1.1) |
| 15 | Pluto TV compilada por completo (antes: só a parte mais usada). Navegação com menos travadas (quadros travados de 37-55% para 22-47%); abertura igual, uns 12 s | `cmd package compile -m speed -f tv.pluto.android` | `cmd package compile -m speed-profile -f tv.pluto.android` (atualização da Pluto pela Play Store também desfaz) |
| 16 | Projectivy Launcher (`com.spocky.projengmenu` 4.72, da Play Store) desinstalado a pedido do dono: a Início faz o que ele fazia | `adb uninstall com.spocky.projengmenu` | reinstalar pela Play Store |
| 17 | Pluto TV trocada da 17.2.0 (Play Store, base do app da Paramount+) pela 5.65.0-leanback (feita para TV, de antes do redesenho de setembro de 2026), baixada do APKMirror com assinatura oficial conferida (`29946c49…e96c`) e compilada por completo. Guia de canais: 11-13% de quadros travados contra 22-55% na 17.2. Pede conta grátis da Pluto | `scripts/trocar-pluto.sh leve` | `scripts/trocar-pluto.sh atual` (cópia da 17.2 em `backup/apk`) |
| 18 | Netflix de fábrica (8.87, de abril de 2025, em `/system/app`) parou de abrir com o erro -1007 ("Não foi possível acessar o serviço Netflix"): rede e DNS ok, o servidor recusava a versão antiga. Atualizada para a 9.89.0 (APKMirror, mesmo certificado oficial da de fábrica para Android até 12: `363863…4e6b`). A Netflix para TV (`com.netflix.ninja` 13.2.1) foi testada e descartada: trava no motor de vídeo neste aparelho | `adb install -r netflix-9.89.0.apk` | `adb uninstall com.netflix.mediaclient` (remove só a atualização e volta para a 8.87 de fábrica) |

Nota sobre a Netflix (27/10/2026): a 9.89 é a versão de celular e não desenha foco. O firmware tem um ajuste escondido no `framework.jar` (`ViewRootImpl$ViewPostImeInputStage`, log "NetFilx Hook") que simula o foco e guia as setas, mas procura os botões por IDs fixos da 8.87; na 9.89 ele se perde e as setas viram volume. O ajuste liga pela propriedade `sys.nl.netflix.simulate.focus.control` (padrão ligado, lida quando a base dos apps inicia; `setprop ... false` + reiniciar a base desliga até o próximo boot). As versões para TV (`com.netflix.ninja` 13.2.1 trava; 8.3.11 dá "não é compatível com seu dispositivo (-13)") não servem: o projetor não tem certificação da Netflix. Solução adotada: usar a Netflix com o mouse aéreo do controle.

| 19 | Stremio 1.12.0 (`com.stremio.one`) instalado pela Play Store e logado na conta do dono (os addons vêm da conta). Usa ~110 MB de memória | Play Store | `adb uninstall com.stremio.one` |
| 20 | HBO Max: a Play Store do projetor diz "device isn't compatible". A versão para TV (`com.wbd.stream` 7.14.0.69, APKMirror) instala mas fica presa na abertura: o servidor responde vazio ao `bootstrap` (aparelho sem certificação). Ficou a versão de celular 7.14.0.530 (APKMirror, mesmo certificado `619cbb…73a8` nas 35 partes), para usar com o mouse aéreo | `adb install-multiple base.apk split_config.armeabi_v7a.apk split_config.pt.apk split_config.en.apk split_config.xhdpi.apk` | `adb uninstall com.wbd.stream` |
| 21 | HBO Max de celular 7.14.0.530 travava na escolha de perfil (app pesado demais: 145 MB, quadros pulados) e a 6.10.2.2 é recusada ("É hora de atualizar"). Ficou a 7.0.0.82 (APKMirror, certificado `619cbb…73a8`): entra no perfil, toca vídeo e som (testado com A Casa do Dragão). Qualidade limitada a 768x384 pela proteção de vídeo básica do projetor (Widevine L3). A Play Store pode atualizar sozinha para a 7.14 | `adb install-multiple -r` da 7.0.0.82 | `adb uninstall com.wbd.stream` |
| 22 | HBO Max (`com.wbd.stream` 7.0.0.82) e Stremio (`com.stremio.one` 1.12.0) desinstalados a pedido do dono, que não vai usá-los. Mesmo sem abrir, os dois voltavam a rodar sozinhos (gatilhos de inicialização e de notificação): ~36 MB da HBO e ~10 MB do servidor do Stremio ocupados o tempo todo | `adb uninstall com.wbd.stream` e `adb uninstall com.stremio.one` | Stremio pela Play Store; HBO pelo caminho da linha 21 |
| 23 | Launcher Início atualizado para a 1.3 (mouse aéreo, aviso de pouca memória, tela de emergência após 3 quedas em 5 minutos). Sem permissão nova | `adb install -r apk/Inicio-1.3.apk` | `adb install -r -d apk/Inicio-1.2.apk` (volta para a 1.2) |
| 24 | Launcher Início atualizado para a 1.4 (descanso de tela desenhado a 20 quadros por segundo, setas de volta na Central de Controle, sem "Liberar memória" e sem "Modo de imagem"). Sai a permissão `KILL_BACKGROUND_PROCESSES` | `adb install -r apk/Inicio-1.4.apk` | `adb install -r -d apk/Inicio-1.3.apk` |

Nota (29/10/2026): conferido que nenhum pacote desativado fornece entrada de vídeo. O HDMI vem de `com.sstar.tv.inputservice` (ativo). Os atualizadores `com.uv.droid.uota` e `uotaback` têm a permissão `TV_INPUT_HARDWARE`, mas não registram entrada nenhuma.

O launcher do fabricante continua instalado, mas desativado. Conferido após reiniciar: a tela padrão continua sendo o Início.
