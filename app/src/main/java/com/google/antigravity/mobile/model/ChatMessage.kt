package com.google.antigravity.mobile.model

import org.json.JSONArray
import org.json.JSONObject
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
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("toolName", toolName)
        put("arguments", arguments)
        put("result", result)
        put("status", status.name)
        put("timestamp", timestamp)
    }

    companion object {
        fun fromJson(json: JSONObject): ToolExecution = ToolExecution(
            id = json.optString("id", UUID.randomUUID().toString()),
            toolName = json.optString("toolName", ""),
            arguments = json.optString("arguments", ""),
            result = json.optString("result", ""),
            status = try { ToolStatus.valueOf(json.optString("status", "SUCCESS")) } catch (e: Exception) { ToolStatus.SUCCESS },
            timestamp = json.optLong("timestamp", System.currentTimeMillis())
        )
    }
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: MessageRole,
    val content: String,
    val toolExecutions: MutableList<ToolExecution> = mutableListOf(),
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("role", role.name)
        put("content", content)
        put("timestamp", timestamp)
        val toolsArray = JSONArray()
        toolExecutions.forEach { toolsArray.put(it.toJson()) }
        put("toolExecutions", toolsArray)
    }

    companion object {
        fun fromJson(json: JSONObject): ChatMessage {
            val role = try { MessageRole.valueOf(json.optString("role", "USER")) } catch (e: Exception) { MessageRole.USER }
            val tools = mutableListOf<ToolExecution>()
            val toolsArray = json.optJSONArray("toolExecutions")
            if (toolsArray != null) {
                for (i in 0 until toolsArray.length()) {
                    tools.add(ToolExecution.fromJson(toolsArray.getJSONObject(i)))
                }
            }
            return ChatMessage(
                id = json.optString("id", UUID.randomUUID().toString()),
                role = role,
                content = json.optString("content", ""),
                toolExecutions = tools,
                timestamp = json.optLong("timestamp", System.currentTimeMillis())
            )
        }
    }
}

data class ConversationSession(
    val id: String = UUID.randomUUID().toString(),
    var title: String = "New Conversation",
    val createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
    }

    companion object {
        fun fromJson(json: JSONObject): ConversationSession = ConversationSession(
            id = json.optString("id", UUID.randomUUID().toString()),
            title = json.optString("title", "New Conversation"),
            createdAt = json.optLong("createdAt", System.currentTimeMillis()),
            updatedAt = json.optLong("updatedAt", System.currentTimeMillis())
        )
    }
}
