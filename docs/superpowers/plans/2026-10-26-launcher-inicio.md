# Launcher "Início" Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Construir e instalar no projetor Bettdow 1073 um launcher Android próprio no estilo macOS (barra de menu, relógio grande, Dock de vidro, Launchpad, Central de Controle, calendário) que use menos de 35 MB e abra em menos de 1 s.

**Architecture:** App Kotlin de uma tela inicial (`HomeActivity`, intent `HOME`) e três telas secundárias (`AllAppsActivity`, `DockEditorActivity`, `WallpaperActivity`), todas com Views nativas montadas em código, sem bibliotecas. A lógica que não depende do Android (Dock, ponto azul, calendário, recorte de imagem, desfoque, formatação de data) fica em `core/` e é testada na JVM. O "vidro" é um desenho do papel de parede já desfocado uma única vez (1/8 do tamanho), recortado atrás de cada painel.

**Tech Stack:** Kotlin 2.2.20, Android Gradle Plugin 8.13.2, Gradle 8.14 (já em cache em `~/.gradle`), Android SDK em `~/Android` (compileSdk 35), JDK 21, JUnit 4.13.2 só nos testes, fonte Inter (OFL), ImageMagick para o script de fotos.

**Spec:** `docs/superpowers/specs/2026-10-26-launcher-macos-design.md`

## Global Constraints

- Pasta do projeto Android: `~/pessoal/projetor-bettdow/launcher/`. Todos os comandos Gradle rodam de dentro dela.
- `applicationId` e `namespace`: `br.gui.inicio`. Nome exibido: `Início`.
- `minSdk 31`, `targetSdk 34`, `compileSdk 35`. Aparelho: Android 12L (API 32), tela 1920x1080, densidade 240.
- Nenhuma dependência de runtime além da biblioteca padrão do Kotlin. Sem AndroidX, sem Compose, sem bibliotecas de imagem. JUnit só em `testImplementation`.
- Permissão única no manifesto: `android.permission.ACCESS_NETWORK_STATE`. Sem `INTERNET`, sem `QUERY_ALL_PACKAGES`, sem `Service`, sem alarmes.
- Nada roda com a tela oculta: receptores e callbacks registrados em `onStart`, removidos em `onStop`.
- Metas medidas no aparelho: memória do launcher < 35 MB PSS; abertura a frio < 1 s (`am start -W`, TotalTime); CPU ~0% parado.
- Dock: até 8 favoritos (`DockConfig.MAX = 8`); padrão YouTube, Netflix, Crunchyroll, Pluto TV; nunca muda sozinho.
- Todos os apps: 6 colunas x 3 linhas por página (18), ponto azul `#3478F6` em app novo.
- Cores: seleção `#3478F6`, hoje no calendário `#FF3B30`, vidro branco 16% com borda branca 22%.
- Idioma da interface: português do Brasil. Proibido o caractere travessão (U+2014) em código, textos, commits e docs; conferir com `grep -rn $'\u2014'` antes de cada commit.
- Git: branch `feature/launcher-inicio`. Mensagem de commit no formato `LAUNCHER | TIPO | DESCRICAO` (TIPO = FEAT, TEST, FIX, DOCS, BUILD), sem linha Co-Authored-By.
- O launcher do fabricante (`com.zeasn.launcher.projector`) NÃO é desativado neste plano. Testes no aparelho abrem o launcher novo por `am start -n`, sem torná-lo padrão. Escolher o padrão é ação do dono, na Task 16.
- ADB do projetor: `192.168.2.100:5555`. Se `adb devices` não mostrar o aparelho, rodar `adb connect 192.168.2.100:5555`.
- Capturas de tela de teste vão para `launcher/build/qa/` (fora do git).

## Review Focus

- **Favorito desinstalado com o Dock cheio:** a vaga dele precisa ficar livre para um app novo; `DockConfig.add` descarta favoritos desinstalados antes de checar o limite (teste `uninstalledFavoriteFreesSlotOnAdd`, Task 1).
- **Preferências corrompidas ou forjadas** (`dock` com lixo, `wallpaper=file:../../x`): devem cair no padrão sem quebrar e sem ler fora da pasta do app (testes `parseDropsGarbageDuplicatesAndExtra`, Task 1, e `rejectsPathTraversal`, Task 2).
- **Foto estranha na pasta de wallpapers** (retrato, minúscula, 8K, arquivo corrompido): deve ser recortada ou cair no degradê padrão, nunca travar (testes de `ImageMath`, Task 4; `WallpaperLoader.load` com `try/catch` e fallback, Task 8).
- **App desinstalado e reinstalado:** deve voltar a aparecer como novo; na primeira execução nenhum app tem ponto (testes `reinstalledAppIsNewAgain` e `firstRunMarksInstalledAsKnown`, Task 2).
- **Calendário em fevereiro bissexto e na virada do ano** (testes `leapFebruary2028` e `decemberToJanuaryTitle`, Task 3).

---

## File Structure

```
launcher/
  settings.gradle.kts, build.gradle.kts, gradle.properties, gradlew, gradle/wrapper/*
  local.properties                      (sdk.dir; fora do git)
  keystore.properties, inicio.jks       (assinatura; fora do git; Task 15)
  app/build.gradle.kts, app/proguard-rules.pro
  app/src/main/AndroidManifest.xml
  app/src/main/res/values/{strings,themes}.xml
  app/src/main/res/drawable/{ic_app,banner}.xml
  app/src/main/res/font/inter_{regular,medium,semibold,bold}.ttf
  app/src/main/assets/wallpapers/{montanha,vale,lago}.jpg
  app/src/main/java/br/gui/inicio/
    core/        DockConfig, NewAppsTracker, WallpaperChoice, CalendarMath, ClockFormat, ImageMath, BoxBlur   (Kotlin puro)
    ui/          Theme, Glass (GlassSource, GlassDrawable, invalidateTree), Glyphs (Glyph, Glyphs, GlyphView), IconFactory,
                 PanelHost, MenuPanel, ControlCenterPanel, CalendarPanel, Backdrop
    data/        Prefs, AppRepository, Shortcuts (SystemShortcut, Shortcuts, ShortcutResolver)
    wallpaper/   Gradients, WallpaperLoader
    home/        HomeActivity, ClockView, DockView, MenuBarView, StatusMonitor
    apps/        AllAppsActivity
    settings/    DockEditorActivity, WallpaperActivity
  app/src/test/java/br/gui/inicio/core/  *Test.kt
scripts/enviar-wallpaper.sh, scripts/medir-launcher.sh
docs/CREDITOS.md, docs/qa/roteiro-launcher.md, README.md
```

Uma responsabilidade por arquivo. `core/` não importa nada de `android.*`.

---

### Task 1: Projeto Gradle e `DockConfig`

**Files:**
- Create: `launcher/settings.gradle.kts`, `launcher/build.gradle.kts`, `launcher/gradle.properties`, `launcher/local.properties`, `launcher/app/build.gradle.kts`, `launcher/app/proguard-rules.pro`, `launcher/app/src/main/AndroidManifest.xml`
- Create: `launcher/app/src/main/java/br/gui/inicio/core/DockConfig.kt`
- Test: `launcher/app/src/test/java/br/gui/inicio/core/DockConfigTest.kt`
- Modify: `.gitignore` (raiz do projeto)

**Interfaces:**
- Produces: `data class DockConfig(val packages: List<String>)` com `visible(installed: Set<String>): List<String>`, `isFull(installed: Set<String>): Boolean`, `contains(pkg: String): Boolean`, `add(pkg: String, installed: Set<String>): DockConfig`, `remove(pkg: String): DockConfig`, `move(pkg: String, delta: Int): DockConfig`, `serialize(): String`; `companion`: `MAX = 8`, `DEFAULT`, `parse(raw: String?): DockConfig`.

- [ ] **Step 1: Criar a branch de trabalho**

```bash
cd ~/pessoal/projetor-bettdow && git checkout -b feature/launcher-inicio
```

- [ ] **Step 2: Criar os arquivos Gradle**

`launcher/settings.gradle.kts`:
```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "Inicio"
include(":app")
```

`launcher/build.gradle.kts`:
```kotlin
plugins {
    id("com.android.application") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "2.2.20" apply false
}
```

`launcher/gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx2g -Dfile.encoding=UTF-8
kotlin.code.style=official
android.nonTransitiveRClass=true
```

`launcher/local.properties`:
```properties
sdk.dir=/home/gui/Android
```

`launcher/app/build.gradle.kts`:
```kotlin
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "br.gui.inicio"
    compileSdk = 35

    defaultConfig {
        applicationId = "br.gui.inicio"
        minSdk = 31
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        create("release") {
            val file = rootProject.file("keystore.properties")
            if (file.exists()) {
                val p = Properties().apply { file.inputStream().use { load(it) } }
                storeFile = rootProject.file(p.getProperty("storeFile"))
                storePassword = p.getProperty("storePassword")
                keyAlias = p.getProperty("keyAlias")
                keyPassword = p.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
```

`launcher/app/proguard-rules.pro`:
```
# Activities são mantidas pelo manifesto; nenhuma reflexão no app.
```

`launcher/app/src/main/AndroidManifest.xml` (versão inicial; a Task 5 substitui):
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application android:label="Início" />
</manifest>
```

Acrescentar ao `.gitignore` da raiz:
```
launcher/keystore.properties
launcher/.kotlin/
```

- [ ] **Step 3: Gerar o Gradle wrapper com o Gradle já em cache**

```bash
cd ~/pessoal/projetor-bettdow/launcher && \
  /home/gui/.gradle/wrapper/dists/gradle-8.14-all/c2qonpi39x1mddn7hk5gh9iqj/gradle-8.14/bin/gradle wrapper --gradle-version 8.14 --distribution-type all --no-daemon
```
Expected: `BUILD SUCCESSFUL`; existem `gradlew` e `gradle/wrapper/gradle-wrapper.properties`.

- [ ] **Step 4: Escrever o teste que falha**

`launcher/app/src/test/java/br/gui/inicio/core/DockConfigTest.kt`:
```kotlin
package br.gui.inicio.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DockConfigTest {
    private val all = setOf("a.yt", "b.nf", "c.cr", "d.pl", "e.x", "f.x", "g.x", "h.x", "i.x")
    private fun cfg(vararg p: String) = DockConfig(p.toList())

    @Test fun parseNullReturnsDefault() = assertEquals(DockConfig.DEFAULT, DockConfig.parse(null))

    @Test fun parseEmptyMeansEmptyDock() = assertEquals(emptyList<String>(), DockConfig.parse("").packages)

    @Test fun parseDropsGarbageDuplicatesAndExtra() {
        val raw = "a.yt,,lixo,a.yt,../x,b.nf,c.cr,d.pl,e.x,f.x,g.x,h.x,i.x"
        assertEquals(
            listOf("a.yt", "b.nf", "c.cr", "d.pl", "e.x", "f.x", "g.x", "h.x"),
            DockConfig.parse(raw).packages,
        )
    }

    @Test fun serializeRoundTrip() {
        val c = cfg("a.yt", "b.nf")
        assertEquals(c, DockConfig.parse(c.serialize()))
    }

    @Test fun addAppendsAtEnd() = assertEquals(listOf("a.yt", "b.nf"), cfg("a.yt").add("b.nf", all).packages)

    @Test fun addIgnoresDuplicate() = assertEquals(listOf("a.yt"), cfg("a.yt").add("a.yt", all).packages)

    @Test fun addRefusesWhenFull() {
        val full = cfg("a.yt", "b.nf", "c.cr", "d.pl", "e.x", "f.x", "g.x", "h.x")
        assertTrue(full.isFull(all))
        assertEquals(full, full.add("i.x", all))
    }

    @Test fun uninstalledFavoriteFreesSlotOnAdd() {
        val full = cfg("a.yt", "b.nf", "c.cr", "d.pl", "e.x", "f.x", "g.x", "h.x")
        val installed = all - "c.cr"
        assertFalse(full.isFull(installed))
        assertEquals(
            listOf("a.yt", "b.nf", "d.pl", "e.x", "f.x", "g.x", "h.x", "i.x"),
            full.add("i.x", installed).packages,
        )
    }

    @Test fun visibleHidesUninstalledButKeepsIt() {
        val c = cfg("a.yt", "b.nf")
        assertEquals(listOf("a.yt"), c.visible(setOf("a.yt")))
        assertEquals(listOf("a.yt", "b.nf"), c.visible(all))
    }

    @Test fun removeDeletes() = assertEquals(listOf("b.nf"), cfg("a.yt", "b.nf").remove("a.yt").packages)

    @Test fun moveShiftsAndClampsAtEdges() {
        val c = cfg("a.yt", "b.nf", "c.cr")
        assertEquals(listOf("b.nf", "a.yt", "c.cr"), c.move("a.yt", 1).packages)
        assertEquals(c, c.move("c.cr", 1))
        assertEquals(c, c.move("a.yt", -1))
        assertEquals(c, c.move("zz.z", 1))
    }
}
```

- [ ] **Step 5: Rodar o teste e ver falhar**

Run: `cd ~/pessoal/projetor-bettdow/launcher && ./gradlew :app:testDebugUnitTest --tests 'br.gui.inicio.core.DockConfigTest'`
Expected: FAIL na compilação com `Unresolved reference 'DockConfig'`.

- [ ] **Step 6: Implementar `DockConfig`**

`launcher/app/src/main/java/br/gui/inicio/core/DockConfig.kt`:
```kotlin
package br.gui.inicio.core

/** Favoritos do Dock, na ordem escolhida. Imutável: cada operação devolve uma cópia. */
data class DockConfig(val packages: List<String>) {

    /** Favoritos ainda instalados, na ordem salva. Desinstalado some, mas volta se for reinstalado. */
    fun visible(installed: Set<String>): List<String> = packages.filter { it in installed }

    fun isFull(installed: Set<String>): Boolean = visible(installed).size >= MAX

    fun contains(pkg: String): Boolean = pkg in packages

    /** Adiciona no fim. Antes descarta favoritos desinstalados, para a vaga deles ficar livre. */
    fun add(pkg: String, installed: Set<String>): DockConfig {
        val kept = visible(installed)
        if (pkg in kept || kept.size >= MAX) return copy(packages = kept)
        return copy(packages = kept + pkg)
    }

    fun remove(pkg: String): DockConfig = copy(packages = packages - pkg)

    fun move(pkg: String, delta: Int): DockConfig {
        val from = packages.indexOf(pkg)
        if (from < 0) return this
        val to = (from + delta).coerceIn(0, packages.lastIndex)
        if (to == from) return this
        val list = packages.toMutableList().apply {
            removeAt(from)
            add(to, pkg)
        }
        return copy(packages = list)
    }

    fun serialize(): String = packages.joinToString(SEP)

    companion object {
        const val MAX = 8
        private const val SEP = ","
        private val PACKAGE_NAME = Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+")

        val DEFAULT = DockConfig(
            listOf(
                "com.google.android.youtube.tv",
                "com.netflix.mediaclient",
                "com.crunchyroll.crunchyroid",
                "tv.pluto.android",
            ),
        )

        /** null = nunca salvo (usa o padrão). Texto vazio = Dock vazio de propósito. */
        fun parse(raw: String?): DockConfig {
            if (raw == null) return DEFAULT
            val list = raw.split(SEP)
                .map { it.trim() }
                .filter { PACKAGE_NAME.matches(it) }
                .distinct()
                .take(MAX)
            return DockConfig(list)
        }
    }
}
```

- [ ] **Step 7: Rodar o teste e ver passar**

Run: `./gradlew :app:testDebugUnitTest --tests 'br.gui.inicio.core.DockConfigTest'`
Expected: `BUILD SUCCESSFUL`, 11 testes passando.

- [ ] **Step 8: Commit**

```bash
cd ~/pessoal/projetor-bettdow && grep -rn $'\u2014' launcher .gitignore; \
git add .gitignore launcher && git commit -m "LAUNCHER | BUILD | Projeto Gradle e regras do Dock"
```
Expected: o `grep` não imprime nada; commit criado.

---

### Task 2: `NewAppsTracker` e `WallpaperChoice`

**Files:**
- Create: `launcher/app/src/main/java/br/gui/inicio/core/NewAppsTracker.kt`
- Create: `launcher/app/src/main/java/br/gui/inicio/core/WallpaperChoice.kt`
- Test: `launcher/app/src/test/java/br/gui/inicio/core/NewAppsTrackerTest.kt`, `launcher/app/src/test/java/br/gui/inicio/core/WallpaperChoiceTest.kt`

**Interfaces:**
- Produces: `data class NewAppsState(val initialized: Boolean = false, val known: Set<String> = emptySet(), val opened: Set<String> = emptySet())`; `object NewAppsTracker` com `ensureInitialized(state, installed: Set<String>)`, `prune(state, installed: Set<String>)`, `isNew(state, pkg: String): Boolean`, `markOpened(state, pkg: String)`.
- Produces: `sealed interface WallpaperChoice` com `Gradient(index: Int)`, `Asset(name: String)`, `UserFile(name: String)`, `serialize(): String`; `companion`: `GRADIENT_COUNT = 4`, `DEFAULT`, `isSafeName(name: String): Boolean`, `parse(raw: String?): WallpaperChoice`.

- [ ] **Step 1: Escrever os testes que falham**

`launcher/app/src/test/java/br/gui/inicio/core/NewAppsTrackerTest.kt`:
```kotlin
package br.gui.inicio.core

import br.gui.inicio.core.NewAppsTracker.ensureInitialized
import br.gui.inicio.core.NewAppsTracker.isNew
import br.gui.inicio.core.NewAppsTracker.markOpened
import br.gui.inicio.core.NewAppsTracker.prune
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NewAppsTrackerTest {
    private val base = setOf("a.yt", "b.nf")

    @Test fun nothingIsNewBeforeInitialization() = assertFalse(isNew(NewAppsState(), "x.new"))

    @Test fun firstRunMarksInstalledAsKnown() {
        val s = ensureInitialized(NewAppsState(), base)
        assertFalse(isNew(s, "a.yt"))
        assertFalse(isNew(s, "b.nf"))
    }

    @Test fun ensureInitializedRunsOnlyOnce() {
        val first = ensureInitialized(NewAppsState(), base)
        val second = ensureInitialized(first, base + "x.new")
        assertEquals(first, second)
        assertTrue(isNew(second, "x.new"))
    }

    @Test fun openedAppIsNoLongerNew() {
        val s = markOpened(ensureInitialized(NewAppsState(), base), "x.new")
        assertFalse(isNew(s, "x.new"))
    }

    @Test fun reinstalledAppIsNewAgain() {
        var s = markOpened(ensureInitialized(NewAppsState(), base), "x.new")
        s = prune(s, base)                 // x.new desinstalado
        assertTrue(isNew(s, "x.new"))      // reinstalado: novo de novo
        s = prune(s, base - "a.yt")        // app de fábrica desinstalado
        assertTrue(isNew(s, "a.yt"))
    }
}
```

`launcher/app/src/test/java/br/gui/inicio/core/WallpaperChoiceTest.kt`:
```kotlin
package br.gui.inicio.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WallpaperChoiceTest {
    @Test fun nullOrGarbageFallsBackToDefault() {
        assertEquals(WallpaperChoice.DEFAULT, WallpaperChoice.parse(null))
        assertEquals(WallpaperChoice.DEFAULT, WallpaperChoice.parse("lixo"))
        assertEquals(WallpaperChoice.DEFAULT, WallpaperChoice.parse("grad:9"))
        assertEquals(WallpaperChoice.DEFAULT, WallpaperChoice.parse("video:a.jpg"))
    }

    @Test fun roundTripsEveryKind() {
        listOf(
            WallpaperChoice.Gradient(3),
            WallpaperChoice.Asset("montanha.jpg"),
            WallpaperChoice.UserFile("minha-foto_1.png"),
        ).forEach { assertEquals(it, WallpaperChoice.parse(it.serialize())) }
    }

    @Test fun rejectsPathTraversal() {
        assertEquals(WallpaperChoice.DEFAULT, WallpaperChoice.parse("file:../../segredo.jpg"))
        assertEquals(WallpaperChoice.DEFAULT, WallpaperChoice.parse("file:pasta/foto.jpg"))
        assertFalse(WallpaperChoice.isSafeName("..jpg"))
        assertFalse(WallpaperChoice.isSafeName(".oculta.jpg"))
        assertFalse(WallpaperChoice.isSafeName("foto.txt"))
        assertTrue(WallpaperChoice.isSafeName("Foto.JPG"))
    }
}
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `./gradlew :app:testDebugUnitTest --tests 'br.gui.inicio.core.NewAppsTrackerTest' --tests 'br.gui.inicio.core.WallpaperChoiceTest'`
Expected: FAIL na compilação (`Unresolved reference 'NewAppsState'`).

- [ ] **Step 3: Implementar**

`launcher/app/src/main/java/br/gui/inicio/core/NewAppsTracker.kt`:
```kotlin
package br.gui.inicio.core

/** Estado do ponto azul. known = apps presentes na primeira execução; opened = apps já abertos pelo launcher. */
data class NewAppsState(
    val initialized: Boolean = false,
    val known: Set<String> = emptySet(),
    val opened: Set<String> = emptySet(),
)

object NewAppsTracker {
    /** Na primeira execução tudo o que já está instalado conta como conhecido. Depois disso não muda. */
    fun ensureInitialized(state: NewAppsState, installed: Set<String>): NewAppsState =
        if (state.initialized) state else NewAppsState(initialized = true, known = installed)

    /** Esquece apps desinstalados, para que uma reinstalação apareça como nova. */
    fun prune(state: NewAppsState, installed: Set<String>): NewAppsState =
        state.copy(known = state.known intersect installed, opened = state.opened intersect installed)

    fun isNew(state: NewAppsState, pkg: String): Boolean =
        state.initialized && pkg !in state.known && pkg !in state.opened

    fun markOpened(state: NewAppsState, pkg: String): NewAppsState =
        state.copy(opened = state.opened + pkg)
}
```

`launcher/app/src/main/java/br/gui/inicio/core/WallpaperChoice.kt`:
```kotlin
package br.gui.inicio.core

/** Papel de parede escolhido: degradê desenhado, foto incluída no app ou foto enviada pelo computador. */
sealed interface WallpaperChoice {
    data class Gradient(val index: Int) : WallpaperChoice
    data class Asset(val name: String) : WallpaperChoice
    data class UserFile(val name: String) : WallpaperChoice

    fun serialize(): String = when (this) {
        is Gradient -> "grad:$index"
        is Asset -> "asset:$name"
        is UserFile -> "file:$name"
    }

    companion object {
        const val GRADIENT_COUNT = 4
        val DEFAULT: WallpaperChoice = Gradient(0)

        private val SAFE_NAME = Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,80}\\.(jpg|jpeg|png|webp)", RegexOption.IGNORE_CASE)

        /** Só nome de arquivo simples: sem barra, sem "..", começando por letra ou número. */
        fun isSafeName(name: String): Boolean = SAFE_NAME.matches(name) && ".." !in name

        fun parse(raw: String?): WallpaperChoice {
            val parts = raw?.split(":", limit = 2)
            if (parts == null || parts.size != 2) return DEFAULT
            val (kind, value) = parts
            return when (kind) {
                "grad" -> value.toIntOrNull()?.takeIf { it in 0 until GRADIENT_COUNT }?.let { Gradient(it) } ?: DEFAULT
                "asset" -> if (isSafeName(value)) Asset(value) else DEFAULT
                "file" -> if (isSafeName(value)) UserFile(value) else DEFAULT
                else -> DEFAULT
            }
        }
    }
}
```

- [ ] **Step 4: Rodar e ver passar**

Run: `./gradlew :app:testDebugUnitTest --tests 'br.gui.inicio.core.NewAppsTrackerTest' --tests 'br.gui.inicio.core.WallpaperChoiceTest'`
Expected: `BUILD SUCCESSFUL`, 8 testes passando.

- [ ] **Step 5: Commit**

```bash
cd ~/pessoal/projetor-bettdow && grep -rn $'\u2014' launcher; \
git add launcher && git commit -m "LAUNCHER | FEAT | Regra do ponto azul e escolha de papel de parede"
```

---

### Task 3: `CalendarMath` e `ClockFormat`

**Files:**
- Create: `launcher/app/src/main/java/br/gui/inicio/core/CalendarMath.kt`
- Create: `launcher/app/src/main/java/br/gui/inicio/core/ClockFormat.kt`
- Test: `launcher/app/src/test/java/br/gui/inicio/core/CalendarMathTest.kt`, `launcher/app/src/test/java/br/gui/inicio/core/ClockFormatTest.kt`

**Interfaces:**
- Produces: `data class CalendarDay(val date: LocalDate, val inMonth: Boolean, val isToday: Boolean)`; `object CalendarMath` com `grid(month: YearMonth, today: LocalDate): List<CalendarDay>` (42 itens, semana começando no domingo) e `title(month: YearMonth): String` ("outubro de 2026").
- Produces: `object ClockFormat` com `longDate(t: LocalDateTime)` ("segunda-feira, 26 de outubro"), `time(t: LocalDateTime)` ("20:45"), `menuBar(t: LocalDateTime)` ("seg 26 out 20:45").

- [ ] **Step 1: Escrever os testes que falham**

`launcher/app/src/test/java/br/gui/inicio/core/CalendarMathTest.kt`:
```kotlin
package br.gui.inicio.core

import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarMathTest {
    private val today = LocalDate.of(2026, 10, 26)

    @Test fun october2026StartsOnSundaySeptember27() {
        val g = CalendarMath.grid(YearMonth.of(2026, 10), today)
        assertEquals(42, g.size)
        assertEquals(LocalDate.of(2026, 9, 27), g[0].date)
        assertFalse(g[0].inMonth)
        assertEquals(LocalDate.of(2026, 10, 1), g[4].date)
        assertTrue(g[4].inMonth)
    }

    @Test fun todayIsMarkedOnlyOnce() {
        val g = CalendarMath.grid(YearMonth.of(2026, 10), today)
        assertEquals(listOf(29), g.indices.filter { g[it].isToday })
        assertTrue(CalendarMath.grid(YearMonth.of(2026, 11), today).none { it.isToday })
    }

    @Test fun leapFebruary2028() {
        val g = CalendarMath.grid(YearMonth.of(2028, 2), today)
        assertEquals(LocalDate.of(2028, 2, 29), g[30].date)
        assertTrue(g[30].inMonth)
        assertEquals(LocalDate.of(2028, 3, 1), g[31].date)
        assertFalse(g[31].inMonth)
    }

    @Test fun decemberToJanuaryTitle() {
        assertEquals("outubro de 2026", CalendarMath.title(YearMonth.of(2026, 10)))
        assertEquals("janeiro de 2027", CalendarMath.title(YearMonth.of(2026, 12).plusMonths(1)))
    }
}
```

`launcher/app/src/test/java/br/gui/inicio/core/ClockFormatTest.kt`:
```kotlin
package br.gui.inicio.core

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ClockFormatTest {
    private val t = LocalDateTime.of(2026, 10, 26, 20, 45)

    @Test fun longDateInPortuguese() = assertEquals("segunda-feira, 26 de outubro", ClockFormat.longDate(t))

    @Test fun time24h() = assertEquals("20:45", ClockFormat.time(t))

    @Test fun earlyHourKeepsLeadingZero() = assertEquals("07:05", ClockFormat.time(t.withHour(7).withMinute(5)))

    @Test fun menuBarHasNoDots() = assertEquals("seg 26 out 20:45", ClockFormat.menuBar(t))
}
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `./gradlew :app:testDebugUnitTest --tests 'br.gui.inicio.core.CalendarMathTest' --tests 'br.gui.inicio.core.ClockFormatTest'`
Expected: FAIL na compilação (`Unresolved reference 'CalendarMath'`).

- [ ] **Step 3: Implementar**

`launcher/app/src/main/java/br/gui/inicio/core/CalendarMath.kt`:
```kotlin
package br.gui.inicio.core

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

data class CalendarDay(val date: LocalDate, val inMonth: Boolean, val isToday: Boolean)

object CalendarMath {
    private val TITLE = DateTimeFormatter.ofPattern("LLLL 'de' yyyy", Locale.forLanguageTag("pt-BR"))

    /** Seis semanas de sete dias, começando no domingo, como o calendário do macOS. */
    fun grid(month: YearMonth, today: LocalDate): List<CalendarDay> {
        val first = month.atDay(1)
        val offset = first.dayOfWeek.value % 7 // domingo = 0
        val start = first.minusDays(offset.toLong())
        return (0 until 42).map { i ->
            val d = start.plusDays(i.toLong())
            CalendarDay(d, YearMonth.from(d) == month, d == today)
        }
    }

    fun title(month: YearMonth): String = month.format(TITLE)
}
```

`launcher/app/src/main/java/br/gui/inicio/core/ClockFormat.kt`:
```kotlin
package br.gui.inicio.core

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object ClockFormat {
    private val PT_BR = Locale.forLanguageTag("pt-BR")
    private val LONG_DATE = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", PT_BR)
    private val TIME = DateTimeFormatter.ofPattern("HH:mm", PT_BR)
    private val MENU_BAR = DateTimeFormatter.ofPattern("EEE d MMM HH:mm", PT_BR)

    fun longDate(t: LocalDateTime): String = LONG_DATE.format(t)

    fun time(t: LocalDateTime): String = TIME.format(t)

    /** O Java põe ponto nas abreviações em pt-BR ("seg."); o macOS não. */
    fun menuBar(t: LocalDateTime): String = MENU_BAR.format(t).replace(".", "")
}
```

- [ ] **Step 4: Rodar e ver passar**

Run: `./gradlew :app:testDebugUnitTest --tests 'br.gui.inicio.core.CalendarMathTest' --tests 'br.gui.inicio.core.ClockFormatTest'`
Expected: `BUILD SUCCESSFUL`, 8 testes passando.

- [ ] **Step 5: Commit**

```bash
cd ~/pessoal/projetor-bettdow && grep -rn $'\u2014' launcher; \
git add launcher && git commit -m "LAUNCHER | FEAT | Calendário do mês e formatos de data e hora"
```

---

### Task 4: `ImageMath` e `BoxBlur`

**Files:**
- Create: `launcher/app/src/main/java/br/gui/inicio/core/ImageMath.kt`
- Create: `launcher/app/src/main/java/br/gui/inicio/core/BoxBlur.kt`
- Test: `launcher/app/src/test/java/br/gui/inicio/core/ImageMathTest.kt`, `launcher/app/src/test/java/br/gui/inicio/core/BoxBlurTest.kt`

**Interfaces:**
- Produces: `data class CropRect(val left: Int, val top: Int, val width: Int, val height: Int)`; `object ImageMath` com `sampleSize(srcW, srcH, dstW, dstH): Int` e `coverCrop(srcW, srcH, dstW, dstH): CropRect` (ambos lançam `IllegalArgumentException` para medidas <= 0).
- Produces: `object BoxBlur` com `blur(pixels: IntArray, w: Int, h: Int, radius: Int, passes: Int = 3): IntArray` (altera e devolve o próprio array ARGB).

- [ ] **Step 1: Escrever os testes que falham**

`launcher/app/src/test/java/br/gui/inicio/core/ImageMathTest.kt`:
```kotlin
package br.gui.inicio.core

import org.junit.Assert.assertEquals
import org.junit.Test

class ImageMathTest {
    @Test fun sampleSizeKeepsAtLeastScreenSize() {
        assertEquals(1, ImageMath.sampleSize(1920, 1080, 1920, 1080))
        assertEquals(2, ImageMath.sampleSize(3840, 2160, 1920, 1080))
        assertEquals(4, ImageMath.sampleSize(7680, 4320, 1920, 1080))
        assertEquals(1, ImageMath.sampleSize(800, 450, 1920, 1080))
        assertEquals(1, ImageMath.sampleSize(1080, 1920, 1920, 1080))
    }

    @Test fun coverCropOfSameAspectIsWholeImage() =
        assertEquals(CropRect(0, 0, 3840, 2160), ImageMath.coverCrop(3840, 2160, 1920, 1080))

    @Test fun coverCropOf4by3TrimsTopAndBottom() =
        assertEquals(CropRect(0, 375, 4000, 2250), ImageMath.coverCrop(4000, 3000, 1920, 1080))

    @Test fun coverCropOfPortraitTakesMiddleBand() =
        assertEquals(CropRect(0, 656, 1080, 607), ImageMath.coverCrop(1080, 1920, 1920, 1080))

    @Test fun coverCropOfUltraWideTrimsSides() =
        assertEquals(CropRect(1120, 0, 1920, 1080), ImageMath.coverCrop(4160, 1080, 1920, 1080))

    @Test(expected = IllegalArgumentException::class)
    fun zeroSizeIsRejected() {
        ImageMath.coverCrop(0, 100, 1920, 1080)
    }
}
```

`launcher/app/src/test/java/br/gui/inicio/core/BoxBlurTest.kt`:
```kotlin
package br.gui.inicio.core

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BoxBlurTest {
    private val black = 0xFF000000.toInt()
    private val white = 0xFFFFFFFF.toInt()

    @Test fun uniformImageStaysUniform() {
        val px = IntArray(16 * 9) { 0xFF336699.toInt() }
        assertArrayEquals(IntArray(16 * 9) { 0xFF336699.toInt() }, BoxBlur.blur(px, 16, 9, 2))
    }

    @Test fun singleBrightPixelSpreadsToNeighbours() {
        val w = 9
        val px = IntArray(w * w) { black }
        px[4 * w + 4] = white
        BoxBlur.blur(px, w, w, 1, passes = 1)
        val center = px[4 * w + 4] and 0xFF
        val neighbour = px[4 * w + 5] and 0xFF
        assertEquals(28, center) // 255 / 9
        assertEquals(28, neighbour)
        assertEquals(0, px[0] and 0xFF)
        assertEquals(255, px[4 * w + 4] ushr 24) // continua opaco
    }

    @Test fun radiusZeroIsNoOp() {
        val px = intArrayOf(black, white, black, white)
        assertArrayEquals(intArrayOf(black, white, black, white), BoxBlur.blur(px, 2, 2, 0))
    }

    @Test fun blurNeverLeavesChannelRange() {
        val px = IntArray(32 * 18) { if (it % 2 == 0) white else black }
        BoxBlur.blur(px, 32, 18, 5)
        assertTrue(px.all { (it ushr 24) == 255 })
    }
}
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `./gradlew :app:testDebugUnitTest --tests 'br.gui.inicio.core.ImageMathTest' --tests 'br.gui.inicio.core.BoxBlurTest'`
Expected: FAIL na compilação (`Unresolved reference 'ImageMath'`).

- [ ] **Step 3: Implementar**

`launcher/app/src/main/java/br/gui/inicio/core/ImageMath.kt`:
```kotlin
package br.gui.inicio.core

data class CropRect(val left: Int, val top: Int, val width: Int, val height: Int)

object ImageMath {
    /** Maior potência de 2 que ainda deixa a imagem decodificada >= destino nas duas dimensões. */
    fun sampleSize(srcW: Int, srcH: Int, dstW: Int, dstH: Int): Int {
        require(srcW > 0 && srcH > 0 && dstW > 0 && dstH > 0) { "medidas devem ser positivas" }
        var size = 1
        while (srcW / (size * 2) >= dstW && srcH / (size * 2) >= dstH) size *= 2
        return size
    }

    /** Recorte central da origem com a proporção do destino (efeito "cover"). */
    fun coverCrop(srcW: Int, srcH: Int, dstW: Int, dstH: Int): CropRect {
        require(srcW > 0 && srcH > 0 && dstW > 0 && dstH > 0) { "medidas devem ser positivas" }
        return if (srcW.toLong() * dstH > srcH.toLong() * dstW) {
            val w = (srcH.toLong() * dstW / dstH).toInt()
            CropRect((srcW - w) / 2, 0, w, srcH)
        } else {
            val h = (srcW.toLong() * dstH / dstW).toInt()
            CropRect(0, (srcH - h) / 2, srcW, h)
        }
    }
}
```

`launcher/app/src/main/java/br/gui/inicio/core/BoxBlur.kt`:
```kotlin
package br.gui.inicio.core

/** Desfoque de caixa em passadas horizontais e verticais (3 passadas aproximam um gaussiano). */
object BoxBlur {
    fun blur(pixels: IntArray, w: Int, h: Int, radius: Int, passes: Int = 3): IntArray {
        require(pixels.size == w * h) { "tamanho não confere" }
        if (radius < 1) return pixels
        val tmp = IntArray(pixels.size)
        repeat(passes) {
            pass(pixels, tmp, length = w, lines = h, step = 1, lineStride = w, radius = radius)
            pass(tmp, pixels, length = h, lines = w, step = w, lineStride = 1, radius = radius)
        }
        return pixels
    }

    /** Média móvel ao longo de cada linha (ou coluna), repetindo a borda nos extremos. */
    private fun pass(src: IntArray, dst: IntArray, length: Int, lines: Int, step: Int, lineStride: Int, radius: Int) {
        val div = 2 * radius + 1
        for (line in 0 until lines) {
            val base = line * lineStride
            fun at(i: Int) = src[base + i.coerceIn(0, length - 1) * step]
            var a = 0; var r = 0; var g = 0; var b = 0
            for (i in -radius..radius) {
                val p = at(i)
                a += p ushr 24; r += (p shr 16) and 0xFF; g += (p shr 8) and 0xFF; b += p and 0xFF
            }
            for (i in 0 until length) {
                dst[base + i * step] = ((a / div) shl 24) or ((r / div) shl 16) or ((g / div) shl 8) or (b / div)
                val out = at(i - radius)
                val inn = at(i + radius + 1)
                a += (inn ushr 24) - (out ushr 24)
                r += ((inn shr 16) and 0xFF) - ((out shr 16) and 0xFF)
                g += ((inn shr 8) and 0xFF) - ((out shr 8) and 0xFF)
                b += (inn and 0xFF) - (out and 0xFF)
            }
        }
    }
}
```

- [ ] **Step 4: Rodar e ver passar (e a suíte inteira)**

Run: `./gradlew :app:testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`, 37 testes passando (11 + 8 + 8 + 10).

- [ ] **Step 5: Commit**

```bash
cd ~/pessoal/projetor-bettdow && grep -rn $'\u2014' launcher; \
git add launcher && git commit -m "LAUNCHER | FEAT | Recorte de imagem e desfoque para o vidro"
```

### Task 5: Recursos, manifesto e tema

**Files:**
- Create: `launcher/app/src/main/res/font/inter_regular.ttf`, `inter_medium.ttf`, `inter_semibold.ttf`, `inter_bold.ttf`
- Create: `launcher/app/src/main/assets/wallpapers/montanha.jpg`, `vale.jpg`, `lago.jpg`
- Create: `launcher/app/src/main/res/values/strings.xml`, `launcher/app/src/main/res/values/themes.xml`
- Create: `launcher/app/src/main/res/drawable/ic_app.xml`, `launcher/app/src/main/res/drawable/banner.xml`
- Create: `docs/CREDITOS.md`
- Modify: `launcher/app/src/main/AndroidManifest.xml` (substituir inteiro)

**Interfaces:**
- Produces: `R.font.inter_regular`, `R.font.inter_medium`, `R.font.inter_semibold`, `R.font.inter_bold`; assets `wallpapers/montanha.jpg`, `wallpapers/vale.jpg`, `wallpapers/lago.jpg` (1920x1080); tema `@style/Theme.Inicio`; as quatro Activities declaradas com os nomes `.home.HomeActivity`, `.apps.AllAppsActivity`, `.settings.DockEditorActivity`, `.settings.WallpaperActivity` (as classes nascem nas Tasks 10 a 14; até lá o build de APK não é executado, só os testes JVM).

- [ ] **Step 1: Baixar a fonte Inter (OFL) e copiar os 4 pesos**

```bash
D=$(mktemp -d) && cd "$D" && \
curl -fsSL -o inter.zip https://github.com/rsms/inter/releases/download/v4.1/Inter-4.1.zip && \
unzip -q inter.zip && \
F=~/pessoal/projetor-bettdow/launcher/app/src/main/res/font && mkdir -p "$F" && \
cp extras/ttf/Inter-Regular.ttf  "$F/inter_regular.ttf" && \
cp extras/ttf/Inter-Medium.ttf   "$F/inter_medium.ttf" && \
cp extras/ttf/Inter-SemiBold.ttf "$F/inter_semibold.ttf" && \
cp extras/ttf/Inter-Bold.ttf     "$F/inter_bold.ttf" && \
cp LICENSE.txt ~/pessoal/projetor-bettdow/docs/LICENCA-INTER.txt && ls -la "$F"
```
Expected: 4 arquivos `.ttf` de 300 a 450 KB cada. Se o caminho `extras/ttf` não existir nessa versão, rodar `find . -name 'Inter-Regular.ttf'` e usar a pasta encontrada.

- [ ] **Step 2: Baixar e preparar as 3 fotos (Unsplash, via Picsum)**

```bash
A=~/pessoal/projetor-bettdow/launcher/app/src/main/assets/wallpapers && mkdir -p "$A" && T=$(mktemp -d) && \
for pair in 1018:montanha 1015:vale 1036:lago; do \
  id=${pair%%:*}; nome=${pair##*:}; \
  curl -fsSL -o "$T/$nome.jpg" "https://picsum.photos/id/$id/2400/1350.jpg" && \
  convert "$T/$nome.jpg" -resize '1920x1080^' -gravity center -extent 1920x1080 -strip -quality 82 "$A/$nome.jpg" && \
  curl -fsSL "https://picsum.photos/id/$id/info" > "$T/$nome.json"; \
done; identify "$A"/*.jpg; cat "$T"/*.json
```
Expected: três JPEG `1920x1080`, cada um com menos de 600 KB; os JSON trazem `author` e `url` de cada foto.

- [ ] **Step 3: Escrever `docs/CREDITOS.md` com os dados dos JSON do passo anterior**

```markdown
# Créditos

## Fonte
Inter, de Rasmus Andersson, licença SIL Open Font License 1.1 (texto em `docs/LICENCA-INTER.txt`).

## Fotos de papel de parede (Unsplash License, obtidas via picsum.photos)
| Arquivo | Autor | Origem |
|---|---|---|
| montanha.jpg | <author do JSON 1018> | <url do JSON 1018> |
| vale.jpg | <author do JSON 1015> | <url do JSON 1015> |
| lago.jpg | <author do JSON 1036> | <url do JSON 1036> |
```
Os valores entre `< >` são copiados literalmente da saída do Step 2 (campos `author` e `url`); nenhum fica entre `< >` no arquivo final.

- [ ] **Step 4: Escrever strings, tema e ícones**

`launcher/app/src/main/res/values/strings.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Início</string>
</resources>
```

`launcher/app/src/main/res/values/themes.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.Inicio" parent="android:Theme.DeviceDefault.NoActionBar.Fullscreen">
        <item name="android:windowBackground">@android:color/black</item>
        <item name="android:defaultFocusHighlightEnabled">false</item>
        <item name="android:windowDisablePreview">false</item>
    </style>
</resources>
```

`launcher/app/src/main/res/drawable/ic_app.xml` (ícone do próprio launcher: squircle cinza com grade 3x3):
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="96dp" android:height="96dp" android:viewportWidth="96" android:viewportHeight="96">
    <path android:fillColor="#FF8E9099"
        android:pathData="M21.6,0h52.8A21.6,21.6 0,0 1,96 21.6v52.8A21.6,21.6 0,0 1,74.4 96H21.6A21.6,21.6 0,0 1,0 74.4V21.6A21.6,21.6 0,0 1,21.6 0z" />
    <path android:fillColor="#FFFFFFFF"
        android:pathData="M22,22h14v14h-14z M41,22h14v14h-14z M60,22h14v14h-14z M22,41h14v14h-14z M41,41h14v14h-14z M60,41h14v14h-14z M22,60h14v14h-14z M41,60h14v14h-14z M60,60h14v14h-14z" />
</vector>
```

`launcher/app/src/main/res/drawable/banner.xml` (banner de TV 320x180):
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="320dp" android:height="180dp" android:viewportWidth="320" android:viewportHeight="180">
    <path android:fillColor="#FF1B1F5E" android:pathData="M0,0h320v180h-320z" />
    <path android:fillColor="#FF6A5CFF" android:pathData="M200,0h120v90a60,60 0,0 1,-60 -60z" />
    <path android:fillColor="#FFFFFFFF"
        android:pathData="M124,54h20v20h-20z M150,54h20v20h-20z M176,54h20v20h-20z M124,80h20v20h-20z M150,80h20v20h-20z M176,80h20v20h-20z M124,106h20v20h-20z M150,106h20v20h-20z M176,106h20v20h-20z" />
</vector>
```

- [ ] **Step 5: Substituir o manifesto**

`launcher/app/src/main/AndroidManifest.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <uses-feature android:name="android.software.leanback" android:required="false" />
    <uses-feature android:name="android.hardware.touchscreen" android:required="false" />

    <!-- Visibilidade de pacotes sem QUERY_ALL_PACKAGES: só apps abríveis e as telas do fabricante. -->
    <queries>
        <intent>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
        </intent>
        <intent>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LEANBACK_LAUNCHER" />
        </intent>
        <package android:name="com.zhiying.settings" />
        <package android:name="com.zhiying.autofocus" />
        <package android:name="com.newlink.source" />
        <package android:name="com.android.tv" />
        <package android:name="com.android.tv.settings" />
        <package android:name="com.android.vending" />
        <package android:name="com.zeasn.launcher.projector" />
    </queries>

    <application
        android:allowBackup="false"
        android:banner="@drawable/banner"
        android:icon="@drawable/ic_app"
        android:label="@string/app_name"
        android:supportsRtl="false"
        android:theme="@style/Theme.Inicio">

        <activity
            android:name=".home.HomeActivity"
            android:clearTaskOnLaunch="true"
            android:configChanges="keyboard|keyboardHidden|navigation|orientation|screenLayout|screenSize|uiMode"
            android:excludeFromRecents="true"
            android:exported="true"
            android:launchMode="singleTask"
            android:resumeWhilePausing="true"
            android:screenOrientation="landscape"
            android:stateNotNeeded="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.HOME" />
                <category android:name="android.intent.category.DEFAULT" />
            </intent-filter>
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LEANBACK_LAUNCHER" />
            </intent-filter>
        </activity>

        <activity android:name=".apps.AllAppsActivity" android:exported="false" android:screenOrientation="landscape" />
        <activity android:name=".settings.DockEditorActivity" android:exported="false" android:screenOrientation="landscape" />
        <activity android:name=".settings.WallpaperActivity" android:exported="false" android:screenOrientation="landscape" />
    </application>
</manifest>
```

- [ ] **Step 6: Conferir que os recursos compilam**

Run: `cd ~/pessoal/projetor-bettdow/launcher && ./gradlew :app:processDebugResources :app:testDebugUnitTest`
Expected: `BUILD SUCCESSFUL` (as Activities ainda não existem, mas o processamento de recursos e os testes JVM não dependem delas).

- [ ] **Step 7: Commit**

```bash
cd ~/pessoal/projetor-bettdow && grep -rn $'\u2014' launcher/app/src docs/CREDITOS.md; \
git add launcher docs/CREDITOS.md docs/LICENCA-INTER.txt && git commit -m "LAUNCHER | BUILD | Fonte Inter, fotos, ícones, tema e manifesto"
```

---

### Task 6: Kit visual (tema, vidro, glifos, ícones)

**Files:**
- Create: `launcher/app/src/main/java/br/gui/inicio/ui/Theme.kt`
- Create: `launcher/app/src/main/java/br/gui/inicio/ui/Glass.kt`
- Create: `launcher/app/src/main/java/br/gui/inicio/ui/Glyphs.kt`
- Create: `launcher/app/src/main/java/br/gui/inicio/ui/IconFactory.kt`

**Interfaces:**
- Consumes: `R.font.*` (Task 5).
- Produces: `object Theme` (constantes `SELECTION`, `TODAY`, `GLASS_FILL`, `GLASS_BORDER`, `GLASS_HIGHLIGHT`, `FOCUS_FILL`, `TILE_FILL`, `TEXT_SHADOW`, `DIM`, `BLUR_DOWNSCALE = 8`, `BLUR_RADIUS = 5`; funções `init(ctx)`, `s(px: Float): Float`, `si(px: Float): Int`, `font(ctx, weight: Int): Typeface`, `text(ctx, sizePx: Float, weight: Int, value: CharSequence = ""): TextView`, `rounded(color: Int, radiusPx: Float): GradientDrawable`, `oval(color: Int): GradientDrawable`).
- Produces: `object GlassSource { var blurred: Bitmap? }`; `class GlassDrawable(host: View, radiusPx: Float) : Drawable`; extensão `fun View.invalidateTree()`.
- Produces: `enum class Glyph { GEAR, INPUT, GRID, WIFI, BLUETOOTH, CONTROLS, FOCUS, KEYSTONE, ZOOM, PICTURE, PROJECTION, SOUND, CHECK }`; `object Glyphs { fun draw(c: Canvas, g: Glyph, cx: Float, cy: Float, size: Float, color: Int) }`; `class GlyphView(ctx: Context, glyph: Glyph) : View` com `var glyph`, `var color`.
- Produces: `object IconFactory` com `squirclePath(size: Float): Path`, `app(icon: Drawable, size: Int): Bitmap`, `banner(banner: Drawable, size: Int): Bitmap`, `glyph(g: Glyph, top: Int, bottom: Int, size: Int): Bitmap`.

- [ ] **Step 1: Escrever `Theme.kt`**

```kotlin
package br.gui.inicio.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.widget.TextView
import br.gui.inicio.R

/** Tokens visuais da especificação (seção 3). Medidas pensadas para 1920x1080 e convertidas por [s]. */
object Theme {
    const val SELECTION = 0xFF3478F6.toInt()
    const val TODAY = 0xFFFF3B30.toInt()
    const val GLASS_FILL = 0x29FFFFFF
    const val GLASS_BORDER = 0x38FFFFFF
    const val GLASS_HIGHLIGHT = 0x40FFFFFF
    const val FOCUS_FILL = 0x4DFFFFFF
    const val TILE_FILL = 0x24FFFFFF
    const val TEXT_SHADOW = 0x59000000
    const val DIM = 0x73000000
    const val BLUR_DOWNSCALE = 8
    const val BLUR_RADIUS = 5

    private var factor = 1f
    private val fonts = HashMap<Int, Typeface>()

    fun init(ctx: Context) {
        factor = ctx.resources.displayMetrics.widthPixels / 1920f
    }

    fun s(px: Float): Float = px * factor

    fun si(px: Float): Int = Math.round(px * factor)

    fun font(ctx: Context, weight: Int): Typeface = fonts.getOrPut(weight) {
        val res = when {
            weight >= 700 -> R.font.inter_bold
            weight >= 600 -> R.font.inter_semibold
            weight >= 500 -> R.font.inter_medium
            else -> R.font.inter_regular
        }
        ctx.resources.getFont(res)
    }

    /** Texto branco com a sombra suave que garante contraste sobre qualquer fundo. */
    fun text(ctx: Context, sizePx: Float, weight: Int, value: CharSequence = ""): TextView =
        TextView(ctx).apply {
            text = value
            setTextSize(TypedValue.COMPLEX_UNIT_PX, s(sizePx))
            typeface = font(ctx, weight)
            setTextColor(Color.WHITE)
            setShadowLayer(s(6f), 0f, s(1f), TEXT_SHADOW)
            includeFontPadding = false
        }

    fun rounded(color: Int, radiusPx: Float) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = s(radiusPx)
    }

    fun oval(color: Int) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
    }
}
```

- [ ] **Step 2: Escrever `Glass.kt`**

```kotlin
package br.gui.inicio.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup

/** Papel de parede reduzido (1/8) e desfocado uma única vez; é a "matéria" do vidro. */
object GlassSource {
    @Volatile var blurred: Bitmap? = null
}

/**
 * Vidro fosco barato: desenha o pedaço do fundo já desfocado que fica atrás de [host],
 * mais um véu branco, borda e um brilho no topo. Nenhum desfoque é calculado por quadro.
 */
class GlassDrawable(private val host: View, private val radiusPx: Float) : Drawable() {
    private val loc = IntArray(2)
    private val rect = RectF()
    private val path = Path()
    private val matrix = Matrix()
    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Theme.GLASS_FILL }
    private val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        color = Theme.GLASS_BORDER
    }
    private val highlight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 1.5f
        color = Theme.GLASS_HIGHLIGHT
    }

    override fun draw(canvas: Canvas) {
        rect.set(bounds)
        path.reset()
        path.addRoundRect(rect, radiusPx, radiusPx, Path.Direction.CW)
        canvas.save()
        canvas.clipPath(path)
        GlassSource.blurred?.let { bmp ->
            host.getLocationOnScreen(loc)
            val scale = host.resources.displayMetrics.widthPixels.toFloat() / bmp.width
            matrix.setScale(scale, scale)
            matrix.postTranslate(-loc[0].toFloat(), -loc[1].toFloat())
            canvas.drawBitmap(bmp, matrix, bitmapPaint)
        }
        canvas.drawRect(rect, fill)
        canvas.restore()
        canvas.drawPath(path, border)
        canvas.drawLine(rect.left + radiusPx, rect.top + 1f, rect.right - radiusPx, rect.top + 1f, highlight)
    }

    override fun setAlpha(alpha: Int) = Unit

    override fun setColorFilter(colorFilter: ColorFilter?) = Unit

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

/** Redesenha a view e todos os filhos (o vidro precisa redesenhar quando o fundo muda). */
fun View.invalidateTree() {
    invalidate()
    if (this is ViewGroup) for (i in 0 until childCount) getChildAt(i).invalidateTree()
}
```

- [ ] **Step 3: Escrever `Glyphs.kt`**

```kotlin
package br.gui.inicio.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.View

enum class Glyph { GEAR, INPUT, GRID, WIFI, BLUETOOTH, CONTROLS, FOCUS, KEYSTONE, ZOOM, PICTURE, PROJECTION, SOUND, CHECK }

/** Ícones desenhados em código, para não depender de fontes de símbolos que o aparelho não tem. */
object Glyphs {
    fun draw(c: Canvas, g: Glyph, cx: Float, cy: Float, size: Float, color: Int) {
        val r = size / 2
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = size * 0.08f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val f = Paint(p).apply { style = Paint.Style.FILL }
        when (g) {
            Glyph.GEAR -> {
                c.drawCircle(cx, cy, r * 0.62f, p)
                c.drawCircle(cx, cy, r * 0.28f, p)
                for (i in 0 until 8) {
                    c.save()
                    c.rotate(i * 45f, cx, cy)
                    c.drawRoundRect(cx - r * 0.13f, cy - r * 0.95f, cx + r * 0.13f, cy - r * 0.6f, r * 0.05f, r * 0.05f, f)
                    c.restore()
                }
            }
            Glyph.INPUT -> {
                c.drawRoundRect(cx - r * 0.9f, cy - r * 0.6f, cx + r * 0.9f, cy + r * 0.45f, r * 0.12f, r * 0.12f, p)
                c.drawLine(cx - r * 0.35f, cy + r * 0.8f, cx + r * 0.35f, cy + r * 0.8f, p)
                c.drawLine(cx - r * 0.4f, cy - r * 0.08f, cx + r * 0.25f, cy - r * 0.08f, p)
                c.drawLine(cx + r * 0.05f, cy - r * 0.3f, cx + r * 0.27f, cy - r * 0.08f, p)
                c.drawLine(cx + r * 0.05f, cy + r * 0.14f, cx + r * 0.27f, cy - r * 0.08f, p)
            }
            Glyph.GRID -> {
                val q = r * 0.42f
                val gap = r * 0.62f
                for (i in -1..1) for (j in -1..1) {
                    val x = cx + i * gap
                    val y = cy + j * gap
                    c.drawRoundRect(x - q / 2, y - q / 2, x + q / 2, y + q / 2, q * 0.28f, q * 0.28f, f)
                }
            }
            Glyph.WIFI -> {
                val base = cy + r * 0.55f
                for (k in 1..3) {
                    val rr = r * 0.32f * k
                    c.drawArc(cx - rr, base - rr, cx + rr, base + rr, 225f, 90f, false, p)
                }
                c.drawCircle(cx, base, r * 0.1f, f)
            }
            Glyph.BLUETOOTH -> {
                val path = Path().apply {
                    moveTo(cx - r * 0.45f, cy - r * 0.4f)
                    lineTo(cx + r * 0.4f, cy + r * 0.38f)
                    lineTo(cx, cy + r * 0.8f)
                    lineTo(cx, cy - r * 0.8f)
                    lineTo(cx + r * 0.4f, cy - r * 0.38f)
                    lineTo(cx - r * 0.45f, cy + r * 0.4f)
                }
                c.drawPath(path, p)
            }
            Glyph.CONTROLS -> {
                for ((dy, knobLeft) in listOf(-0.4f to false, 0.4f to true)) {
                    val y = cy + r * dy
                    c.drawRoundRect(cx - r * 0.85f, y - r * 0.27f, cx + r * 0.85f, y + r * 0.27f, r * 0.27f, r * 0.27f, p)
                    c.drawCircle(if (knobLeft) cx - r * 0.58f else cx + r * 0.58f, y, r * 0.15f, f)
                }
            }
            Glyph.FOCUS -> {
                c.drawCircle(cx, cy, r * 0.3f, p)
                c.drawCircle(cx, cy, r * 0.72f, p)
                for (i in 0 until 4) {
                    c.save()
                    c.rotate(i * 90f, cx, cy)
                    c.drawLine(cx, cy - r * 0.98f, cx, cy - r * 0.82f, p)
                    c.restore()
                }
            }
            Glyph.KEYSTONE -> {
                val path = Path().apply {
                    moveTo(cx - r * 0.6f, cy - r * 0.6f)
                    lineTo(cx + r * 0.6f, cy - r * 0.6f)
                    lineTo(cx + r * 0.9f, cy + r * 0.6f)
                    lineTo(cx - r * 0.9f, cy + r * 0.6f)
                    close()
                }
                c.drawPath(path, p)
            }
            Glyph.ZOOM -> {
                c.drawRect(cx - r * 0.38f, cy - r * 0.38f, cx + r * 0.38f, cy + r * 0.38f, p)
                for ((sx, sy) in listOf(-1f to -1f, 1f to -1f, -1f to 1f, 1f to 1f)) {
                    c.drawLine(cx + sx * r * 0.58f, cy + sy * r * 0.58f, cx + sx * r * 0.92f, cy + sy * r * 0.92f, p)
                }
            }
            Glyph.PICTURE -> {
                c.drawCircle(cx, cy, r * 0.75f, p)
                c.drawArc(cx - r * 0.75f, cy - r * 0.75f, cx + r * 0.75f, cy + r * 0.75f, 90f, 180f, true, f)
            }
            Glyph.PROJECTION -> {
                c.drawRoundRect(cx - r * 0.85f, cy - r * 0.3f, cx + r * 0.25f, cy + r * 0.45f, r * 0.1f, r * 0.1f, p)
                c.drawCircle(cx - r * 0.3f, cy + r * 0.07f, r * 0.18f, p)
                c.drawLine(cx + r * 0.4f, cy - r * 0.1f, cx + r * 0.9f, cy - r * 0.5f, p)
                c.drawLine(cx + r * 0.4f, cy + r * 0.2f, cx + r * 0.9f, cy + r * 0.45f, p)
            }
            Glyph.SOUND -> {
                val path = Path().apply {
                    moveTo(cx - r * 0.9f, cy - r * 0.25f)
                    lineTo(cx - r * 0.5f, cy - r * 0.25f)
                    lineTo(cx - r * 0.1f, cy - r * 0.65f)
                    lineTo(cx - r * 0.1f, cy + r * 0.65f)
                    lineTo(cx - r * 0.5f, cy + r * 0.25f)
                    lineTo(cx - r * 0.9f, cy + r * 0.25f)
                    close()
                }
                c.drawPath(path, f)
                for (k in 1..2) {
                    val rr = r * 0.32f * k
                    c.drawArc(cx + r * 0.05f - rr, cy - rr, cx + r * 0.05f + rr, cy + rr, -45f, 90f, false, p)
                }
            }
            Glyph.CHECK -> {
                c.drawLine(cx - r * 0.5f, cy, cx - r * 0.1f, cy + r * 0.4f, p)
                c.drawLine(cx - r * 0.1f, cy + r * 0.4f, cx + r * 0.55f, cy - r * 0.4f, p)
            }
        }
    }
}

/** View que desenha um [Glyph] centralizado no tamanho da própria view. */
class GlyphView(ctx: Context, glyph: Glyph) : View(ctx) {
    var glyph: Glyph = glyph
        set(v) { field = v; invalidate() }
    var color: Int = Color.WHITE
        set(v) { field = v; invalidate() }

    override fun onDraw(canvas: Canvas) {
        Glyphs.draw(canvas, glyph, width / 2f, height / 2f, minOf(width, height).toFloat(), color)
    }
}
```

- [ ] **Step 4: Escrever `IconFactory.kt`**

```kotlin
package br.gui.inicio.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable

/** Gera os ícones squircle (raio 22,5%) do Dock, do Launchpad e das telas de ajuste. */
object IconFactory {
    fun squirclePath(size: Float): Path = Path().apply {
        addRoundRect(0f, 0f, size, size, size * 0.225f, size * 0.225f, Path.Direction.CW)
    }

    /** Ícone de app: adaptativo usa as próprias camadas; ícone comum vai centralizado sobre fundo branco. */
    fun app(icon: Drawable, size: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.clipPath(squirclePath(size.toFloat()))
        if (icon is AdaptiveIconDrawable) {
            val inset = size / 4 // camadas de 108dp com 72dp visíveis: 18/72 = 0,25
            icon.setBounds(-inset, -inset, size + inset, size + inset)
            icon.draw(c)
        } else {
            c.drawColor(Color.WHITE)
            val pad = (size * 0.14f).toInt()
            icon.setBounds(pad, pad, size - pad, size - pad)
            icon.draw(c)
        }
        return bmp
    }

    /** Banner de TV (16:9) recortado no centro, para apps que só têm ícone de baixa resolução. */
    fun banner(banner: Drawable, size: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.clipPath(squirclePath(size.toFloat()))
        val w = size * 16 / 9
        val left = (size - w) / 2
        banner.setBounds(left, 0, left + w, size)
        banner.draw(c)
        return bmp
    }

    /** Ícone de atalho do sistema: squircle com degradê vertical e um glifo branco. */
    fun glyph(g: Glyph, top: Int, bottom: Int, size: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, 0f, size.toFloat(), top, bottom, Shader.TileMode.CLAMP)
        }
        c.drawPath(squirclePath(size.toFloat()), paint)
        Glyphs.draw(c, g, size / 2f, size / 2f, size * 0.52f, Color.WHITE)
        return bmp
    }
}
```

- [ ] **Step 5: Compilar o Kotlin**

Run: `cd ~/pessoal/projetor-bettdow/launcher && ./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`. Avisos de "deprecated" no `getOpacity` são esperados.

- [ ] **Step 6: Commit**

```bash
cd ~/pessoal/projetor-bettdow && grep -rn $'\u2014' launcher/app/src; \
git add launcher && git commit -m "LAUNCHER | FEAT | Kit visual: tema, vidro, glifos e ícones squircle"
```

---

### Task 7: Dados (preferências, apps, atalhos do sistema)

**Files:**
- Create: `launcher/app/src/main/java/br/gui/inicio/data/Prefs.kt`
- Create: `launcher/app/src/main/java/br/gui/inicio/data/AppRepository.kt`
- Create: `launcher/app/src/main/java/br/gui/inicio/data/Shortcuts.kt`

**Interfaces:**
- Consumes: `DockConfig`, `WallpaperChoice`, `NewAppsState` (Tasks 1 e 2); `Glyph`, `IconFactory` (Task 6).
- Produces: `class Prefs(ctx)` com `var dock: DockConfig`, `var wallpaper: WallpaperChoice`, `var newApps: NewAppsState`.
- Produces: `data class AppEntry(val pkg: String, val label: String, val component: ComponentName, val info: ActivityInfo)`; `class AppRepository(ctx)` com `launchable(): List<AppEntry>`, `launch(entry: AppEntry): Boolean`, `start(intent: Intent): Boolean`, `iconBitmap(entry: AppEntry, size: Int): Bitmap`; constante `AppRepository.PLAY_STORE = "com.android.vending"`.
- Produces: `data class SystemShortcut(val id: String, val title: String, val subtitle: String, val glyph: Glyph, val intents: List<Intent>)`; `object Shortcuts` com `SETTINGS`, `SOURCE`, `WIFI`, `BLUETOOTH`, `DATE`, `AUTOFOCUS`, `KEYSTONE`, `ZOOM`, `PICTURE`, `PROJECTION`, `SOUND`, `ORIGINAL_LAUNCHER` e `CONTROL_CENTER: List<SystemShortcut>`; `class ShortcutResolver(ctx)` com `intentFor(s): Intent?`, `isAvailable(s): Boolean`, `available(list): List<SystemShortcut>`, `open(s): Boolean`.

- [ ] **Step 1: Escrever `Prefs.kt`**

```kotlin
package br.gui.inicio.data

import android.content.Context
import br.gui.inicio.core.DockConfig
import br.gui.inicio.core.NewAppsState
import br.gui.inicio.core.WallpaperChoice

/** Tudo o que o launcher lembra: ordem do Dock, papel de parede e o estado do ponto azul. */
class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("inicio", Context.MODE_PRIVATE)

    var dock: DockConfig
        get() = DockConfig.parse(sp.getString(K_DOCK, null))
        set(v) { sp.edit().putString(K_DOCK, v.serialize()).apply() }

    var wallpaper: WallpaperChoice
        get() = WallpaperChoice.parse(sp.getString(K_WALLPAPER, null))
        set(v) { sp.edit().putString(K_WALLPAPER, v.serialize()).apply() }

    var newApps: NewAppsState
        get() = NewAppsState(
            initialized = sp.getBoolean(K_INIT, false),
            known = sp.getStringSet(K_KNOWN, emptySet())!!.toSet(),
            opened = sp.getStringSet(K_OPENED, emptySet())!!.toSet(),
        )
        set(v) {
            sp.edit()
                .putBoolean(K_INIT, v.initialized)
                .putStringSet(K_KNOWN, HashSet(v.known))
                .putStringSet(K_OPENED, HashSet(v.opened))
                .apply()
        }

    private companion object {
        const val K_DOCK = "dock"
        const val K_WALLPAPER = "wallpaper"
        const val K_INIT = "new_apps_initialized"
        const val K_KNOWN = "new_apps_known"
        const val K_OPENED = "new_apps_opened"
    }
}
```

- [ ] **Step 2: Escrever `AppRepository.kt`**

```kotlin
package br.gui.inicio.data

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.drawable.AdaptiveIconDrawable
import br.gui.inicio.ui.IconFactory
import java.text.Collator
import java.util.Locale

data class AppEntry(val pkg: String, val label: String, val component: ComponentName, val info: ActivityInfo)

/** Pergunta ao Android quais apps podem ser abertos e abre cada um. Não guarda cache: a lista é sempre a atual. */
class AppRepository(private val ctx: Context) {
    private val pm = ctx.packageManager
    private val collator = Collator.getInstance(Locale.forLanguageTag("pt-BR"))

    /** Um item por pacote (preferindo a entrada de TV), em ordem alfabética, sem o próprio launcher. */
    fun launchable(): List<AppEntry> {
        val byPkg = LinkedHashMap<String, AppEntry>()
        for (category in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
            val query = Intent(Intent.ACTION_MAIN).addCategory(category)
            for (ri in pm.queryIntentActivities(query, 0)) {
                val ai = ri.activityInfo
                if (ai.packageName == ctx.packageName || byPkg.containsKey(ai.packageName)) continue
                byPkg[ai.packageName] = AppEntry(
                    pkg = ai.packageName,
                    label = ri.loadLabel(pm).toString(),
                    component = ComponentName(ai.packageName, ai.name),
                    info = ai,
                )
            }
        }
        return byPkg.values.sortedWith { a, b -> collator.compare(a.label, b.label) }
    }

    fun launch(entry: AppEntry): Boolean = start(
        Intent(Intent.ACTION_MAIN)
            .setComponent(entry.component)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED),
    )

    fun start(intent: Intent): Boolean = try {
        ctx.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    } catch (e: SecurityException) {
        false
    }

    /** Ícone squircle; se o app só tiver um ícone pequeno, usa o banner de TV recortado. */
    fun iconBitmap(entry: AppEntry, size: Int): Bitmap {
        val icon = entry.info.loadIcon(pm)
        if (icon !is AdaptiveIconDrawable && icon.intrinsicWidth in 1 until size / 2) {
            entry.info.loadBanner(pm)?.let { return IconFactory.banner(it, size) }
        }
        return IconFactory.app(icon, size)
    }

    companion object {
        const val PLAY_STORE = "com.android.vending"
    }
}
```

- [ ] **Step 3: Escrever `Shortcuts.kt`**

```kotlin
package br.gui.inicio.data

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.tv.TvContract
import android.provider.Settings
import br.gui.inicio.ui.Glyph

/** Atalho para uma tela do sistema ou do fabricante. [intents] em ordem de preferência (o primeiro que existir). */
data class SystemShortcut(
    val id: String,
    val title: String,
    val subtitle: String,
    val glyph: Glyph,
    val intents: List<Intent>,
)

/** Telas verificadas no projetor em 26/10/2026 (ver especificação, seções 2.3 e 2.7). */
object Shortcuts {
    private fun action(a: String) = Intent(a)

    val SETTINGS = SystemShortcut("settings", "Todos os ajustes", "Menu do projetor", Glyph.GEAR, listOf(action(Settings.ACTION_SETTINGS)))
    val SOURCE = SystemShortcut(
        "source", "Fontes", "HDMI e outras", Glyph.INPUT,
        listOf(
            action("android.settings.SOURCE"),
            Intent(Intent.ACTION_VIEW, TvContract.buildChannelUriForPassthroughInput("com.sstar.tv.inputservice/.HdmiInputService/HW1")),
        ),
    )
    val WIFI = SystemShortcut("wifi", "Wi-Fi", "Redes", Glyph.WIFI, listOf(action(Settings.ACTION_WIFI_SETTINGS)))
    val BLUETOOTH = SystemShortcut("bluetooth", "Bluetooth", "Aparelhos", Glyph.BLUETOOTH, listOf(action(Settings.ACTION_BLUETOOTH_SETTINGS)))
    val DATE = SystemShortcut("date", "Data e hora", "", Glyph.GEAR, listOf(action(Settings.ACTION_DATE_SETTINGS)))
    val AUTOFOCUS = SystemShortcut("autofocus", "Foco automático", "Ajusta a nitidez", Glyph.FOCUS, listOf(action("com.zhiying.AUTO_FOCUS")))
    val KEYSTONE = SystemShortcut("keystone", "Keystone", "Corrigir 4 cantos", Glyph.KEYSTONE, listOf(action("android.intent.action.FOUR_POINT")))
    val ZOOM = SystemShortcut("zoom", "Zoom", "Tamanho da imagem", Glyph.ZOOM, listOf(action("android.intent.action.Scale")))
    val PICTURE = SystemShortcut("picture", "Modo de imagem", "Cores e brilho", Glyph.PICTURE, listOf(action("com.zhiying.PICTUREMODE")))
    val PROJECTION = SystemShortcut("projection", "Projeção", "Frontal, teto, traseira", Glyph.PROJECTION, listOf(action("android.settings.PROJECTION")))
    val SOUND = SystemShortcut("sound", "Som", "Saída e volume", Glyph.SOUND, listOf(action(Settings.ACTION_SOUND_SETTINGS)))
    val ORIGINAL_LAUNCHER = SystemShortcut(
        "original", "Launcher original do fabricante", "", Glyph.GRID,
        listOf(Intent(Intent.ACTION_MAIN).setComponent(ComponentName("com.zeasn.launcher.projector", "com.zeasn.launcher.ui.MainActivity"))),
    )

    val CONTROL_CENTER = listOf(AUTOFOCUS, KEYSTONE, ZOOM, PICTURE, PROJECTION, SOUND, SOURCE, SETTINGS)
}

class ShortcutResolver(private val ctx: Context) {
    /** Primeiro intent que existe neste aparelho; null esconde o atalho. Desativado também conta como inexistente. */
    fun intentFor(s: SystemShortcut): Intent? =
        s.intents.firstOrNull { it.resolveActivity(ctx.packageManager) != null }
            ?.let { Intent(it).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }

    fun isAvailable(s: SystemShortcut): Boolean = intentFor(s) != null

    fun available(list: List<SystemShortcut>): List<SystemShortcut> = list.filter(::isAvailable)

    fun open(s: SystemShortcut): Boolean {
        val intent = intentFor(s) ?: return false
        return try {
            ctx.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        } catch (e: SecurityException) {
            false
        }
    }
}
```

- [ ] **Step 4: Compilar**

Run: `cd ~/pessoal/projetor-bettdow/launcher && ./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
cd ~/pessoal/projetor-bettdow && grep -rn $'\u2014' launcher/app/src; \
git add launcher && git commit -m "LAUNCHER | FEAT | Preferências, lista de apps e atalhos do projetor"
```

---

### Task 8: Papel de parede (degradês, carregamento, fundo desfocado)

**Files:**
- Create: `launcher/app/src/main/java/br/gui/inicio/wallpaper/Gradients.kt`
- Create: `launcher/app/src/main/java/br/gui/inicio/wallpaper/WallpaperLoader.kt`
- Create: `launcher/app/src/main/java/br/gui/inicio/ui/Backdrop.kt`

**Interfaces:**
- Consumes: `WallpaperChoice`, `ImageMath`, `BoxBlur` (Tasks 2 e 4); `Theme`, `GlassSource`, `invalidateTree` (Task 6); `Prefs` (Task 7).
- Produces: `object Gradients { val COUNT: Int; fun render(index: Int, w: Int, h: Int): Bitmap }`.
- Produces: `class WallpaperLoader(ctx)` com `data class Result(val full: Bitmap, val blurred: Bitmap)`, `load(choice, w, h): Result` (nunca lança; cai no degradê 0), `render(choice, w, h): Bitmap?`, `assetNames(): List<String>`, `userFiles(): List<String>`.
- Produces: `object Backdrop { fun attach(activity: Activity, root: FrameLayout, prefs: Prefs, io: Executor) }`.

- [ ] **Step 1: Escrever `Gradients.kt`**

```kotlin
package br.gui.inicio.wallpaper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader

/** Os 4 degradês estilo macOS, desenhados por código (sem imagem no APK). */
object Gradients {
    private class Blob(val x: Float, val y: Float, val rx: Float, val ry: Float, val color: Int)
    private class Spec(val from: Int, val mid: Int, val to: Int, val blobs: List<Blob>)

    private fun c(hex: Long) = hex.toInt()

    private val SPECS = listOf(
        Spec(c(0xFF1B1F5E), c(0xFF3B1D6E), c(0xFF120F2E), listOf(
            Blob(0.15f, 0.85f, 0.60f, 0.80f, c(0xFFFF7A59)),
            Blob(0.85f, 0.20f, 0.70f, 0.90f, c(0xFF6A5CFF)),
            Blob(0.60f, 1.00f, 0.80f, 0.80f, c(0xFFE2408F)),
        )),
        Spec(c(0xFF0B2A4A), c(0xFF0A2140), c(0xFF08172E), listOf(
            Blob(0.20f, 0.20f, 0.70f, 0.90f, c(0xFF2FD3C4)),
            Blob(0.80f, 0.90f, 0.70f, 0.90f, c(0xFF2A6CF0)),
        )),
        Spec(c(0xFF3A1C1C), c(0xFF2A1416), c(0xFF1A0F12), listOf(
            Blob(0.80f, 0.20f, 0.70f, 0.90f, c(0xFFFFB347)),
            Blob(0.20f, 0.90f, 0.70f, 0.90f, c(0xFFFF5E62)),
        )),
        Spec(c(0xFF2B2D33), c(0xFF1E1F23), c(0xFF111214), emptyList()),
    )

    val COUNT: Int get() = SPECS.size

    fun render(index: Int, w: Int, h: Int): Bitmap {
        val spec = SPECS[index.coerceIn(0, SPECS.lastIndex)]
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565)
        val canvas = Canvas(bmp)
        val base = Paint(Paint.DITHER_FLAG).apply {
            shader = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), intArrayOf(spec.from, spec.mid, spec.to), null, Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), base)
        for (b in spec.blobs) {
            val paint = Paint(Paint.DITHER_FLAG or Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(0f, 0f, 1f, b.color, b.color and 0x00FFFFFF, Shader.TileMode.CLAMP)
            }
            canvas.save()
            canvas.translate(b.x * w, b.y * h)
            canvas.scale(b.rx * w * 0.6f, b.ry * h * 0.6f) // como no CSS: transparente a 60% do raio
            canvas.drawCircle(0f, 0f, 1f, paint)
            canvas.restore()
        }
        return bmp
    }
}
```

- [ ] **Step 2: Escrever `WallpaperLoader.kt`**

```kotlin
package br.gui.inicio.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import br.gui.inicio.core.BoxBlur
import br.gui.inicio.core.ImageMath
import br.gui.inicio.core.WallpaperChoice
import br.gui.inicio.ui.Theme
import java.io.File
import java.io.InputStream

/** Monta o papel de parede no tamanho exato da tela e a versão desfocada usada pelo vidro. Rodar fora da thread principal. */
class WallpaperLoader(private val ctx: Context) {
    data class Result(val full: Bitmap, val blurred: Bitmap)

    /** Nunca lança: qualquer falha (arquivo sumido, corrompido, memória) cai no degradê padrão. */
    fun load(choice: WallpaperChoice, w: Int, h: Int): Result {
        val full = render(choice, w, h) ?: Gradients.render(0, w, h)
        return Result(full, blur(full))
    }

    fun render(choice: WallpaperChoice, w: Int, h: Int): Bitmap? = try {
        when (choice) {
            is WallpaperChoice.Gradient -> Gradients.render(choice.index, w, h)
            is WallpaperChoice.Asset -> decodeCover({ ctx.assets.open("wallpapers/${choice.name}") }, w, h)
            is WallpaperChoice.UserFile -> {
                val file = userDir()?.let { File(it, choice.name) }
                if (file != null && file.isFile) decodeCover({ file.inputStream() }, w, h) else null
            }
        }
    } catch (e: Exception) {
        null
    } catch (e: OutOfMemoryError) {
        null
    }

    fun assetNames(): List<String> =
        ctx.assets.list("wallpapers")?.filter { WallpaperChoice.isSafeName(it) }?.sorted() ?: emptyList()

    /** Fotos enviadas pelo computador (scripts/enviar-wallpaper.sh). Pasta própria do app: sem permissão. */
    fun userFiles(): List<String> =
        userDir()?.listFiles()?.map { it.name }?.filter { WallpaperChoice.isSafeName(it) }?.sorted() ?: emptyList()

    private fun userDir(): File? = ctx.getExternalFilesDir("wallpapers")

    private fun decodeCover(open: () -> InputStream, w: Int, h: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open().use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val opts = BitmapFactory.Options().apply {
            inSampleSize = ImageMath.sampleSize(bounds.outWidth, bounds.outHeight, w, h)
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        val src = open().use { BitmapFactory.decodeStream(it, null, opts) } ?: return null
        val crop = ImageMath.coverCrop(src.width, src.height, w, h)
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565)
        Canvas(out).drawBitmap(
            src,
            Rect(crop.left, crop.top, crop.left + crop.width, crop.top + crop.height),
            Rect(0, 0, w, h),
            Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG),
        )
        src.recycle()
        return out
    }

    private fun blur(full: Bitmap): Bitmap {
        val sw = maxOf(1, full.width / Theme.BLUR_DOWNSCALE)
        val sh = maxOf(1, full.height / Theme.BLUR_DOWNSCALE)
        val small = Bitmap.createScaledBitmap(full, sw, sh, true).copy(Bitmap.Config.ARGB_8888, true)
        val px = IntArray(sw * sh)
        small.getPixels(px, 0, sw, 0, 0, sw, sh)
        BoxBlur.blur(px, sw, sh, Theme.BLUR_RADIUS)
        small.setPixels(px, 0, sw, 0, 0, sw, sh)
        return small
    }
}
```

- [ ] **Step 3: Escrever `Backdrop.kt`**

```kotlin
package br.gui.inicio.ui

import android.app.Activity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import br.gui.inicio.data.Prefs
import br.gui.inicio.wallpaper.WallpaperLoader
import java.util.concurrent.Executor

/** Fundo das telas secundárias: o papel de parede desfocado e escurecido, como o Launchpad. */
object Backdrop {
    fun attach(activity: Activity, root: FrameLayout, prefs: Prefs, io: Executor) {
        val match = FrameLayout.LayoutParams.MATCH_PARENT
        val image = ImageView(activity).apply { scaleType = ImageView.ScaleType.FIT_XY }
        root.addView(image, 0, FrameLayout.LayoutParams(match, match))
        root.addView(View(activity).apply { setBackgroundColor(Theme.DIM) }, 1, FrameLayout.LayoutParams(match, match))
        val ready = GlassSource.blurred
        if (ready != null) {
            image.setImageBitmap(ready)
            return
        }
        val m = activity.resources.displayMetrics
        io.execute {
            val result = WallpaperLoader(activity).load(prefs.wallpaper, m.widthPixels, m.heightPixels)
            GlassSource.blurred = result.blurred
            activity.runOnUiThread {
                image.setImageBitmap(result.blurred)
                root.invalidateTree()
            }
        }
    }
}
```

- [ ] **Step 4: Compilar**

Run: `cd ~/pessoal/projetor-bettdow/launcher && ./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
cd ~/pessoal/projetor-bettdow && grep -rn $'\u2014' launcher/app/src; \
git add launcher && git commit -m "LAUNCHER | FEAT | Degradês, carregamento de fotos e fundo desfocado"
```

### Task 9: Painéis (camada de painéis e menu em lista)

**Files:**
- Create: `launcher/app/src/main/java/br/gui/inicio/ui/PanelHost.kt`
- Create: `launcher/app/src/main/java/br/gui/inicio/ui/MenuPanel.kt`

**Interfaces:**
- Consumes: `Theme`, `GlassDrawable` (Task 6).
- Produces: `class PanelHost(ctx) : FrameLayout` com `val isOpen: Boolean`, `show(panel: View, lp: FrameLayout.LayoutParams, opener: View?, lockedWhileOpen: List<ViewGroup>)`, `close()`; `companion`: `below(start: Int? = null, end: Int? = null): FrameLayout.LayoutParams`, `centered(): FrameLayout.LayoutParams`.
- Produces: `data class MenuEntry(val title: String, val enabled: Boolean = true, val separatorBefore: Boolean = false, val action: () -> Unit = {})`; `class MenuPanel(ctx, header: String?, entries: List<MenuEntry>, onPick: () -> Unit) : LinearLayout` (`onPick` roda antes da ação; quem usa passa `{ panels.close() }`).

- [ ] **Step 1: Escrever `PanelHost.kt`**

```kotlin
package br.gui.inicio.ui

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout

/** Camada de painéis sobre a tela. Um painel por vez; o foco fica preso nele e volta para quem abriu ao fechar. */
class PanelHost(ctx: Context) : FrameLayout(ctx) {
    private var panel: View? = null
    private var opener: View? = null
    private val locked = mutableListOf<ViewGroup>()

    val isOpen: Boolean get() = panel != null

    /** [lockedWhileOpen]: áreas que não podem receber foco enquanto o painel estiver aberto. */
    fun show(panel: View, lp: FrameLayout.LayoutParams, opener: View?, lockedWhileOpen: List<ViewGroup>) {
        close()
        this.panel = panel
        this.opener = opener
        lockedWhileOpen.forEach {
            it.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            locked += it
        }
        addView(panel, lp)
        panel.alpha = 0f
        panel.animate().alpha(1f).setDuration(120).start()
        panel.post { panel.requestFocus() }
    }

    fun close() {
        val p = panel ?: return
        removeView(p)
        panel = null
        locked.forEach { it.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS }
        locked.clear()
        opener?.requestFocus()
        opener = null
    }

    companion object {
        private const val WRAP = FrameLayout.LayoutParams.WRAP_CONTENT

        /** Logo abaixo da barra de menu, a [start] px da esquerda ou a [end] px da direita. */
        fun below(start: Int? = null, end: Int? = null): FrameLayout.LayoutParams =
            FrameLayout.LayoutParams(WRAP, WRAP).apply {
                topMargin = Theme.si(64f)
                gravity = Gravity.TOP or (if (start != null) Gravity.START else Gravity.END)
                leftMargin = start ?: 0
                rightMargin = end ?: 0
            }

        fun centered(): FrameLayout.LayoutParams = FrameLayout.LayoutParams(WRAP, WRAP, Gravity.CENTER)
    }
}
```

- [ ] **Step 2: Escrever `MenuPanel.kt`**

```kotlin
package br.gui.inicio.ui

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

data class MenuEntry(
    val title: String,
    val enabled: Boolean = true,
    val separatorBefore: Boolean = false,
    val action: () -> Unit = {},
)

/** Menu de vidro em lista, como os menus do macOS. Item focado fica azul; item desabilitado fica esmaecido. */
class MenuPanel(ctx: Context, header: String?, entries: List<MenuEntry>, onPick: () -> Unit) : LinearLayout(ctx) {
    init {
        orientation = VERTICAL
        background = GlassDrawable(this, Theme.s(24f))
        val pad = Theme.si(12f)
        setPadding(pad, pad, pad, pad)
        minimumWidth = Theme.si(360f)
        if (header != null) {
            addView(Theme.text(ctx, 18f, 500, header).apply {
                alpha = 0.7f
                setPadding(Theme.si(20f), Theme.si(8f), Theme.si(20f), Theme.si(8f))
            })
        }
        entries.forEach { e ->
            if (e.separatorBefore) {
                addView(View(ctx).apply { setBackgroundColor(0x33FFFFFF) }, LayoutParams(LayoutParams.MATCH_PARENT, maxOf(1, Theme.si(1.5f))).apply {
                    setMargins(Theme.si(8f), Theme.si(6f), Theme.si(8f), Theme.si(6f))
                })
            }
            addView(item(ctx, e, onPick), LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        }
    }

    private fun item(ctx: Context, e: MenuEntry, onPick: () -> Unit): TextView =
        Theme.text(ctx, 22f, 500, e.title).apply {
            setPadding(Theme.si(20f), Theme.si(12f), Theme.si(20f), Theme.si(12f))
            isFocusable = e.enabled
            alpha = if (e.enabled) 1f else 0.45f
            setOnFocusChangeListener { v, has -> v.background = if (has) Theme.rounded(Theme.SELECTION, 12f) else null }
            setOnClickListener {
                onPick()
                e.action()
            }
        }
}
```

- [ ] **Step 3: Compilar**

Run: `cd ~/pessoal/projetor-bettdow/launcher && ./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
cd ~/pessoal/projetor-bettdow && grep -rn $'\u2014' launcher/app/src; \
git add launcher && git commit -m "LAUNCHER | FEAT | Camada de painéis e menu de vidro"
```

---

### Task 10: Tela "Todos os apps" (Launchpad)

**Files:**
- Create: `launcher/app/src/main/java/br/gui/inicio/apps/AllAppsActivity.kt`

**Interfaces:**
- Consumes: `NewAppsTracker` (Task 2); `Theme`, `Backdrop`, `PanelHost`, `MenuPanel`, `MenuEntry` (Tasks 6, 8, 9); `Prefs`, `AppRepository`, `AppEntry`, `DockConfig` via `Prefs.dock` (Task 7).
- Produces: `class AllAppsActivity : Activity` (aberta pela Home na Task 14 com `Intent(this, AllAppsActivity::class.java)`); `AllAppsActivity.COLS = 6`, `ROWS = 3`, `PAGE = 18`.

- [ ] **Step 1: Escrever `AllAppsActivity.kt`**

```kotlin
package br.gui.inicio.apps

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import br.gui.inicio.core.NewAppsTracker
import br.gui.inicio.data.AppEntry
import br.gui.inicio.data.AppRepository
import br.gui.inicio.data.Prefs
import br.gui.inicio.ui.Backdrop
import br.gui.inicio.ui.MenuEntry
import br.gui.inicio.ui.MenuPanel
import br.gui.inicio.ui.PanelHost
import br.gui.inicio.ui.Theme
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/** "Todos os apps", estilo Launchpad: grade 6 x 3 por página, ponto azul em app novo, menu com Menu ou OK segurado. */
class AllAppsActivity : Activity() {
    private lateinit var prefs: Prefs
    private lateinit var repo: AppRepository
    private lateinit var panels: PanelHost
    private lateinit var grid: GridLayout
    private lateinit var dots: LinearLayout
    private val io: ExecutorService = Executors.newSingleThreadExecutor()
    private var apps: List<AppEntry> = emptyList()
    private var page = 0
    private val pageCount: Int get() = maxOf(1, (apps.size + PAGE - 1) / PAGE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Theme.init(this)
        prefs = Prefs(this)
        repo = AppRepository(this)
        val wrap = FrameLayout.LayoutParams.WRAP_CONTENT
        val match = FrameLayout.LayoutParams.MATCH_PARENT
        val root = FrameLayout(this).apply { clipChildren = false }
        Backdrop.attach(this, root, prefs, io)
        root.addView(
            Theme.text(this, 28f, 600, "Todos os apps").apply { alpha = 0.9f },
            FrameLayout.LayoutParams(wrap, wrap, Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply { topMargin = Theme.si(60f) },
        )
        grid = GridLayout(this).apply {
            columnCount = COLS
            clipChildren = false
            clipToPadding = false
        }
        root.addView(grid, FrameLayout.LayoutParams(wrap, wrap, Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply { topMargin = Theme.si(160f) })
        dots = LinearLayout(this).apply { gravity = Gravity.CENTER }
        root.addView(dots, FrameLayout.LayoutParams(wrap, wrap, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply { bottomMargin = Theme.si(56f) })
        panels = PanelHost(this)
        root.addView(panels, FrameLayout.LayoutParams(match, match))
        setContentView(root)
    }

    override fun onStart() {
        super.onStart()
        io.execute {
            val list = repo.launchable()
            val installed = list.mapTo(HashSet()) { it.pkg }
            prefs.newApps = NewAppsTracker.prune(NewAppsTracker.ensureInitialized(prefs.newApps, installed), installed)
            runOnUiThread {
                if (isDestroyed) return@runOnUiThread
                apps = list
                showPage(page.coerceIn(0, pageCount - 1), focusIndex = 0)
            }
        }
    }

    override fun onDestroy() {
        io.shutdownNow()
        super.onDestroy()
    }

    private fun showPage(p: Int, focusIndex: Int) {
        page = p
        val slice = apps.drop(p * PAGE).take(PAGE)
        val size = Theme.si(144f)
        io.execute {
            val icons = slice.map { repo.iconBitmap(it, size) }
            runOnUiThread {
                if (isDestroyed || page != p) return@runOnUiThread
                val state = prefs.newApps
                grid.removeAllViews()
                slice.forEachIndexed { i, entry -> grid.addView(cell(entry, icons[i], NewAppsTracker.isNew(state, entry.pkg))) }
                renderDots()
                if (slice.isNotEmpty()) grid.getChildAt(focusIndex.coerceIn(0, slice.lastIndex)).requestFocus()
            }
        }
    }

    private fun cell(entry: AppEntry, icon: Bitmap, isNew: Boolean): View {
        val size = Theme.si(144f)
        val ring = GradientDrawable().apply {
            cornerRadius = size * 0.225f
            setStroke(Theme.si(4f), Color.WHITE)
        }
        val iconView = ImageView(this).apply { setImageBitmap(icon) }
        val frame = FrameLayout(this).apply {
            clipChildren = false
            addView(iconView, FrameLayout.LayoutParams(size, size))
        }
        if (isNew) {
            val dot = View(this).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Theme.SELECTION)
                    setStroke(Theme.si(3f), Color.WHITE)
                }
            }
            frame.addView(dot, FrameLayout.LayoutParams(Theme.si(26f), Theme.si(26f), Gravity.TOP or Gravity.END).apply {
                topMargin = -Theme.si(6f)
                rightMargin = -Theme.si(6f)
            })
        }
        val label = Theme.text(this, 20f, 500, entry.label).apply {
            gravity = Gravity.CENTER
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            maxWidth = Theme.si(200f)
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            isFocusable = true
            clipChildren = false
            addView(frame, LinearLayout.LayoutParams(size, size))
            addView(label, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = Theme.si(14f) })
            layoutParams = GridLayout.LayoutParams().apply {
                width = Theme.si(220f)
                setMargins(0, 0, 0, Theme.si(44f))
            }
            setOnFocusChangeListener { _, has ->
                frame.animate().scaleX(if (has) 1.18f else 1f).scaleY(if (has) 1.18f else 1f).setDuration(150).start()
                iconView.foreground = if (has) ring else null
            }
            setOnClickListener { open(entry) }
            setOnLongClickListener {
                showMenu(entry, this)
                true
            }
        }
    }

    private fun renderDots() {
        dots.removeAllViews()
        if (pageCount < 2) return
        repeat(pageCount) { i ->
            dots.addView(View(this).apply { background = Theme.oval(if (i == page) Color.WHITE else 0x59FFFFFF) },
                LinearLayout.LayoutParams(Theme.si(14f), Theme.si(14f)).apply { setMargins(Theme.si(8f), 0, Theme.si(8f), 0) })
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            if (panels.isOpen) {
                if (event.keyCode == KeyEvent.KEYCODE_BACK) {
                    panels.close()
                    return true
                }
                return super.dispatchKeyEvent(event)
            }
            val focused = grid.focusedChild
            if (focused != null) {
                val idx = grid.indexOfChild(focused)
                val col = idx % COLS
                when (event.keyCode) {
                    KeyEvent.KEYCODE_DPAD_LEFT -> if (col == 0 && page > 0) {
                        showPage(page - 1, idx + COLS - 1)
                        return true
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> if ((col == COLS - 1 || idx == grid.childCount - 1) && page < pageCount - 1) {
                        showPage(page + 1, idx - col)
                        return true
                    }
                    KeyEvent.KEYCODE_MENU -> {
                        apps.getOrNull(page * PAGE + idx)?.let { showMenu(it, focused) }
                        return true
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun showMenu(entry: AppEntry, anchor: View) {
        val installed = apps.mapTo(HashSet()) { it.pkg }
        val dock = prefs.dock
        val dockEntry = when {
            entry.pkg in dock.visible(installed) -> MenuEntry("Remover do Dock") { prefs.dock = dock.remove(entry.pkg) }
            dock.isFull(installed) -> MenuEntry("Dock cheio", enabled = false)
            else -> MenuEntry("Adicionar ao Dock") { prefs.dock = dock.add(entry.pkg, installed) }
        }
        val details = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", entry.pkg, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val menu = MenuPanel(this, entry.label, listOf(
            dockEntry,
            MenuEntry("Abrir") { open(entry) },
            MenuEntry("Informações do app") { repo.start(details) },
        )) { panels.close() }
        panels.show(menu, PanelHost.centered(), anchor, listOf(grid))
    }

    private fun open(entry: AppEntry) {
        prefs.newApps = NewAppsTracker.markOpened(prefs.newApps, entry.pkg)
        repo.launch(entry)
    }

    companion object {
        const val COLS = 6
        const val ROWS = 3
        const val PAGE = COLS * ROWS
    }
}
```

- [ ] **Step 2: Compilar**

Run: `cd ~/pessoal/projetor-bettdow/launcher && ./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`. (O teste no aparelho desta tela acontece na Task 14, quando a Home passa a abri-la; ela não é exportada, então `am start` direto não funciona.)

- [ ] **Step 3: Commit**

```bash
cd ~/pessoal/projetor-bettdow && grep -rn $'\u2014' launcher/app/src; \
git add launcher && git commit -m "LAUNCHER | FEAT | Tela Todos os apps com páginas, ponto azul e menu"
```

---

### Task 11: Tela "Organizar Dock"

**Files:**
- Create: `launcher/app/src/main/java/br/gui/inicio/settings/DockEditorActivity.kt`

**Interfaces:**
- Consumes: `DockConfig` (Task 1); `Theme`, `Glyph`, `GlyphView`, `Backdrop` (Tasks 6 e 8); `Prefs`, `AppRepository`, `AppEntry` (Task 7).
- Produces: `class DockEditorActivity : Activity` (aberta pelo menu "Início" na Task 14).

- [ ] **Step 1: Escrever `DockEditorActivity.kt`**

```kotlin
package br.gui.inicio.settings

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import br.gui.inicio.core.DockConfig
import br.gui.inicio.data.AppEntry
import br.gui.inicio.data.AppRepository
import br.gui.inicio.data.Prefs
import br.gui.inicio.ui.Backdrop
import br.gui.inicio.ui.Glyph
import br.gui.inicio.ui.GlyphView
import br.gui.inicio.ui.Theme
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/** Lista de apps para montar o Dock: OK coloca ou tira, ← → mudam a posição. Play Store é atalho fixo e não aparece. */
class DockEditorActivity : Activity() {
    private class Row(val pkg: String, val position: TextView, val mark: FrameLayout, val check: GlyphView)

    private lateinit var prefs: Prefs
    private lateinit var repo: AppRepository
    private lateinit var list: LinearLayout
    private val io: ExecutorService = Executors.newSingleThreadExecutor()
    private val rows = mutableListOf<Row>()
    private var installed: Set<String> = emptySet()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Theme.init(this)
        prefs = Prefs(this)
        repo = AppRepository(this)
        val wrap = LinearLayout.LayoutParams.WRAP_CONTENT
        val match = LinearLayout.LayoutParams.MATCH_PARENT
        val root = FrameLayout(this)
        Backdrop.attach(this, root, prefs, io)
        val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        column.addView(Theme.text(this, 40f, 700, "Organizar Dock"))
        column.addView(
            Theme.text(this, 20f, 400, "OK coloca ou tira do Dock. ← → mudam a posição. Até ${DockConfig.MAX} apps.").apply { alpha = 0.75f },
            LinearLayout.LayoutParams(wrap, wrap).apply {
                topMargin = Theme.si(10f)
                bottomMargin = Theme.si(28f)
            },
        )
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            clipToPadding = false
            setPadding(0, Theme.si(8f), 0, Theme.si(80f))
            addView(list)
        }
        column.addView(scroll, LinearLayout.LayoutParams(match, 0, 1f))
        root.addView(column, FrameLayout.LayoutParams(Theme.si(900f), FrameLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER_HORIZONTAL).apply { topMargin = Theme.si(70f) })
        setContentView(root)
    }

    override fun onStart() {
        super.onStart()
        io.execute {
            val all = repo.launchable()
            val shown = all.filter { it.pkg != AppRepository.PLAY_STORE }
            val icons = shown.map { repo.iconBitmap(it, Theme.si(72f)) }
            runOnUiThread {
                if (isDestroyed) return@runOnUiThread
                installed = all.mapTo(HashSet()) { it.pkg }
                build(shown, icons)
            }
        }
    }

    override fun onDestroy() {
        io.shutdownNow()
        super.onDestroy()
    }

    private fun build(apps: List<AppEntry>, icons: List<Bitmap>) {
        list.removeAllViews()
        rows.clear()
        apps.forEachIndexed { i, entry -> list.addView(row(entry, icons[i])) }
        refresh()
        list.getChildAt(0)?.requestFocus()
    }

    private fun row(entry: AppEntry, icon: Bitmap): View {
        val wrap = LinearLayout.LayoutParams.WRAP_CONTENT
        val position = Theme.text(this, 22f, 600).apply {
            gravity = Gravity.END
            minWidth = Theme.si(40f)
        }
        val check = GlyphView(this, Glyph.CHECK)
        val mark = FrameLayout(this).apply { addView(check, FrameLayout.LayoutParams(Theme.si(26f), Theme.si(26f), Gravity.CENTER)) }
        rows += Row(entry.pkg, position, mark, check)
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            isFocusable = true
            setPadding(Theme.si(16f), Theme.si(10f), Theme.si(20f), Theme.si(10f))
            addView(ImageView(context).apply { setImageBitmap(icon) }, LinearLayout.LayoutParams(Theme.si(72f), Theme.si(72f)))
            addView(Theme.text(context, 24f, 500, entry.label), LinearLayout.LayoutParams(0, wrap, 1f).apply { leftMargin = Theme.si(20f) })
            addView(position, LinearLayout.LayoutParams(wrap, wrap).apply { rightMargin = Theme.si(18f) })
            addView(mark, LinearLayout.LayoutParams(Theme.si(40f), Theme.si(40f)))
            setOnFocusChangeListener { v, has -> v.background = if (has) Theme.rounded(Theme.FOCUS_FILL, 18f) else null }
            setOnClickListener { toggle(entry.pkg) }
            setOnKeyListener { _, code, ev ->
                if (ev.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false
                when (code) {
                    KeyEvent.KEYCODE_DPAD_LEFT -> { move(entry.pkg, -1); true }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> { move(entry.pkg, 1); true }
                    else -> false
                }
            }
        }
    }

    private fun toggle(pkg: String) {
        val dock = prefs.dock
        prefs.dock = when {
            dock.contains(pkg) -> dock.remove(pkg)
            dock.isFull(installed) -> {
                Toast.makeText(this, "Dock cheio: tire um app antes de colocar outro", Toast.LENGTH_SHORT).show()
                dock
            }
            else -> dock.add(pkg, installed)
        }
        refresh()
    }

    private fun move(pkg: String, delta: Int) {
        val dock = prefs.dock
        if (!dock.contains(pkg)) return
        prefs.dock = dock.move(pkg, delta)
        refresh()
    }

    private fun refresh() {
        val order = prefs.dock.visible(installed)
        rows.forEach { r ->
            val pos = order.indexOf(r.pkg)
            r.position.text = if (pos >= 0) "${pos + 1}" else ""
            r.mark.background = if (pos >= 0) {
                Theme.oval(Theme.SELECTION)
            } else {
                GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setStroke(Theme.si(2f), 0x80FFFFFF.toInt())
                }
            }
            r.check.visibility = if (pos >= 0) View.VISIBLE else View.INVISIBLE
        }
    }
}
```

- [ ] **Step 2: Compilar**

Run: `cd ~/pessoal/projetor-bettdow/launcher && ./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
cd ~/pessoal/projetor-bettdow && grep -rn $'\u2014' launcher/app/src; \
git add launcher && git commit -m "LAUNCHER | FEAT | Tela Organizar Dock"
```

---

### Task 12: Tela "Papel de parede" e envio de fotos pelo computador

**Files:**
- Create: `launcher/app/src/main/java/br/gui/inicio/settings/WallpaperActivity.kt`
- Create: `scripts/enviar-wallpaper.sh`

**Interfaces:**
- Consumes: `WallpaperChoice` (Task 2); `Theme`, `Backdrop` (Tasks 6 e 8); `Gradients`, `WallpaperLoader` (Task 8); `Prefs` (Task 7).
- Produces: `class WallpaperActivity : Activity` (aberta pelo menu "Início" na Task 14); script `scripts/enviar-wallpaper.sh <foto> [IP:PORTA]` que grava em `/sdcard/Android/data/br.gui.inicio/files/wallpapers/`.

- [ ] **Step 1: Escrever `WallpaperActivity.kt`**

```kotlin
package br.gui.inicio.settings

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import br.gui.inicio.core.WallpaperChoice
import br.gui.inicio.data.Prefs
import br.gui.inicio.ui.Backdrop
import br.gui.inicio.ui.Theme
import br.gui.inicio.wallpaper.Gradients
import br.gui.inicio.wallpaper.WallpaperLoader
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/** Escolha do fundo: 4 degradês desenhados, fotos incluídas e fotos enviadas pelo computador. */
class WallpaperActivity : Activity() {
    private lateinit var prefs: Prefs
    private lateinit var loader: WallpaperLoader
    private lateinit var gradientRow: LinearLayout
    private lateinit var photoRow: LinearLayout
    private val io: ExecutorService = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Theme.init(this)
        prefs = Prefs(this)
        loader = WallpaperLoader(this)
        val wrap = LinearLayout.LayoutParams.WRAP_CONTENT
        val root = FrameLayout(this)
        Backdrop.attach(this, root, prefs, io)
        val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        column.addView(Theme.text(this, 40f, 700, "Papel de parede"), LinearLayout.LayoutParams(wrap, wrap).apply { bottomMargin = Theme.si(30f) })
        column.addView(section("DEGRADÊS"))
        gradientRow = row()
        column.addView(gradientRow)
        column.addView(section("FOTOS"), LinearLayout.LayoutParams(wrap, wrap).apply { topMargin = Theme.si(30f) })
        photoRow = row()
        column.addView(HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            clipToPadding = false
            addView(photoRow)
        })
        column.addView(
            Theme.text(this, 18f, 400, "Para usar uma foto sua, envie pelo computador: scripts/enviar-wallpaper.sh foto.jpg").apply { alpha = 0.7f },
            LinearLayout.LayoutParams(wrap, wrap).apply { topMargin = Theme.si(24f) },
        )
        root.addView(column, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        setContentView(root)
    }

    override fun onStart() {
        super.onStart()
        val tw = Theme.si(320f)
        val th = Theme.si(180f)
        io.execute {
            val gradients = (0 until Gradients.COUNT).map { WallpaperChoice.Gradient(it) to Gradients.render(it, tw, th) }
            val choices = loader.assetNames().map { WallpaperChoice.Asset(it) } + loader.userFiles().map { WallpaperChoice.UserFile(it) }
            val photos = choices.mapNotNull { c -> loader.render(c, tw, th)?.let { c to it } }
            runOnUiThread {
                if (isDestroyed) return@runOnUiThread
                fill(gradientRow, gradients)
                fill(photoRow, photos)
                focusSelected()
            }
        }
    }

    override fun onDestroy() {
        io.shutdownNow()
        super.onDestroy()
    }

    private fun section(title: String) = Theme.text(this, 18f, 600, title).apply { alpha = 0.7f; setPadding(0, 0, 0, Theme.si(12f)) }

    private fun row() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        clipChildren = false
        clipToPadding = false
        setPadding(Theme.si(10f), Theme.si(10f), Theme.si(10f), Theme.si(10f))
    }

    private fun fill(target: LinearLayout, items: List<Pair<WallpaperChoice, Bitmap>>) {
        target.removeAllViews()
        items.forEach { (choice, bmp) -> target.addView(thumb(choice, bmp)) }
    }

    private fun thumb(choice: WallpaperChoice, bmp: Bitmap): View {
        val tw = Theme.si(320f)
        val th = Theme.si(180f)
        val radius = Theme.s(16f)
        fun ring(color: Int) = GradientDrawable().apply {
            cornerRadius = radius
            setStroke(Theme.si(4f), color)
        }
        val selected = choice == prefs.wallpaper
        val image = ImageView(this).apply {
            setImageBitmap(bmp)
            scaleType = ImageView.ScaleType.CENTER_CROP
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) = outline.setRoundRect(0, 0, view.width, view.height, radius)
            }
            clipToOutline = true
        }
        return FrameLayout(this).apply {
            tag = choice
            isFocusable = true
            addView(image, FrameLayout.LayoutParams(tw, th))
            foreground = if (selected) ring(Theme.SELECTION) else null
            layoutParams = LinearLayout.LayoutParams(tw, th).apply { rightMargin = Theme.si(24f) }
            setOnFocusChangeListener { v, has ->
                v.animate().scaleX(if (has) 1.08f else 1f).scaleY(if (has) 1.08f else 1f).setDuration(150).start()
                (v as FrameLayout).foreground = when {
                    has -> ring(Color.WHITE)
                    selected -> ring(Theme.SELECTION)
                    else -> null
                }
            }
            setOnClickListener {
                prefs.wallpaper = choice
                finish()
            }
        }
    }

    private fun focusSelected() {
        val current = prefs.wallpaper
        val all = (0 until gradientRow.childCount).map { gradientRow.getChildAt(it) } +
            (0 until photoRow.childCount).map { photoRow.getChildAt(it) }
        (all.firstOrNull { it.tag == current } ?: all.firstOrNull())?.requestFocus()
    }
}
```

- [ ] **Step 2: Escrever `scripts/enviar-wallpaper.sh`**

```bash
#!/usr/bin/env bash
# Envia uma foto para o launcher Início, já recortada e reduzida para a tela do projetor (1920x1080).
# Uso: ./enviar-wallpaper.sh foto.jpg [IP:PORTA]
set -euo pipefail
FOTO="${1:?Uso: $0 foto.jpg [IP:PORTA]}"
DEV="${2:-192.168.2.100:5555}"
PASTA="/sdcard/Android/data/br.gui.inicio/files/wallpapers"

# Nome seguro: só letras, números, ponto, hífen e sublinhado (o launcher ignora outros nomes).
BASE="$(basename "${FOTO%.*}")"
NOME="$(printf '%s' "$BASE" | tr -c 'A-Za-z0-9._-' '_' | sed 's/^[^A-Za-z0-9]*//' | cut -c1-60)"
[ -n "$NOME" ] || NOME="foto"

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
SAIDA="$TMP/${NOME}.jpg"
convert "$FOTO" -auto-orient -resize '1920x1080^' -gravity center -extent 1920x1080 -strip -quality 85 "$SAIDA"

adb connect "$DEV" >/dev/null
adb -s "$DEV" shell mkdir -p "$PASTA"
adb -s "$DEV" push "$SAIDA" "$PASTA/${NOME}.jpg"
echo "Enviada: ${NOME}.jpg. No projetor: Início > Papel de parede > Fotos."
```

```bash
chmod +x ~/pessoal/projetor-bettdow/scripts/enviar-wallpaper.sh
```

- [ ] **Step 3: Conferir a sintaxe do script e o recorte local (o envio real é testado na Task 14, com o app já instalado, para a pasta do app ser criada pelo próprio Android com o dono certo)**

```bash
bash -n ~/pessoal/projetor-bettdow/scripts/enviar-wallpaper.sh && echo "sintaxe ok" && \
T=$(mktemp -d) && convert -size 3000x2000 gradient:'#335599-#aa3366' "$T/in.png" && \
convert "$T/in.png" -auto-orient -resize '1920x1080^' -gravity center -extent 1920x1080 -strip -quality 85 "$T/out.jpg" && identify "$T/out.jpg"
```
Expected: `sintaxe ok` e `out.jpg JPEG 1920x1080`.

- [ ] **Step 4: Compilar**

Run: `cd ~/pessoal/projetor-bettdow/launcher && ./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
cd ~/pessoal/projetor-bettdow && grep -rn $'\u2014' launcher/app/src scripts; \
git add launcher scripts/enviar-wallpaper.sh && git commit -m "LAUNCHER | FEAT | Tela de papel de parede e envio de fotos pelo computador"
```

---

### Task 13: Central de Controle e calendário

**Files:**
- Create: `launcher/app/src/main/java/br/gui/inicio/ui/ControlCenterPanel.kt`
- Create: `launcher/app/src/main/java/br/gui/inicio/ui/CalendarPanel.kt`

**Interfaces:**
- Consumes: `CalendarMath` (Task 3); `Theme`, `GlassDrawable`, `Glyph`, `GlyphView` (Task 6); `SystemShortcut` (Task 7).
- Produces: `class ControlCenterPanel(ctx, items: List<SystemShortcut>, onPick: (SystemShortcut) -> Unit) : GridLayout`; `class CalendarPanel(ctx, today: LocalDate, onAdjust: () -> Unit) : LinearLayout`.

- [ ] **Step 1: Escrever `ControlCenterPanel.kt`**

```kotlin
package br.gui.inicio.ui

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.LinearLayout
import br.gui.inicio.data.SystemShortcut

/** Central de Controle: grade 2 x 4 de blocos que abrem as telas oficiais do projetor. */
class ControlCenterPanel(ctx: Context, items: List<SystemShortcut>, onPick: (SystemShortcut) -> Unit) : GridLayout(ctx) {
    init {
        columnCount = 2
        background = GlassDrawable(this, Theme.s(32f))
        val pad = Theme.si(20f)
        setPadding(pad, pad, pad, pad)
        items.forEach { addView(tile(ctx, it, onPick)) }
    }

    private fun tile(ctx: Context, s: SystemShortcut, onPick: (SystemShortcut) -> Unit): View {
        val wrap = LinearLayout.LayoutParams.WRAP_CONTENT
        val idle = 0x38FFFFFF
        val glyph = GlyphView(ctx, s.glyph)
        val circle = FrameLayout(ctx).apply {
            background = Theme.oval(idle)
            addView(glyph, FrameLayout.LayoutParams(Theme.si(30f), Theme.si(30f), Gravity.CENTER))
        }
        val title = Theme.text(ctx, 20f, 500, s.title)
        val subtitle = Theme.text(ctx, 15f, 400, s.subtitle).apply { alpha = 0.7f }
        val texts = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            addView(title)
            addView(subtitle, LinearLayout.LayoutParams(wrap, wrap).apply { topMargin = Theme.si(4f) })
        }
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            isFocusable = true
            setPadding(Theme.si(18f), Theme.si(16f), Theme.si(18f), Theme.si(16f))
            background = Theme.rounded(Theme.TILE_FILL, 22f)
            addView(circle, LinearLayout.LayoutParams(Theme.si(52f), Theme.si(52f)))
            addView(texts, LinearLayout.LayoutParams(wrap, wrap).apply { leftMargin = Theme.si(16f) })
            layoutParams = GridLayout.LayoutParams().apply {
                width = Theme.si(290f)
                setMargins(Theme.si(8f), Theme.si(8f), Theme.si(8f), Theme.si(8f))
            }
            setOnFocusChangeListener { _, has ->
                background = Theme.rounded(if (has) Color.WHITE else Theme.TILE_FILL, 22f)
                val textColor = if (has) 0xFF111111.toInt() else Color.WHITE
                listOf(title, subtitle).forEach {
                    it.setTextColor(textColor)
                    if (has) it.setShadowLayer(0f, 0f, 0f, 0) else it.setShadowLayer(Theme.s(6f), 0f, Theme.s(1f), Theme.TEXT_SHADOW)
                }
                circle.background = Theme.oval(if (has) Theme.SELECTION else idle)
            }
            setOnClickListener { onPick(s) }
        }
    }
}
```

- [ ] **Step 2: Escrever `CalendarPanel.kt`**

```kotlin
package br.gui.inicio.ui

import android.content.Context
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import br.gui.inicio.core.CalendarMath
import java.time.LocalDate
import java.time.YearMonth

/** Calendário do mês, como o do macOS: hoje em vermelho, ← → trocam de mês, "Ajustar data e hora…" no fim. */
class CalendarPanel(ctx: Context, private val today: LocalDate, onAdjust: () -> Unit) : LinearLayout(ctx) {
    private var month: YearMonth = YearMonth.from(today)
    private val title = Theme.text(ctx, 24f, 700).apply { gravity = Gravity.CENTER }
    private val grid = GridLayout(ctx).apply { columnCount = 7 }

    init {
        orientation = VERTICAL
        background = GlassDrawable(this, Theme.s(32f))
        val pad = Theme.si(24f)
        setPadding(pad, pad, pad, pad)
        val wrap = LayoutParams.WRAP_CONTENT
        val match = LayoutParams.MATCH_PARENT

        val header = LinearLayout(ctx).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            isFocusable = true
            setPadding(Theme.si(12f), Theme.si(8f), Theme.si(12f), Theme.si(8f))
            addView(Theme.text(ctx, 26f, 600, "‹"))
            addView(title, LayoutParams(0, wrap, 1f))
            addView(Theme.text(ctx, 26f, 600, "›"))
            setOnFocusChangeListener { v, has -> v.background = if (has) Theme.rounded(Theme.FOCUS_FILL, 12f) else null }
            setOnKeyListener { _, code, ev ->
                if (ev.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener code == KeyEvent.KEYCODE_DPAD_LEFT || code == KeyEvent.KEYCODE_DPAD_RIGHT
                when (code) {
                    KeyEvent.KEYCODE_DPAD_LEFT -> { shift(-1); true }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> { shift(1); true }
                    KeyEvent.KEYCODE_DPAD_UP -> true
                    else -> false
                }
            }
        }
        addView(header, LayoutParams(match, wrap))
        addView(grid, LayoutParams(wrap, wrap).apply {
            topMargin = Theme.si(12f)
            gravity = Gravity.CENTER_HORIZONTAL
        })
        addView(View(ctx).apply { setBackgroundColor(0x33FFFFFF) }, LayoutParams(match, maxOf(1, Theme.si(1.5f))).apply {
            topMargin = Theme.si(12f)
            bottomMargin = Theme.si(8f)
        })
        addView(Theme.text(ctx, 20f, 500, "Ajustar data e hora…").apply {
            isFocusable = true
            setPadding(Theme.si(12f), Theme.si(10f), Theme.si(12f), Theme.si(10f))
            setOnFocusChangeListener { v, has -> v.background = if (has) Theme.rounded(Theme.SELECTION, 12f) else null }
            setOnClickListener { onAdjust() }
            setOnKeyListener { _, code, _ -> code == KeyEvent.KEYCODE_DPAD_DOWN || code == KeyEvent.KEYCODE_DPAD_LEFT || code == KeyEvent.KEYCODE_DPAD_RIGHT }
        }, LayoutParams(match, wrap))
        render()
    }

    private fun shift(delta: Long) {
        month = month.plusMonths(delta)
        render()
    }

    private fun render() {
        title.text = CalendarMath.title(month)
        grid.removeAllViews()
        listOf("D", "S", "T", "Q", "Q", "S", "S").forEach { grid.addView(cell(it, 600, 0.6f, null)) }
        CalendarMath.grid(month, today).forEach { d ->
            grid.addView(cell(d.date.dayOfMonth.toString(), if (d.isToday) 700 else 400, if (d.inMonth) 1f else 0.35f, if (d.isToday) Theme.TODAY else null))
        }
    }

    private fun cell(text: String, weight: Int, alpha: Float, circle: Int?): TextView =
        Theme.text(context, 18f, weight, text).apply {
            gravity = Gravity.CENTER
            this.alpha = alpha
            if (circle != null) background = Theme.oval(circle)
            layoutParams = GridLayout.LayoutParams().apply {
                width = Theme.si(44f)
                height = Theme.si(44f)
                setMargins(Theme.si(2f), Theme.si(2f), Theme.si(2f), Theme.si(2f))
            }
        }
}
```

- [ ] **Step 3: Compilar**

Run: `cd ~/pessoal/projetor-bettdow/launcher && ./gradlew :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
cd ~/pessoal/projetor-bettdow && grep -rn $'\u2014' launcher/app/src; \
git add launcher && git commit -m "LAUNCHER | FEAT | Central de Controle e calendário"
```

### Task 14: Tela inicial (relógio, barra de menu, Dock) e primeiro teste no projetor

**Files:**
- Create: `launcher/app/src/main/java/br/gui/inicio/home/ClockView.kt`
- Create: `launcher/app/src/main/java/br/gui/inicio/home/DockView.kt`
- Create: `launcher/app/src/main/java/br/gui/inicio/home/MenuBarView.kt`
- Create: `launcher/app/src/main/java/br/gui/inicio/home/StatusMonitor.kt`
- Create: `launcher/app/src/main/java/br/gui/inicio/home/HomeActivity.kt`

**Interfaces:**
- Consumes: tudo das Tasks 1 a 13. Em especial: `Prefs.dock/.wallpaper/.newApps`, `AppRepository.launchable/iconBitmap/launch/PLAY_STORE`, `ShortcutResolver.isAvailable/available/open`, `Shortcuts.*`, `WallpaperLoader.load`, `GlassSource.blurred`, `invalidateTree`, `PanelHost.show/close/below`, `MenuPanel`, `MenuEntry`, `ControlCenterPanel`, `CalendarPanel`, `IconFactory.glyph`, `AllAppsActivity`, `DockEditorActivity`, `WallpaperActivity`.
- Produces: `class HomeActivity` (a tela `HOME`); `data class DockItem(val key: String, val label: String, val icon: Bitmap, val groupStart: Boolean = false, val onOpen: () -> Unit)`; `enum class BarItem { START, BLUETOOTH, WIFI, CONTROL, CLOCK }`.

- [ ] **Step 1: Escrever `ClockView.kt`**

```kotlin
package br.gui.inicio.home

import android.content.Context
import android.view.Gravity
import android.widget.LinearLayout
import br.gui.inicio.core.ClockFormat
import br.gui.inicio.ui.Theme
import java.time.LocalDateTime

/** Data por extenso e hora grande, centralizadas, como a tela de bloqueio do Mac. */
class ClockView(ctx: Context) : LinearLayout(ctx) {
    private val date = Theme.text(ctx, 34f, 600).apply { alpha = 0.9f }
    private val time = Theme.text(ctx, 220f, 700).apply {
        alpha = 0.92f
        letterSpacing = -0.04f
    }

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        addView(date)
        addView(time, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply { topMargin = Theme.si(8f) })
    }

    fun update(now: LocalDateTime) {
        date.text = ClockFormat.longDate(now)
        time.text = ClockFormat.time(now)
    }
}
```

- [ ] **Step 2: Escrever `DockView.kt`**

```kotlin
package br.gui.inicio.home

import android.content.Context
import android.graphics.Bitmap
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import br.gui.inicio.ui.GlassDrawable
import br.gui.inicio.ui.Theme

/** Item do Dock: app favorito, atalho fixo ou "Todos os apps". [groupStart] desenha uma divisória antes. */
data class DockItem(
    val key: String,
    val label: String,
    val icon: Bitmap,
    val groupStart: Boolean = false,
    val onOpen: () -> Unit,
)

/** Dock de vidro: o ícone em foco cresce 35% e sobe; a Home mostra o nome num balão. */
class DockView(ctx: Context) : LinearLayout(ctx) {
    var onUp: () -> Unit = {}
    var onFocusItem: (View, DockItem) -> Unit = { _, _ -> }
    private var icons: List<View> = emptyList()
    private var lastFocused: View? = null

    init {
        orientation = HORIZONTAL
        gravity = Gravity.BOTTOM
        clipChildren = false
        clipToPadding = false
        background = GlassDrawable(this, Theme.s(40f))
        setPadding(Theme.si(22f), Theme.si(18f), Theme.si(22f), Theme.si(18f))
    }

    fun setItems(items: List<DockItem>) {
        val keepKey = lastFocused?.tag as? String
        removeAllViews()
        val size = Theme.si(112f)
        val gap = Theme.si(10f)
        val views = mutableListOf<View>()
        items.forEach { item ->
            if (item.groupStart && views.isNotEmpty()) {
                addView(View(context).apply { setBackgroundColor(0x4DFFFFFF) }, LayoutParams(maxOf(1, Theme.si(2f)), Theme.si(96f)).apply {
                    setMargins(Theme.si(12f), 0, Theme.si(12f), Theme.si(8f))
                })
            }
            val v = ImageView(context).apply {
                id = View.generateViewId()
                tag = item.key
                setImageBitmap(item.icon)
                contentDescription = item.label
                isFocusable = true
                setOnFocusChangeListener { view, has ->
                    view.pivotX = view.width / 2f
                    view.pivotY = view.height.toFloat()
                    view.animate()
                        .scaleX(if (has) 1.35f else 1f)
                        .scaleY(if (has) 1.35f else 1f)
                        .translationY(if (has) -Theme.s(16f) else 0f)
                        .setDuration(150)
                        .start()
                    if (has) {
                        lastFocused = view
                        onFocusItem(view, item)
                    }
                }
                setOnClickListener { item.onOpen() }
                setOnKeyListener { _, code, ev ->
                    if (code == KeyEvent.KEYCODE_DPAD_UP && ev.action == KeyEvent.ACTION_DOWN) {
                        onUp()
                        true
                    } else {
                        code == KeyEvent.KEYCODE_DPAD_DOWN || code == KeyEvent.KEYCODE_DPAD_UP
                    }
                }
            }
            addView(v, LayoutParams(size, size).apply { setMargins(gap, 0, gap, 0) })
            views += v
        }
        views.firstOrNull()?.let { it.nextFocusLeftId = it.id }
        views.lastOrNull()?.let { it.nextFocusRightId = it.id }
        icons = views
        lastFocused = views.firstOrNull { it.tag == keepKey }
    }

    fun focusFirst() {
        icons.firstOrNull()?.requestFocus()
    }

    fun focusLast() {
        (lastFocused ?: icons.firstOrNull())?.requestFocus()
    }

    fun hasFocusedItem(): Boolean = icons.any { it.isFocused }
}
```

- [ ] **Step 3: Escrever `MenuBarView.kt`**

```kotlin
package br.gui.inicio.home

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import br.gui.inicio.ui.GlassDrawable
import br.gui.inicio.ui.Glyph
import br.gui.inicio.ui.GlyphView
import br.gui.inicio.ui.Theme

enum class BarItem { START, BLUETOOTH, WIFI, CONTROL, CLOCK }

/** Barra de menu de vidro. Todos os itens recebem foco; ↓ volta ao Dock. Desligado = ícone esmaecido. */
class MenuBarView(ctx: Context, private val onItem: (BarItem, View) -> Unit) : LinearLayout(ctx) {
    var onDown: () -> Unit = {}
    private val items = LinkedHashMap<BarItem, View>()
    private val bluetooth = GlyphView(ctx, Glyph.BLUETOOTH)
    private val wifi = GlyphView(ctx, Glyph.WIFI)
    private val clock = Theme.text(ctx, 20f, 500).apply { gravity = Gravity.CENTER_VERTICAL }

    val startItem: View get() = items.getValue(BarItem.START)

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        background = GlassDrawable(this, 0f)
        setPadding(Theme.si(16f), 0, Theme.si(16f), 0)
        val wrap = LayoutParams.WRAP_CONTENT

        add(BarItem.START, Theme.text(ctx, 20f, 700, "Início").apply { gravity = Gravity.CENTER_VERTICAL }, wrap)
        addView(View(ctx), LayoutParams(0, 1, 1f))
        add(BarItem.BLUETOOTH, glyphBox(bluetooth), Theme.si(56f))
        add(BarItem.WIFI, glyphBox(wifi), Theme.si(56f))
        val control = LinearLayout(ctx).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(GlyphView(ctx, Glyph.CONTROLS), LayoutParams(Theme.si(26f), Theme.si(26f)))
            addView(Theme.text(ctx, 20f, 500, "Controle"), LayoutParams(wrap, wrap).apply { leftMargin = Theme.si(8f) })
        }
        add(BarItem.CONTROL, control, wrap)
        add(BarItem.CLOCK, clock, wrap)

        val all = items.values.toList()
        all.first().let { it.nextFocusLeftId = it.id }
        all.last().let { it.nextFocusRightId = it.id }
    }

    private fun glyphBox(g: GlyphView) = FrameLayout(context).apply {
        addView(g, FrameLayout.LayoutParams(Theme.si(26f), Theme.si(26f), Gravity.CENTER))
    }

    private fun add(item: BarItem, v: View, width: Int) {
        v.id = View.generateViewId()
        v.isFocusable = true
        v.setPadding(Theme.si(14f), 0, Theme.si(14f), 0)
        v.setOnFocusChangeListener { view, has -> view.background = if (has) Theme.rounded(Theme.FOCUS_FILL, 10f) else null }
        v.setOnClickListener { onItem(item, v) }
        v.setOnKeyListener { _, code, ev ->
            when (code) {
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    if (ev.action == KeyEvent.ACTION_DOWN) onDown()
                    true
                }
                KeyEvent.KEYCODE_DPAD_UP -> true
                else -> false
            }
        }
        items[item] = v
        addView(v, LayoutParams(width, Theme.si(42f)).apply { leftMargin = Theme.si(6f) })
    }

    fun setTime(text: String) {
        clock.text = text
    }

    /** [bluetoothOn] null = estado ilegível sem permissão: o item some. */
    fun setStatus(wifiConnected: Boolean, bluetoothOn: Boolean?) {
        wifi.color = if (wifiConnected) Color.WHITE else 0x80FFFFFF.toInt()
        items.getValue(BarItem.BLUETOOTH).visibility = if (bluetoothOn == null) View.GONE else View.VISIBLE
        bluetooth.color = if (bluetoothOn == true) Color.WHITE else 0x80FFFFFF.toInt()
    }

    fun focusFirst() {
        startItem.requestFocus()
    }
}
```

- [ ] **Step 4: Escrever `StatusMonitor.kt`**

```kotlin
package br.gui.inicio.home

import android.bluetooth.BluetoothManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.os.Handler
import android.os.Looper

/** Avisa mudanças de rede e lê o Bluetooth, só entre [start] e [stop] (tela visível). */
class StatusMonitor(private val ctx: Context, private val onChange: (wifi: Boolean, bluetooth: Boolean?) -> Unit) {
    private val cm = ctx.getSystemService(ConnectivityManager::class.java)
    private val main = Handler(Looper.getMainLooper())
    private var connected = false
    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            post(true)
        }

        override fun onLost(network: Network) {
            post(false)
        }
    }

    fun start() {
        connected = cm.activeNetwork != null
        emit()
        cm.registerDefaultNetworkCallback(callback)
    }

    fun stop() {
        runCatching { cm.unregisterNetworkCallback(callback) }
    }

    private fun post(value: Boolean) {
        main.post {
            connected = value
            emit()
        }
    }

    private fun emit() = onChange(connected, bluetoothEnabled())

    /** No Android 12 ler o estado pode exigir BLUETOOTH_CONNECT; sem ela, null esconde o ícone. */
    private fun bluetoothEnabled(): Boolean? = try {
        ctx.getSystemService(BluetoothManager::class.java)?.adapter?.isEnabled
    } catch (e: SecurityException) {
        null
    }
}
```

- [ ] **Step 5: Escrever `HomeActivity.kt`**

```kotlin
package br.gui.inicio.home

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import br.gui.inicio.apps.AllAppsActivity
import br.gui.inicio.core.ClockFormat
import br.gui.inicio.core.NewAppsTracker
import br.gui.inicio.core.WallpaperChoice
import br.gui.inicio.data.AppEntry
import br.gui.inicio.data.AppRepository
import br.gui.inicio.data.Prefs
import br.gui.inicio.data.ShortcutResolver
import br.gui.inicio.data.Shortcuts
import br.gui.inicio.settings.DockEditorActivity
import br.gui.inicio.settings.WallpaperActivity
import br.gui.inicio.ui.CalendarPanel
import br.gui.inicio.ui.ControlCenterPanel
import br.gui.inicio.ui.GlassDrawable
import br.gui.inicio.ui.GlassSource
import br.gui.inicio.ui.Glyph
import br.gui.inicio.ui.IconFactory
import br.gui.inicio.ui.MenuEntry
import br.gui.inicio.ui.MenuPanel
import br.gui.inicio.ui.PanelHost
import br.gui.inicio.ui.Theme
import br.gui.inicio.ui.invalidateTree
import br.gui.inicio.wallpaper.WallpaperLoader
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/** Tela inicial estilo macOS. Nada roda com ela oculta: receptores ligados em onStart e desligados em onStop. */
class HomeActivity : Activity() {
    private lateinit var prefs: Prefs
    private lateinit var repo: AppRepository
    private lateinit var shortcuts: ShortcutResolver
    private lateinit var loader: WallpaperLoader
    private lateinit var root: FrameLayout
    private lateinit var wallpaperView: ImageView
    private lateinit var clock: ClockView
    private lateinit var menuBar: MenuBarView
    private lateinit var dock: DockView
    private lateinit var tooltip: TextView
    private lateinit var panels: PanelHost
    private lateinit var status: StatusMonitor
    private val io: ExecutorService = Executors.newSingleThreadExecutor()
    private var loadedChoice: WallpaperChoice? = null

    private val timeReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) = tick()
    }
    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) = refreshApps()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Theme.init(this)
        prefs = Prefs(this)
        repo = AppRepository(this)
        shortcuts = ShortcutResolver(this)
        loader = WallpaperLoader(this)
        val match = FrameLayout.LayoutParams.MATCH_PARENT
        val wrap = FrameLayout.LayoutParams.WRAP_CONTENT

        root = FrameLayout(this).apply {
            clipChildren = false
            setBackgroundColor(Color.BLACK)
        }
        wallpaperView = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
        root.addView(wallpaperView, FrameLayout.LayoutParams(match, match))
        clock = ClockView(this)
        root.addView(clock, FrameLayout.LayoutParams(match, wrap, Gravity.TOP).apply { topMargin = Theme.si(170f) })
        menuBar = MenuBarView(this, ::onBarItem)
        root.addView(menuBar, FrameLayout.LayoutParams(match, Theme.si(56f), Gravity.TOP))
        dock = DockView(this)
        root.addView(dock, FrameLayout.LayoutParams(wrap, wrap, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply { bottomMargin = Theme.si(46f) })
        tooltip = Theme.text(this, 22f, 500).apply {
            visibility = View.INVISIBLE
            setPadding(Theme.si(18f), Theme.si(8f), Theme.si(18f), Theme.si(8f))
        }
        tooltip.background = GlassDrawable(tooltip, Theme.s(14f))
        root.addView(tooltip, FrameLayout.LayoutParams(wrap, wrap, Gravity.TOP or Gravity.START))
        panels = PanelHost(this)
        root.addView(panels, FrameLayout.LayoutParams(match, match))
        setContentView(root)

        status = StatusMonitor(this) { wifi, bt -> menuBar.setStatus(wifi, bt) }
        dock.onUp = { menuBar.focusFirst() }
        dock.onFocusItem = ::showTooltip
        menuBar.onDown = { dock.focusLast() }
        root.viewTreeObserver.addOnGlobalFocusChangeListener { _, now ->
            if (now == null || now.parent !== dock) tooltip.visibility = View.INVISIBLE
        }
    }

    override fun onStart() {
        super.onStart()
        val time = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        val packages = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        registerReceiver(timeReceiver, time, Context.RECEIVER_NOT_EXPORTED)
        registerReceiver(packageReceiver, packages, Context.RECEIVER_NOT_EXPORTED)
        status.start()
        tick()
        loadWallpaper()
        refreshApps()
    }

    override fun onStop() {
        unregisterReceiver(timeReceiver)
        unregisterReceiver(packageReceiver)
        status.stop()
        super.onStop()
    }

    override fun onDestroy() {
        io.shutdownNow()
        super.onDestroy()
    }

    /** Botão Início com a tela já aberta: fecha o painel e volta o foco ao Dock. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        panels.close()
        dock.focusFirst()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK) {
            if (event.action == KeyEvent.ACTION_DOWN && panels.isOpen) panels.close()
            return true // na tela inicial, Voltar nunca sai
        }
        if (event.keyCode == KeyEvent.KEYCODE_MENU && event.action == KeyEvent.ACTION_DOWN) {
            if (!panels.isOpen) openStartMenu(menuBar.startItem)
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun tick() {
        val now = LocalDateTime.now()
        clock.update(now)
        menuBar.setTime(ClockFormat.menuBar(now))
    }

    private fun loadWallpaper() {
        val choice = prefs.wallpaper
        if (choice == loadedChoice && GlassSource.blurred != null) return
        val m = resources.displayMetrics
        io.execute {
            val result = loader.load(choice, m.widthPixels, m.heightPixels)
            runOnUiThread {
                if (isDestroyed) return@runOnUiThread
                wallpaperView.setImageBitmap(result.full)
                GlassSource.blurred = result.blurred
                loadedChoice = choice
                root.invalidateTree()
            }
        }
    }

    private fun refreshApps() {
        io.execute {
            val apps = repo.launchable()
            val installed = apps.mapTo(HashSet()) { it.pkg }
            prefs.newApps = NewAppsTracker.prune(NewAppsTracker.ensureInitialized(prefs.newApps, installed), installed)
            val items = buildDock(apps, installed)
            runOnUiThread {
                if (isDestroyed) return@runOnUiThread
                val hadFocus = dock.hasFocusedItem() || currentFocus == null
                dock.setItems(items)
                if (hadFocus && !panels.isOpen) dock.focusLast()
            }
        }
    }

    private fun buildDock(apps: List<AppEntry>, installed: Set<String>): List<DockItem> {
        val size = Theme.si(112f)
        val byPkg = apps.associateBy { it.pkg }
        val items = mutableListOf<DockItem>()
        prefs.dock.visible(installed).mapNotNull { byPkg[it] }.forEach { e ->
            items += DockItem(e.pkg, e.label, repo.iconBitmap(e, size)) { openApp(e) }
        }
        val fixed = mutableListOf<DockItem>()
        byPkg[AppRepository.PLAY_STORE]?.let { e ->
            fixed += DockItem(e.pkg, e.label, repo.iconBitmap(e, size), groupStart = fixed.isEmpty()) { openApp(e) }
        }
        if (shortcuts.isAvailable(Shortcuts.SOURCE)) {
            fixed += DockItem("hdmi", "HDMI", IconFactory.glyph(Glyph.INPUT, BLUE_TOP, BLUE_BOTTOM, size), groupStart = fixed.isEmpty()) {
                shortcuts.open(Shortcuts.SOURCE)
            }
        }
        if (shortcuts.isAvailable(Shortcuts.SETTINGS)) {
            fixed += DockItem("settings", "Ajustes", IconFactory.glyph(Glyph.GEAR, GRAY_TOP, GRAY_BOTTOM, size), groupStart = fixed.isEmpty()) {
                shortcuts.open(Shortcuts.SETTINGS)
            }
        }
        items += fixed
        items += DockItem("all", "Todos os apps", IconFactory.glyph(Glyph.GRID, LIGHT_TOP, LIGHT_BOTTOM, size), groupStart = true) {
            startActivity(Intent(this, AllAppsActivity::class.java))
        }
        return items
    }

    private fun openApp(e: AppEntry) {
        prefs.newApps = NewAppsTracker.markOpened(prefs.newApps, e.pkg)
        if (!repo.launch(e)) Toast.makeText(this, "Não foi possível abrir ${e.label}", Toast.LENGTH_SHORT).show()
    }

    private fun showTooltip(anchor: View, item: DockItem) {
        tooltip.text = item.label
        tooltip.visibility = View.VISIBLE
        tooltip.post {
            val loc = IntArray(2)
            anchor.getLocationOnScreen(loc)
            tooltip.x = loc[0] + anchor.width / 2f - tooltip.width / 2f
            tooltip.y = loc[1] - anchor.height * 0.35f - Theme.s(16f) - tooltip.height - Theme.s(18f)
            tooltip.invalidate() // o vidro depende da posição na tela
        }
    }

    private fun locked(): List<ViewGroup> = listOf(menuBar, dock)

    private fun onBarItem(item: BarItem, anchor: View) {
        when (item) {
            BarItem.START -> openStartMenu(anchor)
            BarItem.BLUETOOTH -> shortcuts.open(Shortcuts.BLUETOOTH)
            BarItem.WIFI -> shortcuts.open(Shortcuts.WIFI)
            BarItem.CONTROL -> openControlCenter(anchor)
            BarItem.CLOCK -> openCalendar(anchor)
        }
    }

    private fun openStartMenu(anchor: View) {
        val entries = mutableListOf(
            MenuEntry("Papel de parede…") { startActivity(Intent(this, WallpaperActivity::class.java)) },
            MenuEntry("Organizar Dock…") { startActivity(Intent(this, DockEditorActivity::class.java)) },
        )
        val firstOfGroup = entries.size
        if (shortcuts.isAvailable(Shortcuts.SETTINGS)) {
            entries += MenuEntry("Ajustes do projetor", separatorBefore = entries.size == firstOfGroup) { shortcuts.open(Shortcuts.SETTINGS) }
        }
        if (shortcuts.isAvailable(Shortcuts.SOURCE)) {
            entries += MenuEntry("Fontes de entrada (HDMI)", separatorBefore = entries.size == firstOfGroup) { shortcuts.open(Shortcuts.SOURCE) }
        }
        if (shortcuts.isAvailable(Shortcuts.ORIGINAL_LAUNCHER)) {
            entries += MenuEntry("Launcher original do fabricante", separatorBefore = true) { shortcuts.open(Shortcuts.ORIGINAL_LAUNCHER) }
        }
        val loc = IntArray(2).also { anchor.getLocationOnScreen(it) }
        panels.show(MenuPanel(this, null, entries) { panels.close() }, PanelHost.below(start = loc[0]), anchor, locked())
    }

    private fun openControlCenter(anchor: View) {
        val loc = IntArray(2).also { anchor.getLocationOnScreen(it) }
        val end = resources.displayMetrics.widthPixels - (loc[0] + anchor.width)
        val panel = ControlCenterPanel(this, shortcuts.available(Shortcuts.CONTROL_CENTER)) { s ->
            panels.close()
            shortcuts.open(s)
        }
        panels.show(panel, PanelHost.below(end = end), anchor, locked())
    }

    private fun openCalendar(anchor: View) {
        val panel = CalendarPanel(this, LocalDate.now()) {
            panels.close()
            shortcuts.open(Shortcuts.DATE)
        }
        panels.show(panel, PanelHost.below(end = Theme.si(16f)), anchor, locked())
    }

    private companion object {
        const val BLUE_TOP = 0xFF3A7BFD.toInt()
        const val BLUE_BOTTOM = 0xFF1F4FD8.toInt()
        const val GRAY_TOP = 0xFF9A9CA3.toInt()
        const val GRAY_BOTTOM = 0xFF5D5F66.toInt()
        const val LIGHT_TOP = 0xFFC7C9D1.toInt()
        const val LIGHT_BOTTOM = 0xFF8E9099.toInt()
    }
}
```

- [ ] **Step 6: Compilar, rodar a suíte e instalar a versão de teste**

```bash
cd ~/pessoal/projetor-bettdow/launcher && ./gradlew :app:testDebugUnitTest :app:assembleDebug && \
adb connect 192.168.2.100:5555 && adb -s 192.168.2.100:5555 install -r app/build/outputs/apk/debug/app-debug.apk
```
Expected: `BUILD SUCCESSFUL`, 37 testes passando, `Success` na instalação. O launcher do fabricante continua sendo o padrão.

- [ ] **Step 7: Abrir o launcher novo e conferir a tela inicial**

```bash
Q=~/pessoal/projetor-bettdow/launcher/build/qa; mkdir -p "$Q"
D="adb -s 192.168.2.100:5555"
$D shell am start -W -n br.gui.inicio/.home.HomeActivity | grep -E "Status|TotalTime"
$D exec-out screencap -p > "$Q/01-inicio.png"
```
Abrir `launcher/build/qa/01-inicio.png` com a ferramenta Read e conferir: degradê roxo/laranja de fundo; barra de vidro no topo com "Início", Bluetooth, Wi-Fi, "Controle" e data/hora curtas; data por extenso e hora grande no centro; Dock de vidro com YouTube, Netflix, Crunchyroll, Pluto TV, divisória, Play Store, HDMI, Ajustes, divisória, "Todos os apps"; o primeiro ícone em foco, maior, com o balão "YouTube". Qualquer diferença é bug desta task: corrigir antes de seguir.

- [ ] **Step 8: Navegar pelo controle e conferir cada painel**

```bash
Q=~/pessoal/projetor-bettdow/launcher/build/qa; D="adb -s 192.168.2.100:5555"
k() { $D shell input keyevent "$@"; sleep 0.7; }
cap() { $D exec-out screencap -p > "$Q/$1.png"; }
k DPAD_RIGHT DPAD_RIGHT; cap 02-dock-crunchyroll
k DPAD_UP; cap 03-barra-inicio
k DPAD_CENTER; cap 04-menu-inicio
k BACK; k DPAD_RIGHT DPAD_RIGHT DPAD_RIGHT; k DPAD_CENTER; cap 05-central-controle
k BACK; k DPAD_RIGHT; k DPAD_CENTER; cap 06-calendario
k DPAD_RIGHT; cap 07-calendario-novembro
k BACK; k DPAD_DOWN; cap 08-volta-dock
```
Conferir cada captura com Read:
- `02`: Crunchyroll em foco, com balão "Crunchyroll".
- `03`: "Início" destacado, sem balão no Dock.
- `04`: menu com "Papel de parede…", "Organizar Dock…", divisória, "Ajustes do projetor", "Fontes de entrada (HDMI)", divisória, "Launcher original do fabricante"; primeiro item azul.
- `05`: Central com 8 blocos (Foco automático, Keystone, Zoom, Modo de imagem, Projeção, Som, Fontes, Todos os ajustes), primeiro bloco branco.
- `06`: calendário de outubro de 2026 com o dia 26 em vermelho; `07`: "novembro de 2026".
- `08`: foco de volta no Crunchyroll (último ícone focado).

- [ ] **Step 9: Conferir "Todos os apps", menu de contexto e ponto azul**

```bash
Q=~/pessoal/projetor-bettdow/launcher/build/qa; D="adb -s 192.168.2.100:5555"
k() { $D shell input keyevent "$@"; sleep 0.7; }
cap() { $D exec-out screencap -p > "$Q/$1.png"; }
for i in 1 2 3 4 5 6 7 8 9; do k DPAD_RIGHT; done; k DPAD_CENTER; sleep 1.5; cap 09-todos-os-apps
k MENU; cap 10-menu-contexto
k BACK; k BACK
$D shell pm uninstall --user 0 com.esaba.downloader
$D shell cmd package install-existing com.esaba.downloader
k DPAD_CENTER; sleep 1.5; cap 11-ponto-azul
k BACK
```
Conferir: `09` grade com fundo desfocado, nomes embaixo, primeiro app em foco com anel branco; `10` menu com o nome do app, "Adicionar ao Dock" (ou "Remover do Dock"), "Abrir", "Informações do app"; `11` o app "Downloader" com o ponto azul (ele foi removido e reinstalado para o usuário 0, o que o torna "novo").

- [ ] **Step 10: Testar o envio de foto pelo computador e a tela de papel de parede**

```bash
T=$(mktemp -d) && convert -size 3000x2000 gradient:'#335599-#aa3366' "$T/Teste de Foto (1).png" && \
~/pessoal/projetor-bettdow/scripts/enviar-wallpaper.sh "$T/Teste de Foto (1).png"
Q=~/pessoal/projetor-bettdow/launcher/build/qa; D="adb -s 192.168.2.100:5555"
k() { $D shell input keyevent "$@"; sleep 0.7; }
cap() { $D exec-out screencap -p > "$Q/$1.png"; }
k DPAD_UP; k DPAD_LEFT DPAD_LEFT DPAD_LEFT DPAD_LEFT; k DPAD_CENTER; k DPAD_CENTER; sleep 1.5; cap 12-papel-de-parede
k DPAD_DOWN; k DPAD_RIGHT DPAD_RIGHT DPAD_RIGHT; k DPAD_CENTER; sleep 1.5; cap 13-fundo-trocado
```
Expected: o script imprime `Enviada: Teste_de_Foto__1_.jpg`; `12` mostra 4 degradês e, em "FOTOS", lago, montanha, vale (ordem alfabética) e a foto de teste; `13` mostra a tela inicial com o fundo da 4ª foto (a de teste) e o vidro da barra e do Dock com as cores dela. Depois voltar ao degradê padrão e apagar a foto de teste:
```bash
D="adb -s 192.168.2.100:5555"; k() { $D shell input keyevent "$@"; sleep 0.7; }
k DPAD_UP; k DPAD_LEFT DPAD_LEFT DPAD_LEFT DPAD_LEFT; k DPAD_CENTER; k DPAD_CENTER; sleep 1; k DPAD_UP; k DPAD_LEFT DPAD_LEFT DPAD_LEFT; k DPAD_CENTER
$D shell rm /sdcard/Android/data/br.gui.inicio/files/wallpapers/Teste_de_Foto__1_.jpg
```

- [ ] **Step 11: Voltar o projetor ao launcher do fabricante**

```bash
adb -s 192.168.2.100:5555 shell input keyevent HOME; sleep 1; adb -s 192.168.2.100:5555 shell dumpsys window | grep mCurrentFocus
```
Expected: `com.zeasn.launcher.projector` em foco (o novo não é o padrão).

- [ ] **Step 12: Commit**

```bash
cd ~/pessoal/projetor-bettdow && grep -rn $'\u2014' launcher/app/src; \
git add launcher && git commit -m "LAUNCHER | FEAT | Tela inicial com relógio, barra de menu e Dock"
```

---

### Task 15: Versão final assinada e medições

**Files:**
- Create: `launcher/inicio.jks`, `launcher/keystore.properties` (fora do git)
- Create: `scripts/medir-launcher.sh`
- Modify: `.gitignore` (acrescentar `apk/`)

**Interfaces:**
- Consumes: o app completo (Task 14).
- Produces: `apk/Inicio-1.0.apk` (release assinado, instalado no projetor); números medidos de memória, abertura e CPU, usados na Task 16.

- [ ] **Step 1: Gerar a chave de assinatura (guardar fora do git)**

```bash
cd ~/pessoal/projetor-bettdow/launcher && PW=$(openssl rand -hex 16) && \
keytool -genkeypair -keystore inicio.jks -alias inicio -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass "$PW" -keypass "$PW" -dname "CN=Inicio, O=Pessoal, C=BR" && \
printf 'storeFile=inicio.jks\nstorePassword=%s\nkeyAlias=inicio\nkeyPassword=%s\n' "$PW" "$PW" > keystore.properties && \
cd .. && git check-ignore launcher/inicio.jks launcher/keystore.properties
```
Expected: o `git check-ignore` imprime os dois caminhos (ambos ignorados). Sem essa chave não dá para atualizar o app depois sem desinstalar; anotar no README (Task 16) que ela fica em `launcher/`.

- [ ] **Step 2: Compilar a versão final e instalar no lugar da de teste**

```bash
cd ~/pessoal/projetor-bettdow && printf 'apk/\n' >> .gitignore && mkdir -p apk && \
cd launcher && ./gradlew :app:assembleRelease && \
cp app/build/outputs/apk/release/app-release.apk ../apk/Inicio-1.0.apk && ls -la ../apk && \
adb -s 192.168.2.100:5555 uninstall br.gui.inicio && \
adb -s 192.168.2.100:5555 install ../apk/Inicio-1.0.apk
```
Expected: `BUILD SUCCESSFUL`; APK com menos de 2 MB; `Success`. (Desinstalar é necessário porque a assinatura muda da versão de teste para a final; as preferências de teste somem, o que é desejado.)

- [ ] **Step 3: Escrever `scripts/medir-launcher.sh`**

```bash
#!/usr/bin/env bash
# Mede abertura a frio e memória (PSS) de um launcher, várias rodadas.
# Uso: ./medir-launcher.sh pacote/.Atividade [rodadas] [IP:PORTA]
set -euo pipefail
ALVO="${1:?Uso: $0 pacote/.Atividade [rodadas] [IP:PORTA]}"
N="${2:-3}"
DEV="${3:-192.168.2.100:5555}"
PKG="${ALVO%%/*}"
A=(adb -s "$DEV" shell)

for i in $(seq 1 "$N"); do
  "${A[@]}" am force-stop "$PKG"
  T=$("${A[@]}" am start -W -n "$ALVO" | awk -F': ' '/TotalTime/ {print $2}' | tr -d '\r')
  sleep 8
  PSS=$("${A[@]}" dumpsys meminfo "$PKG" | awk '/TOTAL PSS:/ {print $3}' | tr -d '\r')
  echo "rodada $i: abertura ${T} ms, memória $((PSS / 1024)) MB (${PSS} KB)"
done
CPU=$("${A[@]}" top -b -n 1 | awk -v p="$PKG" '$NF == p {print $9}')
echo "CPU parado: ${CPU:-0}%"
```

```bash
chmod +x ~/pessoal/projetor-bettdow/scripts/medir-launcher.sh
```

- [ ] **Step 4: Medir o launcher novo e o do fabricante (3 rodadas cada)**

```bash
cd ~/pessoal/projetor-bettdow && \
scripts/medir-launcher.sh br.gui.inicio/.home.HomeActivity 3 | tee launcher/build/qa/medicao-inicio.txt && \
scripts/medir-launcher.sh com.zeasn.launcher.projector/com.zeasn.launcher.ui.MainActivity 3 | tee launcher/build/qa/medicao-fabricante.txt; \
adb -s 192.168.2.100:5555 shell input keyevent HOME
```
Expected (metas da especificação): no `Início`, memória < 35 MB e abertura < 1000 ms nas 3 rodadas, CPU parado ~0%. Se alguma meta falhar: anotar o número, aplicar o plano B da especificação (seção "Riscos": trocar o vidro por véu sem desfoque, ou reduzir os ícones do Dock), medir de novo e registrar as duas medições. Não seguir para a Task 16 com meta estourada sem registrar o motivo.

- [ ] **Step 5: Commit**

```bash
cd ~/pessoal/projetor-bettdow && grep -rn $'\u2014' scripts .gitignore; \
git add .gitignore scripts/medir-launcher.sh && git commit -m "LAUNCHER | BUILD | Versão final assinada e script de medição"
```

---

### Task 16: Roteiro de QA, documentação e entrega

**Files:**
- Create: `docs/qa/roteiro-launcher.md`, `docs/qa/resultado-launcher.md`
- Create: `README.md`, `CHANGELOG.md`, `RELATORIO-FINAL.md`
- Modify: `ALTERACOES.md` (nova seção), `scripts/reverter.sh` (novo bloco)

**Interfaces:**
- Consumes: APK instalado e medições (Task 15).
- Produces: documentação de entrega e o portão de teste na tela com o dono do aparelho.

- [ ] **Step 1: Escrever o roteiro ANTES de testar**

`docs/qa/roteiro-launcher.md`:
```markdown
# Roteiro de QA | Launcher Início 1.0

Aparelho: Bettdow 1073, ADB 192.168.2.100:5555. Execução por `adb shell input keyevent` + captura em `launcher/build/qa/`.
Cada cenário: PASS só com captura conferida. FAIL é FAIL.

| # | Cenário | Passos | Esperado |
|---|---|---|---|
| 1 | Abertura | `am start -W -n br.gui.inicio/.home.HomeActivity` | Tela inicial completa; TotalTime < 1000 ms |
| 2 | Foco inicial | Abrir | Primeiro ícone do Dock maior, balão com o nome |
| 3 | Limites do Dock | ← no primeiro ícone; → no último | Foco não sai do Dock |
| 4 | Subir e descer | ↑ no Dock; ↓ na barra | Vai para "Início"; volta ao último ícone focado |
| 5 | Limites da barra | ← em "Início"; → na data/hora; ↑ em qualquer item | Foco não sai da barra |
| 6 | Menu Início | OK em "Início" | 7 linhas (5 itens + 2 divisórias); Voltar fecha e o foco volta para "Início" |
| 7 | Botão Menu | Tecla MENU com foco no Dock | Abre o menu "Início" |
| 8 | Central de Controle | OK em "Controle"; OK em "Keystone" | 8 blocos; abre a tela de keystone do fabricante |
| 9 | Calendário | OK na data; → duas vezes; ↓; OK | Mês avança 2 vezes; "Ajustar data e hora…" abre a tela do sistema |
| 10 | Wi-Fi e Bluetooth | OK em cada um | Abre a tela de redes / de Bluetooth do fabricante |
| 11 | Abrir app do Dock | OK em YouTube; Início | YouTube abre; Início volta à tela do launcher |
| 12 | Todos os apps | OK no último ícone; → até a coluna 6; → | Grade 6 x 3; com mais de 18 apps passa para a página 2 e as bolinhas mudam |
| 13 | Ponto azul | `pm uninstall --user 0` + `install-existing` do Downloader; abrir Todos os apps; abrir o Downloader; voltar | Ponto azul aparece; some depois de abrir |
| 14 | Adicionar ao Dock | MENU sobre o Downloader; "Adicionar ao Dock"; Voltar até a Home | Downloader aparece no fim dos favoritos |
| 15 | Dock cheio | Organizar Dock: marcar até 8; tentar o 9º | Aviso "Dock cheio"; o 9º não entra |
| 16 | Remover do Dock | Organizar Dock: OK no Downloader | Sai do Dock; Home atualiza |
| 17 | Mover no Dock | Organizar Dock: → na Netflix | Netflix passa para a posição 3 |
| 18 | Papel de parede | Trocar para "vale" | Fundo e vidro mudam; escolha persiste após `am force-stop` e reabertura |
| 19 | Foto do computador | `scripts/enviar-wallpaper.sh` com uma foto retrato 3000x4000 | Aparece em Fotos; recortada sem distorção |
| 20 | Foto corrompida | `adb push` de um arquivo texto renomeado para `ruim.jpg`; escolher | Não trava; fundo cai no degradê padrão |
| 21 | Voltar não sai | Voltar várias vezes na tela inicial | Continua na tela inicial |
| 22 | Launcher original | Menu Início > "Launcher original do fabricante" | Abre a tela do fabricante |
```

- [ ] **Step 2: Ler o roteiro inteiro antes de começar (checkpoint) e executar cenário por cenário**

Para cada cenário: rodar os passos com `adb shell input keyevent` (mesma função `k` e `cap` da Task 14), salvar `launcher/build/qa/qa-NN.png`, abrir com Read e registrar em `docs/qa/resultado-launcher.md` uma linha `| NN | PASS/FAIL | o que a captura mostra |`. Ao final, restaurar o estado: tirar o Downloader do Dock se ainda estiver, voltar o papel de parede ao degradê 1, apagar `ruim.jpg` e a foto retrato de teste da pasta do app.

FAIL vira correção (com teste, se for lógica de `core/`), novo build release, reinstalação e nova rodada do cenário. Não adaptar o roteiro para passar.

- [ ] **Step 3: Escrever `RELATORIO-FINAL.md` com os números medidos**

Conteúdo (todos os números saem de `launcher/build/qa/medicao-*.txt`, Task 15, e de `diagnostico/`):
```markdown
# Relatório final | Projetor Bettdow 1073

## Otimização do sistema (26/10/2026)
- Adware removido: `com.android.media.module.services` (instalado em silêncio pelo launcher do fabricante) e `com.android.docks`.
- Desativados: loja do fabricante, OfficeSuite, teclado chinês, impressão, atualizador `uota`/`uotaback`.
- Animações em 0,5x.
- Detalhes e comandos de reversão: `ALTERACOES.md`, `scripts/reverter.sh`.

## Launcher: antes e depois (média de 3 rodadas, medido com scripts/medir-launcher.sh)
| | Fabricante (zeasn) | Início 1.0 | Meta |
|---|---|---|---|
| Memória (PSS) | (média de medicao-fabricante.txt) MB | (média de medicao-inicio.txt) MB | < 35 MB |
| Abertura a frio | (média) ms | (média) ms | < 1000 ms |
| CPU parado | (valor) % | (valor) % | ~0% |

## O que não foi feito
- Foto pelo pendrive (decidido para uma versão futura).
- Desativar o launcher do fabricante (etapa separada, só com aprovação).
```
Trocar cada valor entre parênteses pelo número real antes de salvar; nenhum parêntese de instrução fica no arquivo.

- [ ] **Step 4: Escrever `README.md`, `CHANGELOG.md` e atualizar `ALTERACOES.md` e `scripts/reverter.sh`**

`README.md`:
```markdown
# Projetor Bettdow 1073 | otimização e launcher Início

## Instalar ou atualizar o launcher
    adb connect 192.168.2.100:5555
    adb install -r apk/Inicio-1.0.apk
Para gerar de novo: `cd launcher && ./gradlew :app:assembleRelease` (a chave `launcher/inicio.jks` e o `launcher/keystore.properties` ficam fora do git; sem eles, atualizar exige desinstalar antes).

## Tornar o Início a tela padrão
No controle, aperte Início (casinha). Na lista que aparecer, escolha "Início" e "Sempre".

## Voltar para a tela do fabricante
Pelo próprio launcher: menu "Início" > "Launcher original do fabricante".
De vez: `adb uninstall br.gui.inicio` (o Android volta sozinho para a tela do fabricante).

## Papel de parede com foto sua
    scripts/enviar-wallpaper.sh minha-foto.jpg
Depois: menu "Início" > "Papel de parede…" > Fotos.

## Medir desempenho
    scripts/medir-launcher.sh br.gui.inicio/.home.HomeActivity 3

## Documentos
- `RELATORIO-DIAGNOSTICO.md`: diagnóstico inicial.
- `ALTERACOES.md` e `scripts/reverter.sh`: tudo o que foi alterado e como desfazer.
- `RELATORIO-FINAL.md`: antes e depois.
- `docs/superpowers/specs/` e `docs/superpowers/plans/`: especificação e plano do launcher.
- `docs/qa/`: roteiro e resultado dos testes.
```

`CHANGELOG.md`:
```markdown
# Changelog

## Início 1.0 (26/10/2026)
- Tela inicial estilo macOS: relógio grande, barra de menu de vidro navegável, Dock de vidro com favoritos e atalhos.
- Todos os apps (Launchpad) com páginas, ponto azul em app novo e menu de contexto.
- Central de Controle (foco, keystone, zoom, modo de imagem, projeção, som, fontes, ajustes) e calendário.
- Organizar Dock, papel de parede (degradês, fotos incluídas, fotos enviadas pelo computador).
- Sem internet, sem serviços em segundo plano, uma única permissão.
```

Acrescentar ao fim de `ALTERACOES.md`:
```markdown
## 26/10/2026, launcher Início

| # | Alteração | Comando aplicado | Como desfazer |
|---|---|---|---|
| 9 | Instalado o launcher Início 1.0 (`br.gui.inicio`) | `adb install apk/Inicio-1.0.apk` | `adb uninstall br.gui.inicio` |
| 10 | Início escolhido como tela padrão (pelo dono, na tela do Android) | botão Início > "Início" > "Sempre" | desinstalar, ou menu "Início" > "Launcher original" |

O launcher do fabricante continua instalado e ativo.
```

Acrescentar ao fim de `scripts/reverter.sh`:
```bash

# 4. Remover o launcher Início (o Android volta para a tela do fabricante)
adb -s "$DEV" uninstall br.gui.inicio || true
```

- [ ] **Step 5: Conferir travessões e commitar**

```bash
cd ~/pessoal/projetor-bettdow && grep -rn $'\u2014' README.md CHANGELOG.md RELATORIO-FINAL.md ALTERACOES.md docs scripts launcher/app/src; \
git add -A && git status --short && git commit -m "LAUNCHER | DOCS | Roteiro e resultado de QA, relatório final e instruções"
```
Expected: o `grep` não imprime nada; `git status` não lista `inicio.jks`, `keystore.properties` nem `apk/`.

- [ ] **Step 6: Portão de tela com o dono do aparelho**

Pedir ao dono, no projetor, com o controle de verdade:
1. Apertar o botão Início (casinha) e, na lista, escolher "Início" e "Sempre".
2. Navegar pelo Dock, abrir YouTube, Netflix, Crunchyroll e Pluto TV e voltar com o botão Início.
3. Abrir a Central de Controle e testar Foco automático e Keystone.
4. Dizer se a leitura a 2 ou 3 metros está boa (tamanho do relógio, dos nomes e da barra).

Registrar a resposta em `docs/qa/resultado-launcher.md` (seção "Teste na tela") e commitar. Ajustes pedidos viram novas correções com nova rodada do cenário afetado.

- [ ] **Step 7: Não fazer merge sem perguntar**

A branch `feature/launcher-inicio` fica pronta para merge na `master`. Perguntar ao dono antes de fazer o merge (merge está na lista de ações que pedem confirmação). Desativar o launcher do fabricante também é uma etapa separada, com aprovação própria.
