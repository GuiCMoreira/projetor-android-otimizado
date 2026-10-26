# Launcher "Início" | Especificação de design

Data: 26/10/2026
Aparelho: projetor Bettdow 1073 (SigmaStar `pioneer5`, Android 12 / API 32, perfil low RAM, 2 GB com ~1 GB útil, tela 1920x1080)
Protótipo aprovado: versão H ("macOS"), em `.superpowers/brainstorm/*/content/design-h.html`

## 1. Objetivo

Substituir o launcher do fabricante (`com.zeasn.launcher.projector`, 87 MB, que instalou um adware em silêncio) por uma tela inicial própria, bonita no estilo macOS, que use bem menos memória e abra instantaneamente.

### Critérios de sucesso (medidos no aparelho)

| Critério | Meta | Como medir |
|---|---|---|
| Memória do launcher em repouso | < 35 MB PSS | `dumpsys meminfo <pacote>` (TOTAL PSS) |
| Abertura a frio | < 1 s | `am force-stop` + `am start -W` (TotalTime) |
| CPU com a tela parada | ~0% | `top -n 1` filtrando o processo |
| Navegação | 100% pelo controle (← → ↑ ↓ OK Voltar) | Roteiro de teste com `input keyevent` + captura de tela |
| Apps mantidos | YouTube, Netflix, Crunchyroll, Pluto TV abrem pelo Dock | Teste na tela |
| Volta atrás | Launcher original acessível pelo menu e pelo `pm uninstall` | Teste na tela |

### Fora de escopo (decidido)

- Foto pelo pendrive (fica para uma versão futura; fotos vão pelo computador via ADB).
- Fileiras de recomendações, "continue assistindo", widgets, previsão do tempo, qualquer acesso à internet.
- Bibliotecas de interface (Jetpack Compose, AndroidX pesado), bibliotecas de imagem, analytics.
- Desativar o launcher do fabricante. Isso só acontece depois de o novo ser aprovado na tela, como etapa separada e com aprovação própria.

## 2. Interface

### 2.1 Tela inicial

```
+--------------------------------------------------------------+
| Início                                  ᛒ  Wi-Fi  seg 26 20:45 |  <- barra de menu (vidro, 56 px)
|                                                              |
|                  segunda-feira, 26 de outubro                |
|                           20:45                              |  <- relógio grande (Inter Bold)
|                                                              |
|                                                              |
|        [YT] [N] [C] [Pluto] | [Play] [HDMI] [Ajustes]        |  <- Dock (vidro)
+--------------------------------------------------------------+
```

- Fundo: degradê ou foto (ver 2.4).
- Barra de menu: "Início" à esquerda; à direita, ícone de Bluetooth (só quando ligado), estado do Wi-Fi e data/hora curtas.
- Relógio: data por extenso em pt-BR acima, hora em 24 h, centralizados.
- Dock: favoritos à esquerda da divisória, atalhos fixos à direita.

### 2.2 Navegação pelo controle

| Tecla | Na tela inicial |
|---|---|
| ← → | Move o foco entre os ícones do Dock |
| ↑ | Leva o foco para "Início" na barra de menu |
| ↓ (estando na barra) | Volta para o Dock, no último ícone focado |
| OK | Abre o app focado / abre o menu "Início" |
| Voltar | Fecha o menu ou tela aberta; na tela inicial não faz nada |
| Menu (se o controle tiver) | Abre o menu "Início" |

Foco: o ícone focado cresce (escala 1,35 e sobe 16 px) com animação de 150 ms feita pela GPU, e um balão de vidro com o nome aparece acima. Focar não abre; só o OK abre (regra do tvOS, `focus-and-selection.md`). Ao abrir o launcher, o foco começa no primeiro ícone do Dock.

### 2.3 Menu "Início"

Painel de vidro abaixo de "Início", com:

1. Papel de parede…
2. Organizar Dock…
3. (divisória)
4. Ajustes do projetor → intent `android.settings.SETTINGS` (no aparelho resolve para `com.zhiying.settings/.MainActivity`)
5. Fontes de entrada (HDMI) → intent `android.settings.SOURCE` (resolve para `com.newlink.source/.SourceUIActivity`)
6. (divisória)
7. Launcher original do fabricante → abre `com.zeasn.launcher.projector/com.zeasn.launcher.ui.MainActivity` se estiver instalado e ativo; senão o item não aparece.

### 2.4 Papel de parede

- Tela com duas fileiras: **Degradês** (4, desenhados por código, sem imagem) e **Fotos** (3 incluídas no app + as enviadas pelo computador).
- Fotos incluídas: 3 imagens de paisagem com licença livre (Unsplash License), reduzidas para 1920x1080 em JPEG antes de entrar no projeto.
- Fotos enviadas: o app lê a pasta própria `Android/data/<pacote>/files/wallpapers/` (sem pedir permissão de armazenamento). O script `scripts/enviar-wallpaper.sh foto.jpg` reduz a foto para 1920x1080 no computador e envia por `adb push`.
- A escolha fica salva e vale até ser trocada.

### 2.5 Organizar Dock

- Lista de todos os apps abríveis do aparelho (categorias `LEANBACK_LAUNCHER` e `LAUNCHER`), em ordem alfabética, cada um com ícone e uma marca "no Dock".
- OK alterna a marca. Num app marcado, ← → mudam a posição dele no Dock.
- Atalhos fixos (Play Store, HDMI, Ajustes) ficam sempre à direita da divisória e não entram nessa lista.
- Limite de 8 favoritos, para o Dock caber na tela com folga.
- Padrão de fábrica do Dock: YouTube, Netflix, Crunchyroll, Pluto TV.

## 3. Visual

| Token | Valor | Uso |
|---|---|---|
| Fonte | Inter (OFL), pesos 400, 500, 700, embutida no app | Todo o texto |
| Relógio | Inter Bold 220 px, letter-spacing -0,04 | Hora |
| Data | Inter SemiBold 34 px, 90% opacidade | Data por extenso |
| Barra de menu | Inter Medium 20 px | Itens da barra |
| Balão e menu | Inter Medium 22 px | Nome do app, itens do menu |
| Vidro | Branco 16% sobre o fundo já desfocado (raio 40 px), borda branca 22%, brilho interno 1 px branco 25% | Barra, Dock, balão, menu, painéis |
| Seleção no menu | #3478F6 (azul de seleção do macOS) | Item focado do menu |
| Ícones | Squircle (raio 22,5%), 112 px no Dock | Apps e atalhos |

- Ícones de apps vêm do `PackageManager`. Ícone adaptativo é recortado em squircle; ícone comum é centralizado sobre um fundo branco em squircle.
- Atalhos HDMI e Ajustes usam ícones vetoriais próprios em squircle com degradê (azul para HDMI, cinza para Ajustes), no espírito dos ícones do macOS.
- Contraste: textos sobre vidro recebem uma sombra suave (preto 35%) para manter pelo menos 4,5:1 sobre qualquer fundo claro. Conferir com as fotos incluídas durante a implementação.
- Vidro só na camada funcional (barra, Dock, balão, menu, painéis), nunca no conteúdo (`liquid-glass.md` › Where the material belongs).

## 4. Arquitetura

Kotlin, Views nativas, `minSdk 31`, `targetSdk 34`, `compileSdk 35`. Sem dependências além da biblioteca padrão do Kotlin. Uma única Activity de tela inicial com intent-filter `MAIN` + `HOME` + `DEFAULT`.

| Unidade | Responsabilidade | Depende de |
|---|---|---|
| `HomeActivity` | Monta a tela, liga e desliga receptores ao ficar visível ou oculta, trata teclas | Todas abaixo |
| `ClockView` | Desenha data e hora; atualiza só com `ACTION_TIME_TICK` (registrado em `onStart`, removido em `onStop`) | Nada |
| `DockView` | Ícones focáveis, animação de foco, balão com nome | `AppRepository` |
| `MenuBarView` | "Início", Bluetooth, Wi-Fi, hora curta | `StatusMonitor` |
| `StatusMonitor` | Avisa mudanças de Wi-Fi (`ConnectivityManager.NetworkCallback`) e Bluetooth (broadcast de estado), só enquanto visível | Sistema |
| `WallpaperLoader` | Carrega degradê ou foto no tamanho exato da tela (RGB_565), gera uma vez as faixas desfocadas da barra e do Dock (reduz 1/8, desfoca, amplia) | `Prefs` |
| `AppRepository` | Lista apps abríveis, resolve ícones, ignora apps desinstalados, escuta `PACKAGE_ADDED/REMOVED` | `PackageManager` |
| `DockConfig` (lógica pura) | Ordem do Dock: adicionar, remover, mover, limite de 8, filtrar apps que sumiram | Nada (testável na JVM) |
| `ImageMath` (lógica pura) | Cálculo de redimensionamento e recorte "cover" para 1920x1080 e fator de amostragem | Nada (testável na JVM) |
| `Prefs` | `SharedPreferences`: ordem do Dock, papel de parede escolhido | Nada |
| `WallpaperActivity`, `DockEditorActivity` | Telas de escolha, navegáveis pelo controle | `Prefs`, `AppRepository` |

Fluxo de dados: `AppRepository` → `DockConfig` (filtra e ordena) → `DockView`. A escolha do usuário vai para `Prefs`, e `HomeActivity` recarrega no `onStart`.

Ciclo de vida: nada roda com a tela oculta. Sem `Service`, sem `WorkManager`, sem alarmes, sem `INTERNET`.

Manifesto:
- Permissão única: `ACCESS_NETWORK_STATE` (concedida na instalação, sem pergunta), para o estado do Wi-Fi.
- Bloco `<queries>` com os intents `MAIN` + `LAUNCHER` e `MAIN` + `LEANBACK_LAUNCHER`, para enxergar os apps sem precisar de `QUERY_ALL_PACKAGES`.
- Bluetooth: se ler o estado no Android 12 exigir `BLUETOOTH_CONNECT` (permissão que abre pergunta na tela), o ícone de Bluetooth é removido em vez de pedir a permissão.

## 5. Erros e casos de borda

| Situação | Comportamento |
|---|---|
| App do Dock foi desinstalado | Some do Dock, sem erro; volta se for reinstalado |
| Foto escolhida foi apagada ou corrompida | Volta para o degradê padrão |
| Intent de Ajustes ou Fontes não resolve | Item escondido do menu e atalho escondido do Dock |
| Launcher original desativado | Item do menu escondido |
| Pouca memória: o Android encerra o launcher | Reabre a frio pelo botão Início (meta < 1 s) |
| Primeira execução sem `Prefs` | Dock padrão, degradê 1 |

## 6. Compilação

- Projeto Gradle com wrapper em `launcher/` dentro de `~/pessoal/projetor-bettdow/`. SDK em `~/Android` (plataformas 34 a 36 e build-tools 35/36 já instalados).
- Build de release assinado com chave própria gerada para o projeto (guardada fora do git), com minificação R8 ativada para reduzir o APK e a memória.
- Primeira compilação baixa o Gradle e o Android Gradle Plugin (~200 MB, uma vez).

## 7. Testes

1. **JVM (unitários):** `DockConfig` (adicionar, remover, mover, limite, app sumido) e `ImageMath` (proporções 16:9, 4:3, retrato, 4K, imagem menor que a tela).
2. **No aparelho, via ADB (roteiro escrito antes):** instalação, abertura por `am start`, navegação com `input keyevent` (DPAD_LEFT/RIGHT/UP/DOWN, DPAD_CENTER, BACK) conferida por captura de tela, abertura de cada app do Dock, menu "Início" e cada item, troca de papel de parede, envio de foto pelo script, Organizar Dock.
3. **Medições:** memória (PSS) e abertura a frio do launcher novo comparadas com os 87 MB do original, 3 medições cada.
4. **Na tela, com o dono do aparelho:** visual, legibilidade a distância, controle real, seleção como launcher padrão pela tela do Android, e retorno ao launcher original.

## 8. Reversão

- O launcher do fabricante continua ativo durante todo o desenvolvimento.
- Escolher o novo como padrão é feito pelo dono, na tela de escolha do Android.
- Para remover: `adb uninstall <pacote>`; o Android volta ao launcher do fabricante.
- Os comandos entram em `scripts/reverter.sh` e em `ALTERACOES.md`.

## 9. Riscos conhecidos

- **Escrita do `adb push` na pasta do app:** no Android 12 o usuário `shell` normalmente consegue escrever em `Android/data/<pacote>/`. Se este firmware bloquear, o plano B é enviar para `/sdcard/Download/Inicio/` e o app passa a pedir a permissão de leitura de mídia.
- **Ícones de apps de TV:** alguns apps de TV só têm banner (retangular) e ícone de baixa resolução. Se o ícone ficar ruim em 112 px, o app usa o banner recortado no centro.
- **Contraste do vidro sobre fotos claras:** validar com as 3 fotos incluídas; ajustar a opacidade do vidro se necessário.
