package com.example.ui

import android.app.Application
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

// Cosmic Visual Night Colors
val DeepSableBack = Color(0xFF0F0E17)
val CardSlateBack = Color(0xFF161626)
val BrightLime = Color(0xFF10B981)
val WarmRose = Color(0xFFEF4444)
val SunsetGold = Color(0xFFF59E0B)
val LightElectricBlue = Color(0xFF3B82F6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebCopilotApp(viewModel: WebCopilotViewModel) {
    val context = LocalContext.current
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val currentProject by viewModel.currentProject.collectAsStateWithLifecycle()
    val backups by viewModel.backups.collectAsStateWithLifecycle()
    val fileTree by viewModel.fileTree.collectAsStateWithLifecycle()
    val openedFile by viewModel.openedFilePath.collectAsStateWithLifecycle()
    val editorContent by viewModel.editorContent.collectAsStateWithLifecycle()
    val analysisResult by viewModel.analysisResult.collectAsStateWithLifecycle()
    val proposedFix by viewModel.proposedFixForReview.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val nextActionAdvice by viewModel.nextRecommendedAction.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showProjectSelector by remember { mutableStateOf(false) }
    var activeCategoryTab by remember { mutableStateOf("editor") } // "explorer", "editor", "core"
    var coreSubTab by remember { mutableStateOf("analysis") } // "analysis", "generators", "chat"

    // Launcher to select `.zip` folder uploaded by users
    val zipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    viewModel.createProjectFromUserZip("Uploaded ZIP File", inputStream)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Launcher to export & save completed ZIP
    val exportZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    viewModel.downloadProjectZip(out)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSableBack),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(SunsetGold.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🤖", fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "WebAI Copilot",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Secure Local Web Studio",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }
                },
                actions = {
                    // Export Zip Action Toolbar
                    IconButton(
                        onClick = {
                            val activeName = currentProject?.name ?: "web_project"
                            val cleanName = activeName.replace("\\s+".toRegex(), "_") + "_fixed.zip"
                            exportZipLauncher.launch(cleanName)
                        },
                        enabled = currentProject != null,
                        modifier = Modifier.testTag("downloads_zip_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export repaired project as zip",
                            tint = if (currentProject != null) SunsetGold else Color.White.copy(alpha = 0.2f)
                        )
                    }

                    // Zip Upload selector Icon
                    IconButton(
                        onClick = { zipPickerLauncher.launch("application/zip") },
                        modifier = Modifier.testTag("upload_zip_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Upload personal web project .zip",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CardSlateBack,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            // Adaptive Navigation Bar for Mobile Screens
            NavigationBar(
                containerColor = CardSlateBack,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = activeCategoryTab == "explorer",
                    onClick = { activeCategoryTab = "explorer" },
                    icon = { Icon(Icons.Default.Menu, contentDescription = "Project Trees") },
                    label = { Text("Explorer") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SunsetGold,
                        selectedTextColor = SunsetGold,
                        unselectedIconColor = Color.White.copy(alpha = 0.4f),
                        unselectedTextColor = Color.White.copy(alpha = 0.4f),
                        indicatorColor = SunsetGold.copy(alpha = 0.12f)
                    )
                )

                NavigationBarItem(
                    selected = activeCategoryTab == "editor",
                    onClick = { activeCategoryTab = "editor" },
                    icon = { Icon(Icons.Default.Build, contentDescription = "Source Workspace") },
                    label = { Text("Editor Tab") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SunsetGold,
                        selectedTextColor = SunsetGold,
                        unselectedIconColor = Color.White.copy(alpha = 0.4f),
                        unselectedTextColor = Color.White.copy(alpha = 0.4f),
                        indicatorColor = SunsetGold.copy(alpha = 0.12f)
                    )
                )

                NavigationBarItem(
                    selected = activeCategoryTab == "core",
                    onClick = { activeCategoryTab = "core" },
                    icon = { Icon(Icons.Default.Send, contentDescription = "Analysis Control Center") },
                    label = { Text("AI Copilot") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SunsetGold,
                        selectedTextColor = SunsetGold,
                        unselectedIconColor = Color.White.copy(alpha = 0.4f),
                        unselectedTextColor = Color.White.copy(alpha = 0.4f),
                        indicatorColor = SunsetGold.copy(alpha = 0.12f)
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DeepSableBack)
        ) {
            // Outer background subtle ambient mesh
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(LightElectricBlue.copy(alpha = 0.08f), Color.Transparent),
                        center = Offset(size.width * 0.1f, size.height * 0.8f),
                        radius = size.width * 0.6f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(SunsetGold.copy(alpha = 0.11f), Color.Transparent),
                        center = Offset(size.width * 0.9f, size.height * 0.1f),
                        radius = size.width * 0.7f
                    )
                )
            }

            Column(modifier = Modifier.fillMaxSize()) {
                // Top Status Triage Panel (What the app behaves or plans next)
                StatusTriageCard(
                    project = currentProject,
                    advice = nextActionAdvice,
                    onSelectorClick = { showProjectSelector = true }
                )

                Divider(color = Color.White.copy(alpha = 0.06f))

                // Primary Column Content Switch
                Box(modifier = Modifier.weight(1f)) {
                    when (activeCategoryTab) {
                        "explorer" -> {
                            ExplorerSubpanel(
                                fileTree = fileTree,
                                currentProject = currentProject,
                                activeFile = openedFile,
                                onFileSelected = { viewModel.openFileInEditor(it) },
                                onAddFile = { viewModel.createNewFile(it) },
                                onDeleteFile = { viewModel.removeFileFromProject(it) }
                            )
                        }
                        "editor" -> {
                            EditorAndPreviewSubpanel(
                                activeFile = openedFile,
                                content = editorContent,
                                onContentChanged = { viewModel.updateActiveFileContent(it) }
                            )
                        }
                        "core" -> {
                            CoreControlRoomPanel(
                                subTab = coreSubTab,
                                onSubTabChanged = { coreSubTab = it },
                                result = analysisResult,
                                onScanClick = { viewModel.runOfflineAnalysis() },
                                onApplyIssueFix = { fix -> viewModel.stageProposedFix(fix) },
                                chatMessages = chatMessages,
                                onSendMessage = { text -> viewModel.sendCopilotMessage(text) },
                                backups = backups,
                                onRevertBackup = { bak -> viewModel.revertBackup(bak) },
                                currentProject = currentProject,
                                onGenerateSection = { type, data -> viewModel.generateSiteAddition(type, data) }
                            )
                        }
                    }
                }
            }

            // Spinner Modal blocker overlay for asynchronous operations
            if (uiState is WebUiState.Loading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.75f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardSlateBack),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = SunsetGold, strokeWidth = 3.dp)
                            Spacer(modifier = Modifier.height(18.dp))
                            Text(
                                text = (uiState as WebUiState.Loading).message,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Side-by-side proposed DIFF check modal before applying edits
            ReviewProposedFixDialog(
                proposedFix = proposedFix,
                currentFileContent = editorContent,
                onDismiss = { viewModel.clearProposedFix() },
                onConfirm = { viewModel.applyStagedFix() }
            )

            // Select project Dialog modal
            if (showProjectSelector) {
                ProjectPickerModal(
                    projects = projects,
                    currentProject = currentProject,
                    onDismiss = { showProjectSelector = false },
                    onSelect = {
                        viewModel.selectProject(it)
                        showProjectSelector = false
                    }
                )
            }
        }
    }
}

@Composable
fun StatusTriageCard(
    project: WebProject?,
    advice: String,
    onSelectorClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardSlateBack.copy(alpha = 0.9f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Active Sandbox Workspace",
                        fontSize = 10.sp,
                        color = SunsetGold,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = project?.name ?: "Loading vaults...",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = onSelectorClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("select_project_dropdown")
                ) {
                    Text("Change Site", color = Color.White, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Health bar visual index
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                val score = project?.healthScore ?: 100
                val barColor = when {
                    score >= 90 -> BrightLime
                    score >= 60 -> SunsetGold
                    else -> WarmRose
                }

                LinearProgressIndicator(
                    progress = score / 100f,
                    color = barColor,
                    trackColor = Color.White.copy(alpha = 0.08f),
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "$score%",
                    color = barColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // What the app would do now
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Tactical recommendation",
                    tint = SunsetGold,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = advice,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ExplorerSubpanel(
    fileTree: WebFileNode?,
    currentProject: WebProject?,
    activeFile: String,
    onFileSelected: (String) -> Unit,
    onAddFile: (String) -> Unit,
    onDeleteFile: (String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var newFileName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Structure Files Explorer",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.6f)
            )

            IconButton(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .size(28.dp)
                    .testTag("add_file_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create blank static file",
                    tint = SunsetGold,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (fileTree == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No project initialized", color = Color.White.copy(alpha = 0.3f), fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(CardSlateBack.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                // Flatten the hierarchical tree for simple list depiction
                val itemsList = mutableListOf<WebFileNode>()
                fun addNodeFlat(node: WebFileNode) {
                    if (node.relativePath.isNotBlank()) {
                        itemsList.add(node)
                    }
                    node.children.forEach { addNodeFlat(it) }
                }
                addNodeFlat(fileTree)

                if (itemsList.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(30.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Empty folder", color = Color.White.copy(alpha = 0.3f), fontSize = 12.sp)
                        }
                    }
                } else {
                    items(itemsList, key = { it.relativePath }) { item ->
                        val isSelected = item.relativePath == activeFile
                        val textCol = if (isSelected) SunsetGold else Color.White

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) SunsetGold.copy(alpha = 0.1f) else Color.Transparent)
                                .clickable {
                                    if (!item.isDirectory) {
                                        onFileSelected(item.relativePath)
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (item.isDirectory) Icons.Default.Menu else Icons.Default.Send,
                                    contentDescription = "File Type",
                                    tint = if (item.isDirectory) LightElectricBlue else SunsetGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = item.relativePath,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = textCol,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Delete asset button
                            if (item.relativePath != "index.html") {
                                IconButton(
                                    onClick = { onDeleteFile(item.relativePath) },
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove static asset",
                                        tint = Color.White.copy(alpha = 0.2f),
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Create File Dialog
        if (showAddDialog) {
            Dialog(onDismissRequest = { showAddDialog = false }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardSlateBack),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Add New File", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = newFileName,
                            onValueChange = { newFileName = it },
                            placeholder = { Text("e.g. sitemap.xml", color = Color.White.copy(alpha = 0.4f)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = SunsetGold,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("add_file_text_input")
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(onClick = { showAddDialog = false }) {
                                Text("Cancel", color = Color.White.copy(alpha = 0.5f))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (newFileName.isNotBlank()) {
                                        onAddFile(newFileName)
                                        newFileName = ""
                                        showAddDialog = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SunsetGold)
                            ) {
                                Text("Create", color = DeepSableBack)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditorAndPreviewSubpanel(
    activeFile: String,
    content: String,
    onContentChanged: (String) -> Unit
) {
    var editorModeTab by remember { mutableStateOf("edit") } // "edit" or "renders"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (activeFile.isNotBlank()) "Working: $activeFile" else "No file selected",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            TabRow(
                selectedTabIndex = if (editorModeTab == "edit") 0 else 1,
                modifier = Modifier
                    .width(180.dp)
                    .height(34.dp),
                containerColor = Color.Transparent,
                divider = {},
                indicator = {}
            ) {
                Tab(
                    selected = editorModeTab == "edit",
                    onClick = { editorModeTab = "edit" },
                    text = { Text("Code", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    selectedContentColor = SunsetGold,
                    unselectedContentColor = Color.White.copy(alpha = 0.4f)
                )
                Tab(
                    selected = editorModeTab == "renders",
                    onClick = { editorModeTab = "renders" },
                    text = { Text("Sim Preview", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    selectedContentColor = SunsetGold,
                    unselectedContentColor = Color.White.copy(alpha = 0.4f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (activeFile.isBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("Select a file from the list explorer to load editor.", color = Color.White.copy(alpha = 0.3f), fontSize = 12.sp)
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(CardSlateBack, RoundedCornerShape(14.dp))
                    .padding(8.dp)
            ) {
                if (editorModeTab == "edit") {
                    OutlinedTextField(
                        value = content,
                        onValueChange = onContentChanged,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            cursorColor = SunsetGold
                        ),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("editor_text_input")
                            .verticalScroll(rememberScrollState()),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    )
                } else {
                    SimulationPreRenderingComponent(activeFile, content)
                }
            }
        }
    }
}

// Simulated browser visual output cards rendering actual SEO schemas
@Composable
fun SimulationPreRenderingComponent(fileName: String, code: String) {
    val scrollState = rememberScrollState()

    // Offline heuristic visual parser
    val title = remember(code) {
        val mat = Regex("<title>(.*?)</title>", RegexOption.IGNORE_CASE).find(code)
        mat?.groupValues?.get(1) ?: "Dublin Family dentistry | Clinic"
    }

    val desc = remember(code) {
        val mat = Regex("<meta name=\"description\" content=\"(.*?)\"", RegexOption.IGNORE_CASE).find(code)
        mat?.groupValues?.get(1) ?: "No description configured."
    }

    val isDental = code.contains("Smile", ignoreCase = true) || code.contains("teeth", ignoreCase = true)
    val isBakery = code.contains("Crust", ignoreCase = true) || code.contains("Bread", ignoreCase = true)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Mock Browser top address bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(4.dp).background(Color(0xFFEF4444), CircleShape))
            Spacer(modifier = Modifier.width(3.dp))
            Box(modifier = Modifier.size(4.dp).background(Color(0xFFF59E0B), CircleShape))
            Spacer(modifier = Modifier.width(3.dp))
            Box(modifier = Modifier.size(4.dp).background(Color(0xFF10B981), CircleShape))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "https://localhost/${fileName}",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Visual simulator view depending on code content
        if (isDental) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🦷 BrightSmile Family", color = Color(0xFF0369A1), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF0284C7), RoundedCornerShape(14.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Book Call", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Hero content
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFECFEFF), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text("We Make Your Smile Shine", color = Color(0xFF164E63), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        Text(
                            "Dublin's companion gentle dentists clinic team. Reframe your health beautifully.",
                            color = Color(0xFF0891B2),
                            fontSize = 10.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Button(
                            onClick = {},
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text("Consult", color = Color.White, fontSize = 9.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Services grid simulation
                Text("Treatments:", color = Color(0xFF1E293B), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Teeth Clean", "Laser White", "Root canal").forEach { item ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Column {
                                Text("✨", fontSize = 12.sp)
                                Text(item, color = Color(0xFF475569), fontWeight = FontWeight.Bold, fontSize = 9.sp)
                            }
                        }
                    }
                }
            }
        } else if (isBakery) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFAF6F0), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text("🥖 The Golden Crust | Artisanal", color = Color(0xFF582F0E), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF5EBE0), RoundedCornerShape(8.dp))
                        .padding(16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Warm, Crispy Fresh Sourdough Bread Daily", color = Color(0xFF582F0E), fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Boston's trusted brick oven secret.", color = Color(0xFF7F4F24), fontSize = 11.sp)
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardSlateBack.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(title, color = SunsetGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(desc, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, textAlign = TextAlign.Center)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Crawlers Index SEO scorecard
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("CRAWLER INSPECTION HEADERS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SunsetGold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Title Tags: $title", color = Color.White, fontSize = 11.sp)
                Text("Description Tags: $desc", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun CoreControlRoomPanel(
    subTab: String,
    onSubTabChanged: (String) -> Unit,
    result: AnalysisResult,
    onScanClick: () -> Unit,
    onApplyIssueFix: (ProposedFix) -> Unit,
    chatMessages: List<ChatMessage>,
    onSendMessage: (String) -> Unit,
    backups: List<ProjectBackup>,
    onRevertBackup: (ProjectBackup) -> Unit,
    currentProject: WebProject?,
    onGenerateSection: (String, Map<String, String>) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Toggle bar header
        TabRow(
            selectedTabIndex = when (subTab) {
                "analysis" -> 0
                "generators" -> 1
                "chat" -> 2
                else -> 0
            },
            containerColor = CardSlateBack,
            contentColor = SunsetGold,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[if (subTab == "analysis") 0 else if (subTab == "generators") 1 else 2]),
                    color = SunsetGold
                )
            }
        ) {
            Tab(
                selected = subTab == "analysis",
                onClick = { onSubTabChanged("analysis") },
                text = { Text("Audit Checks", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = subTab == "generators",
                onClick = { onSubTabChanged("generators") },
                text = { Text("Web Generator", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = subTab == "chat",
                onClick = { onSubTabChanged("chat") },
                text = { Text("AI Copilot", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(10.dp)
        ) {
            when (subTab) {
                "analysis" -> {
                    AnalysisCoreSubpanel(result, onScanClick, onApplyIssueFix, backups, onRevertBackup)
                }
                "generators" -> {
                    GeneratorsSuiteSubpanel(currentProject, onGenerateSection)
                }
                "chat" -> {
                    AssistantChatSubpanel(chatMessages, onSendMessage, onApplyIssueFix)
                }
            }
        }
    }
}

@Composable
fun AnalysisCoreSubpanel(
    result: AnalysisResult,
    onScanClick: () -> Unit,
    onApplyIssueFix: (ProposedFix) -> Unit,
    backups: List<ProjectBackup>,
    onRevertBackup: (ProjectBackup) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Scan Trigger Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Discovered Issues (${result.issues.size})", fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 14.sp)
                Button(
                    onClick = onScanClick,
                    colors = ButtonDefaults.buttonColors(containerColor = SunsetGold),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("scan_project_btn")
                ) {
                    Text("Re-Scan", color = DeepSableBack, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (result.issues.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎉 Score index 100%! All audits completely passed.", color = BrightLime, fontSize = 13.sp)
                }
            }
        } else {
            items(result.issues, key = { it.id }) { issue ->
                val badgeColor = when (issue.priority) {
                    IssuePriority.CRITICAL -> WarmRose
                    IssuePriority.MEDIUM -> SunsetGold
                    IssuePriority.LOW -> LightElectricBlue
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardSlateBack.copy(alpha = 0.85f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = issue.priority.name,
                                    color = badgeColor,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = issue.title,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(issue.description, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, lineHeight = 16.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Impact: ${issue.impact}",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = {
                                    val fix = ProposedFix(
                                        relativePath = issue.targetFile,
                                        originalTextBlock = issue.diffContentTarget,
                                        replacementText = issue.diffContentReplacement,
                                        description = "Refactored offline fix: ${issue.id}"
                                    )
                                    onApplyIssueFix(fix)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SunsetGold.copy(alpha = 0.15f)),
                                border = BorderStroke(1.dp, SunsetGold),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("Propose Fix", color = SunsetGold, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        // BACKUPS UNDO SECTION
        item {
            Spacer(modifier = Modifier.height(14.dp))
            Divider(color = Color.White.copy(alpha = 0.05f))
            Spacer(modifier = Modifier.height(4.dp))
            Text("Auto-Backups History", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
        }

        if (backups.isEmpty()) {
            item {
                Text(
                    text = "No sandboxed modifications applied yet today.",
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 11.sp
                )
            }
        } else {
            items(backups, key = { it.id }) { backup ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = backup.backupName,
                                color = if (backup.isReverted) Color.White.copy(alpha = 0.3f) else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Restorable: ${backup.relativeFilePath}",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 10.sp
                            )
                        }

                        if (!backup.isReverted) {
                            Button(
                                onClick = { onRevertBackup(backup) },
                                colors = ButtonDefaults.buttonColors(containerColor = WarmRose.copy(alpha = 0.15f)),
                                border = BorderStroke(1.dp, WarmRose),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                modifier = Modifier
                                    .height(26.dp)
                                    .testTag("undo_revert_btn")
                            ) {
                                Text("Undo", color = WarmRose, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Reverted", color = Color.White.copy(alpha = 0.3f), fontSize = 9.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GeneratorsSuiteSubpanel(
    currentProject: WebProject?,
    onGenerateSection: (String, Map<String, String>) -> Unit
) {
    val scrollState = rememberScrollState()

    var bizName by remember { mutableStateOf("SmileCraft Dental") }
    var street by remember { mutableStateOf("45 Merrion Squ Irish Road") }
    var city by remember { mutableStateOf("Dublin 2") }
    var phone by remember { mutableStateOf("+353 1 800 5550") }
    var email by remember { mutableStateOf("care@smilecraft.ie") }

    val metaMap = remember(bizName, street, city, phone, email) {
        mapOf(
            "businessName" to bizName,
            "street" to street,
            "city" to city,
            "phone" to phone,
            "email" to email,
            "mapQuery" to "$street, $city"
        )
    }

    if (currentProject == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Open a project sandbox first", color = Color.White.copy(alpha = 0.3f), fontSize = 12.sp)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(10.dp)
    ) {
        Text("Local Business Profile Metadata", fontWeight = FontWeight.Bold, color = SunsetGold, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(8.dp))

        listOf(
            "Legal Business Name" to bizName to { s: String -> bizName = s },
            "Street Address" to street to { s: String -> street = s },
            "City  Region" to city to { s: String -> city = s },
            "Phone Number" to phone to { s: String -> phone = s },
            "Public Email" to email to { s: String -> email = s }
        ).forEach { pair ->
            val label = pair.first.first
            val valStr = pair.first.second
            val setter = pair.second

            OutlinedTextField(
                value = valStr,
                onValueChange = setter,
                label = { Text(label, color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = SunsetGold,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Divider(color = Color.White.copy(alpha = 0.04f))
        Spacer(modifier = Modifier.height(10.dp))

        Text("Bespoke Section Constructors", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(10.dp))

        // Trigger selectors
        listOf(
            "faques" to "faq" to "Accordion FAQ Segment" to "FAQ segment containing toggles and details answering common local questions.",
            "components" to "hero" to "CTA Conversion Hero Banner" to "Stunning call-to-action hero block with telephone linkages.",
            "maps" to "contact" to "Contact with Mock Maps Layout" to "Grid including responsive feedback contact nodes + map overlay.",
            "schema" to "schema" to "JSON-LD LocalBusiness Script" to "Search ranking markup informing directories of coordinates & schedules.",
            "txt" to "robots" to "Configure robots.txt Crawler" to "Configure domain scan rules and disallow parameters for search crawlers.",
            "sitemap" to "sitemap" to "Configure sitemap.xml Roadmap" to "Map out static relative domains schema to accelerate Google crawls.",
            "privacy" to "privacy" to "Privacy Policy Static Document" to "Generate GDRP compliant Terms / Privacy HTML to embed link in footer."
        ).forEach { details ->
            val iconType = details.first.first.first
            val assetType = details.first.first.second
            val title = details.first.second
            val desc = details.second

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(desc, color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp, lineHeight = 13.sp)
                    }
                    Button(
                        onClick = { onGenerateSection(assetType, metaMap) },
                        colors = ButtonDefaults.buttonColors(containerColor = SunsetGold.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, SunsetGold),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text("+ Generate", color = SunsetGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AssistantChatSubpanel(
    messages: List<ChatMessage>,
    onSendMessage: (String) -> Unit,
    onApplyProposedFix: (ProposedFix) -> Unit
) {
    var textInput by remember { mutableStateOf("") }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    // Scroll to latest message
    LaunchedEffect(messages) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .background(CardSlateBack.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(14.dp))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("✨ WebAI Assistant", fontWeight = FontWeight.Bold, color = SunsetGold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Write commands like:\n\"revisa este proyecto\"\n\"corrige las imágenes sin alt\"\n\"crea una FAQ\"",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            } else {
                items(messages, key = { it.id }) { msg ->
                    val isUser = msg.sender == "user"
                    val bcolor = if (isUser) SunsetGold.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.04f)
                    val alignment = if (isUser) Alignment.End else Alignment.Start

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalAlignment = alignment
                    ) {
                        Box(
                            modifier = Modifier
                                .widthIn(max = 280.dp)
                                .background(bcolor, RoundedCornerShape(12.dp))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(
                                    text = if (isUser) "Me" else "Copilot WebAI",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isUser) SunsetGold else Color.White.copy(alpha = 0.4f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = msg.content,
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    lineHeight = 16.sp
                                )

                                // Proposed AI Fix Widget display inside bubble chat logs
                                if (msg.proposedFix != null) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = CardSlateBack),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text(
                                                "Proposed code change:",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SunsetGold
                                            )
                                            Text(
                                                "File: ${msg.proposedFix.relativePath}",
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color.White.copy(alpha = 0.6f)
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Button(
                                                onClick = { onApplyProposedFix(msg.proposedFix) },
                                                colors = ButtonDefaults.buttonColors(containerColor = SunsetGold),
                                                contentPadding = PaddingValues(horizontal = 8.dp),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.height(24.dp)
                                            ) {
                                                Text("Bake / See Diff", color = DeepSableBack, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Chat Input Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = { Text("Command Copilot...", fontSize = 12.sp, color = Color.White.copy(alpha = 0.3f)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = SunsetGold,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                ),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("assistant_chat_input"),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (textInput.isNotBlank()) {
                        onSendMessage(textInput)
                        textInput = ""
                    }
                },
                modifier = Modifier
                    .background(SunsetGold, CircleShape)
                    .size(42.dp)
                    .testTag("send_chat_msg_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = DeepSableBack,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun ReviewProposedFixDialog(
    proposedFix: ProposedFix?,
    currentFileContent: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (proposedFix == null) return

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CardSlateBack),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Verify Code Diff Review",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 15.sp
                )
                Text(
                    text = "File target: ${proposedFix.relativePath}",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = SunsetGold
                )

                Spacer(modifier = Modifier.height(14.dp))

                if (proposedFix.originalTextBlock.isNotBlank()) {
                    Text("Original (Blocked Out)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = WarmRose)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(WarmRose.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                            .border(1.dp, WarmRose.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = proposedFix.originalTextBlock,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                Text("Proposed Insertion", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BrightLime)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BrightLime.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                        .border(1.dp, BrightLime.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = proposedFix.replacementText,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "WebAI Copilot auto-creates a backup restore point before modifying any files. Edits are sandboxed and undoable.",
                    fontSize = 9.sp,
                    lineHeight = 12.sp,
                    color = Color.White.copy(alpha = 0.4f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.4f))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = SunsetGold),
                        modifier = Modifier.testTag("submit_diff_fix_btn")
                    ) {
                        Text("Bake & Apply", color = DeepSableBack, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ProjectPickerModal(
    projects: List<WebProject>,
    currentProject: WebProject?,
    onDismiss: () -> Unit,
    onSelect: (WebProject) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CardSlateBack),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Select Local Site Template",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                projects.forEach { project ->
                    val isCurrent = project.id == currentProject?.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onSelect(project) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrent) SunsetGold.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.02f)
                        ),
                        border = if (isCurrent) BorderStroke(1.dp, SunsetGold) else null,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                project.name,
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrent) SunsetGold else Color.White,
                                fontSize = 13.sp
                            )
                            Text(
                                project.description,
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 10.sp,
                                lineHeight = 13.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Back", color = Color.White.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}
