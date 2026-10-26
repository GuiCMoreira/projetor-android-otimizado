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
