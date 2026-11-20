#!/usr/bin/env bash
# Runs only against the disposable GitHub Actions emulator.
set -euo pipefail
baseline=$1
candidate=$2
mkdir -p install-results
pkg=in.synthora.musicbox
adb install -r "$baseline" | tee install-results/baseline-install.txt
before=$(adb shell dumpsys package "$pkg" | sed -n 's/.*firstInstallTime=//p' | head -1 | tr -d '\r')
test -n "$before"
adb install -r "$candidate" | tee install-results/upgrade-install.txt
after=$(adb shell dumpsys package "$pkg" | sed -n 's/.*firstInstallTime=//p' | head -1 | tr -d '\r')
test "$before" = "$after"
adb shell dumpsys package "$pkg" > install-results/upgraded-package.txt
adb shell am force-stop "$pkg"
adb shell am start -W -n "$pkg/.MainActivity" | tee install-results/upgrade-launch.txt
sleep 5
adb shell pidof "$pkg" | tee install-results/upgraded-pid.txt
test -s install-results/upgraded-pid.txt
adb uninstall "$pkg"
adb install "$candidate" | tee install-results/fresh-install.txt
adb logcat -c
adb shell am start -W -n "$pkg/.MainActivity" | tee install-results/fresh-launch.txt
sleep 5
adb shell pidof "$pkg" | tee install-results/fresh-pid.txt
test -s install-results/fresh-pid.txt
adb logcat -d -s AndroidRuntime > install-results/runtime.txt
if rg -q 'FATAL EXCEPTION' install-results/runtime.txt; then cat install-results/runtime.txt; exit 1; fi
adb shell dumpsys package "$pkg" > install-results/fresh-package.txt
adb exec-out screencap -p > install-results/stable-launch.png
