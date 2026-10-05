package com.google.antigravity.mobile.agent

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import com.google.antigravity.mobile.model.ChatMessage
import com.google.antigravity.mobile.model.ConversationSession
import com.google.antigravity.mobile.model.MessageRole
import com.google.antigravity.mobile.model.ToolExecution
import com.google.antigravity.mobile.model.ToolStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

class AgentController(private val context: Context) {
    val messages = mutableStateListOf<ChatMessage>()
    val conversations = mutableStateListOf<ConversationSession>()
    val currentSession = mutableStateOf<ConversationSession?>(null)

    val isRunning = mutableStateOf(false)
    val currentStep = mutableStateOf(0)

    private val prefs = context.getSharedPreferences("antigravity_prefs", Context.MODE_PRIVATE)
    val apiKey = mutableStateOf(prefs.getString("api_key", "") ?: "")
    val selectedModel = mutableStateOf(prefs.getString("selected_model", "gemini-2.5-flash") ?: "gemini-2.5-flash")

    val authManager = GoogleAuthManager(context)
    val toolExecutor = ToolExecutor(context)

    private val sessionsDir = File(context.filesDir, "sessions").apply { mkdirs() }
    private val apiHistory = JSONArray()

    init {
        loadConversations()
        if (conversations.isEmpty()) {
            createNewConversation()
        } else {
            selectConversation(conversations.first())
        }
    }

    fun saveSettings(newKey: String, newModel: String) {
        apiKey.value = newKey
        selectedModel.value = newModel
        prefs.edit().apply {
            putString("api_key", newKey)
            putString("selected_model", newModel)
            apply()
        }
    }

    private fun loadConversations() {
        conversations.clear()
        val indexFile = File(sessionsDir, "index.json")
        if (indexFile.exists()) {
            try {
                val array = JSONArray(indexFile.readText())
                for (i in 0 until array.length()) {
                    conversations.add(ConversationSession.fromJson(array.getJSONObject(i)))
                }
            } catch (e: Exception) {
                // If corrupted, fallback
            }
        }
    }

    private fun saveConversationsIndex() {
        val array = JSONArray()
        conversations.forEach { array.put(it.toJson()) }
        File(sessionsDir, "index.json").writeText(array.toString())
    }

    fun createNewConversation(title: String = "New Conversation") {
        val session = ConversationSession(
            id = UUID.randomUUID().toString(),
            title = title
        )
        conversations.add(0, session)
        saveConversationsIndex()
        selectConversation(session)
    }

    fun selectConversation(session: ConversationSession) {
        currentSession.value = session
        messages.clear()
        apiHistory.let {
            while (it.length() > 0) it.remove(0)
        }

        val sessionFile = File(sessionsDir, "${session.id}.json")
        if (sessionFile.exists()) {
            try {
                val json = JSONObject(sessionFile.readText())
                val msgsArray = json.optJSONArray("messages")
                if (msgsArray != null) {
                    for (i in 0 until msgsArray.length()) {
                        val m = ChatMessage.fromJson(msgsArray.getJSONObject(i))
                        messages.add(m)
                        // Reconstruct apiHistory
                        if (m.role == MessageRole.USER) {
                            apiHistory.put(JSONObject().apply {
                                put("role", "user")
                                put("parts", JSONArray().put(JSONObject().put("text", m.content)))
                            })
                        } else if (m.role == MessageRole.AGENT) {
                            apiHistory.put(JSONObject().apply {
                                put("role", "model")
                                put("parts", JSONArray().put(JSONObject().put("text", m.content)))
                            })
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore corrupted file
            }
        }

        if (messages.isEmpty()) {
            messages.add(
                ChatMessage(
                    role = MessageRole.AGENT,
                    content = "👋 Привет! Я Antigravity Mobile. Я подключен к файловой системе, умею выполнять команды через '/', управлять репозиториями Git/GitHub и компилировать APK прямо на устройстве. Что создадим?"
                )
            )
            saveCurrentConversation()
        }
    }

    fun saveCurrentConversation() {
        val session = currentSession.value ?: return
        val sessionFile = File(sessionsDir, "${session.id}.json")
        val json = JSONObject().apply {
            put("id", session.id)
            put("title", session.title)
            val msgsArray = JSONArray()
            messages.forEach { msgsArray.put(it.toJson()) }
            put("messages", msgsArray)
        }
        sessionFile.writeText(json.toString())
        session.updatedAt = System.currentTimeMillis()
        saveConversationsIndex()
    }

    fun deleteConversation(session: ConversationSession) {
        conversations.remove(session)
        File(sessionsDir, "${session.id}.json").delete()
        saveConversationsIndex()
        if (currentSession.value?.id == session.id) {
            if (conversations.isNotEmpty()) {
                selectConversation(conversations.first())
            } else {
                createNewConversation()
            }
        }
    }

    suspend fun sendMessage(userText: String) = withContext(Dispatchers.IO) {
        val hasAuth = apiKey.value.isNotBlank() || authManager.isAuthorized()

        if (!hasAuth) {
            withContext(Dispatchers.Main) {
                messages.add(
                    ChatMessage(
                        role = MessageRole.AGENT,
                        content = "⚠️ Пожалуйста, войдите через Google Аккаунт или укажите API Key во вкладке Настройки (Settings)."
                    )
                )
                saveCurrentConversation()
            }
            return@withContext
        }

        // Auto update conversation title on first message
        val session = currentSession.value
        if (session != null && (session.title == "New Conversation" || session.title.isBlank())) {
            val autoTitle = if (userText.length > 30) userText.take(30) + "..." else userText
            session.title = autoTitle
            saveConversationsIndex()
        }

        withContext(Dispatchers.Main) {
            messages.add(ChatMessage(role = MessageRole.USER, content = userText))
            saveCurrentConversation()
            isRunning.value = true
            currentStep.value = 0
        }

        val userContentObj = JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().put(JSONObject().put("text", userText)))
        }
        apiHistory.put(userContentObj)

        val client = GeminiApiClient(
            apiKey = apiKey.value.takeIf { it.isNotBlank() },
            oauthToken = authManager.currentOAuthToken,
            model = selectedModel.value
        )
        val toolDeclarations = ToolDefinitions.getDeclarations()

        var turns = 0
        val maxTurns = 15

        try {
            while (turns < maxTurns) {
                turns++
                withContext(Dispatchers.Main) {
                    currentStep.value = turns
                }

                val response = client.generateContent(
                    history = apiHistory,
                    systemInstruction = ToolDefinitions.SYSTEM_PROMPT,
                    toolsDeclarations = toolDeclarations
                )

                val modelContent = response.rawJson.optJSONArray("candidates")
                    ?.optJSONObject(0)
                    ?.optJSONObject("content")

                if (modelContent != null) {
                    apiHistory.put(modelContent)
                }

                if (!response.text.isNullOrBlank()) {
                    withContext(Dispatchers.Main) {
                        messages.add(ChatMessage(role = MessageRole.AGENT, content = response.text))
                        saveCurrentConversation()
                    }

                    // Check for slash commands in model text (e.g. /git, /run, /build, /install, /write)
                    val slashCommands = SlashCommandParser.parseCommands(response.text)
                    for (sc in slashCommands) {
                        executeSlashCommand(sc)
                    }
                }

                if (response.functionCalls.isEmpty()) {
                    break
                }

                // Formal Tool calling
                val functionResponseParts = JSONArray()

                for (fc in response.functionCalls) {
                    val toolExecution = ToolExecution(
                        toolName = fc.name,
                        arguments = fc.args.toString(2),
                        status = ToolStatus.RUNNING
                    )

                    withContext(Dispatchers.Main) {
                        val toolMsg = ChatMessage(
                            role = MessageRole.TOOL,
                            content = "Tool: ${fc.name}",
                            toolExecutions = mutableListOf(toolExecution)
                        )
                        messages.add(toolMsg)
                        saveCurrentConversation()
                    }

                    val result = toolExecutor.execute(fc.name, fc.args)

                    withContext(Dispatchers.Main) {
                        toolExecution.result = result
                        toolExecution.status = if (result.startsWith("Error") || result.contains("BUILD FAILED")) {
                            ToolStatus.FAILED
                        } else {
                            ToolStatus.SUCCESS
                        }
                        saveCurrentConversation()
                    }

                    functionResponseParts.put(JSONObject().apply {
                        put("functionResponse", JSONObject().apply {
                            put("name", fc.name)
                            put("response", JSONObject().apply {
                                put("output", result)
                            })
                        })
                    })
                }

                apiHistory.put(JSONObject().apply {
                    put("role", "function")
                    put("parts", functionResponseParts)
                })
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                messages.add(
                    ChatMessage(
                        role = MessageRole.AGENT,
                        content = "❌ Ошибка: ${e.message}"
                    )
                )
                saveCurrentConversation()
            }
        } finally {
            withContext(Dispatchers.Main) {
                isRunning.value = false
                currentStep.value = 0
                saveCurrentConversation()
            }
        }
    }

    private suspend fun executeSlashCommand(cmd: ParsedCommand) {
        val toolName = when (cmd.commandType) {
            "git" -> "git_command"
            "build" -> "build_apk"
            "install" -> "install_apk"
            "write" -> "write_to_file"
            else -> "run_command"
        }

        val argsJson = JSONObject()
        when (toolName) {
            "git_command" -> argsJson.put("args", cmd.argument)
            "run_command" -> argsJson.put("command", "${cmd.commandType} ${cmd.argument}".trim())
            "write_to_file" -> {
                argsJson.put("file_path", cmd.argument)
                argsJson.put("content", cmd.payload)
            }
            else -> {}
        }

        val toolExecution = ToolExecution(
            toolName = "/${cmd.commandType} ${cmd.argument}".trim(),
            arguments = argsJson.toString(2),
            status = ToolStatus.RUNNING
        )

        withContext(Dispatchers.Main) {
            messages.add(
                ChatMessage(
                    role = MessageRole.TOOL,
                    content = "Slash Command: /${cmd.commandType}",
                    toolExecutions = mutableListOf(toolExecution)
                )
            )
            saveCurrentConversation()
        }

        val res = toolExecutor.execute(toolName, argsJson)

        withContext(Dispatchers.Main) {
            toolExecution.result = res
            toolExecution.status = if (res.startsWith("Error") || res.contains("Failed")) ToolStatus.FAILED else ToolStatus.SUCCESS
            saveCurrentConversation()
        }
    }
}
