#!/usr/bin/env bash

set -euo pipefail

origin=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd) || exit

# shellcheck disable=SC1091
. "${origin}/utils.sh"

terminate_crashpad_handler() {
  # Emulator might hang forever in some circumstances
  # Try to kill problematic process
  # See https://github.com/opatry/taskfolio/issues/248
  # See https://github.com/ReactiveCircus/android-emulator-runner/issues/385
  # /!\ this is Linux impl, on macOS signals & process kill is slightly different, would need adjustments

  # Try SIGTERM first
  info "Try stopping ${YELLOW}crashpad_handler${RESET}…"
  pkill -f -SIGTERM crashpad_handler || true

  # Wait for the process to terminate, and SIGKILL after 5 seconds if still alive
  sleep 5
  if pgrep -f crashpad_handler >/dev/null; then
    # 🔥🔥🔥
    warn "${YELLOW}crashpad_handler${RESET} still not terminated, try killing ☠️…"
    pkill -f -SIGKILL crashpad_handler || true
  fi
}

trap terminate_crashpad_handler EXIT

./gradlew --no-daemon --parallel \
  :tasks-app-android:connectedStoreDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=net.opatry.tasks.app.test.e2e.TaskfolioE2ETest
