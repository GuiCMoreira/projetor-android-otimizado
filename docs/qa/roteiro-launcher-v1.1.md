# Roteiro de QA | Launcher Início 1.1

Aparelho: Bettdow 1073, ADB 192.168.2.100:5555, versão `apk/Inicio-1.1.apk`. Execução por `adb shell input keyevent` + captura em `launcher/build/qa/v11-NN.png`. PASS só com captura ou saída do sistema conferida.

| # | Cenário | Passos | Esperado |
|---|---|---|---|
| 1 | Clima | Abrir a tela inicial | Cartão de vidro abaixo do relógio com ícone, temperatura, condição, cidade (Bragança Paulista), máxima e mínima |
| 2 | Troca automática de fundo | Abrir a tela inicial com troca "A cada 1 hora" | Fundo é uma das 4 fotos enviadas (a da hora atual) |
| 3 | Pílulas da troca | Menu Início > Papel de parede > fileira "Trocar sozinho" > "A cada 3 horas" | Pílula fica azul, a tela não fecha; voltar para "A cada 1 hora" |
| 4 | Foco volta ao Dock (M6) | Sair da tela de papel de parede com Voltar | Foco no Dock, não na barra |
| 5 | Volume | Central de Controle; foco na linha do volume; → duas vezes; ← duas vezes | Barra e porcentagem mudam; volume do sistema muda e volta ao valor inicial |
| 6 | Timer para desligar | Central de Controle > "Timer para desligar" | Abre `com.zhiying.settings/.general.GeneralSettingActivity` (Outros ajustes) |
| 7 | Menu Início | OK em "Início" | Itens novos "Cidade do clima…" e "Descanso de tela: 5 min" |
| 8 | Ciclo do descanso | OK em "Descanso de tela" | Aviso "Descanso de tela depois de 10 min parado"; repetir até voltar a 5 min |
| 9 | Cidade | Menu > Cidade do clima…; digitar "Campinas"; OK; escolher o 1º | Lista com Campinas; ao escolher, o cartão mostra Campinas |
| 10 | Detectar de novo | Menu > Cidade do clima… > "Detectar pela internet" | Cartão volta para Bragança Paulista |
| 11 | Descanso de tela | Ficar 5 min sem tecla na tela inicial | Foto em tela cheia, relógio e data no canto, linha do clima |
| 12 | Sair do descanso | Apertar OK | Volta à tela inicial; nenhum app abre |
| 13 | Todos os apps | Último ícone do Dock | Grade normal (regressão) |
| 14 | Memória | `scripts/medir-launcher.sh br.gui.inicio/.home.HomeActivity 3` | Registrar e comparar com os 61 MB da 1.0 |

Ao final: troca automática em 1 hora, descanso em 5 min, cidade detectada pela internet, volume no valor inicial.
