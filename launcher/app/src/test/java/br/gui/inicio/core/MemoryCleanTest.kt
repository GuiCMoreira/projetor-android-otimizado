package br.gui.inicio.core

import org.junit.Assert.assertEquals
import org.junit.Test

/** "Liberar memória": fecha só apps de uso parados; a tela inicial, o sistema e o projetor ficam. */
class MemoryCleanTest {
    private val own = "br.gui.inicio"

    @Test fun streamingAppsAreClosed() = assertEquals(
        listOf("tv.pluto.android", "com.google.android.youtube.tv", "com.wbd.stream"),
        MemoryClean.targets(listOf("tv.pluto.android", "com.google.android.youtube.tv", "com.wbd.stream"), own),
    )

    @Test fun launcherSystemAndProjectorStay() = assertEquals(
        emptyList<String>(),
        MemoryClean.targets(
            listOf(
                own, "android", "com.android.vending", "com.android.systemui", "com.google.android.gms",
                "com.google.android.gsf", "com.google.android.inputmethod.latin", "com.newlink.service",
                "com.sstar.systemcontrol", "com.zhiying.settings",
            ),
            own,
        ),
    )

    @Test fun repeatedPackagesOnlyOnce() =
        assertEquals(listOf("com.stremio.one"), MemoryClean.targets(listOf("com.stremio.one", "com.stremio.one"), own))

    @Test fun reportsFreedMegabytes() = assertEquals("Liberados 180 MB", MemoryClean.message(beforeKb = 250 * 1024, afterKb = 430 * 1024))

    @Test fun littleGainSaysMemoryWasFree() = assertEquals("A memória já estava livre", MemoryClean.message(beforeKb = 300 * 1024, afterKb = 305 * 1024))

    @Test fun lessMemoryAfterIsNotNegative() = assertEquals("A memória já estava livre", MemoryClean.message(beforeKb = 300 * 1024, afterKb = 290 * 1024))
}
