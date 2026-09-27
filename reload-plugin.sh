#!/usr/bin/env bash
# Rebuild the plugin and restart the running Docker test server so it loads the new JAR.
set -euo pipefail

CONTAINER="herald-test-mc-server"
# post-create.sh copies the JAR from this directory into plugins/ every time the container starts.
BUILD_TARGET="/testmcserver-build/Herald/target"
PLUGINS_DIR="/testmcserver/plugins"

if [ "$(docker inspect -f '{{.State.Running}}' "$CONTAINER" 2>/dev/null)" != "true" ]; then
  echo "❌ Container $CONTAINER is not running. Start the test server with ./up.sh first."
  exit 1
fi

echo "Building plugin..."
./gradlew build

JAR=$(find build/libs -maxdepth 1 -name 'Herald-*.jar' ! -name '*-sources.jar' ! -name '*-javadoc.jar' | head -1)

if [ -z "$JAR" ]; then
  echo "❌ No JAR found in build/libs/"
  exit 1
fi

echo "Replacing the plugin JAR in the container..."
# Remove every Herald JAR first so a version bump, or a Herald.jar left by an older
# version of this script, does not leave two copies of the plugin in plugins/.
docker exec "$CONTAINER" sh -c "rm -f $BUILD_TARGET/Herald-*.jar $PLUGINS_DIR/Herald*.jar"
docker cp "$JAR" "$CONTAINER:$BUILD_TARGET/"

echo "Restarting the test server..."
docker restart "$CONTAINER" > /dev/null

echo "✅ Copied $(basename "$JAR") and restarted $CONTAINER. Follow startup with: docker logs -f $CONTAINER"
