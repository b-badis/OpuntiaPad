package io.github.bbadis.opuntiapad

import android.app.Application
import io.github.bbadis.opuntiapad.core.files.DocumentRepository
import io.github.bbadis.opuntiapad.core.language.LanguageCatalog
import io.github.bbadis.opuntiapad.core.settings.SettingsRepository
import io.github.bbadis.opuntiapad.editor.TextMateSupport

/** Application entry point; holds the (few) app-wide singletons. */
class OpuntiaPadApp : Application() {

    val settingsRepository by lazy { SettingsRepository(this) }
    val documentRepository by lazy { DocumentRepository(this) }
    val catalog by lazy { LanguageCatalog.load(assets) }

    override fun onCreate() {
        super.onCreate()
        // Parse grammars in the background so the first file opens highlighted.
        TextMateSupport.initialize(this)
    }
}
