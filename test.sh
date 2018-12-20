#!/usr/bin/env bash

set -e

COMPOSE=docker-compose

for file in test/*.groovy; do
  echo "Running $file..."
  ${COMPOSE} run --rm groovy $file
done
