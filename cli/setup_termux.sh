#!/data/data/com.termux/files/usr/bin/bash
# setup_termux.sh - Installs build toolchain and Antigravity Mobile CLI on Termux

set -e

echo -e "\033[1;36m========================================================\033[0m"
echo -e "\033[1;36m      Installing Antigravity Mobile On-Device Toolchain \033[0m"
echo -e "\033[1;36m========================================================\033[0m"

echo "[1/4] Updating Termux packages..."
pkg update -y

echo "[2/4] Installing OpenJDK, Python, AAPT, ECJ, DX, and build tools..."
pkg install -y openjdk-17 python aapt ecj dx apksigner git curl zip unzip

echo "[3/4] Preparing Antigravity directory & Android framework JAR..."
mkdir -p ~/.antigravity/workspace
mkdir -p ~/.antigravity/tools

# Download minimal android.jar (API 30 / 33) if not present
if [ ! -f ~/.antigravity/android.jar ]; then
    echo "Downloading android.jar framework library..."
    curl -L "https://raw.githubusercontent.com/Sable/android-platforms/master/android-30/android.jar" -o ~/.antigravity/android.jar || true
fi

# Generate debug keystore for signing APKs
if [ ! -f ~/.antigravity/debug.keystore ]; then
    echo "Generating debug keystore..."
    keytool -genkeypair -v \
        -keystore ~/.antigravity/debug.keystore \
        -alias androiddebugkey \
        -keyalg RSA \
        -keysize 2048 \
        -validity 10000 \
        -dname "CN=Android Debug,O=Android,C=US" \
        -storepass android \
        -keypass android
fi

echo "[4/4] Setting environment variables..."
grep -q "ANTIGRAVITY" ~/.bashrc || cat << 'EOF' >> ~/.bashrc
export ANDROID_JAR="$HOME/.antigravity/android.jar"
export ANTIGRAVITY_WORKSPACE="$HOME/.antigravity/workspace"
alias antigravity="python3 $HOME/antigravity-android/cli/antigravity_cli.py"
EOF

echo -e "\n\033[1;32mInstallation complete!\033[0m"
echo "To run the agent:"
echo "1. export GEMINI_API_KEY='your-key-here'"
echo "2. python3 antigravity_cli.py"
