#!/usr/bin/env bash

set -e

COMPOSE=docker-compose

${COMPOSE} run --rm groovy test/AllTestsRunner.groovy
