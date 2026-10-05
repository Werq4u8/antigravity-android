package com.google.antigravity.mobile.agent

import org.json.JSONArray
import org.json.JSONObject

object ToolDefinitions {

    val SYSTEM_PROMPT = """
        ты можешь управлять файловой системой, выполнять команды, и т.д. но для выполнения команд ставь /, кстати у тебя есть тоже самое, ты можешь управлять моим гитхабом через гит.

        You are Antigravity Mobile, an autonomous AI software engineer running directly inside an Android application.
        You have direct access to an on-device Android build toolchain, filesystem, and Git.
        You can build complete, installable Android APK applications right on this phone, and sync changes with GitHub!

        Rules for Commands & Actions:
        1. When executing shell or system commands in plain text responses, prefix the command with `/`, for example:
           /git status
           /git add .
           /git commit -m "feat: added new UI"
           /git push origin main
           /build
           /install
           /write <file_path>
           ```code```
        2. You also have formal function-calling tools available: `view_file`, `write_to_file`, `replace_file_content`, `list_files`, `run_command`, `git_command`, `build_apk`, `install_apk`.
        3. If `build_apk` reports compilation errors, read the error output, use `replace_file_content` to fix issues, and compile again.
        4. When the APK builds successfully, trigger `install_apk` or `/install` so the user can test the app on their device.
    """.trimIndent()

    fun getDeclarations(): JSONArray {
        val array = JSONArray()

        // 1. view_file
        array.put(JSONObject().apply {
            put("name", "view_file")
            put("description", "Reads the contents of a file in the workspace.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("file_path", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Relative or absolute path to the file.")
                    })
                    put("start_line", JSONObject().apply {
                        put("type", "INTEGER")
                        put("description", "Optional start line number (1-based).")
                    })
                    put("end_line", JSONObject().apply {
                        put("type", "INTEGER")
                        put("description", "Optional end line number (1-based).")
                    })
                })
                put("required", JSONArray().put("file_path"))
            })
        })

        // 2. write_to_file
        array.put(JSONObject().apply {
            put("name", "write_to_file")
            put("description", "Creates or overwrites a file in the workspace.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("file_path", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Path to the destination file.")
                    })
                    put("content", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Full text content of the file.")
                    })
                })
                put("required", JSONArray().put("file_path").put("content"))
            })
        })

        // 3. replace_file_content
        array.put(JSONObject().apply {
            put("name", "replace_file_content")
            put("description", "Replaces an exact snippet of text in a file with new code.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("file_path", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Path to the file to modify.")
                    })
                    put("target_content", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Existing text to replace.")
                    })
                    put("replacement_content", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "New replacement code.")
                    })
                })
                put("required", JSONArray().put("file_path").put("target_content").put("replacement_content"))
            })
        })

        // 4. list_files
        array.put(JSONObject().apply {
            put("name", "list_files")
            put("description", "Lists files and folders in a workspace directory.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("directory_path", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Subdirectory to list (empty for workspace root).")
                    })
                })
            })
        })

        // 5. run_command
        array.put(JSONObject().apply {
            put("name", "run_command")
            put("description", "Runs a shell command inside the app's local environment.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("command", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Command string to execute.")
                    })
                })
                put("required", JSONArray().put("command"))
            })
        })

        // 6. git_command
        array.put(JSONObject().apply {
            put("name", "git_command")
            put("description", "Manages the GitHub / Git repository (commit, push, pull, status, diff).")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("args", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Git arguments, e.g. 'status', 'add .', 'commit -m \"Update\"', 'push origin main'.")
                    })
                })
                put("required", JSONArray().put("args"))
            })
        })

        // 7. build_apk
        array.put(JSONObject().apply {
            put("name", "build_apk")
            put("description", "Compiles the Android project directly on this Android device into a signed APK.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("project_dir", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Path to project folder (or empty for current workspace).")
                    })
                })
            })
        })

        // 8. install_apk
        array.put(JSONObject().apply {
            put("name", "install_apk")
            put("description", "Prompts the user to install the compiled APK file.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("apk_path", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Path to the .apk file (optional if build_apk was recently run).")
                    })
                })
            })
        })

        return array
    }
}
