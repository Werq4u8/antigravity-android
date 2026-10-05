package com.google.antigravity.mobile.model

import java.util.UUID

enum class MessageRole {
    USER,
    AGENT,
    TOOL,
    SYSTEM
}

enum class ToolStatus {
    RUNNING,
    SUCCESS,
    FAILED
}

data class ToolExecution(
    val id: String = UUID.randomUUID().toString(),
    val toolName: String,
    val arguments: String,
    var result: String = "",
    var status: ToolStatus = ToolStatus.RUNNING,
    val timestamp: Long = System.currentTimeMillis()
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: MessageRole,
    val content: String,
    val toolExecutions: MutableList<ToolExecution> = mutableListOf(),
    val timestamp: Long = System.currentTimeMillis()
)
