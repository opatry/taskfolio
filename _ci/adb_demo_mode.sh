#!/usr/bin/env bash

origin=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd) || exit

# shellcheck disable=SC1091
. "${origin}/utils.sh"

if [ $# -lt 1 ]; then
  echo "Usage: $0 [on|off] [hhmm]" >&2
  exit
fi

cmd=${1}

hhmm=${2:-"1200"}

# see available commands https://android.googlesource.com/platform/frameworks/base/+/master/packages/SystemUI/docs/demo_mode.md
case "${cmd,,}" in
  on)
  warn "Don't forget to switch phone in English language"
  info "Enabling demo mode"
  adb shell settings put global sysui_demo_allowed 1
  adb shell am broadcast -a com.android.systemui.demo -e command enter || exit
  adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm "${hhmm}"
  adb shell am broadcast -a com.android.systemui.demo -e command battery -e plugged false -e level 100 -e powersave false
  adb shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4
  adb shell am broadcast -a com.android.systemui.demo -e command network -e mobile show -e datatype none -e level 4 -e fully true
  adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false
  adb shell cmd overlay enable com.android.internal.systemui.navbar.gestural
  ;;
off)
  info "Disabling demo mode"
  adb shell am broadcast -a com.android.systemui.demo -e command exit
  adb shell settings put global sysui_demo_allowed 0
  adb shell cmd overlay enable com.android.internal.systemui.navbar.threebutton
  ;;
*)
  step_error "Invalid command '${cmd}'"
esac

step_done
