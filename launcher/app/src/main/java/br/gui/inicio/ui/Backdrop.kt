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
