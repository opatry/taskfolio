#!/usr/bin/env bash

set -euo pipefail

demo_mode_on=${1:-true}

function start_clean_status_bar {
    # Start demo mode
    adb shell settings put global sysui_demo_allowed 1

    # Display time 10:30
    adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 1030
    # Display full mobile data without type
    adb shell am broadcast -a com.android.systemui.demo -e command network -e mobile show -e level 4 -e datatype false
    adb shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4 -e fully true
    # Hide notifications
    adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false
    # Show full battery but not in charging state
    adb shell am broadcast -a com.android.systemui.demo -e command battery -e plugged false -e level 100
    # Light mode
    adb shell "cmd uimode night no"
}

function stop_clean_status_bar {
    adb shell am broadcast -a com.android.systemui.demo -e command exit
}

if ${demo_mode_on}; then
  start_clean_status_bar
else
  stop_clean_status_bar
fi
