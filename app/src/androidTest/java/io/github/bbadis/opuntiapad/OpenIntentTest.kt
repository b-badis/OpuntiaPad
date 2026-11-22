package io.github.bbadis.opuntiapad

import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.bbadis.opuntiapad.ui.MainViewModel
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Smoke tests for the ways other apps hand files to OpuntiaPad. */
@RunWith(AndroidJUnit4::class)
class OpenIntentTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun sharedTextOpensAsDetectedLanguage() {
        val intent = Intent(context, MainActivity::class.java)
            .setAction(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, "{\n  \"name\": \"codexa\"\n}")
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val vm = ViewModelProvider(activity)[MainViewModel::class.java]
                assertEquals(1, vm.tabs.size)
                assertEquals("json", vm.activeDocument?.language?.id)
            }
        }
    }

    @Test
    fun launcherStartsWithoutDocuments() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val vm = ViewModelProvider(activity)[MainViewModel::class.java]
                assertEquals(0, vm.tabs.size)
            }
        }
    }
}
