# Music Box

Native Android music playback, offline downloads, lyrics and shared control of phones, browsers and Alexa Echo devices.

**Source snapshot:** 2.4.54 (106), `feat/alexa-backend`, reviewed 2026-10-25. Source versions and verified debug builds are not the same as published GitHub releases.

[Changelog](CHANGELOG.md) · [Architecture](ARCHITECTURE.md) · [Published releases](https://github.com/arookiecoder-ip/MusicBox-APP/releases) · [Companion backend](https://github.com/arookiecoder-ip/youtube-music-alexa-skill)

## Features

- **Playback:** Media3/ExoPlayer, background MediaSession controls, shuffle/repeat, sleep timer, equalizer, bass/volume processing and playback speed. Phone playback and remote playback use separate owners and controllers.
- **Shared output:** select an online phone, browser or Echo; transfer the queue, exact occurrence, play intent and cursor using acknowledged, token-fenced handoffs. Offline outputs are hidden/reconciled without silently reviving a saved song.
- **Queue:** explicit row selection starts playback, including from pause; occurrence-bound metadata and stable gestures avoid old-song artwork flashes and missed taps. Failed requests retain the requested song and Retry instead of silently returning to the old song.
- **Streaming and offline:** start progressive backend audio without waiting for a complete server download. The phone retains the current stream and can warm the next five locally; explicit downloads and collection manifests remain separate and persistent.
- **Discovery:** native YouTube Music Home, search/suggestions, Explore, account collections and paginated artist/album/playlist pages. Pasted YouTube links wait for submission and then open Now Playing. Radio can build a fresh queue from the current song on every request.
- **Artists:** on-demand artist portraits; top songs, releases, artist playlists, featured-on collections and music-video songs. Additional provider shelves, such as live performances and items from your library, retain their names and item actions; empty shelves stay hidden. Videos show a five-song preview and a separate full-list page. These entries use the music playback path; this is not a video-rendering player.
- **Lyrics:** timestamp-preserving romanization with damaged-cache repair; LRCLib timed lookup, caption/YouTube Music fallbacks, synchronized Now Playing and mini-player presentation, offsets and optional romanization. Tracks without usable timestamps retain plain lyrics.
- **Interface:** consistent queue/navigation surfaces, compact create/rename/delete playlist dialogs, square collection/download buttons, phone/tablet layouts, speed-dial song menus and labels, compact matching navigation/mini-player surfaces, artwork-derived player blends and a changing Home blend. The status-bar fill clears when scrolling back to the top; expanded artist descriptions keep the banner fixed.
- **Permissions:** microphone access is neither requested nor declared in the installed manifest.
- **Account and tools:** owner login/TOTP, YouTube account and audio-cookie reconnect flows, likes, playlists, artist subscriptions, listening history, Jam sharing and bounded playback/crash diagnostics. Spotify metadata/import and local-library support remain in the codebase.

## Install an update-compatible build

Requires **Android 8.0 / API 26 or later**. Current package: `in.synthora.musicbox`.

For the current feature build, use the signed **app-debug** artifact from [Android Feature Build](https://github.com/arookiecoder-ip/MusicBox-APP/actions/workflows/android-feature.yml), on `feat/alexa-backend`, with `stable_release=false`.

The [verified 2.4.54 build](https://github.com/arookiecoder-ip/MusicBox-APP/actions/runs/40160853198) contains a debug APK with version code 106. Actions artifacts have finite retention. For published stable packages, check [GitHub Releases](https://github.com/arookiecoder-ip/MusicBox-APP/releases) rather than assuming every source version has a release tag.

Android updates require the same package, the original signing certificate and a compatible version code. The 2.4.54 artifact was checked against the signed 2.4.51 feature build. Earlier `com.example.juke` builds have a different package identity. A disposable-key installation also cannot be updated with a differently signed APK.

The `music-box-review-apk` from Android CI is a review artifact; it does not establish update compatibility. Preserve the feature branch's signing identity and follow [APK signing](.github/signing/README.md). Never replace the key to solve an installation failure.

## Connect the backend

The [self-hosted companion](https://github.com/arookiecoder-ip/youtube-music-alexa-skill) supplies authenticated catalog/library access, progressive audio and authoritative output/queue state. Sign in through the app's account flow. An Echo additionally requires the custom skill and an Amazon/Alexa session on the server; browser/phone audio does not require an online Echo.

For a configured build, use a developer checkout with JDK 17 and Android SDK 36, and set these non-secret/example values in an untracked `local.properties`:

```properties
ALEXA_SESSION_BASE_URL=https://music.example.com
ALEXA_BASE_URL=https://music.example.com
POSTHOG_API_KEY=
POSTHOG_HOST=
```

Session and audio origins can be split only when both services are configured for that arrangement. Current builds set `ALEXA_API_KEY` to an empty value and obtain scoped audio credentials through the owner session; do not embed a server-wide API key in the APK.

For this deployment, build installable APKs **only in GitHub Actions**, on `feat/alexa-backend`. Keep `stable_release=false` for debug builds. The workflow restores/verifies the original signing key from its preserved cache or configured secret and fails if the pinned identity cannot be recovered. Do not build Android packages on the music server. Historical release and optional benchmark workflows are described in the signing/performance docs; they are not required to update this debug build.

## Streaming storage and network behavior

| Storage | Lifetime and purpose |
| --- | --- |
| Backend current-track bytes | One anonymous temporary current-song file; no persistent multi-song server cache. |
| Phone stream cache | Default 256 MiB, configurable 128–1024 MiB; current plus a rolling next-five set. Separate from saved downloads. |
| Explicit downloads | Persistent local files and Room-backed download/collection manifests. |
| Catalog/Home/artwork/lyrics metadata | Separate caches; removing server song storage does not remove these features. |

Speculative next-song caching respects connectivity, buffering, metered-data preferences, battery saver, thermal state and low battery. Normal conditions permit five complete upcoming streams; moderate thermal pressure permits one; severe constraints stop speculation. Demand caching of the selected song remains separate. Settings and storage limits can prevent all five from being fully cached. Older temporary entries retire as the retained queue window advances.

Available local audio can recover from network interruptions. A DNS failure or a missing uncached track still needs connectivity. Online ownership is reconciled before resuming shared playback; no distributed system can guarantee exclusive playback across devices isolated by a network partition.

## Development and verification

| Area | Entry point |
| --- | --- |
| Application/bootstrap/navigation | [JukeApplication.kt](app/src/main/java/in/synthora/musicbox/JukeApplication.kt), [MainActivity.kt](app/src/main/java/in/synthora/musicbox/MainActivity.kt) |
| UI and state | [screens](app/src/main/java/in/synthora/musicbox/ui/screens), [components](app/src/main/java/in/synthora/musicbox/ui/components), [viewmodels](app/src/main/java/in/synthora/musicbox/viewmodels) |
| Audio/output/queues/downloads | [services](app/src/main/java/in/synthora/musicbox/services) |
| Sessions, catalog, audio credentials, lyrics | [network](app/src/main/java/in/synthora/musicbox/network) |
| Persistent data | [database](app/src/main/java/in/synthora/musicbox/database) |
| JVM and emulator regressions | [unit tests](app/src/test), [Android tests](app/src/androidTest) |

[Android CI](.github/workflows/android.yml) runs JVM tests and assembles a review APK. [Android Feature Build](.github/workflows/android-feature.yml) produces the signed debug artifact and runs focused protocol/UI regressions on feature pushes. It also exposes optional broader phone/tablet and performance checks.

Verified functional snapshot: **451 JVM tests and 34 API 35 emulator tests passed** in [Android CI](https://github.com/arookiecoder-ip/MusicBox-APP/actions/runs/40160853247) and [Feature Build](https://github.com/arookiecoder-ip/MusicBox-APP/actions/runs/40160853198). The optional broad tablet suite was not part of that run. Test counts describe that commit, not a permanent minimum.

See [architecture](ARCHITECTURE.md), [performance measurement](docs/PERFORMANCE_MEASUREMENT.md), [feature parity](docs/alexa-web-feature-parity.md) and [release/signing history](CHANGELOG.md).

## Known limits

- Alexa seeks restart custom-skill playback at an offset. The latest single live probe acknowledged the seek request in about 90 ms but confirmed playback after about 3.7 seconds; first-byte streaming latency is a different measurement.
- Last-second Alexa queue edits have deterministic race/recovery coverage. The longer live boundary probe was interrupted by a separate Pause request and did not prove an audible end-to-end transition.
- Phone battery-discharge and Samsung Android 17 frame-rate gains have not been measured. Recent changes reduce avoidable rendering, polling and cache scans without promising a battery percentage or eliminating every device-specific stall.
- Call recovery preserves intent, allows explicit Play during a call and honors explicit Pause. Emulator coverage does not certify every carrier/Bluetooth/OEM audio route.
- Lyrics availability, YouTube authorization and provider response formats remain external dependencies.

## Contributing and license

Report reproducible issues with the app version, output device, steps and redacted diagnostics at [Issues](https://github.com/arookiecoder-ip/MusicBox-APP/issues). Keep credentials, keystores and APK binaries out of commits. Target the active feature branch unless the maintainer directs otherwise.

Licensed under [GNU AGPL v3](LICENSE). Music Box retains its JUKES history and original contributor credits: **MADE WITH ❤️ BY MEEK**.
