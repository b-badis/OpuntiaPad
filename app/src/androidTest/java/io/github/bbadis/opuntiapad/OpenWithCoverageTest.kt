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
}
