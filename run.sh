#!/bin/bash

# Script to run the Flowchart Editor application

set -e

JGRAPHX_VERSION="4.2.2"
LIB_DIR="lib"
JGRAPHX_JAR="${LIB_DIR}/jgraphx-${JGRAPHX_VERSION}.jar"
BUILD_DIR="build"

echo "======================================"
echo "Flowchart Editor - Run"
echo "======================================"
echo ""

# Check if compiled
if [ ! -d "${BUILD_DIR}" ] || [ ! "$(ls -A ${BUILD_DIR})" ]; then
    echo "⚠️  Project not compiled yet!"
    echo "Running build script..."
    echo ""
    ./build.sh
fi

# Check if JGraphX is present
if [ ! -f "${JGRAPHX_JAR}" ]; then
    echo "❌ JGraphX library not found!"
    echo "Running build script to download it..."
    echo ""
    ./build.sh
fi

echo "🚀 Starting Flowchart Editor..."
echo ""

# Run the application
java -cp "${BUILD_DIR}:${JGRAPHX_JAR}" FlowchartEditorApp
