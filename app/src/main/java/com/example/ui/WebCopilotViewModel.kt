package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "user", "copilot", "system"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val proposedFix: ProposedFix? = null
)

data class ProposedFix(
    val relativePath: String,
    val originalTextBlock: String,
    val replacementText: String,
    val description: String
)

sealed interface WebUiState {
    object Idle : WebUiState
    data class Loading(val message: String) : WebUiState
    data class Success(val message: String) : WebUiState
    data class Error(val message: String) : WebUiState
}

class WebCopilotViewModel(
    application: Application,
    private val webDao: WebDao
) : AndroidViewModel(application) {

    private val context = application.applicationContext

    // 1. Projects List
    val projects: StateFlow<List<WebProject>> = webDao.getAllProjectsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _currentProject = MutableStateFlow<WebProject?>(null)
    val currentProject: StateFlow<WebProject?> = _currentProject.asStateFlow()

    // 2. Backups List
    private val _backups = MutableStateFlow<List<ProjectBackup>>(emptyList())
    val backups: StateFlow<List<ProjectBackup>> = _backups.asStateFlow()

    // 3. UI states
    private val _uiState = MutableStateFlow<WebUiState>(WebUiState.Idle)
    val uiState: StateFlow<WebUiState> = _uiState.asStateFlow()

    // 4. File Tree / Active File Editor state
    private val _fileTree = MutableStateFlow<WebFileNode?>(null)
    val fileTree: StateFlow<WebFileNode?> = _fileTree.asStateFlow()

    private val _openedFilePath = MutableStateFlow<String>("")
    val openedFilePath: StateFlow<String> = _openedFilePath.asStateFlow()

    private val _editorContent = MutableStateFlow<String>("")
    val editorContent: StateFlow<String> = _editorContent.asStateFlow()

    // 5. Analysis checklist issues & stats
    private val _analysisResult = MutableStateFlow<AnalysisResult>(AnalysisResult(100, emptyList()))
    val analysisResult: StateFlow<AnalysisResult> = _analysisResult.asStateFlow()

    private val _proposedFixForReview = MutableStateFlow<ProposedFix?>(null)
    val proposedFixForReview: StateFlow<ProposedFix?> = _proposedFixForReview.asStateFlow()

    // 6. Chat logs
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    // 7. General explanatory "What the app would do now" label
    private val _nextRecommendedAction = MutableStateFlow<String>("Select or upload a project to begin secure SEO verification.")
    val nextRecommendedAction: StateFlow<String> = _nextRecommendedAction.asStateFlow()

    init {
        setupPresetProjectsOnStart()
    }

    private fun setupPresetProjectsOnStart() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = WebUiState.Loading("Initializing local vault...")
            // Setup static project templates on disk
            WebProjectManager.setupPresets(context, webDao, {})

            // Check if DB projects exists
            val existing = webDao.getAllProjectsFlow().stateIn(this).value
            if (existing.isEmpty()) {
                // Populate databases
                val dentistPid = webDao.insertProject(
                    WebProject(
                        name = "Dublin Family Dentistry",
                        description = "Dentist Static Hub with double H1, un-alt images, missing sitemaps and bad mobile scales.",
                        folderName = "dentist_clinic",
                        healthScore = 50
                    )
                ).toInt()

                val bakeryPid = webDao.insertProject(
                    WebProject(
                        name = "Classic Bakery & Flour",
                        description = "Boston local Bakery shop static page lacking robots.txt, maps schemas and legal templates.",
                        folderName = "local_bakery",
                        healthScore = 70
                    )
                ).toInt()

                val emptyPid = webDao.insertProject(
                    WebProject(
                        name = "Modern Starter Canvas",
                        description = "Empty index/style scaffold for compiling a brand new local business layout safely.",
                        folderName = "empty_scaffolding",
                        healthScore = 95
                    )
                ).toInt()
            }

            // Select first project as default active project
            val loaded = webDao.getAllProjectsFlow().stateIn(this).value
            if (loaded.isNotEmpty()) {
                selectProject(loaded.first())
            } else {
                _uiState.value = WebUiState.Idle
            }
        }
    }

    fun selectProject(project: WebProject) {
        viewModelScope.launch {
            _currentProject.value = project
            _uiState.value = WebUiState.Loading("Loading index assets...")
            
            withContext(Dispatchers.IO) {
                // Read files & compute initial scores
                reloadProjectFileExplorer()
                
                // Select index.html as primary if exists
                val tree = _fileTree.value
                val hasIndex = tree?.children?.any { it.name == "index.html" } == true
                if (hasIndex) {
                    openFileInEditor("index.html")
                } else {
                    val firstFile = tree?.children?.firstOrNull { !it.isDirectory }
                    if (firstFile != null) {
                        openFileInEditor(firstFile.relativePath)
                    } else {
                        _openedFilePath.value = ""
                        _editorContent.value = ""
                    }
                }

                // Analyze
                runLocalAnalysisInternal()
                
                // Load Backups flow
                webDao.getBackupsForProjectFlow(project.id).collect { backupList ->
                    _backups.value = backupList
                }
            }
            _uiState.value = WebUiState.Idle
            updateNextRecommendation()
        }
    }

    private suspend fun reloadProjectFileExplorer() {
        val proj = _currentProject.value ?: return
        val tree = WebProjectManager.getProjectTree(context, proj.folderName)
        _fileTree.value = tree
    }

    fun openFileInEditor(relativePath: String) {
        val proj = _currentProject.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val content = WebProjectManager.readFile(context, proj.folderName, relativePath)
            _openedFilePath.value = relativePath
            _editorContent.value = content
            updateNextRecommendation()
        }
    }

    fun updateActiveFileContent(newContent: String) {
        _editorContent.value = newContent
        val proj = _currentProject.value ?: return
        val path = _openedFilePath.value
        if (path.isNotBlank()) {
            viewModelScope.launch(Dispatchers.IO) {
                // Simple auto-save for user edits on active file
                WebProjectManager.writeFile(context, proj.folderName, path, newContent)
                runLocalAnalysisInternal()
            }
        }
    }

    fun removeFileFromProject(relativePath: String) {
        val proj = _currentProject.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            WebProjectManager.deleteFile(context, proj.folderName, relativePath)
            reloadProjectFileExplorer()
            if (_openedFilePath.value == relativePath) {
                _openedFilePath.value = ""
                _editorContent.value = ""
            }
            runLocalAnalysisInternal()
        }
    }

    fun createNewFile(name: String, initialContent: String = "") {
        val proj = _currentProject.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            WebProjectManager.writeFile(context, proj.folderName, name, initialContent)
            reloadProjectFileExplorer()
            openFileInEditor(name)
            runLocalAnalysisInternal()
        }
    }

    fun createProjectFromUserZip(name: String, zipInputStream: InputStream) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = WebUiState.Loading("Decompressing assets...")
            try {
                val folderName = "user_project_" + System.currentTimeMillis()
                WebProjectManager.importFromZip(context, folderName, zipInputStream)
                
                val newProj = WebProject(
                    name = name.ifBlank { "Uploaded Project" },
                    description = "Custom uploaded zip folder. Auto backup sandbox active.",
                    folderName = folderName
                )
                val newId = webDao.insertProject(newProj).toInt()
                val completeProj = newProj.copy(id = newId)
                
                selectProject(completeProj)
                _uiState.value = WebUiState.Success("Zip project opened successfully!")
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = WebUiState.Error("Failed to extract ZIP: ${e.localizedMessage}")
            }
        }
    }

    fun downloadProjectZip(outStream: OutputStream) {
        val proj = _currentProject.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                WebProjectManager.exportToZip(context, proj.folderName, outStream)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Runs offline analytical checks and updates DB model
    fun runOfflineAnalysis() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = WebUiState.Loading("Scanning workspace rules...")
            runLocalAnalysisInternal()
            _uiState.value = WebUiState.Success("Scan complete!")
            updateNextRecommendation()
        }
    }

    private suspend fun runLocalAnalysisInternal() {
        val proj = _currentProject.value ?: return
        val projectDirectory = WebProjectManager.getProjectDir(context, proj.folderName)
        val result = OfflineAnalyzer.analyzeProject(projectDirectory)
        _analysisResult.value = result

        // Update database score
        val updatedProj = proj.copy(healthScore = result.healthScore, lastAnalyzed = System.currentTimeMillis())
        _currentProject.value = updatedProj
        webDao.updateProject(updatedProj)
    }

    // Set a proposed fix to be shown in the Diff View
    fun stageProposedFix(fix: ProposedFix) {
        _proposedFixForReview.value = fix
    }

    fun clearProposedFix() {
        _proposedFixForReview.value = null
    }

    // Backs up the file and applies the fix
    fun applyStagedFix() {
        val fix = _proposedFixForReview.value ?: return
        val proj = _currentProject.value ?: return
        
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = WebUiState.Loading("Creating backup restore point...")
            val currentFileContent = WebProjectManager.readFile(context, proj.folderName, fix.relativePath)
            
            // Generate modified text using simple block replacement
            val modifiedContent = if (fix.originalTextBlock.isBlank()) {
                // If original is blank, prepend or replace entire file
                if (currentFileContent.isBlank()) fix.replacementText else currentFileContent + "\n" + fix.replacementText
            } else {
                currentFileContent.replace(fix.originalTextBlock, fix.replacementText)
            }

            // Create backup model
            val backup = ProjectBackup(
                projectId = proj.id,
                relativeFilePath = fix.relativePath,
                contentBefore = currentFileContent,
                contentAfter = modifiedContent,
                backupName = fix.description
            )
            webDao.insertBackup(backup)

            // Write to project folder
            WebProjectManager.writeFile(context, proj.folderName, fix.relativePath, modifiedContent)
            
            // Reload tree and active editor
            reloadProjectFileExplorer()
            if (_openedFilePath.value == fix.relativePath) {
                _editorContent.value = modifiedContent
            }

            _proposedFixForReview.value = null
            runLocalAnalysisInternal()
            _uiState.value = WebUiState.Success("Backup saved & applied securely!")
            updateNextRecommendation()
        }
    }

    // UNDO / Revert backup operation
    fun revertBackup(backup: ProjectBackup) {
        val proj = _currentProject.value ?: return
        if (backup.isReverted) return

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = WebUiState.Loading("Restoring file to before edit...")
            // Overwrite file with contentBefore
            WebProjectManager.writeFile(context, proj.folderName, backup.relativeFilePath, backup.contentBefore)

            // Mark in room DB
            webDao.updateBackup(backup.copy(isReverted = true))

            // Refresh UX
            reloadProjectFileExplorer()
            if (_openedFilePath.value == backup.relativeFilePath) {
                _editorContent.value = backup.contentBefore
            }
            runLocalAnalysisInternal()
            _uiState.value = WebUiState.Success("Reverted successfully to restore point!")
            updateNextRecommendation()
        }
    }

    // 1-Click Local business details generators
    fun generateSiteAddition(type: String, metadata: Map<String, String>) {
        val proj = _currentProject.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = WebUiState.Loading("Engineering localized component...")
            val result = CodeAssetGenerator.generateAsset(type, metadata)
            
            // Prepare a Proposed Fix
            val relativePath = result.targetFileName
            val currentFileContent = WebProjectManager.readFile(context, proj.folderName, relativePath)

            val testBlock = when (relativePath) {
                "sitemap.xml" -> ""
                "robots.txt" -> ""
                else -> "</body>"
            }
            val writeBlock = when (relativePath) {
                "sitemap.xml" -> result.contents
                "robots.txt" -> result.contents
                else -> result.contents + "\n</body>"
            }

            val fix = ProposedFix(
                relativePath = relativePath,
                originalTextBlock = testBlock,
                replacementText = writeBlock,
                description = "Insert organic generated ${type.uppercase()} layout."
            )
            _proposedFixForReview.value = fix
            _uiState.value = WebUiState.Idle
        }
    }

    // Gemini-powered interactive Copilot Chat
    fun sendCopilotMessage(userText: String) {
        if (userText.isBlank()) return
        val proj = _currentProject.value ?: return

        val userMsg = ChatMessage(sender = "user", content = userText)
        _chatMessages.value = _chatMessages.value + userMsg

        viewModelScope.launch {
            _uiState.value = WebUiState.Loading("Querying Gemini brain...")
            val apiKey = BuildConfig.GEMINI_API_KEY

            if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                // Return clear descriptive fallback message to guide user to input their secrets key
                val systemAdvice = """
                    I am WebAI Copilot, and I am standing by to assist with your local business web projects!
                    To enable smart code edits, real-time refactoring, and AI-powered FAQ generation directly in this chat, please enter your **GEMINI_API_KEY** into the Secrets panel of Google AI Studio.
                    
                    **In the meantime, enjoy 100% offline verification!** Click on the issues checklist or the "Web Generator" tab to fix SEO and generate structural elements instantly.
                """.trimIndent()
                delayState(800)
                _chatMessages.value = _chatMessages.value + ChatMessage(sender = "copilot", content = systemAdvice)
                _uiState.value = WebUiState.Idle
                return@launch
            }

            withContext(Dispatchers.IO) {
                try {
                    // Gather context files
                    val activeFile = _openedFilePath.value
                    val fileContent = _editorContent.value
                    val currentIssues = _analysisResult.value.issues.joinToString("\n") { "- ${it.title} (${it.priority})" }

                    val systemInstructionPrompt = """
                        You are WebAI Copilot, an expert professional local-business SEO consultant and static HTML web specialist.
                        You help users optimize small local static sites in HTML, CSS, JS, and metadata assets.
                        When answering, provide a friendly explanation. If the user's input asks for a code modification or creation, you MUST return a valid JSON format using the CopilotReply envelope:
                        {
                          "explanation": "Your textual help and suggestions.",
                          "targetFile": "the filename to edit, e.g., 'index.html', or null if text-only",
                          "originalBlockToReplace": "the exact string block in their file that needs replacement, or empty/null if creating a new file.",
                          "replacementContent": "the new code block or generated elements."
                        }
                    """.trimIndent()

                    val prompt = """
                        Current business project: ${proj.name}
                        Context Issues detected offline:
                        $currentIssues
                        
                        Currently open file: $activeFile
                        Currently open file content:
                        $fileContent
                        
                        User query: "$userText"
                        
                        Provide a supportive response. Remember to strictly format your response as JSON matching the CopilotReply schema if proposing a code change.
                    """.trimIndent()

                    val request = GeminiReq(
                        contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt)))),
                        generationConfig = GeminiGenConfig(responseMimeType = "application/json", temperature = 0.5f),
                        systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemInstructionPrompt)))
                    )

                    val response = RetrofitWebClient.apiService.generateContent(apiKey, request)
                    val responseContentText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

                    if (responseContentText != null) {
                        val parsed = RetrofitWebClient.parseCopilotReply(responseContentText)
                        if (parsed != null) {
                            val fix = if (!parsed.targetFile.isNullOrBlank() && !parsed.replacementContent.isNullOrBlank()) {
                                ProposedFix(
                                    relativePath = parsed.targetFile,
                                    originalTextBlock = parsed.originalBlockToReplace ?: "",
                                    replacementText = parsed.replacementContent,
                                    description = "Gemini AI: Chat triggered optimization."
                                )
                            } else null

                            withContext(Dispatchers.Main) {
                                _chatMessages.value = _chatMessages.value + ChatMessage(
                                    sender = "copilot",
                                    content = parsed.explanation,
                                    proposedFix = fix
                                )
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                _chatMessages.value = _chatMessages.value + ChatMessage(
                                    sender = "copilot",
                                    content = responseContentText
                                )
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            _chatMessages.value = _chatMessages.value + ChatMessage(
                                sender = "copilot",
                                content = "I listened closely, but the stars remained quiet. Please try again."
                            )
                        }
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        _chatMessages.value = _chatMessages.value + ChatMessage(
                            sender = "copilot",
                            content = "Could not reach the Gemini server: ${e.localizedMessage}. Verify internet settings and API key variables."
                        )
                    }
                }
            }
            _uiState.value = WebUiState.Idle
        }
    }

    private fun updateNextRecommendation() {
        val score = _currentProject.value?.healthScore ?: 100
        val issues = _analysisResult.value.issues
        _nextRecommendedAction.value = when {
            score == 100 -> "Your project has reached 100% health! You can safely export your static zip ready for deployment."
            issues.any { it.priority == IssuePriority.CRITICAL } -> {
                val critical = issues.first { it.priority == IssuePriority.CRITICAL }
                "Action Recommended: Resolve CRITICAL issue '${critical.title}' in the right control panel."
            }
            issues.any { it.priority == IssuePriority.MEDIUM } -> {
                val med = issues.first { it.priority == IssuePriority.MEDIUM }
                "Improve local SEO: Address MEDIUM issue '${med.title}' to boost localized maps results."
            }
            else -> "Fine tune aesthetics: Address LOW priority tags or include social Open Graph card reviews."
        }
    }

    private suspend fun delayState(ms: Long) {
        withContext(Dispatchers.IO) {
            try { Thread.sleep(ms) } catch (e: Exception) {}
        }
    }

    fun resetState() {
        _uiState.value = WebUiState.Idle
    }
}

class WebCopilotViewModelFactory(
    private val application: Application,
    private val webDao: WebDao
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WebCopilotViewModel::class.java)) {
            return WebCopilotViewModel(application, webDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
