# Projetor Bettdow 1073 | otimização e launcher Início

## Instalar ou atualizar o launcher
    adb connect 192.168.2.100:5555
    adb install -r apk/Inicio-1.1.apk

Para gerar de novo: `cd launcher && ./gradlew :app:assembleRelease`. A chave `launcher/inicio.jks` e o `launcher/keystore.properties` ficam fora do git; guarde uma cópia. Sem eles, atualizar exige desinstalar antes.

## Tornar o Início a tela padrão
No controle, aperte Início (casinha). Na lista que aparecer, escolha "Início" e "Sempre".

## Voltar para a tela do fabricante
Pelo próprio launcher: menu "Início" > "Launcher original do fabricante".
De vez: `adb uninstall br.gui.inicio` (o Android volta sozinho para a tela do fabricante).

## Papel de parede com foto sua
    scripts/enviar-wallpaper.sh minha-foto.jpg

Depois: menu "Início" > "Papel de parede…" > Fotos. A foto é recortada e reduzida para 1920x1080 no computador antes de ir.

## Clima, descanso de tela e troca de fundo (1.1)
- Clima: cidade detectada pela internet na primeira vez. Para trocar: menu "Início" > "Cidade do clima…".
- Descanso de tela: menu "Início" > "Descanso de tela" (cada OK passa para a próxima opção).
- Troca automática de fundo: menu "Início" > "Papel de parede…" > fileira "Trocar sozinho entre as fotos". Escolher uma foto específica desliga a troca.
- Timer para desligar: barra de cima > "Controle" > "Timer para desligar" (abre o "Desligamento programado" do projetor).

## Testes por ADB com trava de segurança
    source scripts/qa-seguro.sh
    ok "Texto do item"   # só aperta OK se a tela for do launcher e o item focado for o esperado

## Medir desempenho
    scripts/medir-launcher.sh br.gui.inicio/.home.HomeActivity 3

## Documentos
- `RELATORIO-DIAGNOSTICO.md`: diagnóstico inicial.
- `ALTERACOES.md` e `scripts/reverter.sh`: tudo o que foi alterado e como desfazer.
- `RELATORIO-FINAL.md`: antes e depois.
- `docs/superpowers/specs/` e `docs/superpowers/plans/`: especificação e plano do launcher.
- `docs/qa/`: roteiro e resultado dos testes.
- `docs/CREDITOS.md`: fonte e fotos.
