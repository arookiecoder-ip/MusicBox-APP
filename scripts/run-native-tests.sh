#!/usr/bin/env bash
# Offline emulator fixtures only. Keep diagnostics even when instrumentation hangs.
set -euo pipefail
finish() {
  task_status=$?
  trap - EXIT
  set +e
  timeout 15 adb pull /sdcard/Android/data/in.synthora.musicbox/files/performance performance-results >/dev/null 2>&1
  if [ "$task_status" -ne 0 ]; then
    timeout 15 adb logcat -d -t 2000 > emulator-logcat.txt 2>&1
    timeout 15 adb shell dumpsys activity activities > emulator-activities.txt 2>&1
    timeout 15 adb shell dumpsys gfxinfo in.synthora.musicbox > emulator-gfxinfo.txt 2>&1
  fi
  cat ui-results.txt 2>/dev/null
  exit "$task_status"
}
trap finish EXIT
timeout 480 adb shell am instrument -w -e class "$1" in.synthora.musicbox.test/androidx.test.runner.AndroidJUnitRunner > ui-results.txt
! grep -E 'FAILURES|INSTRUMENTATION_FAILED|Process crashed' ui-results.txt
grep -E 'OK \([0-9]+ tests?\)' ui-results.txt
