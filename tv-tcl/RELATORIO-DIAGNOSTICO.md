# TV da avó | diagnóstico de fábrica (02/11/2026)

Só leitura: nada foi alterado na TV. Arquivos brutos em `diagnostico/` (o `getprop.txt` fica fora do git: tem
número de série).

## O aparelho

| | |
|---|---|
| Modelo | TCL "Smart TV", placa C06 (`C06_2K_GB`), firmware AR2101 |
| Sistema | Android TV 11 (interface de TV do Google, não Google TV), versão de usuário (sem acesso root) |
| Processador | MediaTek MT5867, 4 núcleos ARMv7, 32 bits |
| Memória | 940 MB no total, cerca de 330 MB livres; o sistema se declara "pouca memória" (`ro.config.low_ram`) |
| Interface | 1280x720, densidade 213 |
| Armazenamento | 2,5 GB para apps e dados, 80% ocupado (516 MB livres) |
| Entradas | TV digital (antena, `com.tcl.tvinput/.TDTVInputService`) e HDMI |
| Rede | ADB em 192.168.2.120:5555 (IP do roteador de casa; muda na casa da avó) |
| Tela inicial | `com.google.android.tvlauncher` (a do Google); reserva `com.android.tv.settings/.system.FallbackHome` |

## Por que trava

- **A tela inicial do Google é o maior peso**: 157 MB de memória (PSS) sozinha, com fileiras de filmes, banner de
  propaganda (Paramount+ na captura) e recomendações baixadas o tempo todo.
- **Serviços que ela nunca vai usar rodando sempre**: Chromecast embutido (`mediashell`, 100 MB em dois processos),
  busca por voz (`katniss`, 46 MB), Play Store (39 MB), Play Services (70 MB), recomendações e descanso de tela do
  Google (`backdrop`), e os da TCL (`guard`, `ttvs`, `partnercustomizer`, `tv`).
- **Armazenamento quase cheio**: 516 MB livres. Os dados do Play Services ocupam 751 MB; a tela inicial do Google
  guarda 64 MB de dados e 59 MB de cache; o Chromecast ocupa 158 MB de atualização.
- **Animações em velocidade normal** (1,0), num aparelho que não acompanha.

## Apps com ícone

Netflix (versão de TV, `com.netflix.ninja`, a TCL é certificada), YouTube, Prime Video, Google Play Filmes,
Play Store, Configurações e os da TCL: `dashboard`, `guard`, `partnercustomizer`, `smartalexa`, `suspension`,
`t_solo`, `tv`, `ui_mediaCenter`. Já vinham desativados de fábrica: Stan, YouTube Music, Rakuten TV, Play Games.

Nenhum app instalado pelo usuário.

## O que a avó vai usar (combinado com o neto)

Globoplay (Globo ao vivo de graça com conta), YouTube e Pluto TV, como no projetor. Tela inicial simples, com
poucos botões grandes, só setas, OK, Voltar e volume. Uma tela "Todos os apps" para o neto escolher o que fica fixo.
A TV fica aqui até tudo estar pronto e depois vai para a casa dela (outra rede Wi-Fi).
