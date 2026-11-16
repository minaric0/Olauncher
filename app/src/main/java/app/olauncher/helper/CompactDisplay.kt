package app.olauncher.helper

import android.content.res.Configuration
import android.util.DisplayMetrics
import android.view.ViewGroup
import kotlin.math.min
import kotlin.math.roundToInt

/** Keeps this launcher's UI legible when wm density is far below Android's usual range. */
internal object CompactDisplay {
    fun isTiny(metrics: DisplayMetrics): Boolean =
        min(metrics.widthPixels, metrics.heightPixels) in 1..320

    fun configuration(base: Configuration, metrics: DisplayMetrics, textScale: Float): Configuration =
        Configuration(base).apply {
            fontScale = textScale
            if (isTiny(metrics) && metrics.densityDpi in 1 until 80) {
                // 200 px becomes 400 dp: 28 sp settings text renders at 14 px at 1x.
                // This is local to the activity; no system density setting is written.
                densityDpi = 80
                screenWidthDp = (metrics.widthPixels * 160f / densityDpi).roundToInt()
                screenHeightDp = (metrics.heightPixels * 160f / densityDpi).roundToInt()
                smallestScreenWidthDp = min(screenWidthDp, screenHeightDp)
                screenLayout = (screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK.inv()) or
                    Configuration.SCREENLAYOUT_SIZE_NORMAL
            }
        }

    fun swipeDistance(metrics: DisplayMetrics): Int =
        if (isTiny(metrics)) (min(metrics.widthPixels, metrics.heightPixels) * 0.15f)
            .roundToInt().coerceIn(20, 48) else 100

    fun connectSettingRows(group: ViewGroup) {
        if (group.tag == "settingRow" && group.childCount == 2) {
            val control = group.getChildAt(1)
            group.setOnClickListener { control.performClick() }
        }
        for (index in 0 until group.childCount) {
            (group.getChildAt(index) as? ViewGroup)?.let(::connectSettingRows)
        }
    }
}
