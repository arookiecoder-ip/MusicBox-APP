# Repeatable performance measurements

The `optimizedDebug` APK is **debug-signed**, profileable and R8 optimized. It has
no production-release publication. It keeps the package/data/signing identity of
the debug APK, so install only one variant at a time. Avoid comparing ordinary
debug with optimized debug as if code changes alone caused the difference.

Build both app and instrumentation:

```
./gradlew :app:assembleOptimizedDebug :performance:assembleOptimizedDebug
```

Install the app APK on the Samsung; sign in and prepare the same cached Home,
Library, selected song and queue used for the baseline. Fix screen orientation,
brightness, refresh-rate setting, power mode, charging state and playback output.
Stop unrelated foreground apps. Do not capture while navigating Android Settings.

Run startup/browsing/player benchmarks on a physical device:

```
./gradlew :performance:connectedOptimizedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=in.synthora.musicbox.performance.PlaybackPerformanceTest
```

Test preconditions are explicit assumptions: missing sign-in/song/portrait queue
causes a **skipped journey**, not a claimed passing performance result. Results
include frame timing and Perfetto traces; inspect the main thread, RenderThread,
network/caching workers and GC around missed deadlines. Compare the same journey
and variant before/after. Also test tablet landscape queue and locked local audio
manually; preserve offline playback and remote device switching correctness.

## Baseline Profiles

Run on a suitable API 28+ rooted emulator or a supported API 33+ physical device:

```
./gradlew :performance:connectedOptimizedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=in.synthora.musicbox.performance.PlaybackBaselineProfileTest
```

Review the generated `baseline-prof.txt` from the instrumentation output. Only
a real, successful journey's output should be copied into
`app/src/main/baseline-prof.txt`; rebuild and run the same benchmarks to verify it.
Diagnostics now identifies the build variant so debug/performance captures can be distinguished.
No fabricated or empty application profile is shipped. Library-provided profiles
continue to be packaged by AGP. Device generation and quantitative performance,
battery and heating results remain pending until the device runs are completed.

For request/battery comparison, capture the same one-minute foreground journey
and a longer locked-playback interval. Use `adb shell dumpsys gfxinfo
in.synthora.musicbox reset` before each run, then capture `top`, `gfxinfo`,
`thermalservice`, and a Perfetto trace. Respect the CPU report's per-core scale;
full swap or a cached temperature alone does not prove an application leak or
current thermal throttling.

## Bounded cross-platform capture

With ADB connected and Music Box open, run:

```
python scripts/capture_performance.py --output captures/after-optimized-debug --seconds 60
```

Use `--adb C:\path\to\adb.exe` on Windows if ADB is not on PATH, and `--serial`
when multiple devices are attached. The tool exits on missing/unauthorized ADB,
never overwrites an existing capture, and saves CPU/rendering plus before/after
thermal readings. It does not collect logcat, account cookies or network URLs.
