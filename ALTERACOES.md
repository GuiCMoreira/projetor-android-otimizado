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

O launcher do fabricante continua instalado, mas desativado. Conferido após reiniciar: a tela padrão continua sendo o Início.
