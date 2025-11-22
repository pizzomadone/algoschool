#!/bin/bash

# Script to run the custom renderer test

set -e

echo "======================================"
echo "Custom Flowchart Renderer - Test"
echo "======================================"
echo ""

# Compile if needed
if [ ! -d "build" ] || [ ! -f "build/TestCustomRenderer.class" ]; then
    echo "🔨 Compiling..."
    javac -d build src/FlowchartNode.java src/FlowchartEdge.java src/CustomFlowchartPanel.java src/TestCustomRenderer.java
    echo "✅ Compilation successful!"
    echo ""
fi

echo "🚀 Running test application..."
java -cp build TestCustomRenderer
