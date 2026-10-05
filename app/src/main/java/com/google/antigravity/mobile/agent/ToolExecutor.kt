package com.google.antigravity.mobile.agent

import android.content.Context
import com.google.antigravity.mobile.compiler.OnDeviceBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class ToolExecutor(
    private val context: Context,
    val workspaceDir: File = File(context.filesDir, "workspace").apply { mkdirs() },
    private val builder: OnDeviceBuilder = OnDeviceBuilder(context)
) {
    var lastGeneratedApk: File? = null
        private set

    suspend fun execute(toolName: String, args: JSONObject): String = withContext(Dispatchers.IO) {
        try {
            when (toolName) {
                "view_file" -> {
                    val filePath = args.getString("file_path")
                    val startLine = args.optInt("start_line", 1)
                    val endLine = if (args.has("end_line")) args.getInt("end_line") else null
                    viewFile(filePath, startLine, endLine)
                }
                "write_to_file" -> {
                    val filePath = args.getString("file_path")
                    val content = args.getString("content")
                    writeToFile(filePath, content)
                }
                "replace_file_content" -> {
                    val filePath = args.getString("file_path")
                    val targetContent = args.getString("target_content")
                    val replacementContent = args.getString("replacement_content")
                    replaceFileContent(filePath, targetContent, replacementContent)
                }
                "list_files" -> {
                    val dirPath = args.optString("directory_path", "")
                    listFiles(dirPath)
                }
                "run_command" -> {
                    val command = args.getString("command")
                    runCommand(command)
                }
                "git_command" -> {
                    val gitArgs = args.getString("args")
                    runGitCommand(gitArgs)
                }
                "build_apk" -> {
                    val projectDirArg = args.optString("project_dir", "")
                    val target = if (projectDirArg.isNotBlank()) resolveFile(projectDirArg) else workspaceDir
                    val result = builder.buildProject(target)
                    if (result.success) {
                        lastGeneratedApk = result.apkFile
                        "BUILD SUCCESSFUL!\n${result.logs}\nAPK File: ${result.apkFile?.absolutePath}"
                    } else {
                        "BUILD FAILED!\nError: ${result.error}\nLogs:\n${result.logs}"
                    }
                }
                "install_apk" -> {
                    val apkPath = args.optString("apk_path", "")
                    val targetApk = if (apkPath.isNotBlank()) resolveFile(apkPath) else lastGeneratedApk
                    if (targetApk == null || !targetApk.exists()) {
                        "Error: APK file not found to install."
                    } else {
                        val launched = builder.launchInstallIntent(targetApk)
                        if (launched) {
                            "Prompted system PackageInstaller to install APK: ${targetApk.name}"
                        } else {
                            "Failed to launch PackageInstaller. File exists at: ${targetApk.absolutePath}"
                        }
                    }
                }
                else -> "Error: Unknown tool '$toolName'"
            }
        } catch (e: Exception) {
            "Error executing $toolName: ${e.message}"
        }
    }

    private fun resolveFile(pathStr: String): File {
        val f = File(pathStr)
        return if (f.isAbsolute) f else File(workspaceDir, pathStr)
    }

    private fun viewFile(pathStr: String, startLine: Int, endLine: Int?): String {
        val file = resolveFile(pathStr)
        if (!file.exists()) return "Error: File '$pathStr' not found."
        val lines = file.readLines()
        val s = maxOf(1, startLine)
        val e = minOf(lines.size, endLine ?: lines.size)
        if (s > e) return "(empty line range)"
        val selected = lines.subList(s - 1, e)
        return selected.mapIndexed { idx, line -> "${s + idx}: $line" }.joinToString("\n")
    }

    private fun writeToFile(pathStr: String, content: String): String {
        val file = resolveFile(pathStr)
        file.parentFile?.mkdirs()
        file.writeText(content)
        return "Successfully wrote ${content.length} characters to ${file.name}"
    }

    private fun replaceFileContent(pathStr: String, target: String, replacement: String): String {
        val file = resolveFile(pathStr)
        if (!file.exists()) return "Error: File '$pathStr' not found."
        val text = file.readText()
        if (!text.contains(target)) {
            return "Error: target_content not found in ${file.name}."
        }
        val newText = text.replaceFirst(target, replacement)
        file.writeText(newText)
        return "Successfully replaced text in ${file.name}."
    }

    private fun listFiles(pathStr: String): String {
        val dir = if (pathStr.isBlank()) workspaceDir else resolveFile(pathStr)
        if (!dir.exists() || !dir.isDirectory) return "Error: Directory '$pathStr' does not exist."
        val items = dir.listFiles()?.sortedBy { it.name } ?: return "(empty directory)"
        return items.joinToString("\n") {
            if (it.isDirectory) "[DIR]  ${it.name}/" else "[FILE] ${it.name} (${it.length()} bytes)"
        }
    }

    private fun runCommand(command: String): String {
        return try {
            val process = ProcessBuilder("/system/bin/sh", "-c", command)
                .directory(workspaceDir)
                .redirectErrorStream(true)
                .start()
            val output = BufferedReader(InputStreamReader(process.inputStream)).readText()
            val code = process.waitFor()
            "Exit Code: $code\nOutput:\n$output"
        } catch (e: Exception) {
            "Error running command: ${e.message}"
        }
    }

    fun runGitCommand(gitArgs: String): String {
        return try {
            val fullCmd = "git $gitArgs"
            val process = ProcessBuilder("/system/bin/sh", "-c", fullCmd)
                .directory(workspaceDir)
                .redirectErrorStream(true)
                .start()
            val output = BufferedReader(InputStreamReader(process.inputStream)).readText()
            val code = process.waitFor()
            if (code == 0) {
                "Git Output:\n$output"
            } else {
                "Git Command Failed (code $code):\n$output"
            }
        } catch (e: Exception) {
            "Git Error: ${e.message}"
        }
    }
}
