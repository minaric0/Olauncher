package app.olauncher.helper

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.util.DisplayMetrics
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import app.olauncher.R
import app.olauncher.MainActivity
import app.olauncher.data.Prefs
import app.olauncher.databinding.AdapterAppDrawerBinding
import app.olauncher.databinding.DialogBaseBinding
import app.olauncher.databinding.FragmentHomeBinding
import app.olauncher.databinding.FragmentSettingsBinding
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CompactDisplayTest {
    private fun metrics(dpi: Int = 20, width: Int = 200, height: Int = 200) = DisplayMetrics().apply {
        setToDefaults()
        densityDpi = dpi
        density = dpi / 160f
        scaledDensity = density
        widthPixels = width
        heightPixels = height
    }

    private fun context(night: Boolean = false, korean: Boolean = false): Context {
        val app = RuntimeEnvironment.getApplication()
        val config = CompactDisplay.configuration(app.resources.configuration, metrics(), 1f).apply {
            uiMode = Configuration.UI_MODE_TYPE_NORMAL or
                if (night) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
            if (korean) setLocale(java.util.Locale.KOREAN)
        }
        return ContextThemeWrapper(app.createConfigurationContext(config), R.style.AppTheme).apply {
            theme.applyStyle(R.style.BoldFontOverlay, true)
        }
    }

    private fun layout(view: View, height: Int = 200) {
        view.measure(View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
        view.layout(0, 0, 200, height)
    }

    private fun descendants(view: View): List<View> = listOf(view) +
        if (view is ViewGroup) (0 until view.childCount).flatMap { descendants(view.getChildAt(it)) }
        else emptyList()

    private fun render(view: View, filename: String, night: Boolean = false) {
        val bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(if (night) Color.BLACK else Color.WHITE)
        view.draw(canvas)
        File("build/reports/compact-ui/$filename").apply { parentFile?.mkdirs() }.outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
    }

    @Test fun extremeDensityGetsReadableLocalViewportWithoutMutatingSystemConfiguration() {
        val base = Configuration().apply { densityDpi = 20; screenWidthDp = 1600; screenHeightDp = 1600 }
        val result = CompactDisplay.configuration(base, metrics(), 1f)
        assertEquals(80, result.densityDpi)
        assertEquals(400, result.screenWidthDp)
        assertEquals(400, result.screenHeightDp)
        assertEquals(400, result.smallestScreenWidthDp)
        assertEquals(20, base.densityDpi)
        assertEquals(1600, base.screenWidthDp)
    }

    @Test fun nearbyExtremeDensitiesHaveTheSameReadableViewport() {
        for (dpi in listOf(10, 20, 30, 40, 60, 79)) {
            assertEquals(80, CompactDisplay.configuration(Configuration(), metrics(dpi), 1f).densityDpi)
        }
    }

    @Test fun normalPhoneRetainsItsDensityAndUserTextAdjustment() {
        val base = Configuration().apply { densityDpi = 450; screenWidthDp = 384; screenHeightDp = 832 }
        val result = CompactDisplay.configuration(base, metrics(450, 1080, 2340), 1.2f)
        assertEquals(450, result.densityDpi)
        assertEquals(384, result.screenWidthDp)
        assertEquals(1.2f, result.fontScale, 0.001f)
    }

    @Test fun tinyScreenGesturesNeedOnlyThirtyPixels() {
        assertEquals(30, CompactDisplay.swipeDistance(metrics()))
        assertEquals(100, CompactDisplay.swipeDistance(metrics(450, 1080, 2340)))
    }

    @Test fun settingsTextIsFourteenPixelsAndRowsNeverOverlapInEitherTheme() {
        for (night in listOf(false, true)) {
            val binding = FragmentSettingsBinding.inflate(LayoutInflater.from(context(night)))
            binding.homeAppsNum.text = "4"
            binding.textSizeValue.text = "200%"
            binding.swipeLeftApp.text = "Poweramp"
            binding.swipeRightApp.text = "Settings"
            binding.compactBackHome.visibility = View.VISIBLE
            layout(binding.root)
            assertEquals(14f, binding.homeAppsNum.textSize, 0.1f)
            assertEquals(26f, binding.olauncherHiddenApps.textSize, 0.1f)
            assertEquals(0f, binding.homeAppsNum.shadowRadius, 0.01f)
            val rows = descendants(binding.root).filter { it.tag == "settingRow" }.map { it as ViewGroup }
            assertTrue(rows.size >= 13)
            for (row in rows) {
                val label = row.getChildAt(0) as TextView
                val value = row.getChildAt(1) as TextView
                assertTrue(label.text.toString(), label.bottom <= value.top)
                assertTrue(label.text.toString(), value.height >= 24)
                assertTrue(label.text.toString(), value.right <= row.width)
            }
            CompactDisplay.connectSettingRows(binding.scrollLayout)
            var clicked = false
            binding.textSizeValue.setOnClickListener { clicked = true }
            (binding.textSizeValue.parent as View).performClick()
            assertTrue(clicked)
            render(binding.root, "settings-${if (night) "dark" else "light"}-200.png", night)
            val scroll = binding.scrollView
            scroll.scrollTo(0, binding.homeAppsNum.top + (binding.homeAppsNum.parent as View).top)
        }
    }

    @Test fun koreanSettingsAlsoWrapWithoutCoveringControls() {
        val binding = FragmentSettingsBinding.inflate(LayoutInflater.from(context(korean = true)))
        binding.homeAppsNum.text = "4"
        binding.textSizeValue.text = "200%"
        layout(binding.root)
        for (row in descendants(binding.root).filter { it.tag == "settingRow" }.map { it as ViewGroup }) {
            assertTrue(row.getChildAt(0).bottom <= row.getChildAt(1).top)
        }
        render(binding.root, "settings-korean-200.png")
    }

    @Test fun drawerActionsKeepFullLabelsAndScrollRatherThanShrinking() {
        val binding = AdapterAppDrawerBinding.inflate(LayoutInflater.from(context()))
        binding.appTitle.text = "Poweramp"
        binding.appHideLayout.visibility = View.VISIBLE
        binding.appTitle.visibility = View.INVISIBLE
        binding.appRename.visibility = View.VISIBLE
        binding.appHide.text = "Hide"
        layout(binding.root, 42)
        assertEquals(12f, binding.appRename.textSize, 0.1f)
        val scrolling = descendants(binding.appHideLayout).filterIsInstance<android.widget.HorizontalScrollView>().single()
        assertTrue(scrolling.getChildAt(0).width > scrolling.width)
        for (text in listOf(binding.appDelete, binding.appRename, binding.appHide, binding.appInfo, binding.appMenuClose)) {
            assertEquals(0, text.layout.getEllipsisCount(0))
            assertTrue(text.text.toString(), text.height <= binding.root.height)
        }
    }

    @Test fun popupAndDialogTextAlsoUseTheLargerSizes() {
        val ctx = context()
        val attributes = ctx.obtainStyledAttributes(R.style.OlPopupMenuText, intArrayOf(android.R.attr.textSize))
        assertEquals(14f, attributes.getDimension(0, 0f), 0.1f)
        attributes.recycle()
        val binding = DialogBaseBinding.inflate(LayoutInflater.from(ctx))
        assertEquals(18f, binding.tvAction.textSize, 0.1f)
        assertEquals(14f, binding.tvMessage.textSize, 0.1f)
        assertEquals(26f, binding.tvTitle.textSize, 0.1f)
    }

    @Test fun fourHomeAppsFitAndEightStayReachableByScrolling() {
        val binding = FragmentHomeBinding.inflate(LayoutInflater.from(context(night = true)))
        val density = binding.root.resources.displayMetrics.density
        fun dp(value: Int) = (value * density + 0.5f).toInt()
        binding.compactNavigation.visibility = View.VISIBLE
        (binding.homeAppsScroll.layoutParams as android.widget.FrameLayout.LayoutParams).bottomMargin = dp(48)
        binding.homeAppsLayout.setPadding(dp(12), dp(8), dp(12), dp(8))
        val apps = listOf(binding.homeApp1, binding.homeApp2, binding.homeApp3, binding.homeApp4,
            binding.homeApp5, binding.homeApp6, binding.homeApp7, binding.homeApp8)
        for ((index, view) in apps.withIndex()) {
            view.text = listOf("Poweramp", "Termux", "Phone", "Settings")[index % 4]
            view.visibility = if (index < 4) View.VISIBLE else View.GONE
        }
        layout(binding.root)
        assertEquals(26f, binding.homeApp1.textSize, 0.1f)
        assertTrue(binding.homeApp4.bottom <= binding.homeAppsScroll.height)
        assertTrue(binding.compactNavigation.top >= binding.homeAppsScroll.bottom)
        render(binding.root, "home-dark-200.png", true)
        apps.forEach { it.visibility = View.VISIBLE }
        layout(binding.root)
        assertTrue(binding.homeApp8.bottom > binding.homeAppsScroll.height)
        binding.homeAppsScroll.scrollTo(0, binding.homeAppsLayout.height)
        assertTrue(binding.homeAppsScroll.scrollY > 0)
        assertTrue(binding.homeApp8.bottom - binding.homeAppsScroll.scrollY <= binding.homeAppsScroll.height)
    }

    @Test fun activityLaunchesWithLocalDensityAndVisibleNavigationWorks() {
        RuntimeEnvironment.setQualifiers("w1600dp-h1600dp-20dpi")
        val app = RuntimeEnvironment.getApplication()
        assertEquals(200, app.resources.displayMetrics.widthPixels)
        assertEquals(20, app.resources.configuration.densityDpi)
        Prefs(app).apply {
            firstOpen = false
            firstSettingsOpen = false
            appTheme = androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
        }
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity = controller.get()
            assertEquals(80, activity.resources.configuration.densityDpi)
            val apps = activity.findViewById<TextView>(R.id.compactApps)
            assertEquals(View.VISIBLE, apps.visibility)
            assertTrue(apps.performClick())
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            assertNotNull(activity.findViewById<View>(R.id.search))
            assertTrue(activity.findViewById<TextView>(R.id.compactDrawerHome).performClick())
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            assertTrue(activity.findViewById<TextView>(R.id.compactSettings).performClick())
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            assertNotNull(activity.findViewById<ScrollView>(R.id.scrollView))
            assertTrue(activity.findViewById<TextView>(R.id.compactBackHome).performClick())
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            assertNotNull(activity.findViewById<TextView>(R.id.compactApps))
        } finally {
            controller.pause().stop().destroy()
        }
    }
}
