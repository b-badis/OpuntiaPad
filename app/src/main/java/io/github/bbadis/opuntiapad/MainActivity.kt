package io.github.bbadis.opuntiapad

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import io.github.bbadis.opuntiapad.ui.OpuntiaPadRoot
import java.io.File
import io.github.bbadis.opuntiapad.ui.MainViewModel

/**
 * Single activity. Handles "Open with" (VIEW/EDIT), "Share" (SEND / SEND_MULTIPLE) and
 * launcher intents; the UI itself is 100 % Compose.
 */
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    /** Set by the editor screen to receive Ctrl/Alt+key shortcuts from hardware keyboards. */
    var shortcutHandler: ((KeyEvent) -> Boolean)? = null

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && (event.isCtrlPressed || event.isAltPressed) &&
            shortcutHandler?.invoke(event) == true
        ) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleIntent(intent)
        setContent { OpuntiaPadRoot(viewModel) }
        publishShortcuts()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onStop() {
        super.onStop()
        // Android may close the app while it's in the background: keep tabs and unsaved changes.
        viewModel.saveSession()
    }

    /** Long-press launcher shortcut: "New file". */
    private fun publishShortcuts() {
        val newFile = ShortcutInfoCompat.Builder(this, SHORTCUT_NEW_FILE)
            .setShortLabel(getString(R.string.action_new))
            .setIcon(IconCompat.createWithResource(this, R.drawable.ic_shortcut_new_file))
            .setIntent(Intent(this, MainActivity::class.java).setAction(ACTION_NEW_FILE))
            .build()
        runCatching { ShortcutManagerCompat.setDynamicShortcuts(this, listOf(newFile)) }
    }

    // Only URIs are read from incoming intents; nothing is ever re-launched from them.
    @SuppressLint("UnsafeIntentLaunch")
    private fun handleIntent(intent: Intent?) {
        intent ?: return
        when (intent.action) {
            Intent.ACTION_VIEW, Intent.ACTION_EDIT -> intent.data?.let(::openExternal)
            Intent.ACTION_SEND -> {
                val stream = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                if (stream != null) {
                    openExternal(stream)
                } else {
                    intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.let {
                        viewModel.openText(it.toString(), intent.getStringExtra(Intent.EXTRA_SUBJECT) ?: intent.getStringExtra(Intent.EXTRA_TITLE))
                    }
                }
            }
            ACTION_NEW_FILE -> {
                ShortcutManagerCompat.reportShortcutUsed(this, SHORTCUT_NEW_FILE)
                viewModel.newFile()
            }
            // "Open in OpuntiaPad" in the text-selection menu of other apps.
            Intent.ACTION_PROCESS_TEXT -> intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.let {
                viewModel.openText(it.toString(), null)
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                    ?.forEach(::openExternal)
            }
        }
    }

    /**
     * Opens a URI received from another app. `file://` URIs pointing into OpuntiaPad's private
     * storage are refused so that a malicious app cannot make OpuntiaPad expose its own data.
     */
    private fun openExternal(uri: Uri) {
        if (uri.scheme == ContentResolver.SCHEME_FILE) {
            val path = uri.path?.let { runCatching { File(it).canonicalPath }.getOrNull() } ?: return
            val privateDir = runCatching { File(applicationInfo.dataDir).canonicalPath }.getOrNull() ?: return
            if (path.startsWith(privateDir)) return
        }
        viewModel.open(uri)
    }

    private companion object {
        const val ACTION_NEW_FILE = "io.github.bbadis.opuntiapad.action.NEW_FILE"
        const val SHORTCUT_NEW_FILE = "new_file"
    }
}
