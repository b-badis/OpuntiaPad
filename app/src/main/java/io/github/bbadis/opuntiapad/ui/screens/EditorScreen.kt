package io.github.bbadis.opuntiapad.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.KeyEvent
import android.webkit.MimeTypeMap
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.automirrored.outlined.NoteAdd
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.bbadis.opuntiapad.MainActivity
import io.github.bbadis.opuntiapad.R
import io.github.bbadis.opuntiapad.core.language.EditorTheme
import io.github.bbadis.opuntiapad.core.language.Language
import io.github.bbadis.opuntiapad.core.settings.EditorSettings
import io.github.bbadis.opuntiapad.core.text.LineEnding
import io.github.bbadis.opuntiapad.core.text.TextCodec
import io.github.bbadis.opuntiapad.editor.CodeEditorHost
import io.github.bbadis.opuntiapad.editor.EditorController
import io.github.bbadis.opuntiapad.editor.EditorDocument
import io.github.bbadis.opuntiapad.ui.MainViewModel
import io.github.bbadis.opuntiapad.ui.UiEvent
import io.github.bbadis.opuntiapad.ui.components.ChoiceDialog
import io.github.bbadis.opuntiapad.ui.components.DocumentTabs
import io.github.bbadis.opuntiapad.ui.components.DocumentsPanel
import io.github.bbadis.opuntiapad.ui.components.ExitDialog
import io.github.bbadis.opuntiapad.ui.components.GoToLineDialog
import io.github.bbadis.opuntiapad.ui.components.HomeContent
import io.github.bbadis.opuntiapad.ui.components.SearchPanel
import io.github.bbadis.opuntiapad.ui.components.StatusBar
import io.github.bbadis.opuntiapad.ui.components.SymbolBar
import io.github.bbadis.opuntiapad.ui.components.ChecksumsDialog
import io.github.bbadis.opuntiapad.ui.components.LargeFileBanner
import io.github.bbadis.opuntiapad.ui.components.ToolsSheet
import io.github.bbadis.opuntiapad.ui.components.UnsavedChangesDialog
import io.github.bbadis.opuntiapad.ui.tools.Checksums
import io.github.bbadis.opuntiapad.ui.tools.DocumentTools
import io.github.bbadis.opuntiapad.core.text.DocumentFormatter
import androidx.compose.material.icons.outlined.AutoFixHigh
import kotlinx.coroutines.launch

private enum class EditorDialog { NONE, GO_TO_LINE, LANGUAGE, ENCODING, LINE_ENDING, EXIT }

/** Width from which the files panel is shown permanently next to the editor. */
private val EXPANDED_WIDTH = 840.dp
private val MEDIUM_WIDTH = 600.dp

@Composable
fun EditorScreen(
    viewModel: MainViewModel,
    settings: EditorSettings,
    editorTheme: EditorTheme,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val controller = remember { EditorController() }
    val recents by viewModel.recentFiles.collectAsStateWithLifecycle()
    val document = viewModel.activeDocument

    var searchVisible by rememberSaveable { mutableStateOf(false) }
    var dialog by rememberSaveable { mutableStateOf(EditorDialog.NONE) }
    var toolsVisible by rememberSaveable { mutableStateOf(false) }
    var checksums by remember { mutableStateOf<Checksums?>(null) }
    val tools = remember(viewModel, controller) { DocumentTools(viewModel, controller, scope) { viewModel.settings.value } }
    var saveAsTarget by rememberSaveable { mutableLongStateOf(-1L) }

    val openLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEach { viewModel.open(it, persistPermission = true) }
    }
    val saveAsLauncher = rememberLauncherForActivityResult(SaveAsContract) { uri ->
        viewModel.saveAs(saveAsTarget, uri)
        saveAsTarget = -1L
    }
    val openFile = { openLauncher.launch(arrayOf("*/*")) }

    LaunchedEffect(viewModel) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is UiEvent.Message -> {
                    val args = event.text.args.toTypedArray()
                    val text = if (event.plural) {
                        resources.getQuantityString(event.text.res, (args.first() as Number).toInt(), *args)
                    } else {
                        resources.getString(event.text.res, *args)
                    }
                    snackbar.showSnackbar(text)
                }
                is UiEvent.RequestSaveAs -> {
                    saveAsTarget = event.documentId
                    saveAsLauncher.launch(SaveAsRequest(event.suggestedName, mimeFor(event.suggestedName, event.mimeType)))
                }
            }
        }
    }

    // Hardware keyboard shortcuts (Ctrl+S, Ctrl+F…). Ctrl+Z/Y/C/V/X/A are handled by the editor itself.
    DisposableEffect(activity, document) {
        val main = activity as? MainActivity
        main?.shortcutHandler = { event ->
            handleShortcut(event, viewModel, controller, tools, openFile, { searchVisible = true }, { dialog = EditorDialog.GO_TO_LINE })
        }
        onDispose { main?.shortcutHandler = null }
    }

    BackHandler(enabled = searchVisible) {
        controller.stopSearch()
        searchVisible = false
    }
    BackHandler(enabled = !searchVisible && viewModel.hasUnsavedChanges) { dialog = EditorDialog.EXIT }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val expanded = maxWidth >= EXPANDED_WIDTH
        val medium = maxWidth >= MEDIUM_WIDTH
        val drawerState = rememberDrawerState(DrawerValue.Closed)

        val panel = @Composable {
            DocumentsPanel(
                tabs = viewModel.tabs,
                activeIndex = viewModel.activeIndex,
                recents = recents,
                onSelectTab = {
                    viewModel.select(it)
                    scope.launch { drawerState.close() }
                },
                onCloseTab = viewModel::requestClose,
                onOpenRecent = {
                    viewModel.open(it.uri)
                    scope.launch { drawerState.close() }
                },
                onRemoveRecent = viewModel::removeRecent,
                onClearRecents = viewModel::clearRecents,
            )
        }

        val content = @Composable {
            Scaffold(
                topBar = {
                    EditorTopBar(
                        document = document,
                        controller = controller,
                        showMenuButton = !expanded,
                        medium = medium,
                        settings = settings,
                        onMenu = { scope.launch { drawerState.open() } },
                        onOpen = openFile,
                        onNew = viewModel::newFile,
                        onSave = { document?.let(viewModel::save) },
                        onSaveAs = { document?.let(viewModel::requestSaveAs) },
                        onClose = { document?.let(viewModel::requestClose) },
                        onSearch = { searchVisible = !searchVisible },
                        onGoToLine = { dialog = EditorDialog.GO_TO_LINE },
                        onLanguage = { dialog = EditorDialog.LANGUAGE },
                        onEncoding = { dialog = EditorDialog.ENCODING },
                        onLineEnding = { dialog = EditorDialog.LINE_ENDING },
                        onTools = { toolsVisible = true },
                        onToggleReadOnly = { document?.let(viewModel::toggleReadOnly) },
                        onToggleWordWrap = { viewModel.updateSettings { it.copy(wordWrap = !it.wordWrap) } },
                        onZoom = { delta ->
                            if (delta == 0f) viewModel.updateSettings { it.copy(fontSize = EditorSettings().fontSize) } else controller.zoomBy(delta)
                        },
                        onShare = { document?.let { share(context, it) } },
                        onSettings = onOpenSettings,
                        onAbout = onOpenAbout,
                    )
                },
                snackbarHost = { SnackbarHost(snackbar) },
            ) { padding ->
                Column(
                    Modifier
                        .padding(padding)
                        .consumeWindowInsets(padding)
                        .imePadding()
                        .fillMaxSize(),
                ) {
                    if (viewModel.loading || controller.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    if (viewModel.tabs.size > 1 || (viewModel.tabs.isNotEmpty() && medium)) {
                        DocumentTabs(viewModel.tabs, viewModel.activeIndex, viewModel::select, viewModel::requestClose)
                    }
                    AnimatedVisibility(visible = searchVisible && document != null) {
                        SearchPanel(controller, onClose = { searchVisible = false })
                    }
                    if (document != null && !document.highlighting && !document.language.isPlainText) {
                        LargeFileBanner(onEnable = { viewModel.setHighlighting(document, true) })
                    }
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        if (document != null) {
                            CodeEditorHost(
                                document = document,
                                settings = settings,
                                editorTheme = editorTheme,
                                grammarsReady = viewModel.grammarsReady,
                                controller = controller,
                                onFontSizeChanged = viewModel::onFontSizeChanged,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            HomeContent(
                                recents = recents,
                                showRecents = !expanded,
                                onOpen = openFile,
                                onNew = viewModel::newFile,
                                onOpenRecent = { viewModel.open(it.uri) },
                                onRemoveRecent = viewModel::removeRecent,
                                onClearRecents = viewModel::clearRecents,
                            )
                        }
                    }
                    if (document != null) {
                        if (settings.symbolBar && !document.readOnly) {
                            SymbolBar(
                                controller = controller,
                                onToggleComment = { tools.toggleComment(document) },
                                onDuplicate = { tools.duplicateLines(document) },
                                onMove = { delta -> tools.moveLines(document, delta) },
                            )
                        }
                        StatusBar(
                            document = document,
                            controller = controller,
                            onLanguageClick = { dialog = EditorDialog.LANGUAGE },
                            onEncodingClick = { dialog = EditorDialog.ENCODING },
                            onLineEndingClick = { dialog = EditorDialog.LINE_ENDING },
                        )
                    }
                }
            }
        }

        if (expanded) {
            Row(Modifier.fillMaxSize()) {
                Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.width(300.dp).fillMaxHeight()) {
                    Box(Modifier.padding(top = 24.dp)) { panel() }
                }
                VerticalDivider()
                Box(Modifier.weight(1f)) { content() }
            }
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                gesturesEnabled = drawerState.isOpen,
                drawerContent = { ModalDrawerSheet { panel() } },
            ) { content() }
        }
    }

    // --- Dialogs -------------------------------------------------------------------------------
    viewModel.pendingClose?.let { pending ->
        UnsavedChangesDialog(
            fileName = pending.name,
            onSave = viewModel::saveAndClose,
            onDiscard = viewModel::discardAndClose,
            onDismiss = viewModel::dismissClose,
        )
    }

    if (toolsVisible && document != null) {
        ToolsSheet(
            document = document,
            tools = tools,
            hasSelection = controller.hasSelection,
            onShowChecksums = { scope.launch { checksums = tools.checksums(document) } },
            onDismiss = { toolsVisible = false },
        )
    }
    checksums?.let { values ->
        ChecksumsDialog(
            checksums = values,
            onCopied = { viewModel.showMessage(R.string.msg_copied) },
            onDismiss = { checksums = null },
        )
    }

    val dismiss = { dialog = EditorDialog.NONE }
    when (dialog) {
        EditorDialog.GO_TO_LINE -> GoToLineDialog(
            lineCount = controller.lineCount,
            onGo = {
                controller.goToLine(it)
                dismiss()
            },
            onDismiss = dismiss,
        )
        EditorDialog.LANGUAGE -> document?.let { doc ->
            ChoiceDialog(
                title = stringResource(R.string.dialog_language_title),
                options = viewModel.catalog.detector.languages,
                selected = doc.language,
                label = Language::name,
                onSelect = {
                    viewModel.setLanguage(doc, it)
                    dismiss()
                },
                onDismiss = dismiss,
                searchable = true,
            )
        }
        EditorDialog.ENCODING -> document?.let { doc ->
            ChoiceDialog(
                title = stringResource(R.string.dialog_encoding_title),
                options = TextCodec.COMMON_CHARSETS,
                selected = TextCodec.COMMON_CHARSETS.firstOrNull { it.equals(doc.charset.name(), ignoreCase = true) },
                label = { it },
                onSelect = {
                    viewModel.reopenWithEncoding(doc, it)
                    dismiss()
                },
                onDismiss = dismiss,
            )
        }
        EditorDialog.LINE_ENDING -> document?.let { doc ->
            val labels = mapOf(
                LineEnding.LF to stringResource(R.string.line_ending_lf),
                LineEnding.CRLF to stringResource(R.string.line_ending_crlf),
                LineEnding.CR to stringResource(R.string.line_ending_cr),
            )
            ChoiceDialog(
                title = stringResource(R.string.dialog_line_endings_title),
                options = LineEnding.entries,
                selected = doc.lineEnding,
                label = { labels.getValue(it) },
                onSelect = {
                    viewModel.convertLineEndings(doc, it)
                    dismiss()
                },
                onDismiss = dismiss,
            )
        }
        EditorDialog.EXIT -> ExitDialog(
            onQuit = {
                dismiss()
                viewModel.closeAll()
                activity?.finish()
            },
            onDismiss = dismiss,
        )
        EditorDialog.NONE -> Unit
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorTopBar(
    document: EditorDocument?,
    controller: EditorController,
    showMenuButton: Boolean,
    medium: Boolean,
    settings: EditorSettings,
    onMenu: () -> Unit,
    onOpen: () -> Unit,
    onNew: () -> Unit,
    onSave: () -> Unit,
    onSaveAs: () -> Unit,
    onClose: () -> Unit,
    onSearch: () -> Unit,
    onGoToLine: () -> Unit,
    onLanguage: () -> Unit,
    onEncoding: () -> Unit,
    onLineEnding: () -> Unit,
    onTools: () -> Unit,
    onToggleReadOnly: () -> Unit,
    onToggleWordWrap: () -> Unit,
    onZoom: (Float) -> Unit,
    onShare: () -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    TopAppBar(
        navigationIcon = {
            if (showMenuButton) {
                IconButton(onClick = onMenu) { Icon(Icons.Filled.Menu, stringResource(R.string.action_menu)) }
            }
        },
        title = {
            Column {
                Text(
                    text = document?.let { (if (it.isDirty) "● " else "") + it.name } ?: stringResource(R.string.app_name),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                )
                if (document != null) {
                    Text(
                        text = document.language.name + if (document.readOnly) " · " + stringResource(R.string.status_read_only) else "",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        },
        actions = {
            if (document == null || medium) {
                IconButton(onClick = onOpen) { Icon(Icons.Outlined.FolderOpen, stringResource(R.string.action_open)) }
                if (medium) IconButton(onClick = onNew) { Icon(Icons.AutoMirrored.Outlined.NoteAdd, stringResource(R.string.action_new)) }
            }
            if (document != null) {
                if (medium) {
                    IconButton(onClick = { controller.undo() }, enabled = controller.canUndo) {
                        Icon(Icons.AutoMirrored.Filled.Undo, stringResource(R.string.action_undo))
                    }
                    IconButton(onClick = { controller.redo() }, enabled = controller.canRedo) {
                        Icon(Icons.AutoMirrored.Filled.Redo, stringResource(R.string.action_redo))
                    }
                }
                IconButton(onClick = onTools) { Icon(Icons.Outlined.AutoFixHigh, stringResource(R.string.tools_title)) }
                IconButton(onClick = onSearch) { Icon(Icons.Filled.Search, stringResource(R.string.action_search)) }
                IconButton(onClick = onSave, enabled = document.isDirty || document.isUntitled) {
                    Icon(Icons.Outlined.Save, stringResource(R.string.action_save))
                }
            }
            Box {
                IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, stringResource(R.string.action_more)) }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    val item = @Composable { label: String, enabled: Boolean, action: () -> Unit ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            enabled = enabled,
                            onClick = {
                                menuOpen = false
                                action()
                            },
                        )
                    }
                    val toggle = @Composable { label: String, checked: Boolean, action: () -> Unit ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            trailingIcon = { Checkbox(checked = checked, onCheckedChange = null) },
                            onClick = {
                                menuOpen = false
                                action()
                            },
                        )
                    }
                    item(stringResource(R.string.action_new), true, onNew)
                    item(stringResource(R.string.action_open), true, onOpen)
                    if (document != null) {
                        item(stringResource(R.string.action_save), true, onSave)
                        item(stringResource(R.string.action_save_as), true, onSaveAs)
                        item(stringResource(R.string.action_close_tab), true, onClose)
                        HorizontalDivider()
                        if (!medium) {
                            item(stringResource(R.string.action_undo), controller.canUndo) { controller.undo() }
                            item(stringResource(R.string.action_redo), controller.canRedo) { controller.redo() }
                        }
                        item(stringResource(R.string.action_go_to_line), true, onGoToLine)
                        item(stringResource(R.string.action_select_all), true) { controller.selectAll() }
                        item(stringResource(R.string.tools_title), true, onTools)
                        HorizontalDivider()
                        item(stringResource(R.string.action_language, document.language.name), true, onLanguage)
                        item(stringResource(R.string.action_encoding, document.charset.name()), !document.isUntitled, onEncoding)
                        item(stringResource(R.string.action_line_endings, document.lineEnding.label), !document.readOnly, onLineEnding)
                        toggle(stringResource(R.string.action_read_only), document.readOnly, onToggleReadOnly)
                        toggle(stringResource(R.string.action_word_wrap), settings.wordWrap, onToggleWordWrap)
                        HorizontalDivider()
                        item(stringResource(R.string.action_zoom_in), true) { onZoom(2f) }
                        item(stringResource(R.string.action_zoom_out), true) { onZoom(-2f) }
                        item(stringResource(R.string.action_zoom_reset), true) { onZoom(0f) }
                        item(stringResource(R.string.action_share), true, onShare)
                    }
                    HorizontalDivider()
                    item(stringResource(R.string.action_settings), true, onSettings)
                    item(stringResource(R.string.action_about), true, onAbout)
                }
            }
        },
    )
}


private fun handleShortcut(
    event: KeyEvent,
    viewModel: MainViewModel,
    controller: EditorController,
    tools: DocumentTools,
    openFile: () -> Unit,
    showSearch: () -> Unit,
    showGoToLine: () -> Unit,
): Boolean {
    val doc = viewModel.activeDocument
    // Alt+↑ / Alt+↓ move the current lines (like most desktop editors).
    if (event.isAltPressed && !event.isCtrlPressed) {
        val delta = when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> -1
            KeyEvent.KEYCODE_DPAD_DOWN -> 1
            else -> return false
        }
        if (doc == null || doc.readOnly) return false
        tools.moveLines(doc, delta)
        return true
    }
    if (!event.isCtrlPressed) return false
    return when (event.keyCode) {
        KeyEvent.KEYCODE_SLASH -> { if (doc != null && !doc.readOnly) tools.toggleComment(doc); doc != null }
        KeyEvent.KEYCODE_D -> { if (doc != null && !doc.readOnly) tools.duplicateLines(doc); doc != null }
        KeyEvent.KEYCODE_F if event.isShiftPressed && doc != null && DocumentFormatter.canFormat(doc.language.id) -> {
            if (!doc.readOnly) tools.format(doc)
            true
        }
        KeyEvent.KEYCODE_S -> {
            doc?.let { if (event.isShiftPressed) viewModel.requestSaveAs(it) else viewModel.save(it) }
            true
        }
        KeyEvent.KEYCODE_O -> { openFile(); true }
        KeyEvent.KEYCODE_N -> { viewModel.newFile(); true }
        KeyEvent.KEYCODE_W -> { doc?.let(viewModel::requestClose); true }
        KeyEvent.KEYCODE_F, KeyEvent.KEYCODE_H -> { if (doc != null) showSearch(); doc != null }
        KeyEvent.KEYCODE_G -> { if (doc != null) showGoToLine(); doc != null }
        KeyEvent.KEYCODE_PLUS, KeyEvent.KEYCODE_EQUALS, KeyEvent.KEYCODE_NUMPAD_ADD -> { controller.zoomBy(2f); true }
        KeyEvent.KEYCODE_MINUS, KeyEvent.KEYCODE_NUMPAD_SUBTRACT -> { controller.zoomBy(-2f); true }
        KeyEvent.KEYCODE_TAB -> {
            if (viewModel.tabs.size > 1) {
                val step = if (event.isShiftPressed) -1 else 1
                viewModel.select(Math.floorMod(viewModel.activeIndex + step, viewModel.tabs.size))
                true
            } else {
                false
            }
        }
        else -> false
    }
}

private fun share(context: Context, document: EditorDocument) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        val uri = document.uri
        if (uri != null && !document.isDirty) {
            type = document.mimeType ?: "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } else {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, document.content.toString())
        }
        putExtra(Intent.EXTRA_SUBJECT, document.name)
        putExtra(Intent.EXTRA_TITLE, document.name)
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_chooser_title)))
}

private fun mimeFor(fileName: String, fallback: String): String {
    val ext = fileName.substringAfterLast('.', "").lowercase()
    // An explicit, extension-matching MIME type stops pickers from appending ".txt".
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
        ?: if (ext.isEmpty()) fallback else "application/octet-stream"
}

private data class SaveAsRequest(val fileName: String, val mimeType: String)

/** Like [ActivityResultContracts.CreateDocument], but with a per-request MIME type. */
private object SaveAsContract : ActivityResultContract<SaveAsRequest, Uri?>() {
    override fun createIntent(context: Context, input: SaveAsRequest): Intent =
        Intent(Intent.ACTION_CREATE_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType(input.mimeType)
            .putExtra(Intent.EXTRA_TITLE, input.fileName)

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        if (resultCode == Activity.RESULT_OK) intent?.data else null
}
