# Projetor Bettdow 1073 | Diagnóstico (26/10/2026)

Nenhuma alteração foi feita até aqui. Tudo abaixo veio de comandos de leitura via ADB.

## Aparelho

| Item | Valor |
|---|---|
| Chip | SigmaStar, plataforma `pioneer5` |
| Firmware | `sstar_pioneer5_sdp_lr-userdebug 12 SD2A.220601.004.B2` (compilado em 03/04/2025) |
| Android | 12, perfil "low RAM" |
| Fabricante do software | Newlink / Zhiying (OEM chinês por trás da marca Bettdow) |
| Launcher original | `com.zeasn.launcher.projector` (ZhiXiang Launcher) |
| Pacotes | 100 (7 fora do sistema) |

## Memória

| Medida | Valor |
|---|---|
| RAM total informada | 2.048 MB |
| Reservada pelo hardware de vídeo ("Lost RAM") | ~1.040 MB |
| Usada pelo Android e apps | ~632 MB |
| Livre + cache | ~600 MB |

Cerca de metade da RAM fica reservada para o processamento de imagem do projetor e não pode ser recuperada. Na prática o Android trabalha com ~1 GB.

Maiores consumidores em segundo plano (sem contar os apps que estavam abertos na hora):

| Processo | RAM | O que é |
|---|---|---|
| `com.zeasn.launcher.projector` | 73 MB | Launcher original |
| `system` | 68 MB | Núcleo do Android (essencial) |
| `com.google.android.gms` | 38 + 23 MB | Google Play Services (essencial para Play/YouTube) |
| `com.google.android.inputmethod.latin` | 31 MB | Teclado Gboard (essencial) |
| `com.newlink.service` | 19 MB | Serviço do projetor (essencial) |
| `com.uv.droid.uota` + `uotaback` | 12 + 5 MB | Atualizador de firmware de terceiros |
| `com.android.tv` + `tv.common` | 5 + 5 MB | TV ao vivo / entradas (manter, ver abaixo) |

## Achado principal: adware disfarçado

`com.android.media.module.services`
- O nome imita um módulo do Android, mas **não é do sistema**: fica em `/data/app` e foi instalado em 27/06/2026 **pelo launcher original** (`installer=com.zeasn.launcher.projector`), sem passar pela Play Store. Foi atualizado em 01/09/2026.
- O código interno se chama `com.ad.service`, com os serviços `MetaSdkService`, `ZeroOneSdkService` e `PluzedSdkService`, típicos de SDKs de propaganda ou de monetização de rede.
- Permissões: localização precisa, ID de anúncio, atribuição de anúncios, encerrar processos de outros apps, ler lista de apps, acesso total ao armazenamento.

`com.android.docks`
- Mesmo padrão de nome falso, também em `/data/app`, instalado de fábrica (09/03/2026). Inicia no boot e roda serviço de sincronização. **Já está desativado** no aparelho.

## Observações de segurança

- O firmware é `userdebug` com `test-keys` e se apresenta como "ADT-3" (aparelho de desenvolvedor do Google) para ter acesso à Play Store. É comum em projetores baratos, não dá para mudar sem trocar firmware (fora do escopo).
- A verificação de apps do Play Protect está desligada (`package_verifier_enable=0`).
- O atualizador `com.uv.droid.uota` tem permissões de superusuário, instalar e remover apps e alterar configurações seguras. É ele quem atualiza o firmware, mas também é o tipo de componente usado para empurrar apps.
- Com "Depuração USB" ligada, o ADB fica aberto na rede (porta 5555). Desligar ao final.

## Classificação dos apps

### Essenciais (não mexer)
Núcleo Android, Google (`gms`, `gsf`, `vending`), WebView, Gboard, Bluetooth, Wi-Fi, provedores, `com.android.tv.settings`, e todos os componentes do projetor:
`com.sstar.*` (controle do sistema, Miracast, troca de fonte, entradas), `com.newlink.projector.service`, `com.newlink.service`, `com.newlink.source`, `com.newlink.sourceMenu`, `com.newlink.cast`, `com.newlink.provision`, `com.zhiying.autofocus`, `com.zhiying.powerservice`, `com.zhiying.settings`.

`com.android.tv` (TV ao vivo) também fica: em projetores ele costuma ser o caminho das entradas HDMI. Para ele não abrir sozinho, a opção "entrada na inicialização" em Outros ajustes já resolve sem desativar nada.

### Que você quer manter
YouTube (`com.google.android.youtube.tv`), Netflix (`com.netflix.mediaclient`), Crunchyroll, Pluto TV, Projectivy Launcher.

### Potencialmente dispensáveis

| Pacote | O que é | Ganho | Risco |
|---|---|---|---|
| `com.android.media.module.services` | Adware disfarçado | Privacidade + RAM quando ativo | Baixo |
| `com.android.docks` | App disfarçado, já desativado | Nenhum agora | Baixo |
| `zeasn.open.technical.tv.store` | Loja do fabricante | Menos propaganda | Baixo |
| `com.mobisystems.editor.office_registered` | OfficeSuite | Pouco | Baixo |
| `com.android.inputmethod.pinyin` | Teclado chinês | Pouco | Baixo |
| `com.android.printspooler` | Serviço de impressão | Pouco | Baixo |
| `com.uv.droid.uota` + `uotaback` | Atualizador de firmware | ~17 MB, sempre ativo | Médio: sem atualizações de firmware |
| `com.zeasn.launcher.projector` | Launcher original (instalou o adware) | ~73 MB | Médio: só depois de testar o Projectivy |

Apps de fábrica e de teste (`newlink.autotest`, `tvtestingtools`, `factorymenu`, `sstar.factorymenu`, `exoplayer2.demo`) **não ficam rodando**, então desativá-los não libera memória. Ficam como estão.

## Animações

`window_animation_scale=1.0`, `transition_animation_scale=1.0`, `animator_duration_scale` (padrão 1.0). Reduzir para 0.5 deixa a navegação mais ágil, sem custo.
