package io.github.bbadis.opuntiapad

import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.bbadis.opuntiapad.ui.MainViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Smoke tests for the ways other apps hand files and text to OpuntiaPad. */
@RunWith(AndroidJUnit4::class)
class OpenIntentTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun clearSession() {
        runBlocking { (context.applicationContext as OpuntiaPadApp).sessionStore.clear() }
    }

    private fun launch(intent: Intent, check: (MainViewModel) -> Unit) {
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            lateinit var vm: MainViewModel
            scenario.onActivity { vm = ViewModelProvider(it)[MainViewModel::class.java] }
            // Documents are added asynchronously (after the session restore): wait for them.
            val deadline = System.currentTimeMillis() + 20_000
            while (System.currentTimeMillis() < deadline) {
                var done = false
                scenario.onActivity { done = vm.tabs.isNotEmpty() }
                if (done) break
                Thread.sleep(100)
            }
            scenario.onActivity { check(vm) }
        }
    }

    @Test
    fun sharedTextOpensAsDetectedLanguage() {
        val intent = Intent(context, MainActivity::class.java)
            .setAction(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, "{\n  \"name\": \"opuntiapad\"\n}")
        launch(intent) { vm ->
            assertEquals(1, vm.tabs.size)
            assertEquals("json", vm.activeDocument?.language?.id)
        }
    }

    @Test
    fun selectedTextFromAnotherAppOpens() {
        val intent = Intent(context, MainActivity::class.java)
            .setAction(Intent.ACTION_PROCESS_TEXT)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_PROCESS_TEXT, "SELECT id FROM users WHERE active = 1;")
        launch(intent) { vm ->
            assertEquals(1, vm.tabs.size)
            assertEquals("sql", vm.activeDocument?.language?.id)
        }
    }

    @Test
    fun launcherStartsWithoutDocuments() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertEquals(0, ViewModelProvider(activity)[MainViewModel::class.java].tabs.size)
            }
        }
    }
}
