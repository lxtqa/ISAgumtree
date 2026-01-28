#!/usr/bin/env bash
set -e

./gradlew build -x test

rm -rf ./dist/build/distributions/gumtree-4.0.0-beta7-SNAPSHOT

unzip ./dist/build/distributions/gumtree-4.0.0-beta7-SNAPSHOT.zip \
  -d ./dist/build/distributions
