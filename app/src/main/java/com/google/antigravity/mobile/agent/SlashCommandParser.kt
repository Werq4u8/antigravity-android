package com.google.antigravity.mobile.agent

import java.io.File

data class ParsedCommand(
    val commandType: String,
    val argument: String,
    val payload: String = ""
)

object SlashCommandParser {

    /**
     * Extracts all commands starting with "/" from Gemini's response text.
     * Examples:
     *   /git status
     *   /git commit -m "Initial commit"
     *   /git push origin main
     *   /build
     *   /install
     *   /run ls -la
     *   /write src/MainActivity.java
     *   ```java ... ```
     */
    fun parseCommands(text: String): List<ParsedCommand> {
        val commands = mutableListOf<ParsedCommand>()
        val lines = text.lines()
        var i = 0

        while (i < lines.size) {
            val line = lines[i].trim()
            if (line.startsWith("/")) {
                val fullCmd = line.substring(1).trim()
                val parts = fullCmd.split("\\s+".toRegex(), limit = 2)
                val cmdType = parts[0]
                val arg = if (parts.size > 1) parts[1] else ""

                if (cmdType == "write" || cmdType == "create") {
                    // Check if subsequent lines have code block
                    val payloadBuilder = StringBuilder()
                    var j = i + 1
                    var inCodeBlock = false
                    while (j < lines.size) {
                        val nextLine = lines[j]
                        if (nextLine.trim().startsWith("```")) {
                            if (inCodeBlock) {
                                // End of code block
                                j++
                                break
                            } else {
                                inCodeBlock = true
                            }
                        } else if (inCodeBlock) {
                            payloadBuilder.append(nextLine).append("\n")
                        } else if (nextLine.trim().startsWith("/")) {
                            // Hit another command
                            break
                        }
                        j++
                    }
                    commands.add(ParsedCommand(cmdType, arg, payloadBuilder.toString()))
                    i = j - 1
                } else {
                    commands.add(ParsedCommand(cmdType, arg))
                }
            }
            i++
        }

        return commands
    }
}
