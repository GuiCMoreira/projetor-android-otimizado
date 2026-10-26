# Relatório final | Projetor Bettdow 1073

## Otimização do sistema (26/10/2026)
- Adware removido: `com.android.media.module.services` (instalado em silêncio pelo launcher do fabricante) e `com.android.docks`.
- Desativados: loja do fabricante, OfficeSuite, teclado chinês, impressão, atualizador `uota`/`uotaback`.
- Animações em 0,5x.
- Detalhes e comandos de reversão: `ALTERACOES.md`, `scripts/reverter.sh`.

## Launcher: antes e depois (versão final, 3 rodadas cada, `scripts/medir-launcher.sh`)
| | Fabricante (zeasn) | Início 1.0 | Meta da especificação |
|---|---|---|---|
| Memória (PSS) | 101 MB (101, 91, 111) | 61 MB (64, 54, 64) | < 35 MB |
| Abertura a frio | 1663 ms (1784, 1259, 1947) | 1061 ms (1397, 964, 822) | < 1000 ms |
| CPU parado | 0,0% | 0,0% | ~0% |
| Tamanho do APK | (vem no sistema) | 2,1 MB | |

Leitura honesta dos números:
- O Início usa **40% menos memória** e abre **36% mais rápido** que o launcher do fabricante.
- A meta de 35 MB **não foi atingida** e não é atingível neste aparelho: dos ~61 MB, só ~16 MB são do launcher (telas, papel de parede, ícones). Os outros ~38 MB são a base que qualquer app paga neste Android (bibliotecas do sistema). Para comparar: o Activity Launcher, um app pequeno que só mostra uma lista, usa 127 MB.
- A abertura ficou em torno de 1 s: a primeira rodada (1,4 s) inclui o cache de disco frio; as seguintes ficaram abaixo de 1 s.

## Qualidade
- 37 testes automáticos da lógica (Dock, ponto azul, calendário, recorte de foto, desfoque, datas), todos passando.
- 22 cenários de QA no aparelho: 20 PASS, 1 não se aplica (paginação: só 11 apps), 1 pendente para o teste na tela (ponto azul com reinstalação real). Detalhes: `docs/qa/resultado-launcher.md`.

## O que não foi feito
- Foto pelo pendrive (decidido para uma versão futura; fotos vão pelo computador).
- Desativar o launcher do fabricante (etapa separada, só com aprovação).
