package br.gui.inicio.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Lógica da 1.2: ícones que preenchem o quadrado e a fileira "Continuar assistindo". */
class V12Test {
    private val yellow = 0xFFFFF301.toInt()
    private val clear = 0x00000000

    // Ícones: logo opaco até a borda (Pluto, Crunchyroll) preenche; logo solto (Play Store) fica sobre o branco
    @Test fun opaqueEdgesFillWithTheirColor() =
        assertEquals(IconFill.Full(yellow), IconStyle.classify(IntArray(12) { yellow }))

    @Test fun transparentEdgesKeepWhitePadding() =
        assertEquals(IconFill.Padded, IconStyle.classify(IntArray(12) { clear }))

    @Test fun twoTransparentSamplesStillFill() {
        val s = IntArray(12) { yellow }.also { it[0] = clear; it[5] = clear }
        assertEquals(IconFill.Full(yellow), IconStyle.classify(s))
    }

    @Test fun mostlyTransparentKeepsPadding() {
        val s = IntArray(12) { clear }.also { it[1] = yellow; it[4] = yellow }
        assertEquals(IconFill.Padded, IconStyle.classify(s))
    }

    @Test fun fillUsesTheMostCommonEdgeColor() {
        val orange = 0xFFFF5E00.toInt()
        val s = IntArray(12) { orange }.also { it[3] = 0xFFFFFFFF.toInt() }
        assertEquals(IconFill.Full(orange), IconStyle.classify(s))
    }

    @Test fun edgeSamplesStayInsideTheImage() {
        val pts = IconStyle.edgeSamples(48, 48)
        assertEquals(12, pts.size)
        assertTrue(pts.all { (x, y) -> x in 0 until 48 && y in 0 until 48 })
    }

    @Test fun netflixGetsSmallerLetter() {
        assertEquals(0.8f, IconStyle.foregroundScale("com.netflix.mediaclient"), 0f)
        assertEquals(1f, IconStyle.foregroundScale("com.crunchyroll.crunchyroid"), 0f)
    }

    /** A Pluto 5.x para TV tem ícone adaptativo com o logo maior que a área segura: o "tv" saía cortado. */
    @Test fun plutoLogoFitsTheSquare() = assertEquals(0.8f, IconStyle.foregroundScale("tv.pluto.android"), 0f)

    // Continuar assistindo
    private fun item(
        title: String,
        engaged: Long,
        pkg: String = "tv.pluto.android",
        pos: Long = 0,
        dur: Long = 0,
        season: String? = null,
        episode: String? = null,
        series: String? = null,
    ) = WatchNextItem(pkg, title, season, episode, poster = null, positionMs = pos, durationMs = dur, intentUri = null, lastEngagedMs = engaged, seriesId = series)

    @Test fun sameSeriesWithAnotherTitleShowsOnce() {
        val items = listOf(item("Boruto: Naruto Next Generations", 1, series = "boruto"), item("Boruto Uzumaki!", 2, series = "boruto"), item("NCIS", 3, series = "ncis"))
        assertEquals(listOf("NCIS", "Boruto Uzumaki!"), WatchNextList.pick(items) { true }.map { it.title })
    }

    @Test fun mostRecentFirstAndCapped() {
        val items = (1..9).map { item("S$it", engaged = it.toLong()) }
        val picked = WatchNextList.pick(items) { true }
        assertEquals(WatchNextList.MAX, picked.size)
        assertEquals("S9", picked.first().title)
    }

    @Test fun appsThatCannotOpenAreHidden() {
        val items = listOf(item("Boruto", 2), item("Série", 1, pkg = "com.removido"))
        assertEquals(listOf("Boruto"), WatchNextList.pick(items) { it == "tv.pluto.android" }.map { it.title })
    }

    @Test fun duplicatesAndBlankTitlesAreDropped() {
        val items = listOf(item("Naruto", 3), item("Naruto", 1), item("  ", 2))
        assertEquals(1, WatchNextList.pick(items) { true }.size)
    }

    @Test fun progressOnlyWhenKnown() {
        assertEquals(0.25f, WatchNextList.progress(item("A", 1, pos = 300_000, dur = 1_200_000))!!, 0.001f)
        assertNull(WatchNextList.progress(item("A", 1)))
        assertEquals(1f, WatchNextList.progress(item("A", 1, pos = 2_000, dur = 1_000))!!, 0f)
    }

    @Test fun metaLineInPortuguese() {
        val it = item("Boruto", 1, pos = 330_000, dur = 1_470_000, season = "1", episode = "1")
        assertEquals("Pluto TV · Temporada 1 · Episódio 1 · faltam 19 min", WatchNextList.meta(it, "Pluto TV"))
        assertEquals("Pluto TV · Episódio 3", WatchNextList.meta(item("N", 1, episode = "3"), "Pluto TV"))
        assertEquals("YouTube", WatchNextList.meta(item("V", 1, pkg = "yt"), "YouTube"))
    }

    @Test fun lessThanAMinuteLeft() =
        assertEquals("Pluto TV · falta menos de 1 min", WatchNextList.meta(item("A", 1, pos = 1_190_000, dur = 1_200_000), "Pluto TV"))
}

/** Descanso estilo tela de bloqueio: só OK (ou Enter) abre o episódio do widget; o resto só acorda. */
class ScreensaverKeysTest {
    @Test fun okOpensTheEpisode() {
        assertTrue(Screensaver.opensEpisode(23))
        assertTrue(Screensaver.opensEpisode(66))
        assertTrue(Screensaver.opensEpisode(160))
    }

    @Test fun otherKeysOnlyWake() {
        listOf(19, 20, 21, 22, 4, 3, 82).forEach { assertEquals(false, Screensaver.opensEpisode(it)) }
    }
}

/** À noite não existe "sol": o código 1 do Open-Meteo vira "Céu quase limpo" (cabe no widget do descanso). */
class WeatherNightTest {
    @Test fun mostlyClearAtNight() = assertEquals("Céu quase limpo", WeatherCodes.describe(1, isDay = false))

    @Test fun mostlySunnyByDay() = assertEquals("Predomínio de sol", WeatherCodes.describe(1, isDay = true))

    @Test fun otherCodesIgnoreTheHour() = assertEquals("Nublado", WeatherCodes.describe(3, isDay = false))
}

/** Capas guardadas no aparelho: a fileira aparece na hora depois da primeira vez. */
class PosterCacheTest {
    @Test fun sameUriAndSizeSameFile() =
        assertEquals(PosterCache.fileName("https://x/a.jpg", 380, 214), PosterCache.fileName("https://x/a.jpg", 380, 214))

    @Test fun otherSizeOtherFile() =
        assertTrue(PosterCache.fileName("https://x/a.jpg", 380, 214) != PosterCache.fileName("https://x/a.jpg", 190, 107))

    @Test fun nameIsSafeForTheDisk() =
        assertTrue(PosterCache.fileName("content://tv.pluto/capa?id=1&w=2", 1, 1).matches(Regex("[0-9a-f]{40}\\.jpg")))

    @Test fun onlyPostersOutOfTheListAreDeleted() {
        val keep = setOf("a.jpg", "b.jpg")
        assertEquals(listOf("velho.jpg"), PosterCache.stale(listOf("a.jpg", "velho.jpg", "b.jpg"), keep))
    }
}

/** O link de um episódio só pode abrir o app que publicou o episódio. */
class EpisodeLinkTest {
    @Test fun linkToTheSameAppIsAllowed() = assertTrue(EpisodeLink.allowed(target = "tv.pluto.android", publisher = "tv.pluto.android"))

    @Test fun linkWithoutAppIsAllowedAndPinned() = assertTrue(EpisodeLink.allowed(target = null, publisher = "tv.pluto.android"))

    @Test fun linkToAnotherAppIsRefused() {
        assertEquals(false, EpisodeLink.allowed(target = "br.gui.inicio", publisher = "tv.pluto.android"))
        assertEquals(false, EpisodeLink.allowed(target = "com.outro.app", publisher = "tv.pluto.android"))
    }
}

/** A Pluto publicou o mesmo episódio duas vezes, com título, capa e link diferentes (formato antigo e novo do app). */
class SameEpisodeTest {
    private fun real(title: String, poster: String, link: String, engaged: Long) = WatchNextItem(
        "tv.pluto.android", title, "1", "1", poster, 0, 0, link, engaged,
    )

    private val old = real(
        "Boruto: Naruto Next Generations",
        "https://images.pluto.tv/episodes/653aa0b12a6001001a508694/screenshot16_9.jpg?fill=blur&fit=fill&fm=jpg&h=540&q=75&w=960",
        "https://pluto.tv/on-demand/series/653a849025fde2001a81c872/episode/boruto-uzumaki-2017-1-1-ptv1?utm_source=plutotvapp",
        1,
    )
    private val new = real(
        "Boruto Uzumaki!",
        "https://thumbnails.cbsig.net/q70/ptv/episodes/653aa0b12a6001001a508694/screenshot16_9.jpg?format=webp",
        "plutotv://pluto.tv/shows/3800027/episode/653aa0b12a6001001a508694/?searchReferral=AndroidTV_Watch+Next",
        2,
    )
    private val naruto = real(
        "Naruto",
        "https://images.pluto.tv/episodes/5f91d42afdb601001aefb80f/screenshot16_9.jpg?fill=blur",
        "https://pluto.tv/on-demand/series/5f89ec9e7ecf48001a34156c/episode/o-sharingan-revivido-2020-1-30",
        3,
    )

    @Test fun sameEpisodeIdShowsOnlyTheNewest() =
        assertEquals(listOf("Naruto", "Boruto Uzumaki!"), WatchNextList.pick(listOf(old, new, naruto)) { true }.map { it.title })

    @Test fun differentEpisodesStay() = assertEquals(2, WatchNextList.pick(listOf(old, naruto)) { true }.size)

    @Test fun idsAreTheLongCodesInPosterAndLink() =
        assertTrue("653aa0b12a6001001a508694" in WatchNextList.ids(new))
}
