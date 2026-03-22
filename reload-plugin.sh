#!/usr/bin/env bash
# Rebuild the plugin and hot-reload it in the running Docker test server.
set -euo pipefail

echo "Building plugin..."
./gradlew build

CONTAINER="herald-test-mc-server"
PLUGIN_NAME="Herald"
JAR=$(find build/libs -maxdepth 1 -name '*.jar' ! -name '*-sources.jar' ! -name '*-javadoc.jar' | head -1)

if [ -z "$JAR" ]; then
  echo "❌ No JAR found in build/libs/"
  exit 1
fi

echo "Copying $JAR into container..."
docker cp "$JAR" "$CONTAINER:/testmcserver/plugins/$PLUGIN_NAME.jar"

echo "Reloading plugin via ServerUtils..."
docker exec "$CONTAINER" rcon-cli "serverutils reload $PLUGIN_NAME"

echo "✅ $PLUGIN_NAME reloaded."
