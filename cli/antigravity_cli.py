#!/usr/bin/env python3
"""
Antigravity Android CLI - On-device AI Agentic Coding Shell for Android.
Runs natively on Android (e.g. inside Termux or Android Shell) using Google Gemini API.
Capable of reading, editing files, running shell commands, and autonomously building & installing APKs.
"""

import os
import sys
import json
import time
import shutil
import urllib.request
import urllib.error
import subprocess
from pathlib import Path
from typing import List, Dict, Any, Optional

# Color codes for terminal UI
class Colors:
    HEADER = '\033[95m'
    BLUE = '\033[94m'
    CYAN = '\033[96m'
    GREEN = '\033[92m'
    WARNING = '\033[93m'
    FAIL = '\033[91m'
    ENDC = '\033[0m'
    BOLD = '\033[1m'
    DIM = '\033[2m'

SYSTEM_PROMPT = """ты можешь управлять файловой системой, выполнять команды, и т.д. но для выполнения команд ставь /, кстати у тебя есть тоже самое, ты можешь управлять моим гитхабом через гит.

You are Antigravity Mobile, an autonomous AI coding assistant running directly on an Android device.
You have access to a toolchain to create, inspect, modify Android projects, manage Git/GitHub, and compile them into native APK files directly on the device.

Your capabilities include:
1. File operations: `view_file`, `write_to_file`, `replace_file_content`, `list_directory`.
2. Shell commands & Git: `run_command` (executes shell commands in Android/Termux), `git_command` (runs git status, commit, push to GitHub).
3. APK Compilation: `build_apk` (compiles the Android project into an APK using on-device aapt2, ecj/javac, d8, and apksigner).
4. APK Installation: `install_apk` (prompts the user to install the compiled APK via Android PackageInstaller or Termux).
5. Slash Commands: When suggesting or executing commands in text, prefix them with `/`, for example `/git status`, `/git push origin main`, `/build`, `/install`.
"""

# Gemini Tool Declarations (OpenAPI / Gemini schema)
TOOL_DECLARATIONS = [
    {
        "name": "view_file",
        "description": "View the contents of a file on the device.",
        "parameters": {
            "type": "OBJECT",
            "properties": {
                "file_path": {
                    "type": "STRING",
                    "description": "Absolute or relative path to the file."
                },
                "start_line": {
                    "type": "INTEGER",
                    "description": "Optional 1-based start line number."
                },
                "end_line": {
                    "type": "INTEGER",
                    "description": "Optional 1-based end line number."
                }
            },
            "required": ["file_path"]
        }
    },
    {
        "name": "write_to_file",
        "description": "Write or overwrite content to a file. Creates parent directories if needed.",
        "parameters": {
            "type": "OBJECT",
            "properties": {
                "file_path": {
                    "type": "STRING",
                    "description": "Path to the file to create or overwrite."
                },
                "content": {
                    "type": "STRING",
                    "description": "Full text content of the file."
                }
            },
            "required": ["file_path", "content"]
        }
    },
    {
        "name": "replace_file_content",
        "description": "Replace a specific snippet of text in a file with new content.",
        "parameters": {
            "type": "OBJECT",
            "properties": {
                "file_path": {
                    "type": "STRING",
                    "description": "Path to the file."
                },
                "target_content": {
                    "type": "STRING",
                    "description": "Exact text substring to replace."
                },
                "replacement_content": {
                    "type": "STRING",
                    "description": "New replacement text."
                }
            },
            "required": ["file_path", "target_content", "replacement_content"]
        }
    },
    {
        "name": "list_directory",
        "description": "List files and subdirectories in a directory.",
        "parameters": {
            "type": "OBJECT",
            "properties": {
                "directory_path": {
                    "type": "STRING",
                    "description": "Directory path (defaults to current working directory)."
                }
            }
        }
    },
    {
        "name": "run_command",
        "description": "Execute a shell command on the Android device.",
        "parameters": {
            "type": "OBJECT",
            "properties": {
                "command": {
                    "type": "STRING",
                    "description": "Shell command line to execute."
                }
            },
            "required": ["command"]
        }
    },
    {
        "name": "git_command",
        "description": "Execute git commands to manage GitHub repositories (status, add, commit, push, pull, remote).",
        "parameters": {
            "type": "OBJECT",
            "properties": {
                "args": {
                    "type": "STRING",
                    "description": "Git arguments, e.g. 'status', 'add .', 'commit -m \"msg\"', 'push origin main'."
                }
            },
            "required": ["args"]
        }
    },
    {
        "name": "build_apk",
        "description": "Build an Android APK directly on device using the on-device AAPT2 + ECJ/javac + D8 + apksigner toolchain.",
        "parameters": {
            "type": "OBJECT",
            "properties": {
                "project_dir": {
                    "type": "STRING",
                    "description": "Root directory of the Android project containing AndroidManifest.xml and src/."
                }
            },
            "required": ["project_dir"]
        }
    },
    {
        "name": "install_apk",
        "description": "Trigger installation of an APK file on the Android device.",
        "parameters": {
            "type": "OBJECT",
            "properties": {
                "apk_path": {
                    "type": "STRING",
                    "description": "Path to the signed APK file."
                }
            },
            "required": ["apk_path"]
        }
    }
]

class GeminiClient:
    def __init__(self, api_key: str, model: str = "gemini-2.5-flash"):
        self.api_key = api_key
        self.model = model
        self.base_url = f"https://generativelanguage.googleapis.com/v1beta/models/{self.model}:generateContent?key={self.api_key}"

    def send_chat(self, contents: List[Dict[str, Any]], tools: Optional[List[Dict[str, Any]]] = None) -> Dict[str, Any]:
        payload: Dict[str, Any] = {
            "contents": contents,
            "systemInstruction": {
                "parts": [{"text": SYSTEM_PROMPT}]
            }
        }
        if tools:
            payload["tools"] = [{"functionDeclarations": tools}]

        data = json.dumps(payload).encode("utf-8")
        req = urllib.request.Request(
            self.base_url,
            data=data,
            headers={"Content-Type": "application/json"}
        )

        try:
            with urllib.request.urlopen(req, timeout=60) as resp:
                result = json.loads(resp.read().decode("utf-8"))
                return result
        except urllib.error.HTTPError as e:
            error_body = e.read().decode("utf-8")
            raise RuntimeError(f"Gemini API Error {e.code}: {error_body}")
        except Exception as e:
            raise RuntimeError(f"Request failed: {str(e)}")

class ToolExecutor:
    def __init__(self, workspace_dir: str):
        self.workspace_dir = Path(workspace_dir).resolve()

    def _resolve_path(self, p: str) -> Path:
        path = Path(p)
        if not path.is_absolute():
            path = self.workspace_dir / path
        return path.resolve()

    def view_file(self, file_path: str, start_line: Optional[int] = None, end_line: Optional[int] = None) -> str:
        target = self._resolve_path(file_path)
        if not target.exists() or not target.is_file():
            return f"Error: File '{file_path}' does not exist."
        try:
            with open(target, "r", encoding="utf-8", errors="replace") as f:
                lines = f.readlines()
            total = len(lines)
            s = max(1, start_line or 1)
            e = min(total, end_line or total)
            selected = lines[s - 1:e]
            numbered = [f"{s + idx}: {line}" for idx, line in enumerate(selected)]
            return "".join(numbered) if numbered else "(empty file)"
        except Exception as e:
            return f"Error reading file: {str(e)}"

    def write_to_file(self, file_path: str, content: str) -> str:
        target = self._resolve_path(file_path)
        try:
            target.parent.mkdir(parents=True, exist_ok=True)
            with open(target, "w", encoding="utf-8") as f:
                f.write(content)
            return f"Successfully wrote {len(content)} characters to {target.name}"
        except Exception as e:
            return f"Error writing file: {str(e)}"

    def replace_file_content(self, file_path: str, target_content: str, replacement_content: str) -> str:
        target = self._resolve_path(file_path)
        if not target.exists():
            return f"Error: File '{file_path}' does not exist."
        try:
            with open(target, "r", encoding="utf-8") as f:
                content = f.read()
            if target_content not in content:
                return f"Error: target_content not found in '{file_path}'."
            count = content.count(target_content)
            new_content = content.replace(target_content, replacement_content, 1)
            with open(target, "w", encoding="utf-8") as f:
                f.write(new_content)
            return f"Replaced 1 occurrence of target_content in {target.name} (previously matched {count} times)."
        except Exception as e:
            return f"Error replacing content: {str(e)}"

    def list_directory(self, directory_path: Optional[str] = None) -> str:
        target = self._resolve_path(directory_path or ".")
        if not target.exists() or not target.is_dir():
            return f"Error: Directory '{directory_path}' not found."
        entries = []
        try:
            for item in sorted(target.iterdir()):
                item_type = "[DIR] " if item.is_dir() else "[FILE]"
                size = f"({item.stat().st_size} bytes)" if item.is_file() else ""
                entries.append(f"{item_type} {item.name} {size}")
            return "\n".join(entries) if entries else "(empty directory)"
        except Exception as e:
            return f"Error listing directory: {str(e)}"

    def run_command(self, command: str) -> str:
        try:
            res = subprocess.run(
                command,
                shell=True,
                cwd=str(self.workspace_dir),
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True,
                timeout=120
            )
            out = res.stdout.strip()
            err = res.stderr.strip()
            result = []
            if out:
                result.append(f"STDOUT:\n{out}")
            if err:
                result.append(f"STDERR:\n{err}")
            result.append(f"Exit Code: {res.returncode}")
            return "\n".join(result)
        except subprocess.TimeoutExpired:
            return "Error: Command timed out after 120 seconds."
        except Exception as e:
            return f"Error running command: {str(e)}"

    def git_command(self, args: str) -> str:
        cmd = f"git {args}"
        return self.run_command(cmd)

    def build_apk(self, project_dir: str) -> str:
        target_dir = self._resolve_path(project_dir)
        builder_script = Path(__file__).parent / "builder.py"
        if not builder_script.exists():
            return "Error: builder.py script not found."

        cmd = [sys.executable, str(builder_script), "--project", str(target_dir)]
        res = subprocess.run(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True
        )
        output = res.stdout + ("\n" + res.stderr if res.stderr else "")
        if res.returncode == 0:
            return f"SUCCESS:\n{output.strip()}"
        else:
            return f"BUILD FAILED (Exit Code {res.returncode}):\n{output.strip()}"

    def install_apk(self, apk_path: str) -> str:
        target = self._resolve_path(apk_path)
        if not target.exists():
            return f"Error: APK '{apk_path}' does not exist."
        
        # In Termux: termux-open or am start
        if shutil.which("termux-open"):
            cmd = f"termux-open '{str(target)}'"
            res = subprocess.run(cmd, shell=True, capture_output=True, text=True)
            return f"Triggered termux-open: {res.stdout} {res.stderr}"
        elif shutil.which("am"):
            cmd = f"am start -a android.intent.action.VIEW -d 'file://{str(target)}' -t 'application/vnd.android.package-archive'"
            res = subprocess.run(cmd, shell=True, capture_output=True, text=True)
            return f"Triggered am start: {res.stdout} {res.stderr}"
        else:
            return f"APK is ready at {str(target)}. On Android, use a file manager or termux-open to install."

    def execute(self, tool_name: str, args: Dict[str, Any]) -> str:
        method = getattr(self, tool_name, None)
        if not method:
            return f"Error: Unknown tool '{tool_name}'"
        try:
            return method(**args)
        except TypeError as te:
            return f"Error calling {tool_name}: invalid arguments ({str(te)})"
        except Exception as e:
            return f"Error during {tool_name}: {str(e)}"

class AntigravityMobileAgent:
    def __init__(self, api_key: str, workspace_dir: str, model: str = "gemini-2.5-flash"):
        self.client = GeminiClient(api_key, model=model)
        self.executor = ToolExecutor(workspace_dir)
        self.workspace_dir = workspace_dir
        self.conversation_history: List[Dict[str, Any]] = []

    def run_turn(self, user_prompt: str):
        print(f"\n{Colors.BOLD}{Colors.CYAN}User > {Colors.ENDC}{user_prompt}\n")
        self.conversation_history.append({
            "role": "user",
            "parts": [{"text": user_prompt}]
        })

        max_steps = 15
        step = 0

        while step < max_steps:
            step += 1
            print(f"{Colors.DIM}[Step {step}] Asking Gemini...{Colors.ENDC}")
            try:
                response = self.client.send_chat(self.conversation_history, tools=TOOL_DECLARATIONS)
            except Exception as e:
                print(f"{Colors.FAIL}Error communicating with Gemini: {e}{Colors.ENDC}")
                break

            candidates = response.get("candidates", [])
            if not candidates:
                print(f"{Colors.WARNING}No response candidates received.{Colors.ENDC}")
                break

            candidate = candidates[0]
            content = candidate.get("content", {})
            parts = content.get("parts", [])

            # Add model's response to history
            self.conversation_history.append({
                "role": "model",
                "parts": parts
            })

            tool_calls = [p["functionCall"] for p in parts if "functionCall" in p]
            text_parts = [p["text"] for p in parts if "text" in p]

            for text in text_parts:
                print(f"\n{Colors.GREEN}{Colors.BOLD}Antigravity > {Colors.ENDC}{text}\n")
                # Parse and execute slash commands if present in text
                for line in text.splitlines():
                    trimmed = line.strip()
                    if trimmed.startswith("/") and len(trimmed) > 1:
                        cmd_str = trimmed[1:].strip()
                        print(f"{Colors.YELLOW}[Executing Slash Command] /{cmd_str}{Colors.ENDC}")
                        if cmd_str.startswith("git "):
                            git_res = self.executor.git_command(cmd_str[4:])
                            print(f"{Colors.DIM}{git_res}{Colors.ENDC}")
                        elif cmd_str.startswith("build"):
                            build_res = self.executor.build_apk(".")
                            print(f"{Colors.DIM}{build_res}{Colors.ENDC}")
                        elif cmd_str.startswith("install"):
                            install_res = self.executor.install_apk("build/app-debug.apk")
                            print(f"{Colors.DIM}{install_res}{Colors.ENDC}")
                        else:
                            sh_res = self.executor.run_command(cmd_str)
                            print(f"{Colors.DIM}{sh_res}{Colors.ENDC}")

            if not tool_calls:
                # No more tools called, turn is complete
                break

            # Execute tool calls and prepare function responses
            response_parts = []
            for call in tool_calls:
                name = call.get("name")
                args = call.get("args", {})
                print(f"{Colors.BLUE}[Tool Call] {Colors.BOLD}{name}{Colors.ENDC}{Colors.BLUE}({json.dumps(args, ensure_ascii=False)}){Colors.ENDC}")

                result = self.executor.execute(name, args)

                # Show preview of result
                preview = result.strip()
                if len(preview) > 300:
                    preview = preview[:300] + "... [truncated]"
                print(f"{Colors.DIM} -> Result:\n{preview}{Colors.ENDC}\n")

                response_parts.append({
                    "functionResponse": {
                        "name": name,
                        "response": {
                            "output": result
                        }
                    }
                })

            # Append tool execution results back to conversation history
            self.conversation_history.append({
                "role": "function",
                "parts": response_parts
            })

def main():
    print(f"{Colors.BOLD}{Colors.HEADER}======================================================{Colors.ENDC}")
    print(f"{Colors.BOLD}{Colors.HEADER}     Google Antigravity Mobile - On-Device Android Agent    {Colors.ENDC}")
    print(f"{Colors.BOLD}{Colors.HEADER}======================================================{Colors.ENDC}")
    print(f"Autonomous agent shell for building native APKs right on your device.\n")

    api_key = os.environ.get("GEMINI_API_KEY")
    if not api_key:
        api_key = input("Enter your Gemini API Key: ").strip()
        if not api_key:
            print(f"{Colors.FAIL}Error: GEMINI_API_KEY is required to run the agent.{Colors.ENDC}")
            sys.exit(1)

    workspace = os.environ.get("ANTIGRAVITY_WORKSPACE", os.getcwd())
    print(f"Active Workspace: {Colors.BOLD}{workspace}{Colors.ENDC}")
    
    agent = AntigravityMobileAgent(api_key=api_key, workspace_dir=workspace)

    print(f"\nType your request (e.g. 'Создай простое приложение фонарик и собери APK') or 'exit' to quit.\n")

    while True:
        try:
            user_input = input(f"{Colors.BOLD}Antigravity-Android > {Colors.ENDC}").strip()
            if not user_input:
                continue
            if user_input.lower() in ["exit", "quit", "q"]:
                print("Goodbye!")
                break
            agent.run_turn(user_input)
        except (KeyboardInterrupt, EOFError):
            print("\nExiting...")
            break

if __name__ == "__main__":
    main()
