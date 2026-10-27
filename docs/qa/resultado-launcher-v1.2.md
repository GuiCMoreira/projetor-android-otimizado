# Resultado de QA | Launcher Início 1.2

Data: 26/10/2026. Versão: `apk/Inicio-1.2.apk` (release). Capturas em `launcher/build/qa/v12-*.png`. Teclas pela trava de `scripts/qa-seguro.sh`; a tela inicial aberta pelo caminho do botão Início.

| # | Resultado | Evidência |
|---|---|---|
| 1 | PASS | Clima no canto superior direito, relógio no esquerdo, fileira com Boruto, NCIS e Naruto; nada sobreposto ao balão do Dock (`v12-09`, `v12-51`) |
| 2 | PASS | Netflix com N médio; Pluto inteira amarela; Crunchyroll inteira laranja; YouTube e Play Store como antes (`v12-09`) |
| 3 | PASS | ↑ no Dock: foco no Boruto |
| 4 | PASS | →: foco no NCIS e a linha de baixo troca |
| 5 | PASS | ↑: foco na barra ("Início") |
| 6 | PASS | ↓: foco volta à fileira |
| 7 | PASS | ↓: foco volta ao Dock (YouTube) |
| 8 | PASS | OK no Boruto abre o player da Pluto no episódio, com a Pluto fechada e também com ela já aberta (log `abrir "Boruto..."` e `VideoPlayerActivity`) |
| 9 | PASS | Início volta à tela inicial com a fileira (`v12-34`) |
| 10 | PASS | Todos os apps com os ícones novos (`v12-40`) |
| 11 | REGISTRADO | Sem a Pluto na memória: 68/71/57 MB, abertura 1060/810/745 ms. Com a Pluto aberta em segundo plano: 67/76/71 MB, abertura 2109/2101/2149 ms. CPU parado 0,0% |
| 12 | PASS | Descanso: data e hora grandes, widgets de clima e episódio, "Projetor do Gui" (`v12-41`) |
| 13 | PASS | → sai do descanso; o foco continua no YouTube (`v12-42`) |
| 14 | PASS | OK no descanso abre o episódio: link às 23:44:33, player às 23:44:52 (log do sistema) |

## Defeitos encontrados e corrigidos durante o QA
- Fileira demorava mais de 8 s para aparecer (capas baixadas a cada abertura): capas guardadas no aparelho, agora 0,2 a 0,6 s.
- Pluto já aberta ignorava o link do episódio e ficava na tela em que estava: o episódio abre numa tarefa nova.
- Boruto duplicado: a Pluto publicou o mesmo episódio de novo com outro título, capa e link; entradas com o mesmo código de episódio viram uma só.
- Widget do clima do descanso cortava a linha de máxima e mínima; à noite dizia "Predomínio de sol": textos e tamanhos ajustados ("Céu quase limpo").
- Primeira tentativa de teste: o gerenciador de memória do fabricante (`com.sstar.systemcontrol`) fechou a launcher aberta direto pelo ADB; os scripts passaram a abrir pelo caminho do botão Início.

## Correções da revisão de código
- Com o menu aberto, a seta para baixo podia levar o foco à fileira: a fileira entrou na trava de foco dos painéis.
- Uma capa lenta (Wi-Fi sem internet) segurava a fileira inteira: a fileira aparece primeiro com as capas guardadas e as que faltam chegam depois, cada uma com prazo de 15 s; capa que falhou só é tentada de novo depois de 10 min.
- Capa gravada pela metade: gravação atômica (temporário e renomear); download único por capa, limitado a 8 MB.
- Falha de capa agora fica no log.
- Link de episódio só abre o app que publicou o episódio (nunca a própria tela inicial ou um terceiro).
- Fileira que esvazia com o foco nela devolve o foco ao Dock.
