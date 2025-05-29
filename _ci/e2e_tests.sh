#!/usr/bin/env bash

set -euo pipefail

./gradlew --no-daemon --parallel \
  :tasks-app-android:connectedStoreDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=net.opatry.tasks.app.test.e2e.TaskfolioE2ETest
