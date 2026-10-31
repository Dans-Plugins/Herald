#!/usr/bin/env bash
./down.sh
# compose.yml bind-mounts this directory with the long volume syntax, which does not
# create a missing host path, so a fresh clone would fail to start without it.
mkdir -p testmcserver
docker compose up --build -d
