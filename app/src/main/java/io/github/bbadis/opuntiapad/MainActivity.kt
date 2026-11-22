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
import io.github.bbadis.opuntiapad.ui.OpuntiaPadRoot
import java.io.File
import io.github.bbadis.opuntiapad.ui.MainViewModel

/**
 * Single activity. Handles "Open with" (VIEW/EDIT), "Share" (SEND / SEND_MULTIPLE) and
 * launcher intents; the UI itself is 100 % Compose.
 */
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    /** Set by the editor screen to receive Ctrl+key shortcuts from hardware keyboards. */
    var shortcutHandler: ((KeyEvent) -> Boolean)? = null

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && event.isCtrlPressed && event.repeatCount == 0 &&
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
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
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
}
