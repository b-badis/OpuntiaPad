package io.github.bbadis.opuntiapad

import android.content.Intent
import android.content.pm.PackageManager
import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * "Open with → OpuntiaPad" must be offered for every file extension of every supported language,
 * with the MIME type Android itself assigns to it (which is sometimes surprising: ".ts" is
 * "video/mp2ts", ".rs" is "application/rls-services+xml"…). Unknown extensions are reported by
 * file managers as "application/octet-stream".
 */
@RunWith(AndroidJUnit4::class)
class OpenWithCoverageTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun everyLanguageExtensionOffersOpuntiaPad() {
        val app = context.applicationContext as OpuntiaPadApp
        val extensions = (app.catalog.languages.flatMap { it.extensions } + listOf("txt", "text", "log", "nfo")).distinct()
        val missing = extensions.mapNotNull { ext ->
            val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
            val intent = Intent(Intent.ACTION_VIEW).setDataAndType("content://com.example.files/document/sample.$ext".toUri(), mime)
            val handlers = context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            if (handlers.none { it.activityInfo.packageName == context.packageName }) ".$ext ($mime)" else null
        }
        assertTrue("Open with is not offered for: ${missing.joinToString()}", missing.isEmpty())
    }

    /** File managers may label files with their own types, a wildcard, or nothing at all. */
    @Test
    fun anyLabelOrNoLabelOffersOpuntiaPad() {
        val uri = "content://com.google.android.apps.nbu.files.provider/1/sample.yaml".toUri()
        val cases = listOf("*/*", "application/x-yaml", "text/x-yaml", "application/vnd.unknown", "image/png", null)
        val missing = cases.filter { type ->
            val intent = Intent(Intent.ACTION_VIEW).apply { if (type == null) data = uri else setDataAndType(uri, type) }
            context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
                .none { it.activityInfo.packageName == context.packageName }
        }
        assertTrue("Open with is not offered for types: $missing", missing.isEmpty())
    }

    /** Web links must never open in the editor. */
    @Test
    fun webLinksAreNotClaimed() {
        val intent = Intent(Intent.ACTION_VIEW, "https://example.com/data.json".toUri()).addCategory(Intent.CATEGORY_BROWSABLE)
        val handlers = context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        assertTrue(handlers.none { it.activityInfo.packageName == context.packageName })
    }
}
