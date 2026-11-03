package br.gui.inicio.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Continuar assistindo pelo que tocou: YouTube, Netflix e Crunchyroll não publicam na lista do Android neste
 * projetor, então a tela inicial anota o que cada app informa enquanto toca (sessão de mídia).
 */
class PlayedLogTest {
    private val day = 24 * 60 * 60_000L
    private val yt = "com.google.android.youtube.tv"
    private val spotify = "com.spotify.tv.android"

    private fun played(
        title: String,
        at: Long,
        pkg: String = yt,
        pos: Long = 60_000,
        dur: Long = 0,
        subtitle: String? = null,
        mediaId: String? = null,
        art: String? = null,
    ) = PlayedItem(pkg, title, subtitle, mediaId, pos, dur, at, art)

    /** Prévia que toca sozinha na tela do app (Netflix, YouTube) não é "assistir". */
    @Test fun previewIsNotRecorded() {
        assertFalse(PlayedLog.watched(10_000))
        assertTrue(PlayedLog.watched(30_000))
    }

    @Test fun sameTitleIsUpdatedNotRepeated() {
        val log = PlayedLog.record(listOf(played("A", 1), played("B", 2)), played("A", 5, pos = 90_000))
        assertEquals(listOf("A" to 5L, "B" to 2L), log.map { it.title to it.playedAtMs })
        assertEquals(90_000L, log.first().positionMs)
    }

    /** Combinado com o dono: música ocupa um cartão só, o da última que tocou. */
    @Test fun musicKeepsOnlyTheLastSong() {
        val log = PlayedLog.record(listOf(played("Pérola Negra", 1, pkg = spotify)), played("Streets of Bakersfield", 2, pkg = spotify))
        assertEquals(listOf("Streets of Bakersfield"), log.map { it.title })
    }

    @Test fun keepsOnlyTheNewest() {
        val log = (1..PlayedLog.MAX + 5).fold(emptyList<PlayedItem>()) { acc, i -> PlayedLog.record(acc, played("V$i", i.toLong())) }
        assertEquals(PlayedLog.MAX, log.size)
        assertEquals("V${PlayedLog.MAX + 5}", log.first().title)
    }

    @Test fun oldEntriesAreHidden() {
        val now = 30 * day
        val log = listOf(played("Ontem", now - day), played("Mês passado", now - 20 * day))
        assertEquals(listOf("Ontem"), PlayedLog.visible(log, now, publishers = emptySet()).map { it.title })
    }

    /** Episódio que chegou ao fim sai; música não tem "fim" que importe. */
    @Test fun finishedVideoIsHiddenButMusicStays() {
        val log = listOf(
            played("Acabou", 3, pos = 2_370_000, dur = 2_400_000),
            played("No meio", 2, pos = 600_000, dur = 2_400_000),
            played("Música", 1, pkg = spotify, pos = 200_000, dur = 210_000),
        )
        assertEquals(listOf("No meio", "Música"), PlayedLog.visible(log, 10, publishers = emptySet()).map { it.title })
    }

    /** A Pluto publica na lista do Android: a anotação dela repetiria o mesmo episódio. */
    @Test fun appsThatPublishAreLeftToTheList() {
        val log = listOf(played("Naruto", 2, pkg = "tv.pluto.android"), played("Vídeo", 1))
        assertEquals(listOf("Vídeo"), PlayedLog.visible(log, 10, publishers = setOf("tv.pluto.android")).map { it.title })
    }

    @Test fun youTubeOpensTheVideoWhereItStopped() {
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=95s", PlayedLog.link(yt, "dQw4w9WgXcQ", 95_400))
        assertNull(PlayedLog.link(yt, "não é um vídeo", 0))
        assertNull(PlayedLog.link("com.netflix.mediaclient", "dQw4w9WgXcQ", 0))
    }

    /** "ATliens", do ATliens: em música o artista aparece mesmo igual ao título; em vídeo, repetir não acrescenta nada. */
    @Test fun secondLineEqualToTitleOnlyStaysInMusic() {
        assertEquals("ATliens", PlayedLog.secondLine("com.spotify.tv.android", "ATliens", "ATliens"))
        assertNull(PlayedLog.secondLine(yt, "Vídeo", " Vídeo "))
        assertEquals("Canal", PlayedLog.secondLine(yt, "Vídeo", "Canal"))
        assertNull(PlayedLog.secondLine(yt, "Vídeo", "  "))
    }

    @Test fun becomesACard() {
        val item = PlayedLog.toWatchNext(played("ASPAS APOSENTANDO", 7, subtitle = "Vai Pro Chão", mediaId = "dQw4w9WgXcQ", art = "file:///capa.jpg", dur = 900_000))
        assertEquals(yt, item.pkg)
        assertEquals("Vai Pro Chão", item.subtitle)
        assertEquals("file:///capa.jpg", item.poster)
        assertEquals(7L, item.lastEngagedMs)
        assertEquals(900_000L, item.durationMs)
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=60s", item.intentUri)
    }

    @Test fun savedAndReadBack() {
        val log = listOf(
            played("Título com\ttab, quebra\ne barra \\ no meio", 3, subtitle = null, mediaId = "x", art = null),
            played("P1:E1 - Capítulo 1", 2, pkg = "com.netflix.mediaclient", subtitle = "Capítulo 1", dur = 3_000_000),
        )
        assertEquals(log, PlayedLog.parse(PlayedLog.format(log)))
    }

    @Test fun brokenLinesAreIgnored() {
        val ok = played("A", 1)
        assertEquals(listOf(ok), PlayedLog.parse("lixo\n" + PlayedLog.format(listOf(ok)) + "\nmeio\tquebrado"))
        assertEquals(emptyList<PlayedItem>(), PlayedLog.parse(null))
    }
}

/** A fileira com os dois tipos de item: no máximo 2 por app, e 1 só de música. */
class PerAppTest {
    private fun item(title: String, engaged: Long, pkg: String) =
        WatchNextItem(pkg, title, null, null, null, 0, 0, null, engaged)

    @Test fun atMostTwoPerApp() {
        val items = (1..5).map { item("Y$it", 10L + it, "com.google.android.youtube.tv") } + item("Naruto", 1, "tv.pluto.android")
        assertEquals(listOf("Y5", "Y4", "Naruto"), WatchNextList.pick(items) { true }.map { it.title })
    }

    @Test fun musicTakesOneCard() {
        val items = listOf(item("M2", 3, "com.spotify.tv.android"), item("M1", 2, "com.spotify.tv.android"), item("V", 1, "com.google.android.youtube.tv"))
        assertEquals(listOf("M2", "V"), WatchNextList.pick(items) { true }.map { it.title })
    }

    /** "faltam 1 min" numa música não diz nada: música mostra só o artista. */
    @Test fun musicShowsNoTimeLeft() {
        val song = WatchNextItem("com.spotify.tv.android", "Calm Down", null, null, null, 150_000, 210_000, null, 1, subtitle = "Rema")
        assertEquals("Spotify · Rema", WatchNextList.meta(song, "Spotify"))
        assertNull(WatchNextList.progress(song))
    }

    @Test fun subtitleGoesInTheDetails() {
        val item = WatchNextItem("com.google.android.youtube.tv", "ASPAS", null, null, null, 0, 0, null, 1, subtitle = "Vai Pro Chão")
        assertEquals("YouTube · Vai Pro Chão", WatchNextList.meta(item, "YouTube"))
    }
}

/** O Crunchyroll publicou um episódio já no fim (faltavam 18 s) e de outro perfil: não há campo de perfil na lista. */
class ShowCleanupTest {
    private fun item(title: String, engaged: Long, pkg: String = "com.crunchyroll.crunchyroid", pos: Long = 0, dur: Long = 0) =
        WatchNextItem(pkg, title, null, null, null, pos, dur, null, engaged)

    @Test fun finishedEpisodeFromTheListIsHidden() {
        val items = listOf(item("The Apothecary Diaries", 2, pos = 1_422_000, dur = 1_440_066), item("Outra", 1, pos = 60_000, dur = 1_440_000))
        assertEquals(listOf("Outra"), WatchNextList.pick(items) { true }.map { it.title })
    }

    @Test fun finishedSongStays() {
        val song = item("Música", 1, pkg = "com.spotify.tv.android", pos = 200_000, dur = 210_000)
        assertEquals(listOf("Música"), WatchNextList.pick(listOf(song)) { true }.map { it.title })
    }

    @Test fun removedShowStaysOut() {
        val diaries = item("The Apothecary Diaries", 2)
        val hidden = HiddenShows.add(emptySet(), HiddenShows.key(diaries))
        val items = listOf(diaries, item("The Apothecary Diaries ", 3), item("Outra", 1))
        assertEquals(listOf("Outra"), HiddenShows.filter(items, hidden).map { it.title })
    }

    @Test fun sameNameInAnotherAppIsNotHidden() {
        val hidden = HiddenShows.add(emptySet(), HiddenShows.key(item("Naruto", 1)))
        assertEquals(1, HiddenShows.filter(listOf(item("Naruto", 1, pkg = "tv.pluto.android")), hidden).size)
    }

    @Test fun hiddenSavedAndReadBack() {
        val hidden = setOf("com.crunchyroll.crunchyroid|The Apothecary Diaries", "com.google.android.youtube.tv|Vídeo")
        assertEquals(hidden, HiddenShows.parse(HiddenShows.format(hidden)))
        assertEquals(emptySet<String>(), HiddenShows.parse(null))
    }
}
