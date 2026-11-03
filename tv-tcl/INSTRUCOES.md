# TV da avó | instruções

## Para quem estiver com ela

**Ligar e assistir**

1. Ligue a TV. Ela abre direto na tela com "Bom dia", "Boa tarde" ou "Boa noite" e o nome dela (configurado em `launcher/app/src/main/res/values/strings.xml`).
2. Use as **setas** para escolher o app (Globoplay, YouTube ou Pluto TV) e aperte **OK**.
3. Para sair de um app, aperte o botão **Início** (a casinha) do controle. Ela sempre volta para essa tela.

**A Globo ao vivo** fica no Globoplay, de graça: abra o Globoplay e escolha a TV Globo ao vivo.

**Se algo estranho acontecer**

- **A tela travou ou ficou preta:** desligue a TV pelo controle, espere 10 segundos e ligue de novo.
- **Apareceu uma tela escura dizendo "A tela inicial teve um problema":** aperte **OK** em "Tentar a tela inicial de
  novo". Os apps também aparecem ali e abrem normalmente.
- **Apareceu um aviso de atualização do sistema:** pode apertar **Cancelar**. Quem cuida da TV atualiza depois.
- **Sem internet:** confira se o roteador está ligado. Na tela inicial, o relógio e os botões continuam funcionando;
  só os apps precisam de internet.

## Para o neto

**Escolher os apps da tela inicial:** "Todos os apps", embaixo dos botões. Ali aparecem só Configurações, Globoplay,
Netflix, Play Store, Pluto TV e YouTube; as peças da TCL que precisam ficar no sistema (Protetor de Segurança, Quick
Panel, TCL Home, TV, Alexa) foram escondidas da lista, mas os botões do controle continuam abrindo cada uma. OK abre o app; **segurar OK** fixa ou tira
da tela inicial (até 6). Com 4 apps, fica uma fileira só; com 5 ou 6, duas fileiras de 3.

**Wi-Fi na casa dela:** Configurações > Rede e Internet. A depuração pela rede continua ligada; o IP muda com o
roteador, e `scripts/achar-tv.sh` acha a TV na rede (`./scripts/achar-tv.sh 192.168.0` se a rede for outra).

**Coisas que não podem voltar sozinhas, e o que fazer:**

| O que | Por quê | O que fazer |
|---|---|---|
| Atualização automática da Play Store | Ela trocaria a Pluto 5.65 pela versão pesada (já aconteceu uma vez) | Deixar em Play Store > Configurações > "Não atualizar apps automaticamente" |
| Atualização do sistema da TCL | Pode religar apps desativados | Depois de atualizar, conferir com `adb shell pm list packages -d` contra o `ALTERACOES.md` e desativar de novo com `scripts/desativar.sh` |
| Alexa da TCL | O atualizador da TCL a religa em todo reinício | Nada: ela não fica rodando nem aparece na tela inicial |
| Netflix sem pré-carregar | Uma atualização da Netflix ou do sistema pode apagar a regra | Conferir com `adb shell cmd appops get com.netflix.ninja RUN_ANY_IN_BACKGROUND` (tem que dizer `ignore`) |

**Desfazer tudo:** `scripts/reverter.sh` devolve a tela do Google e reativa o que foi desativado. Cada mudança, com
o comando para desfazer, está no `ALTERACOES.md`.
