#!/bin/bash
# ============================================
#   Bilingual IR Search Engine - Build Script
# ============================================

set -e

# ---- CONFIGURATION: Set your paths here ----
# For Linux:
JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk}"
JAVAFX_LIB="${JAVAFX_LIB:-/opt/javafx-sdk-21/lib}"

# For Mac (Homebrew):
# JAVA_HOME="$(/usr/libexec/java_home -v 17)"
# JAVAFX_LIB="/usr/local/opt/openjfx/libexec/lib"

# ---- Derived ----
JAVAC="$JAVA_HOME/bin/javac"
JAVA="$JAVA_HOME/bin/java"
SRC_DIR="src"
OUT_DIR="out/classes"

mkdir -p "$OUT_DIR/ui"

echo "============================================"
echo "  Bilingual IR Search Engine - Build Script"
echo "============================================"
echo

echo "[1/3] Compiling Java sources..."
"$JAVAC" \
    --module-path "$JAVAFX_LIB" \
    --add-modules javafx.controls,javafx.fxml \
    -encoding UTF-8 \
    -d "$OUT_DIR" \
    -sourcepath "$SRC_DIR" \
    "$SRC_DIR"/engine/*.java \
    "$SRC_DIR"/ui/MainApp.java

echo "[2/3] Copying resources..."
cp "$SRC_DIR/ui/styles.css" "$OUT_DIR/ui/" 2>/dev/null || true

echo "[3/3] Launching application..."
echo
"$JAVA" \
    --module-path "$JAVAFX_LIB" \
    --add-modules javafx.controls,javafx.fxml \
    -cp "$OUT_DIR" \
    ui.MainApp
