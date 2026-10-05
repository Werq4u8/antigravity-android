#!/usr/bin/env python3
"""
builder.py - On-Device Android APK Build Engine
Compiles an Android project into an installable signed APK directly on the device
using AAPT/AAPT2, ECJ/javac, D8/DX, and apksigner.
"""

import os
import sys
import shutil
import argparse
import subprocess
from pathlib import Path
import zipfile

def find_tool(name: str) -> Optional[str]:
    # Check PATH first
    p = shutil.which(name)
    if p:
        return p
    # Common Termux & Android locations
    search_paths = [
        "/data/data/com.termux/files/usr/bin",
        "/data/data/com.termux/files/usr/lib/jvm/openjdk-17/bin",
        "/data/data/com.termux/files/usr/lib/jvm/openjdk-21/bin",
        "/system/bin",
        "/system/xbin",
    ]
    for sp in search_paths:
        candidate = Path(sp) / name
        if candidate.exists() and os.access(candidate, os.X_OK):
            return str(candidate)
    return None

def find_android_jar() -> Optional[str]:
    # Environment variable check
    if os.environ.get("ANDROID_JAR") and os.path.exists(os.environ["ANDROID_JAR"]):
        return os.environ["ANDROID_JAR"]
    # Common locations on Android / Termux
    candidates = [
        "/data/data/com.termux/files/usr/share/java/android.jar",
        "/data/data/com.termux/files/home/android-sdk/platforms/android-34/android.jar",
        "/data/data/com.termux/files/home/android-sdk/platforms/android-33/android.jar",
        "/data/data/com.termux/files/home/android-sdk/platforms/android-31/android.jar",
        "/sdcard/android.jar",
        "./tools/android.jar",
        os.path.expanduser("~/.antigravity/android.jar")
    ]
    for c in candidates:
        if os.path.exists(c):
            return str(Path(c).resolve())
    return None

def ensure_debug_keystore(keystore_path: Path) -> bool:
    if keystore_path.exists():
        return True
    keytool = find_tool("keytool")
    if not keytool:
        return False
    cmd = [
        keytool,
        "-genkeypair",
        "-v",
        "-keystore", str(keystore_path),
        "-alias", "androiddebugkey",
        "-keyalg", "RSA",
        "-keysize", "2048",
        "-validity", "10000",
        "-dname", "CN=Android Debug,O=Android,C=US",
        "-storepass", "android",
        "-keypass", "android"
    ]
    res = subprocess.run(cmd, capture_output=True, text=True)
    return res.returncode == 0

def run_cmd(cmd: list, cwd: Path = None) -> tuple[int, str, str]:
    res = subprocess.run(
        cmd,
        cwd=cwd,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True
    )
    return res.returncode, res.stdout, res.stderr

def build_project(project_dir: Path) -> dict:
    manifest = project_dir / "AndroidManifest.xml"
    res_dir = project_dir / "res"
    src_dir = project_dir / "src"
    build_dir = project_dir / "build"

    if not manifest.exists():
        return {"success": False, "error": f"AndroidManifest.xml not found in {project_dir}"}
    if not src_dir.exists():
        return {"success": False, "error": f"src directory not found in {project_dir}"}

    android_jar = find_android_jar()
    if not android_jar:
        return {
            "success": False,
            "error": "android.jar not found! Please place android.jar in ~/.antigravity/android.jar or set ANDROID_JAR env variable."
        }

    # Preparation
    gen_dir = build_dir / "gen"
    classes_dir = build_dir / "classes"
    dex_dir = build_dir / "dex"
    output_apk = build_dir / "app-debug.apk"
    unsigned_apk = build_dir / "app-unsigned.apk"
    keystore = Path.home() / ".antigravity" / "debug.keystore"
    keystore.parent.mkdir(parents=True, exist_ok=True)
    ensure_debug_keystore(keystore)

    for d in [gen_dir, classes_dir, dex_dir]:
        if d.exists():
            shutil.rmtree(d)
        d.mkdir(parents=True, exist_ok=True)

    logs = []

    # 1. Resource compilation & R.java generation
    aapt2 = find_tool("aapt2")
    aapt = find_tool("aapt")

    if aapt2:
        logs.append("[1/5] Compiling resources with AAPT2...")
        compiled_res_zip = build_dir / "compiled_res.zip"
        if res_dir.exists():
            code, out, err = run_cmd([aapt2, "compile", "--dir", str(res_dir), "-o", str(compiled_res_zip)])
            if code != 0:
                return {"success": False, "step": "aapt2 compile", "error": err or out}
        
        logs.append("[2/5] Linking resources & generating R.java with AAPT2...")
        link_cmd = [
            aapt2, "link",
            "-I", android_jar,
            "--manifest", str(manifest),
            "--java", str(gen_dir),
            "-o", str(unsigned_apk),
            "--auto-add-overlay"
        ]
        if res_dir.exists() and compiled_res_zip.exists():
            link_cmd.append(str(compiled_res_zip))
        code, out, err = run_cmd(link_cmd)
        if code != 0:
            return {"success": False, "step": "aapt2 link", "error": err or out}
    elif aapt:
        logs.append("[1/2] Generating R.java and packaging resources with AAPT...")
        cmd = [
            aapt, "package",
            "-f",
            "-m",
            "-J", str(gen_dir),
            "-M", str(manifest),
            "-I", android_jar,
            "-F", str(unsigned_apk)
        ]
        if res_dir.exists():
            cmd.extend(["-S", str(res_dir)])
        code, out, err = run_cmd(cmd)
        if code != 0:
            return {"success": False, "step": "aapt package", "error": err or out}
    else:
        return {"success": False, "error": "Neither aapt2 nor aapt found on system."}

    # 2. Compile Java sources (ECJ or javac)
    java_files = list(src_dir.rglob("*.java")) + list(gen_dir.rglob("*.java"))
    if not java_files:
        return {"success": False, "error": "No Java source files found."}

    logs.append(f"[3/5] Compiling {len(java_files)} Java files...")
    ecj = find_tool("ecj")
    javac = find_tool("javac")

    file_list_arg = [str(f) for f in java_files]

    if ecj:
        compile_cmd = [
            ecj,
            "-1.8",
            "-nowarn",
            "-cp", android_jar,
            "-d", str(classes_dir)
        ] + file_list_arg
        code, out, err = run_cmd(compile_cmd)
        if code != 0:
            return {"success": False, "step": "ECJ Java Compilation", "error": err or out}
    elif javac:
        compile_cmd = [
            javac,
            "-source", "1.8",
            "-target", "1.8",
            "-cp", android_jar,
            "-d", str(classes_dir)
        ] + file_list_arg
        code, out, err = run_cmd(compile_cmd)
        if code != 0:
            return {"success": False, "step": "javac Compilation", "error": err or out}
    else:
        return {"success": False, "error": "Neither ecj nor javac found on system."}

    # 3. DEX conversion (D8 or DX)
    logs.append("[4/5] Converting bytecode to Dalvik DEX...")
    class_files = list(classes_dir.rglob("*.class"))
    if not class_files:
        return {"success": False, "error": "No .class files generated by compiler."}

    d8 = find_tool("d8")
    dx = find_tool("dx")

    dex_file = dex_dir / "classes.dex"

    if d8:
        d8_cmd = [
            d8,
            "--lib", android_jar,
            "--output", str(dex_dir)
        ] + [str(f) for f in class_files]
        code, out, err = run_cmd(d8_cmd)
        if code != 0:
            return {"success": False, "step": "D8 dexing", "error": err or out}
    elif dx:
        dx_cmd = [
            dx,
            "--dex",
            f"--output={dex_file}",
            str(classes_dir)
        ]
        code, out, err = run_cmd(dx_cmd)
        if code != 0:
            return {"success": False, "step": "DX dexing", "error": err or out}
    else:
        return {"success": False, "error": "Neither d8 nor dx found on system."}

    if not dex_file.exists():
        return {"success": False, "error": "classes.dex was not generated."}

    # 4. Inject classes.dex into APK using Python's zipfile
    logs.append("[5/5] Packaging classes.dex into APK...")
    # Open unsigned APK, append classes.dex
    with zipfile.ZipFile(unsigned_apk, 'a', compression=zipfile.ZIP_DEFLATED) as apk_zip:
        apk_zip.write(dex_file, arcname="classes.dex")

    # 5. Sign APK with apksigner
    apksigner = find_tool("apksigner")
    if apksigner and keystore.exists():
        logs.append("Signing APK with debug key...")
        sign_cmd = [
            apksigner, "sign",
            "--ks", str(keystore),
            "--ks-pass", "pass:android",
            "--ks-key-alias", "androiddebugkey",
            "--key-pass", "pass:android",
            "--out", str(output_apk),
            str(unsigned_apk)
        ]
        code, out, err = run_cmd(sign_cmd)
        if code != 0:
            # Fallback to copy unsigned to output if signing fails
            shutil.copy2(unsigned_apk, output_apk)
            logs.append(f"Warning: apksigner failed ({err}), provided unsigned apk.")
        else:
            logs.append("APK signed successfully!")
    else:
        shutil.copy2(unsigned_apk, output_apk)
        logs.append("Notice: apksigner or keystore not found, outputting unsigned APK.")

    apk_size_kb = output_apk.stat().st_size / 1024
    logs.append(f"Build Completed: {output_apk.name} ({apk_size_kb:.1f} KB)")

    return {
        "success": True,
        "apk_path": str(output_apk.resolve()),
        "size_kb": apk_size_kb,
        "logs": "\n".join(logs)
    }

def main():
    parser = argparse.ArgumentParser(description="On-Device Android APK Builder")
    parser.add_argument("--project", "-p", required=True, help="Path to project directory")
    args = parser.parse_args()

    project_path = Path(args.project).resolve()
    result = build_project(project_path)

    if result["success"]:
        print(result["logs"])
        print(f"\nAPK Generated: {result['apk_path']}")
        sys.exit(0)
    else:
        print(f"Error in step [{result.get('step', 'general')}]:", file=sys.stderr)
        print(result.get("error", "Unknown build error"), file=sys.stderr)
        sys.exit(1)

if __name__ == "__main__":
    main()
