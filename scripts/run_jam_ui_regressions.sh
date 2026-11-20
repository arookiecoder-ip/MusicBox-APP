#!/usr/bin/env bash
set -euo pipefail
trap 'adb pull /sdcard/Android/data/in.synthora.musicbox/files/jam-screenshots jam-screenshots || true' EXIT
sh ./gradlew :app:connectedDebugAndroidTest -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true -Pandroid.testInstrumentationRunnerArguments.class=in.synthora.musicbox.JamManagementUiTest,in.synthora.musicbox.JamVisualRegressionTest,in.synthora.musicbox.PlaybackPresentationRegressionTest,in.synthora.musicbox.AccountUiRegressionTest,in.synthora.musicbox.RemoteNotificationLifecycleTest --no-daemon
