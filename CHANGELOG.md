# Changelog

## Início 1.2 (26/10/2026)
- Fileira "Continuar assistindo" com o que você estava vendo na Pluto TV, Crunchyroll e YouTube (lista "Ver a seguir" do Android TV): capa, progresso, temporada, episódio e quanto falta. OK abre direto no episódio, no ponto em que parou. A Netflix deste aparelho não publica nessa lista.
- Capas guardadas no aparelho: depois da primeira vez, a fileira aparece em menos de meio segundo.
- Relógio e data no canto superior esquerdo e clima no direito, alinhados com a fileira.
- Ícones: apps com ícone antigo (logo sobre fundo branco) passam a preencher o quadrado inteiro com a própria cor (Pluto TV, Crunchyroll, Miracast, Gerenciador); N da Netflix em tamanho médio. Vale também para apps instalados depois.
- Descanso de tela no estilo da tela de bloqueio do macOS: data e hora grandes no alto, widgets de clima e do último episódio, "Projetor do Gui" no rodapé. OK sai do descanso e abre o episódio; as outras teclas só voltam.
- À noite o clima diz "Predomínio de céu limpo" em vez de "Predomínio de sol".
- Novas permissões: ler a lista "Ver a seguir" (`READ_EPG_DATA` e `READ_TV_LISTINGS`).
- Ícone da Pluto TV 5.x com o logo inteiro (o "tv" saía cortado).
- Ícones da HBO Max e do Stremio sem cortes: o logo da HBO encostava nas bordas e o losango do Stremio saía cortado nas pontas.
- "Continuar assistindo" mostra no máximo 4 blocos: um 5º sairia cortado na borda da tela e o foco iria para fora dela.
- Central de Controle: bloco "Liberar memória", que fecha os apps parados em segundo plano (nunca a tela inicial, o sistema ou o projetor) e mostra quanto voltou. No teste, a memória livre foi de 176 MB para 343 MB. Bom para usar antes de abrir apps pesados como HBO Max e Stremio.
- Descanso de tela: nova opção de 2 minutos parado (menu Início > Descanso de tela). Ele só aparece na tela inicial, nunca dentro de um app.

## Início 1.1 (26/10/2026)
- Widget de clima abaixo do relógio (Open-Meteo, sem conta). Cidade detectada pela internet na primeira vez; "Cidade do clima…" no menu Início para buscar outra.
- Papel de parede que troca sozinho entre as fotos enviadas (a cada 1, 3 ou 6 horas, ou 1 vez por dia), com esmaecimento.
- Descanso de tela estilo Apple TV após 5 min parado (fotos com zoom lento, relógio e clima). Ajustável no menu: desligado, 5, 10, 15 ou 30 min.
- Central de Controle com volume de verdade (← →) e o bloco "Timer para desligar", que abre o "Desligamento programado" do projetor.
- Correções da revisão da 1.0: foco volta ao Dock, Bluetooth atualizado, fotos minúsculas, panoramas, EXIF, preferências corrompidas, mover no Dock, páginas de Todos os apps.
- Nova permissão: internet (só para o clima).
- Correções da revisão final: a troca de papel de parede não acumula mais as fotos anteriores na memória; o clima busca em fila própria (sem internet de verdade, o Dock não fica vazio) e espera 10 min depois de uma falha; segurar uma tecla para sair do descanso não move o foco; o botão Início sai do descanso; trocar de cidade durante a busca não mostra o clima da anterior.

## Início 1.0 (26/10/2026)
- Tela inicial estilo macOS: relógio grande, barra de menu de vidro navegável, Dock de vidro com favoritos e atalhos.
- Todos os apps (Launchpad) com páginas, ponto azul em app novo e menu de contexto.
- Central de Controle (foco, keystone, zoom, modo de imagem, projeção, som, fontes, ajustes) e calendário.
- Organizar Dock, papel de parede (degradês, fotos incluídas, fotos enviadas pelo computador).
- Sem internet, sem serviços em segundo plano, uma única permissão.
