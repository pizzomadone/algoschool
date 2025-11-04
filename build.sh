#!/bin/bash

# Script to build the Flowchart Editor application
# Downloads JGraphX library if needed and compiles the project

set -e

JGRAPHX_VERSION="4.2.2"
JGRAPHX_URL="https://repo1.maven.org/maven2/com/github/jgraph/jgraphx/${JGRAPHX_VERSION}/jgraphx-${JGRAPHX_VERSION}.jar"
LIB_DIR="lib"
JGRAPHX_JAR="${LIB_DIR}/jgraphx-${JGRAPHX_VERSION}.jar"
BUILD_DIR="build"

echo "======================================"
echo "Flowchart Editor - Build Script"
echo "======================================"
echo ""

# Create directories
mkdir -p "${LIB_DIR}"
mkdir -p "${BUILD_DIR}"

# Download JGraphX if not present
if [ ! -f "${JGRAPHX_JAR}" ]; then
    echo "📦 Downloading JGraphX library..."
    wget -q "${JGRAPHX_URL}" -O "${JGRAPHX_JAR}"
    echo "✅ JGraphX downloaded successfully!"
else
    echo "✅ JGraphX library already present"
fi

echo ""
echo "🔨 Compiling Java sources..."

# Compile all Java files
javac -d "${BUILD_DIR}" -cp "${JGRAPHX_JAR}" src/*.java

if [ $? -eq 0 ]; then
    echo "✅ Compilation successful!"
    echo ""
    echo "To run the application, execute:"
    echo "  ./run.sh"
else
    echo "❌ Compilation failed!"
    exit 1
fi
