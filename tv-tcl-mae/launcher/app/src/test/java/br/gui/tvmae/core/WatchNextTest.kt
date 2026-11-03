package br.gui.tvmae.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test


/** Continuar assistindo, vindo do projetor (V12Test): a lista, as capas guardadas, o link seguro e o episódio repetido. */
class WatchNextTest {
    private fun item(
        title: String,
        engaged: Long,
        pkg: String = "tv.pluto.android",
        pos: Long = 60_000,
        dur: Long = 0,
        season: String? = null,
        episode: String? = null,
        series: String? = null,
    ) = WatchNextItem(pkg, title, season, episode, poster = null, positionMs = pos, durationMs = dur, intentUri = null, lastEngagedMs = engaged, seriesId = series)

    @Test fun sameSeriesWithAnotherTitleShowsOnce() {
        val items = listOf(item("Boruto: Naruto Next Generations", 1, series = "boruto"), item("Boruto Uzumaki!", 2, series = "boruto"), item("NCIS", 3, series = "ncis"))
        assertEquals(listOf("NCIS", "Boruto Uzumaki!"), WatchNextList.pick(items) { true }.map { it.title })
    }

    /** Cabem 4 cartões inteiros na largura da tela; um 5º sairia cortado e o foco iria para fora da tela. */
    @Test fun mostRecentFirstAndCappedAtFour() {
        val items = (1..9).map { item("S$it", engaged = it.toLong(), pkg = "app$it") }
        val picked = WatchNextList.pick(items) { true }
        assertEquals(listOf("S9", "S8", "S7", "S6"), picked.map { it.title })
    }

    /** Pedido do neto: um app só (a Globoplay) não pode ocupar a fileira inteira; no máximo 2 cartões por app. */
    @Test fun atMostTwoPerApp() {
        val globo = (1..6).map { item("G$it", engaged = 10L + it, pkg = "com.globo.globotv") }
        val netflix = (1..3).map { item("N$it", engaged = it.toLong(), pkg = "com.netflix.ninja") }
        assertEquals(listOf("G6", "G5", "N3", "N2"), WatchNextList.pick(globo + netflix) { true }.map { it.title })
    }

    /** A Globoplay põe prévias na lista (ponto zerado) ao abrir; só entra o que foi assistido de verdade. */
    @Test fun notStartedIsHidden() {
        val items = listOf(item("Territórios", 5, pkg = "com.globo.globotv", pos = 0), item("CSI", 1, pkg = "com.globo.globotv"))
        assertEquals(listOf("CSI"), WatchNextList.pick(items) { true }.map { it.title })
    }

    @Test fun openedHereShowsEvenWithoutProgress() {
        val preview = item("Territórios", 5, pkg = "com.globo.globotv", pos = 0)
        val opened = mapOf(WatchNextList.key(preview) to 10L)
        assertEquals(listOf("Territórios"), WatchNextList.pick(listOf(preview), opened) { true }.map { it.title })
    }

    /** O YouTube não atualiza o horário de um vídeo revisto: vale o horário em que foi aberto pelo cartão. */
    @Test fun openedHereGoesFirst() {
        val paraiso = item("Paraíso", 1, pkg = "com.google.android.youtube.tv")
        val csi = item("CSI", 50, pkg = "com.globo.globotv")
        val opened = mapOf(WatchNextList.key(paraiso) to 100L)
        assertEquals(listOf("Paraíso", "CSI"), WatchNextList.pick(listOf(csi, paraiso), opened) { true }.map { it.title })
    }

    /** O que tocou dentro do app chega só com o título (sessão de mídia): vale também para item que tem série. */
    @Test fun playedInsideTheAppCountsByTitle() {
        val csi = item("CSI", 1, pkg = "com.globo.globotv", series = "csi")
        val paraiso = item("Paraíso", 50, pkg = "com.google.android.youtube.tv")
        val opened = mapOf(WatchNextList.playedKey("com.globo.globotv", " CSI ")!! to 100L)
        assertEquals(listOf("CSI", "Paraíso"), WatchNextList.pick(listOf(paraiso, csi), opened) { true }.map { it.title })
    }

    @Test fun playedWithoutTitleIsIgnored() = assertNull(WatchNextList.playedKey("com.google.android.youtube.tv", "  "))

    @Test fun keyUsesSeriesWhenThereIsOne() {
        assertEquals("com.globo.globotv|csi", WatchNextList.key(item("CSI: episódio 6", 1, pkg = "com.globo.globotv", series = "csi")))
        assertEquals("com.globo.globotv|CSI", WatchNextList.key(item(" CSI ", 1, pkg = "com.globo.globotv")))
    }

    @Test fun singleAppStopsAtTwo() {
        val globo = (1..6).map { item("G$it", engaged = it.toLong(), pkg = "com.globo.globotv") }
        assertEquals(listOf("G6", "G5"), WatchNextList.pick(globo) { true }.map { it.title })
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
        "tv.pluto.android", title, "1", "1", poster, 60_000, 0, link, engaged,
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

/** Registro do que foi aberto pelo cartão da tela inicial (guardado nas preferências). */
class OpenedLogTest {
    @Test fun roundTrip() {
        val log = mapOf("a|CSI" to 10L, "b|Paraíso" to 20L)
        assertEquals(log, OpenedLog.parse(OpenedLog.format(log)))
    }

    @Test fun reopeningUpdatesTheTime() = assertEquals(mapOf("a|CSI" to 30L), OpenedLog.record(mapOf("a|CSI" to 10L), "a|CSI", 30L))

    @Test fun keepsOnlyTheNewest() {
        val full = (1..OpenedLog.MAX).associate { "k$it" to it.toLong() }
        val after = OpenedLog.record(full, "novo", 1_000L)
        assertEquals(OpenedLog.MAX, after.size)
        assertTrue("novo" in after)
        assertTrue("k1" !in after)
    }

    @Test fun brokenLinesAreIgnored() = assertEquals(mapOf("a|CSI" to 10L), OpenedLog.parse("lixo\nx\tsem número\n10\ta|CSI"))

    @Test fun nothingSavedIsEmpty() = assertEquals(emptyMap<String, Long>(), OpenedLog.parse(null))
}
