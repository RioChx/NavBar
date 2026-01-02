#!/usr/bin/env bash
# Simplified Gradle wrapper that uses system gradle if available
if command -v gradle >/dev/null 2>&1; then
    gradle "$@"
else
    echo "Gradle not found in PATH. Please install Gradle or use a standard wrapper."
    exit 1
fi
