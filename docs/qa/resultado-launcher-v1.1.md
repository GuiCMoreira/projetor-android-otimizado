# Resultado de QA | Launcher Início 1.1

Data: 26/10/2026. Versão: `apk/Inicio-1.1.apk` (release). Capturas em `launcher/build/qa/v11-*.png`. A partir do cenário 9, as teclas foram enviadas com a trava de `scripts/qa-seguro.sh` (só aperta OK se a tela for do launcher e o item focado for o esperado).

| # | Resultado | Evidência |
|---|---|---|
| 1 | PASS | Cartão: 14°, Parcialmente nublado, Bragança Paulista, máx 21° mín 13° (`v11-01`) |
| 2 | PASS | Fundo é uma das 4 fotos enviadas; às 20:00 trocou sozinho para a foto da hora seguinte (`v11-12`) |
| 3 | PASS | Pílula "A cada 1 hora" fica azul; → na última pílula não sai da fileira; a tela não fecha (`v11-03a`, `v11-03b`) |
| 4 | PASS | Ao sair do papel de parede o foco volta ao Dock (`v11-04`) |
| 5 | PASS | Volume de mídia 20 → 22 → 20 no sistema (`dumpsys audio`); barra e % acompanham (`v11-05a`, `v11-05b`) |
| 6 | PASS | "Timer para desligar" abre `com.zhiying.settings/.general.GeneralSettingActivity` com "Desligamento programado" (`v11-06b`) |
| 7 | PASS | Menu com "Cidade do clima…" e "Descanso de tela: 5 min"; item do launcher desativado não aparece mais (`v11-07`) |
| 8 | PASS | Aviso "Descanso de tela depois de 10 min parado"; ciclo volta para 5 min (`v11-08a`, `v11-08b`) |
| 9 | PASS | Busca "Campinas" lista 5 cidades; teclado fecha sozinho; escolhida, o cartão mostra Campinas 16° Predomínio de sol, ícone de lua à noite (`v11-09a`, `v11-09b`) |
| 10 | PASS | "Detectar pela internet" volta para Bragança Paulista (`v11-10b`) |
| 11 | PASS | Após 5 min parado: foto em tela cheia, relógio e data no canto, linha do clima; 20 s depois, foto seguinte (`v11-11a`, `v11-11b`) |
| 12 | PASS | Tecla → sai do descanso e é consumida: o foco no Dock não mudou (`v11-12`) |
| 13 | PASS | Todos os apps normal; o launcher do fabricante (desativado) não aparece mais (`v11-13`) |
| 14 | REGISTRADO | Memória 69/60/67 MB (média 65; 1.0 tinha 61 com degradê). Abertura 1301/1279/1517 ms. CPU parado 0,0% |

## Defeitos encontrados e corrigidos durante o QA
- "Launcher original do fabricante" aparecia no menu com o app desativado: o atalho agora confere se o app está ativo.
- Na fileira "Trocar sozinho", → na última opção saltava para as fotos: as pontas da fileira agora seguram o foco.
- Central de Controle ocupava a tela inteira (filho sem tamanho explícito): grade com WRAP_CONTENT.
- Na busca de cidade, o teclado ficava aberto sobre a lista e as setas iam para ele: o teclado fecha quando os resultados chegam.

## Correções da revisão final (depois do QA acima)
| Achado | Correção | Teste |
|---|---|---|
| C1 troca de fundo guardava a foto anterior | Esmaecimento parte só da camada de cima e, terminado, fica só a foto nova | Aparelho: memória (abaixo) |
| C2 clima na mesma fila do Dock | Fila de rede própria; nova tentativa no máximo a cada 10 min | `V11FixesTest` (4 casos) |
| I1 tecla segurada vazava do descanso | A tecla que acorda é consumida até soltar | `V11FixesTest` (3 casos) + aparelho |
| I2 Início não saía do descanso | Início para o descanso e reinicia a contagem | Aparelho |
| I3 corrida na troca de cidade | Clima só é gravado se a cidade não mudou; cidade detectada não sobrescreve a escolhida | Revisão de código |

No aparelho, com a versão corrigida (capturas `launcher/build/qa/fix-*.png`):
- I2 PASS: após 5 min parado entrou o descanso; botão Início voltou à tela inicial com foco no YouTube (`fix-02`, `fix-03`).
- I1 PASS: novo descanso; → segurado (`--longpress`) só acordou, o foco continuou no YouTube (`fix-04`, `fix-05`).
- Memória depois de dois ciclos de descanso: 51 MB (PSS), sem acúmulo. Medição limpa: 69/65/64 MB, abertura 1255/1377/1547 ms, CPU parado 0,0%.

## Incidente de teste (sem efeito no aparelho)
- Uma sequência de teclas às cegas entrou na tela de Bluetooth do sistema e abriu o menu do controle remoto `TXCZ_RCM` com "desconectar" em destaque. Nenhuma tecla foi apertada nesse menu: ele foi fechado com Voltar e o controle continuou conectado (`dumpsys bluetooth_manager`: STATE_CONNECTED). Depois disso, todo OK passou pela trava de `scripts/qa-seguro.sh`.

## Estado ao final
Troca automática a cada 1 hora, descanso em 5 min, cidade detectada pela internet (Bragança Paulista), volume em 20 (o valor inicial).
