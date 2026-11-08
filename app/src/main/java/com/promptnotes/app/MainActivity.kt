package com.promptnotes.app

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.ClipboardManager
import android.widget.Toast
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import java.text.DateFormat
import java.util.Calendar

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent { PromptNotesApp(this) }
    }
}

private val Brand = Color(0xFFB85C3B)
private val Cream = Color(0xFFF8F6F2)
private val Ink = Color(0xFF24221F)
private val Muted = Color(0xFF77736D)

@Composable
private fun PromptNotesApp(activity: ComponentActivity) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("prompt_notes_native", 0) }
    var onboardingDone by remember { mutableStateOf(prefs.getBoolean("onboarding_done", false)) }
    var darkMode by remember { mutableStateOf(prefs.getBoolean("dark_mode", false)) }
    var selectedTab by rememberSaveable { mutableStateOf("home") }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    val database = remember {
        Room.databaseBuilder(context.applicationContext, NotesDatabase::class.java, "prompt-notes.db").build()
    }
    val vm: NotesViewModel = viewModel(factory = NotesViewModelFactory(database.noteDao()))
    val notes by vm.notes.collectAsStateWithLifecycle()
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    LaunchedEffect(darkMode) {
        prefs.edit().putBoolean("dark_mode", darkMode).apply()
        activity.window.statusBarColor = if (darkMode) android.graphics.Color.BLACK else android.graphics.Color.rgb(248, 246, 242)
        activity.window.navigationBarColor = if (darkMode) android.graphics.Color.BLACK else android.graphics.Color.rgb(248, 246, 242)
        WindowCompat.getInsetsController(activity.window, activity.window.decorView).isAppearanceLightStatusBars = !darkMode
        WindowCompat.getInsetsController(activity.window, activity.window.decorView).isAppearanceLightNavigationBars = !darkMode
    }

    val colors = if (darkMode) darkColorScheme(
        primary = Color(0xFFE6A184), secondary = Color(0xFFD4B49F),
        background = Color(0xFF11100F), surface = Color(0xFF1C1A18),
        onPrimary = Color(0xFF35180E), onBackground = Color(0xFFF3EEE9),
        onSurface = Color(0xFFF3EEE9)
    ) else lightColorScheme(
        primary = Brand, secondary = Color(0xFF795548),
        background = Cream, surface = Color(0xFFFFFEFC),
        onPrimary = Color.White, onBackground = Ink, onSurface = Ink
    )

    MaterialTheme(colorScheme = colors) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when {
                !onboardingDone -> OnboardingScreen(
                    onEnableNotifications = {
                        if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        prefs.edit().putBoolean("onboarding_done", true).apply()
                        onboardingDone = true
                    },
                    onSkip = {
                        prefs.edit().putBoolean("onboarding_done", true).apply()
                        onboardingDone = true
                    }
                )
                editingId != null -> {
                    val current = notes.firstOrNull { it.id == editingId }
                    EditorScreen(
                        note = current,
                        isNew = editingId == 0L,
                        onBack = { editingId = null },
                        onSave = { title, body, folder ->
                            vm.save(editingId?.takeIf { it > 0 }, title, body, folder)
                            editingId = null
                        },
                        onReminder = { title, body ->
                            val id = editingId
                            if (id != null && id > 0) {
                                val now = Calendar.getInstance()
                                val dateDialog = DatePickerDialog(context, { _, year, month, day ->
                                    val timeDialog = TimePickerDialog(context, { _, hour, minute ->
                                        val chosen = Calendar.getInstance().apply {
                                            set(year, month, day, hour, minute, 0)
                                            set(Calendar.MILLISECOND, 0)
                                        }.timeInMillis
                                        if (chosen > System.currentTimeMillis()) {
                                            vm.setReminder(id, chosen)
                                            ReminderWorker.schedule(context, NoteEntity(id = id, title = title, content = body), chosen)
                                        }
                                    }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), false)
                                    timeDialog.show()
                                }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH))
                                dateDialog.datePicker.minDate = System.currentTimeMillis()
                                dateDialog.show()
                            }
                        }
                    )
                }
                else -> MainShell(
                    selectedTab = selectedTab,
                    notes = notes,
                    onTab = { selectedTab = it },
                    onNew = { editingId = 0L },
                    onEdit = { editingId = it },
                    onPin = vm::togglePinned,
                    onDuplicate = vm::duplicate,
                    onDelete = vm::delete,
                    darkMode = darkMode,
                    onDarkMode = { darkMode = it },
                    onEnableNotifications = {
                        if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        else ReminderWorker.createChannel(context)
                    },
                    onImportMarkdown = vm::importMarkdown
                )
        }
    }
}

@Composable
private fun OnboardingScreen(onEnableNotifications: () -> Unit, onSkip: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp).imePadding(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier.size(72.dp).background(Brand, RoundedCornerShape(22.dp)),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Default.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(34.dp)) }
        Spacer(Modifier.height(28.dp))
        Text("PROMPT NOTES", color = Brand, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text("Your thoughts,\nbeautifully organised.", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))
        Text(
            "A calm space for ideas, plans and everything worth remembering. Your notes stay on this device.",
            style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(30.dp))
        FeatureLine("Private notes stored on your device")
        FeatureLine("Pin, organise and search your ideas")
        FeatureLine("Set reminders so important thoughts return")
        FeatureLine("AI tools will be available after secure setup")
        Spacer(Modifier.height(36.dp))
        Button(onClick = onEnableNotifications, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)) {
            Text("Get started")
        }
        TextButton(onClick = onSkip, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Not now")
        }
        Spacer(Modifier.height(12.dp))
        Text("A product of CRYONA", modifier = Modifier.align(Alignment.CenterHorizontally), color = Muted, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun FeatureLine(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
        Icon(Icons.Default.Check, null, tint = Brand, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainShell(
    selectedTab: String, notes: List<NoteEntity>, onTab: (String) -> Unit,
    onNew: () -> Unit, onEdit: (Long) -> Unit, onPin: (NoteEntity) -> Unit,
    onDuplicate: (NoteEntity) -> Unit, onDelete: (NoteEntity) -> Unit,
    darkMode: Boolean, onDarkMode: (Boolean) -> Unit, onEnableNotifications: () -> Unit,
    onImportMarkdown: (String) -> Unit
) {
    val view = LocalView.current
    fun tap(action: () -> Unit) {
        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        action()
    }
    Scaffold(
        contentWindowInsets = WindowInsets.navigationBars,
        topBar = {
            TopAppBar(title = {
                Column {
                    Text("Prompt Notes", fontWeight = FontWeight.Bold)
                    Text("A product of Cryona", style = MaterialTheme.typography.labelSmall, color = Muted)
                }
            })
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selectedTab == "home", { tap { onTab("home") } }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Notes") })
                NavigationBarItem(selectedTab == "ai", { tap { onTab("ai") } }, icon = { Icon(Icons.Default.AutoAwesome, null) }, label = { Text("AI Studio") })
                NavigationBarItem(selectedTab == "settings", { tap { onTab("settings") } }, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("Settings") })
            }
        },
        floatingActionButton = {
            if (selectedTab == "home") FloatingActionButton(onClick = { tap(onNew) }, containerColor = Brand, contentColor = Color.White) {
                Icon(Icons.Default.Add, contentDescription = "Create note")
            }
        }
    ) { padding ->
        when (selectedTab) {
            "home" -> NotesHome(
                notes, Modifier.padding(padding), onEdit, onPin, onDuplicate, onDelete
            )
            "ai" -> AiStudio(Modifier.padding(padding))
            else -> SettingsScreen(
                modifier = Modifier.padding(padding), darkMode = darkMode, onDarkMode = onDarkMode,
                notes = notes, onEnableNotifications = onEnableNotifications,
                onImportMarkdown = onImportMarkdown
            )
        }
    }
}

@Composable
private fun NotesHome(
    notes: List<NoteEntity>, modifier: Modifier, onEdit: (Long) -> Unit,
    onPin: (NoteEntity) -> Unit, onDuplicate: (NoteEntity) -> Unit, onDelete: (NoteEntity) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = notes.filter {
        query.isBlank() || it.title.contains(query, true) || it.content.contains(query, true) || it.folder.contains(query, true)
    }
    Column(modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search your notes") }, leadingIcon = { Icon(Icons.Default.Search, null) },
            singleLine = true, shape = RoundedCornerShape(16.dp)
        )
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(if (query.isBlank()) "Your workspace" else "Search results", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("${filtered.size} ${if (filtered.size == 1) "note" else "notes"}", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            if (notes.any { it.pinned }) Text("PINNED FIRST", color = Brand, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AutoAwesome, null, tint = Brand, modifier = Modifier.size(42.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(if (query.isBlank()) "A fresh page" else "No matching notes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(if (query.isBlank()) "Tap + to capture your first idea." else "Try another search.", color = Muted)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)) {
                items(filtered, key = { it.id }) { note ->
                    NoteCard(note, onEdit, onPin, onDuplicate, onDelete)
                }
            }
        }
    }
}

@Composable
private fun NoteCard(
    note: NoteEntity, onEdit: (Long) -> Unit, onPin: (NoteEntity) -> Unit,
    onDuplicate: (NoteEntity) -> Unit, onDelete: (NoteEntity) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onEdit(note.id) },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(note.title.ifBlank { "Untitled note" }, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (note.pinned) Icon(Icons.Default.PushPin, null, tint = Brand, modifier = Modifier.size(17.dp))
            }
            if (note.content.isNotBlank()) {
                Spacer(Modifier.height(7.dp))
                Text(note.content, color = Muted, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(note.folder.uppercase(), color = Brand, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { onPin(note) }, modifier = Modifier.size(34.dp)) { Icon(Icons.Default.PushPin, "Pin note", modifier = Modifier.size(18.dp), tint = if (note.pinned) Brand else Muted) }
                IconButton(onClick = { onDuplicate(note) }, modifier = Modifier.size(34.dp)) { Icon(Icons.Default.ContentCopy, "Duplicate note", modifier = Modifier.size(18.dp), tint = Muted) }
                IconButton(onClick = { onDelete(note) }, modifier = Modifier.size(34.dp)) { Icon(Icons.Default.Delete, "Delete note", modifier = Modifier.size(18.dp), tint = Muted) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorScreen(
    note: NoteEntity?, isNew: Boolean, onBack: () -> Unit,
    onSave: (String, String, String) -> Unit, onReminder: (String, String) -> Unit
) {
    var title by remember(note?.id) { mutableStateOf(note?.title.orEmpty()) }
    var body by remember(note?.id) { mutableStateOf(note?.content.orEmpty()) }
    var folder by remember(note?.id) { mutableStateOf(note?.folder ?: "Personal") }
    var showDiscard by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "New note" else "Edit note") },
                navigationIcon = { IconButton(onClick = { showDiscard = true }) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = {
                    if (!isNew) IconButton(onClick = { onReminder(title, body) }) { Icon(Icons.Default.Event, "Set reminder") }
                    TextButton(onClick = { onSave(title, body, folder) }) { Text("Save", fontWeight = FontWeight.Bold) }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp).imePadding()) {
            OutlinedTextField(value = title, onValueChange = { title = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Note title") }, textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), singleLine = false)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = folder, onValueChange = { folder = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Folder") }, singleLine = true)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = body, onValueChange = { body = it }, modifier = Modifier.fillMaxWidth().weight(1f),
                placeholder = { Text("Start writing…") }, textStyle = MaterialTheme.typography.bodyLarge,
                shape = RoundedCornerShape(16.dp)
            )
            if (!isNew && note?.reminderAt != null) {
                Text("Reminder set for ${DateFormat.getDateTimeInstance().format(note.reminderAt)}", color = Brand, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(vertical = 8.dp))
            }
            Spacer(Modifier.height(12.dp))
        }
    }
    if (showDiscard) {
        AlertDialog(
            onDismissRequest = { showDiscard = false },
            title = { Text("Leave this note?") },
            text = { Text("Any unsaved changes on this screen will be lost.") },
            confirmButton = { TextButton(onClick = { showDiscard = false; onBack() }) { Text("Leave") } },
            dismissButton = { TextButton(onClick = { showDiscard = false }) { Text("Keep editing") } }
        )
    }
}

@Composable
private fun AiStudio(modifier: Modifier) {
    data class ChatLine(val fromUser: Boolean, val text: String)
    val messages = remember { mutableStateListOf<ChatLine>() }
    var prompt by rememberSaveable { mutableStateOf("") }
    Column(modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Spacer(Modifier.height(12.dp))
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, null, tint = Brand)
                    Spacer(Modifier.width(10.dp))
                    Text("AI Studio", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Text("Your writing partner for ideas, summaries and clearer notes.", color = Muted)
                Spacer(Modifier.height(12.dp))
                Text("AI connection pending", color = Brand, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
                Text("The interface is ready. Responses will activate after the secure OpenRouter connection is configured.", style = MaterialTheme.typography.bodySmall, color = Muted)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("QUICK ACTIONS", color = Muted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "Rewrite" to "Help me rewrite this text clearly and naturally.",
                "Summarize" to "Summarize my note into concise key points.",
                "Generate" to "Generate a well-structured note about: ",
                "Continue" to "Continue writing from this point: ",
                "Change tone" to "Rewrite this in a more professional tone: ",
                "Create title" to "Suggest a clear, memorable title for this note."
            ).forEach { (label, promptText) ->
                AssistChip(onClick = { prompt = promptText }, label = { Text(label) })
            }
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)) {
            items(messages) { line ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (line.fromUser) Arrangement.End else Arrangement.Start) {
                    Surface(shape = RoundedCornerShape(16.dp), color = if (line.fromUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface) {
                        Text(line.text, modifier = Modifier.padding(13.dp), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        OutlinedTextField(
            value = prompt, onValueChange = { prompt = it }, modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Ask AI to help with your notes…") },
            trailingIcon = {
                IconButton(onClick = {
                    if (prompt.isNotBlank()) {
                        messages.add(ChatLine(true, prompt.trim()))
                        messages.add(ChatLine(false, "AI responses are not connected yet. Your message is ready for the OpenRouter integration."))
                        prompt = ""
                    }
                }) { Icon(Icons.Default.Send, "Send prompt", tint = Brand) }
            }, shape = RoundedCornerShape(18.dp)
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SettingsScreen(
    modifier: Modifier, darkMode: Boolean, onDarkMode: (Boolean) -> Unit,
    notes: List<NoteEntity>, onEnableNotifications: () -> Unit,
    onImportMarkdown: (String) -> Unit
) {
    val context = LocalContext.current
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }
                .getOrNull()?.let(onImportMarkdown)
        }
    }
    Column(modifier.fillMaxSize().padding(20.dp)) {
        Text("Preferences", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LightMode, null, tint = Brand)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Dark appearance", fontWeight = FontWeight.SemiBold)
                        Text("Use a comfortable low-light theme", color = Muted, style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = darkMode, onCheckedChange = onDarkMode)
                }
                Divider(Modifier.padding(vertical = 12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.NotificationsActive, null, tint = Brand)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Notifications", fontWeight = FontWeight.SemiBold)
                        Text("Allow scheduled note reminders", color = Muted, style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = onEnableNotifications) { Text("Enable") }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(16.dp)) {
                Text("Your data", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("${notes.size} notes stored locally on this device.", color = Muted, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = {
                    val backup = MarkdownBackup.format(notes)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Prompt Notes backup")
                        putExtra(Intent.EXTRA_TEXT, backup.ifBlank { "No notes to export." })
                    }
                    context.startActivity(Intent.createChooser(intent, "Export notes"))
                }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.ContentCopy, null); Spacer(Modifier.width(8.dp)); Text("Export notes as text")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { importLauncher.launch(arrayOf("text/markdown", "text/plain", "text/*")) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Import Markdown backup")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = {
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val backup = clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
                    if (backup.isBlank()) Toast.makeText(context, "No Markdown backup found on clipboard", Toast.LENGTH_SHORT).show()
                    else onImportMarkdown(backup)
                }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.ContentCopy, null); Spacer(Modifier.width(8.dp)); Text("Import from clipboard")
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Text("Prompt Notes", fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
        Text("A product of CRYONA · Version 2.0.0", color = Muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}
