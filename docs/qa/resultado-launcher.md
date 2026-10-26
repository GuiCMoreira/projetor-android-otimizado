# Resultado de QA | Launcher Início 1.0

Data: 26/10/2026. Versão: release `apk/Inicio-1.0.apk`. Execução: `adb shell input keyevent` + capturas em `launcher/build/qa/` (folhas `sheet-qa1` a `sheet-qa4`), conferidas uma a uma.

| # | Resultado | O que a captura ou o sistema mostra |
|---|---|---|
| 1 | PASS | Tela completa; TotalTime 850 ms (`qa-01`) |
| 2 | PASS | YouTube em foco, maior, balão "YouTube" (`qa-01`) |
| 3 | PASS | ← no primeiro mantém YouTube (`qa-03a`); 10x → para em "Todos os apps" (`qa-03b`) |
| 4 | PASS | ↑ destaca "Início" (`qa-04a`); ↓ volta para "Todos os apps", o último focado (`qa-04b`) |
| 5 | PASS | ← em "Início" fica nele (`qa-05a`); 6x → para na data/hora (`qa-05b`); ↑ não sai (`qa-05c`) |
| 6 | PASS | Menu com 5 itens e 2 divisórias (`qa-06a`); Voltar fecha e "Início" segue em foco (`qa-06b`) |
| 7 | PASS | Tecla MENU no Dock abre o menu "Início" (`qa-07`) |
| 8 | PASS | Central com 8 blocos (`qa-08a`); Keystone abre `com.zhiying.settings/.keystone.FourPointActivity` (`qa-08b`) |
| 9 | PASS | Calendário avança para dezembro de 2026 (`qa-09a`); "Ajustar data e hora…" abre `DateTimeSettingActivity` |
| 10 | PASS | Wi-Fi abre `WifiSettingActivity`; Bluetooth abre `BluetoothActivity` |
| 11 | PASS | OK no YouTube abre `com.google.android.youtube.tv/...MainActivity` |
| 12 | NÃO SE APLICA | O projetor tem 11 apps (menos de 18): não há página 2. Grade 6 x 3 e limite da linha conferidos (`qa-12`) |
| 13 | PENDENTE (tela) | Fica para o teste na tela com o dono: reinstalar o Downloader pela Play Store. Na versão de teste o ponto azul foi conferido no Pluto TV (`crop-11`) |
| 14 | PASS | Menu de contexto com "Adicionar ao Dock" (`qa-14a`); Miracast entra no fim dos favoritos (`qa-14b`) |
| 15 | PASS | 8 marcados com posições 1 a 8 (`qa-15a`); 9º mostra "Dock cheio: tire um app antes de colocar outro" (`qa-15b`) |
| 16 | PASS | Apps desmarcados (`qa-16a`); Home volta aos 4 favoritos (`qa-16b`) |
| 17 | PASS | Netflix vai para a posição 3 (`qa-17a`) e volta para a 2 (`qa-17b`) |
| 18 | PASS | Fundo "vale" com vidro nas cores dele (`qa-18a`); mantido após `am force-stop` e reabertura (`qa-18b`) |
| 19 | PASS | Foto retrato 3000x4000 aparece recortada no centro, círculo sem distorção (`qa-19a`, `qa-19b`) |
| 20 | PASS | Arquivo de texto no lugar da foto: sem travar (processo vivo), fundo cai no degradê padrão (`qa-20`); arquivo ruim some da lista (`qa-20b`) |
| 21 | PASS | Voltar 3 vezes: continua no Início (`qa-21`) |
| 22 | PASS | "Launcher original do fabricante" abre `com.zeasn.launcher.projector/...MainActivity` |

Estado restaurado ao final: degradê 1, 4 favoritos no Dock, pasta de fotos do app vazia (`qa-final`).

## Observações
- Ao voltar de uma tela aberta pelo menu "Início" (papel de parede, organizar Dock), o foco fica em "Início" na barra, não no Dock. Um ↓ volta ao Dock.
- Incidente no teste da versão de desenvolvimento: o comando usado para simular reinstalação (`pm uninstall --user 0`) removeu de vez o Downloader, que tinha vindo da Play Store. Ele pode ser reinstalado pela Play Store.

## Teste na tela
(preenchido após o teste com o dono do aparelho)
