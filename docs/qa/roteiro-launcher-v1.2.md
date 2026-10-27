# Roteiro de QA | Launcher Início 1.2

Aparelho: Bettdow 1073, ADB 192.168.2.100:5555, versão `apk/Inicio-1.2.apk`. Teclas pela trava de `scripts/qa-seguro.sh`; tela inicial aberta sempre pelo caminho do botão Início. Capturas em `launcher/build/qa/v12-NN.png`.

| # | Cenário | Passos | Esperado |
|---|---|---|---|
| 1 | Tela inicial | Abrir a tela inicial | Clima no canto superior direito; fileira "Continuar assistindo" com Boruto, NCIS e Naruto; nada sobreposto ao balão do Dock |
| 2 | Ícones | Olhar o Dock | Netflix com N médio sobre o preto; Pluto inteira amarela; Crunchyroll inteira laranja; YouTube e Play Store como antes |
| 3 | Dock para a fileira | No Dock, ↑ | Foco no Boruto (cartão cresce, contorno branco) |
| 4 | Andar na fileira | → | Foco no NCIS; linha de baixo mostra "NCIS" e os detalhes dele |
| 5 | Fileira para a barra | ↑ | Foco na barra de menu |
| 6 | Barra para a fileira | ↓ | Foco volta à fileira |
| 7 | Fileira para o Dock | ↓ | Foco volta ao Dock |
| 8 | Abrir episódio | ↑, ← até o Boruto, OK | Abre a Pluto TV (`tv.pluto.android`) no Boruto |
| 9 | Voltar | Início | Volta à tela inicial com a fileira |
| 10 | Todos os apps | Último ícone do Dock | Ícones novos também na grade (Pluto, Crunchyroll, Netflix, Miracast, Gerenciador) |
| 11 | Memória | `scripts/medir-launcher.sh br.gui.inicio/.home.HomeActivity 3` | Até 80 MB; abertura até 2 s; CPU parado 0% |
| 12 | Descanso novo | 5 min parado na tela inicial | Data e hora grandes centralizadas no alto; widgets de clima e do episódio (Boruto, capa e progresso); "Projetor da sala" e o aviso no rodapé; fotos trocando |
| 13 | Tecla qualquer | No descanso, → | Volta à tela inicial; nada abre; o foco não muda |
| 14 | OK abre o episódio | No descanso, OK | Sai do descanso e abre a Pluto TV no Boruto |
