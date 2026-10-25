# Music Box changelog

Coverage reviewed 2026-10-25: Android `feat/alexa-backend` through `22aa629d15a8d6f362d36b20c3dc81695eb7efb0`, plus companion backend `08139f16aac382d80703d59104838c4ecd1fb40a`. [Current architecture](ARCHITECTURE.md) · [Backend changelog](https://github.com/arookiecoder-ip/youtube-music-alexa-skill/blob/master/CHANGELOG.md).

Entries below distinguish source/debug builds from published releases. Dates follow commit metadata; they are not evidence that an APK was published on that date. The version-introduction ledger, PR register and full reachable commit index preserve detailed provenance. Existing inherited release notes are retained as historical records.

## 2.4.61 (113)

- Recognize song titles with different word spacing, prefer strong song matches over loosely matching playlists, and preserve explicit artist/playlist queries.

- Separate download-cookie checks from account refresh and bound their total wait; keep replacement available during account checks.
- Remove the redundant now-playing heading while retaining close and song-option controls.

- Let ordinary queue-grip swipes scroll; hold and drag the grip to reorder without opening song options.
- Strengthen the active queue-row background and animate its artwork bars only during playback.
- Complete committed delete swipes off-screen before collapsing the row; retained or failed rows return to position.

## 2.4.60 (112)

- Update existing remote media notifications in place during track and artwork changes instead of requesting another foreground-service start while backgrounded.
- Include bounded, anonymous remote-notification lifecycle events in diagnostics.

## 2.4.59 (111)

- Preserve remote playback notifications when the local media session updates during track changes; local cleanup no longer removes remote-owned controls.
- Keep remote controls buffering through unconfirmed track changes, with a bounded timeout; confirmed pause, stop and receiver changes remain authoritative.
- Make the floating collection play button circular with an explicit white play icon.

## 2.4.58 (110)

- Wait for stable post-call audio mode, recover once from a delayed call focus-loss callback, and preserve explicit pause/output/song changes and other apps’ playback.
- Give the account WebView explicit viewport bounds.

## 2.4.57 (109)

- Make the queue heading, queue options, song options and close icon explicitly white on the dark queue surface.

## 2.4.56 (108)

- Open account sign-in in the actual app account browser, wait for authenticated cookies before loading, and offer account switching for Amazon and YouTube Music. Use responsive dedicated server sign-in pages instead of the VNC viewer.
- Mark the profile red when Amazon is disconnected. Retain active voice-started Alexa playback as read-only, explain blocked controls, and allow handoff to another device.

## 2.4.55 (107)

- Remove empty Echo-device messages from the playback picker and Settings; offline devices remain hidden.

## 2.4.54 (106)

- Keep playlist-dialog keyboard requests scoped to the dialog window and prevent dismissal from hiding a subsequently opened search keyboard.

## 2.4.53 (105)

- Match queue surfaces to the navigation dock, refine create/rename/delete playlist dialogs, and use square collection/download floating buttons.

- Preserve additional artist shelves supplied by YouTube Music, including live performances and items from your library, with the correct song, album, artist and playlist actions.
- Includes the artist playlist/featured shelves, normal song-row spacing and microphone-permission removal from the 2.4.52 feature updates.

## 2.4.52 (104)

- Reduce extra spacing between artist top-song and video rows.
- Show playlists by the artist and featured-on collections on artist pages when available, with full collection navigation and empty-shelf handling.

## 2.4.51 (103)

- Preserve synchronized lyrics when romanization is enabled, including whole-second, comma-fraction and repeated timestamps. Repair cached romanizations that lost timing in both the lyrics page and mini player; retain source timing during network failures.

## 2.4.50 (102) — 2026-10-25

- Clear the black status-bar background after scrolling back to the top; page changes still reset it.
- Show five artist music-video songs and open a separate full-list page with browse-ID pagination and stale-response fencing. Keep the artist banner at its original height when More expands the description.
- Cache ambient drawing brushes and remove an extra full-screen offscreen mask while keeping Home/player blends and smooth color changes.
- Reduce ordinary audio-mode checks to a five-second fallback, retaining immediate mode/focus wakeups and fast interruption recovery.
- Wake remote-state reads from existing presence events; stable healthy foreground observation has a five-second backup, with faster processing/unhealthy/legacy fallbacks. Preserve exponential error backoff despite presence changes.
- Prune the temporary stream cache when its retained current/next-five set changes, rather than repeatedly scanning/canceling work for unchanged input. Keep current-song demand caching, rolling next-five eligibility and saved downloads.
- Reduce app seek coalescing from 220 to 120 ms. Companion backend dispatch debounce drops from 700 to 150 ms; it also fences late Alexa stop/finish events and recovers live successors after boundary edits. These changes do not eliminate Alexa skill/device startup latency.
- Keep a live search input session across navigation re-taps instead of disposing/hiding its IME. Preserve close/submit cancellation and replacement-field cleanup fences.

Evidence: [444 JVM tests](https://github.com/arookiecoder-ip/MusicBox-APP/actions/runs/40127897537), [30 API 35 interaction tests and signed debug packaging](https://github.com/arookiecoder-ip/MusicBox-APP/actions/runs/40127897516). Package/certificate and version-code increase from 2.4.49 were checked. Companion [server checks](https://github.com/arookiecoder-ip/youtube-music-alexa-skill/actions/runs/40127521237) passed 841 Python tests, 31 JavaScript files, 44 skill tests and 35 browser tests.

Limits: battery/FPS gains were not measured on the physical phone. One live Echo seek was acknowledged in about 90 ms and confirmed after about 3.7 seconds; a longer boundary probe was interrupted by a separate Pause request. Automated race success is not an audible end-to-end timing guarantee.

## 2.4.49 (101) — 2026-10-25

- Handle audio focus arriving before call mode, repeated call states and missed/delayed mode callbacks. Preserve explicit Pause/Play and media/ownership fencing.
- Show artist video-song shelves from existing response data; normalize malformed/duplicate rows and video identities without confusing channel IDs with queue entries. Retain usable video-only artist content on partial errors.
- Fade Now Playing artwork into the actual ambient background. Artist expanded-banner sizing was revised again in 2.4.50 to keep its original fixed height.
- Make the first Search navigation issue a real focus request; fence queued keyboard cleanup across window changes and submission.
- Improve exact title/artist ranking, preserve Indic vowel marks and remix/live distinctions, recover mixed-result shelves, quote suggestion input and reject stale suggestion errors.
- Add black status-area fill after scrolling; return-to-top removal follows in 2.4.50.

## 2.4.48 (100) — 2026-10-24

- Restore official LRCLib endpoint/query metadata, timed-result selection, rate limiting and caption/YouTube Music fallbacks.
- Share robust timed lyric parsing/highlighting between Now Playing and the mini-player, including paused, before-first-line and backward-seek states. Cancel old lookups and preserve newer timed lyrics across output changes.
- Add a changing multicolor Home blend and smooth artwork-derived Now Playing background. Compact navigation and match its color to the mini-player.
- Retain call-held intent for hang-up recovery and allow an explicit user Play during a call; later builds strengthen focus/mode ordering.

## 2.4.47 (99) — 2026-10-24

- Make explicit queue selection play through the authorized service path even when Media3 was previously paused or had pending controller commands.
- Keep failed song B selected with Retry instead of reviving A; clear stale pending intent on explicit navigation and prevent seeking a superseded track.
- Improve Search IME activation for actual touch/navigation, while keeping pasted YouTube links submit-only.
- Refresh radio from the current song, prioritize unseen recommendations and avoid reusing the same initial order when alternatives exist.

## 2.4.46 (98) — 2026-10-24

- Fix the playlist-download crash caused by nullable progress disappearing before a deferred Compose progress/semantics lambda read it.
- Fence delayed call/controller resumes and retain pending playback/recovery feedback; reset explicit retry budgets.
- Exercise offline selected-song failure and retry through the app model, with badge completion/cancel/restart frame coverage.

## 2.4.45 (97) — 2026-10-24

- Preserve canonical queue row names/artwork through track transitions and verify intermediate frames, not only settled selection.

## 2.4.44 (96) — 2026-10-24

- Bind rows/actions to queue entries; keep callbacks and gesture state valid while playback/edit flags change.
- Recover invisible offline Echo selection, including server restart snapshots without a serial. Promptly publish lifecycle visibility and reject delayed foreground updates.
- Correct speed-dial hold menus/metadata, radio refresh, local rolling audio recovery and explicit selection intent.
- Keep remote queue removal bound to the intended occurrence; cancel superseded YouTube-link requests.

## 2.4.43 (95) — 2026-10-24

- Request artist portraits only when the artist picker opens, with cancellation/fallback handling.
- Improve Search focus, cover-play contrast, translucent cached progress, single-line playlist titles and the description Read more presentation.
- Wait for explicit pasted-link submission; open Now Playing after selection and fence slow/stale playback responses.

## 2.4.42 (94) — 2026-10-24

- Improve diagonal scrolling, queue selection, stable tap callbacks and device-handoff presentation.
- Refine speed-dial corners/labels/menus and playlist header/back-button presentation.
- Preserve exact paused phone-to-Echo cursor and connect the destination renderer earlier.

## 2.4.41 (93) — 2026-10-24

- Publish transport feedback promptly, stabilize output handoff feedback and deliver an update-compatible debug build.
- Preserve controls for dismissed outputs without restarting saved queues; reduce redundant startup/playlist work and stale poll side effects.

## 2.4.35–2.4.40 (87–92) — 2026-10-09 onward

- Validate full/incomplete cached audio, fragmented transfers and EOF fallback; retain cache generation across handoffs and source reads.
- Save automatic safe audio-error/crash diagnostics and expose bounded interruption events.
- Improve call-held playback, Alexa control reconciliation, queue/player responsiveness and debug UI/background overhead.
- Harden lifecycle snapshot ordering, controller disposal and CI APK component/entry-point validation. Follow-up commits on this source line continued through 2026-10-24 before 2.4.41.

## 2.4.23–2.4.34 (75–86) — 2026-10-09

- Cache the complete current stream and lazily warm the next five, with demand promotion, retention and integrity checks.
- Separate playback cache reads from background writes; preserve buffered/downloaded audio through outages and reconcile ownership on recovery.
- Add immediate bounded remote transport/track-boundary previews and reconcile them with fresh receiver revisions.
- Persist Home metadata, add personalized speed dial, preserve structured artist credits and repair invalid audio responses/downloads.
- Restore notification/earbud playback without opening the UI, with cancellation/ownership guards and actionable errors.

## 2.4.1–2.4.22 (53–74) — 2026-10-07 to 2026-10-08

- Recover network-interrupted/background phone playback, maintain presence while preparing and fence orphaned/stale handoff acknowledgments.
- Improve phone/tablet/landscape controls, target volume-key routing and output selection without auto-playing saved content.
- Separate background ownership synchronization from presentation; reduce redundant polls and atomic handoff reads.
- Repair controller binding/reconnection, cached corruption, loading indicators and manifest startup validation.
- Rename package/namespace to `in.synthora.musicbox` in 2.4.17 (69); this is a distinct identity from earlier `com.example.juke` packages.

## Stable baseline and native beta integration — 2026-10-04 to 2026-10-06

- Native account-backed Home/Search/Explore/Library, artist/album/playlist pages, output selection, Echo transport, subscriptions/history and Jam flows replace remote-page embedding for music controls.
- Keep Spotify/local music, lyrics, equalizer/effects, downloads, imports and Power Tools from the inherited application.
- Add shared phone/Alexa queue ownership, exact duplicate-song cursors, scoped audio credentials, Room download manifests, validated artwork and adaptive preloading.
- Start large playlists promptly and backfill their remainder; reject inflated counts, preserve paused/native advancement and avoid account/audio budget contention.
- Prepare stable 2.4.0 signing/update policy; preserve original signing material, parse newer SDK signer labels and avoid disposable-key fallback.
- Add a silent-but-advancing playback watchdog and repair its waveform conversion/type checks. See the PR register and inherited 2.4.0 release notes below.

## Source-version introduction ledger

Each row is a `versionName` change found in reachable Gradle history, including intermediate/unreleased labels. The commit subject identifies that introduction, not every later fix shipped under the same label. Entries with unchanged `versionCode` use its value from that commit.

| Source label | Code | Introduction | Commit subject |
| --- | --- | --- | --- |
| 2.4.50 | 102 | [d5d7a43](https://github.com/arookiecoder-ip/MusicBox-APP/commit/d5d7a43b092bd31fdfba4709c89eb8b11cdb6673) (2026-10-25) | Reduce rendering and idle work; restore top status bar and artist video navigation |
| 2.4.49 | 101 | [13f7931](https://github.com/arookiecoder-ip/MusicBox-APP/commit/13f7931caaf9ea440ecf272099f1b3c3646de7f5) (2026-10-25) | Fix call recovery, search navigation and artist presentation |
| 2.4.48 | 100 | [009e9b1](https://github.com/arookiecoder-ip/MusicBox-APP/commit/009e9b1ffe56b69765331a5d75c591340d29f2e3) (2026-10-24) | Restore timed lyrics, add smooth artwork and home blends, compact the dock and fix call playback intent |
| 2.4.47 | 99 | [ef8ed31](https://github.com/arookiecoder-ip/MusicBox-APP/commit/ef8ed31778a07df639642ba5fdc11756bcc5750e) (2026-10-24) | Fix explicit queue playback, retain failed song selection, open search IME and refresh radio mixes |
| 2.4.46 | 98 | [3fcf13a](https://github.com/arookiecoder-ip/MusicBox-APP/commit/3fcf13a1af9f78f67a95e12d743e7b577bb787ca) (2026-10-24) | Fix playlist download badge crash when progress disappears before semantics reads |
| 2.4.45 | 97 | [9e25a24](https://github.com/arookiecoder-ip/MusicBox-APP/commit/9e25a2424268e43d2dc202a078ccf0fa76db09a7) (2026-10-24) | Preserve queue row metadata across playback transitions and verify intermediate frames |
| 2.4.44 | 96 | [1f7698c](https://github.com/arookiecoder-ip/MusicBox-APP/commit/1f7698cb6cc57106f94d2924b40000c4e0c119cd) (2026-10-24) | Bind queue metadata to entries, play selected rows and correct song menus and device fallback |
| 2.4.43 | 95 | [233d415](https://github.com/arookiecoder-ip/MusicBox-APP/commit/233d415c140a8b4d8a387f749a87ee5a804a9b84) (2026-10-24) | Fix search IME focus, load artist portraits on demand and improve cover controls |
| 2.4.42 | 94 | [5877c9c](https://github.com/arookiecoder-ip/MusicBox-APP/commit/5877c9c8cae95e89c951a4bddc1760e4842de144) (2026-10-24) | Fix queue selection, diagonal scrolling and handoff stalls; polish speed dial and playlist header |
| 2.4.41 | 93 | [247592f](https://github.com/arookiecoder-ip/MusicBox-APP/commit/247592ff99da22a5b8f0a3008f06c85b583fbbb3) (2026-10-24) | Bump debug update to version 2.4.41 (93) |
| 2.4.40 | 92 | [3f93fef](https://github.com/arookiecoder-ip/MusicBox-APP/commit/3f93fef1bd79d73c352aa1c6dfa88360b2389641) (2026-10-12) | Improve queue selection and player responsiveness; silence routine slow polls |
| 2.4.39 | 91 | [0730716](https://github.com/arookiecoder-ip/MusicBox-APP/commit/07307166d8386abc7e9502236f3d5e30dfa09fa6) (2026-10-12) | Reduce playback UI and background overhead in debug 2.4.39 |
| 2.4.38 | 90 | [382aff3](https://github.com/arookiecoder-ip/MusicBox-APP/commit/382aff3250e05a892f14a78ede14de8e30a2d78f) (2026-10-11) | Fix Alexa control reconciliation and call-held playback in debug 2.4.38 |
| 2.4.37 | 89 | [715b9ca](https://github.com/arookiecoder-ip/MusicBox-APP/commit/715b9cadbc3d85c4e6b975b986d2bf5d44c7eedc) (2026-10-10) | Fence cached audio handoffs and repair EOF from completed streams |
| 2.4.36 | 88 | [f6837c1](https://github.com/arookiecoder-ip/MusicBox-APP/commit/f6837c12c87e5db1c97adc7d23f05020d60e8af0) (2026-10-10) | Persist automatic playback error reports for debug diagnostics |
| 2.4.35 | 87 | [d9c5244](https://github.com/arookiecoder-ip/MusicBox-APP/commit/d9c52448d781d0f30e007b6ade5c7b6ff327516a) (2026-10-09) | Keep background audio continuous across truncated transfers and cache completion |
| 2.4.34 | 86 | [ed3188a](https://github.com/arookiecoder-ip/MusicBox-APP/commit/ed3188a4a12b0e668fe54aee799044f44abeb719) (2026-10-09) | Restore notification and earbud playback safely without opening the app |
| 2.4.33 | 85 | [94b2ddf](https://github.com/arookiecoder-ip/MusicBox-APP/commit/94b2ddfc6b48a7ad8e7b62203b3a9f856f50e9f1) (2026-10-09) | Match web artist credit parsing and preserve structured band names |
| 2.4.32 | 84 | [28d5fcb](https://github.com/arookiecoder-ip/MusicBox-APP/commit/28d5fcbd2dfb6feabf03df939c337cd4e2652d26) (2026-10-09) | Keep cached phone audio running when ownership server is unreachable |
| 2.4.31 | 83 | [9c6c44a](https://github.com/arookiecoder-ip/MusicBox-APP/commit/9c6c44aa663ff722e10966e51e1a1f83e4a83ea8) (2026-10-09) | Verify reconnect retry rate limits and identify debug build 83 |
| 2.4.30 | 82 | [a43417d](https://github.com/arookiecoder-ip/MusicBox-APP/commit/a43417dd02e72f92d14466d8c803a08ab6f0d645) (2026-10-09) | Repair invalid audio responses and downloads; refine song dial and notification radio |
| 2.4.29 | 81 | [3e2f232](https://github.com/arookiecoder-ip/MusicBox-APP/commit/3e2f232cecd5d03c5d1ab8db7ba6ff4e287ea15a) (2026-10-09) | Preview remote notification track transitions and reconcile confirmed playback |
| 2.4.28 | 80 | [c21a8ae](https://github.com/arookiecoder-ip/MusicBox-APP/commit/c21a8ae9ae89ea1112bd2de3231f678aa5eaa0a0) (2026-10-09) | Add personalized Home speed dial and protect current audio caching |
| 2.4.27 | 79 | [ccf0eeb](https://github.com/arookiecoder-ip/MusicBox-APP/commit/ccf0eebb4760a90bc2bf350b65cd7fd6157b2d31) (2026-10-09) | Persist Home feed for thirty minutes with background refresh |
| 2.4.26 | 78 | [bfaa4d5](https://github.com/arookiecoder-ip/MusicBox-APP/commit/bfaa4d5c3cd8ff74a9e6e9f114878dcd846b4f0c) (2026-10-09) | Fix offline Home crash and downloaded playback restoration |
| 2.4.25 | 77 | [81b7439](https://github.com/arookiecoder-ip/MusicBox-APP/commit/81b74392fc3c6899d6cdacb268678fefceae1ebe) (2026-10-09) | Preview remote phone transport instantly and reconcile bounded confirmations |
| 2.4.24 | 76 | [1a0d30e](https://github.com/arookiecoder-ip/MusicBox-APP/commit/1a0d30e6789ab1d17a4664142caa443ede054634) (2026-10-09) | Present mobile and tablet queues immediately with a short entrance |
| 2.4.23 | 75 | [45ce499](https://github.com/arookiecoder-ip/MusicBox-APP/commit/45ce4990dec3c2ba83975f5d1cdaa84f8a169ead) (2026-10-09) | Verify full current-song caching and lazily cache five upcoming streams |
| 2.4.22 | 74 | [7c7b27a](https://github.com/arookiecoder-ip/MusicBox-APP/commit/7c7b27a3102e53663d191ccb4fe1f436bbc84a08) (2026-10-09) | Refresh live song metadata and expose loading for notification skips |
| 2.4.21 | 73 | [d698906](https://github.com/arookiecoder-ip/MusicBox-APP/commit/d6989065266c6bb6c67f6540684a13add1c5be58) (2026-10-08) | Promote every connection service start and refresh truncated audio on retry |
| 2.4.20 | 72 | [1ca7017](https://github.com/arookiecoder-ip/MusicBox-APP/commit/1ca701734a0cfbf403dad8a7b8fc085eca223e64) (2026-10-08) | Retain buffering notifications and show loading during remote phone recovery |
| 2.4.19 | 71 | [a6fe9b9](https://github.com/arookiecoder-ip/MusicBox-APP/commit/a6fe9b9f9d0a6f9c0d91edec48e4d61022d79054) (2026-10-08) | Fix volume feedback expiry and restore playback with current stream caching |
| 2.4.18 | 70 | [b308e59](https://github.com/arookiecoder-ip/MusicBox-APP/commit/b308e59ad4fe18f06ef67e3ef2c6d857f669fcda) (2026-10-08) | Include application bootstrap and reject APKs missing manifest entry points |
| 2.4.17 | 69 | [daac938](https://github.com/arookiecoder-ip/MusicBox-APP/commit/daac93839f9de5646d5accad039ea39425c7ec2b) (2026-10-08) | Rename Music Box to in.synthora.musicbox and confirm Alexa transport UI promptly |
| 2.4.16 | 68 | [fca5285](https://github.com/arookiecoder-ip/MusicBox-APP/commit/fca5285d88b158bc7538288c62208ee7e1fd0fa2) (2026-10-08) | Retry malformed cached audio once and report exact safe player failure details |
| 2.4.15 | 67 | [5099632](https://github.com/arookiecoder-ip/MusicBox-APP/commit/50996328934c43026693ab0823d84f4033d69839) (2026-10-08) | Reload failed phone audio before toggling play and show buffered progress |
| 2.4.14 | 66 | [61e48d8](https://github.com/arookiecoder-ip/MusicBox-APP/commit/61e48d8196273883d2eecf4a81f744ef4e565a19) (2026-10-08) | Fix asynchronous Media3 binding cleanup and controller reconnection |
| 2.4.13 | 65 | [00659ac](https://github.com/arookiecoder-ip/MusicBox-APP/commit/00659ac82e8a805ffb82391917d6db714719a305) (2026-10-08) | Remove redundant handoff requests and prepare selected audio before bulk queue work |
| 2.4.12 | 64 | [1cc1def](https://github.com/arookiecoder-ip/MusicBox-APP/commit/1cc1def47835fd83d76985641bfce9c2db47c8ea) (2026-10-08) | Reduce background presence traffic and coalesce unchanged playback cursors |
| 2.4.11 | 63 | [8920647](https://github.com/arookiecoder-ip/MusicBox-APP/commit/8920647b79ba0daad4fcb6b5f6aa3266278c7b1c) (2026-10-08) | fix: preserve destination foreground authorization across background phone handoffs |
| 2.4.10 | 62 | [c2f686a](https://github.com/arookiecoder-ip/MusicBox-APP/commit/c2f686a07ff78f1f86269c8b924172a7a0defafe) (2026-10-07) | fix: serialize remote song preparation and preserve phone handoff presentation |
| 2.4.9 | 61 | [acfcee9](https://github.com/arookiecoder-ip/MusicBox-APP/commit/acfcee9e933f69a545582218929540307e19cd9f) (2026-10-07) | fix: restore player interactions and reliable background device handoffs |
| 2.4.8 | 60 | [53a4467](https://github.com/arookiecoder-ip/MusicBox-APP/commit/53a4467aef24c0af3abd4fd2134aef51fd7b3445) (2026-10-07) | fix: keep presence and recovery alive during slow background handoffs |
| 2.4.7 | 59 | [3640485](https://github.com/arookiecoder-ip/MusicBox-APP/commit/3640485855e159d522555cf20f0f8c6c231db3ab) (2026-10-07) | fix: resume closed phone sessions and avoid false silence restarts |
| 2.4.6 | 58 | [b689632](https://github.com/arookiecoder-ip/MusicBox-APP/commit/b689632c6d68fcbd5cd5048ecc65f7bb20eb1f3d) (2026-10-07) | fix: blend and center landscape heroes and speed up player presentation |
| 2.4.5 | 57 | [5b028f1](https://github.com/arookiecoder-ip/MusicBox-APP/commit/5b028f1a3c16b308ddd99ec5f91ab213991ca013) (2026-10-07) | fix: center landscape artwork and lyrics and route remote volume keys |
| 2.4.4 | 56 | [15e87a4](https://github.com/arookiecoder-ip/MusicBox-APP/commit/15e87a4e988996af0f919d696e7c3d574e645046) (2026-10-07) | Route remote volume keys and select silent local defaults with responsive loaders |
| 2.4.3 | 55 | [a2d59b1](https://github.com/arookiecoder-ip/MusicBox-APP/commit/a2d59b1981a8db2b6b22370334e2bf60aa4fee95) (2026-10-07) | Fix remote phone preparation, player reconnects and landscape presentation |
| 2.4.2 | 54 | [0246d30](https://github.com/arookiecoder-ip/MusicBox-APP/commit/0246d30eecb93f3cfd0aaf9647bd83eeca2f6540) (2026-10-07) | Fix transport presentation, swipe intent and tablet landscape layouts |
| 2.4.1 | 53 | [1e9e43a](https://github.com/arookiecoder-ip/MusicBox-APP/commit/1e9e43a1d4a5bfcba96fc9f1d58a73809c01ef43) (2026-10-07) | Recover background playback and preloading after network outages |
| 2.4.0 | 52 | [612997c](https://github.com/arookiecoder-ip/MusicBox-APP/commit/612997cf12a17d5cfbe5043741ab1f0e4009e87a) (2026-10-06) | Prepare signed stable 2.4.0 release and correct update channel |
| 2.3.37-beta | 51 | [a5890ff](https://github.com/arookiecoder-ip/MusicBox-APP/commit/a5890ff87b74ec585b58371f2fb49a841eb42aa1) (2026-10-06) | Preserve real phone play intent and avoid duplicate Alexa handoff pause |
| 2.3.36-beta | 50 | [07dacde](https://github.com/arookiecoder-ip/MusicBox-APP/commit/07dacde65b8e6baef83383eb739308f5ed0441ba) (2026-10-06) | Centralize remote playback sync, persist downloads in Room and scope audio credentials |
| 2.3.35-beta | 49 | [b52cd85](https://github.com/arookiecoder-ip/MusicBox-APP/commit/b52cd85edfaabbc322ae4e3a7b949d7bb1c0eb6c) (2026-10-06) | Start radio from artist songs, animate optimistic queue removal, and refine profile and Jam states |
| 2.3.34-beta | 48 | [9d09f9e](https://github.com/arookiecoder-ip/MusicBox-APP/commit/9d09f9ed44861deb890210fdcc77f9b47d848062) (2026-10-06) | Fix stable share insets, queue dismissal, artwork recovery and playback restoration after task removal |
| 2.3.33-beta | 47 | [e1b2d78](https://github.com/arookiecoder-ip/MusicBox-APP/commit/e1b2d78da48de7c6cd339af3de3c40b0dba3b3e1) (2026-10-06) | Retry failed HD artwork and fix playback lifecycle and handoff reporting; refine Settings modals |
| 2.3.32-beta | 46 | [2e158b5](https://github.com/arookiecoder-ip/MusicBox-APP/commit/2e158b56fd4e46056c03471c373e179cb14cff67) (2026-10-06) | Fix queue drag placement, compact action sheets and reorganize settings and search focus |
| 2.3.31-beta | 45 | [5bcf550](https://github.com/arookiecoder-ip/MusicBox-APP/commit/5bcf5502580bcf30e6a17f9cbb2de5f9648f0641) (2026-10-06) | Fix complete playlist loading and native queue continuation; reconcile queue conflicts |
| 2.3.30-beta | 44 | [0e0d8ab](https://github.com/arookiecoder-ip/MusicBox-APP/commit/0e0d8ab4562ea26d6ce2eafcaa432e288a65176f) (2026-10-06) | Fix download metadata, Library controls and icons; streamline playlist and offline playback |
| 2.3.29-beta | 43 | [d90000f](https://github.com/arookiecoder-ip/MusicBox-APP/commit/d90000fd79221130622617e048b31dd678baf647) (2026-10-06) | Acknowledge actual phone pause directly in Alexa handoff |
| 2.3.28-beta | 42 | [954f38e](https://github.com/arookiecoder-ip/MusicBox-APP/commit/954f38ea92f78cb5e1c3b2285088a5f3dda98e17) (2026-10-05) | Coordinate shared phone and Alexa queue ownership and handoffs |
| 2.3.27-beta | 41 | [76fa1c2](https://github.com/arookiecoder-ip/MusicBox-APP/commit/76fa1c2131b451b26931037e2c2a8bd493d3f0fb) (2026-10-05) | Install and read phone queues atomically on the audio backend |
| 2.3.26-beta | 40 | [a706e87](https://github.com/arookiecoder-ip/MusicBox-APP/commit/a706e87dbce64c785fa161fc18d9bb50b4c380e3) (2026-10-05) | Expose rejected queue endpoint and HTTP status safely |
| 2.3.25-beta | 39 | [d0d5da2](https://github.com/arookiecoder-ip/MusicBox-APP/commit/d0d5da260d84bf2e0384ac0f94c548d225732e14) (2026-10-05) | Keep valid login sessions when queue sync rejects access |
| 2.3.24-beta | 38 | [50e1023](https://github.com/arookiecoder-ip/MusicBox-APP/commit/50e1023a8347ba9ee1045f9b90a67df31dd162fd) (2026-10-05) | Fix shared queue sync retries before phone queue is ready |
| 2.3.23-beta | 37 | [131e928](https://github.com/arookiecoder-ip/MusicBox-APP/commit/131e9282891232b33796e2d80f9794c6c0e31eb7) (2026-10-05) | Warm the next five streams and recover queues search songs and download controls |
| 2.3.22-beta | 36 | [e28af62](https://github.com/arookiecoder-ip/MusicBox-APP/commit/e28af6283218938fde152ad88391ac5331a2a7dd) (2026-10-05) | Unify downloaded collection pages and close prior UI and offline action gaps |
| 2.3.21-beta | 35 | [e224aab](https://github.com/arookiecoder-ip/MusicBox-APP/commit/e224aaba26e649a75916cf20c09c2d5e1c28f4ec) (2026-10-05) | Adapt tablet player layouts and fix Alexa notifications, lyrics and Library controls |
| 2.3.20-beta | 34 | [97a2341](https://github.com/arookiecoder-ip/MusicBox-APP/commit/97a234165dbb1d5f1a1736193e28e3211259edae) (2026-10-05) | Fix shared playback queues and refresh downloads, Library and search interactions |
| 2.3.19-beta | 33 | [5dd559c](https://github.com/arookiecoder-ip/MusicBox-APP/commit/5dd559cf432ae825a4e5d22cceb010d6b50455a4) (2026-10-05) | Authenticate configured audio routes and exercise real phone playback service |
| 2.3.18-beta | 32 | [2f2dad7](https://github.com/arookiecoder-ip/MusicBox-APP/commit/2f2dad7a2fe3b5238b19e9536c8568b3d0f15073) (2026-10-05) | Make online phone audio seekable and allow cold-cache preparation |
| 2.3.17-beta | 31 | [1e4bffd](https://github.com/arookiecoder-ip/MusicBox-APP/commit/1e4bffd80a01d477bdedd9a0e2024b2bbccc9455) (2026-10-05) | Stabilize output handoff, improve artwork and startup, and add collection downloads |
| 2.3.16-beta | 30 | [9f07871](https://github.com/arookiecoder-ip/MusicBox-APP/commit/9f078713b4b1756f2f9d548c6091bea1ae70c91f) (2026-10-05) | Add account song downloads and recover device playback with retained artist paging |
| 2.3.15-beta | 29 | [123cf86](https://github.com/arookiecoder-ip/MusicBox-APP/commit/123cf86662d6641ac0600a30c2940b337b65c350) (2026-10-05) | Refine discovery shelves, retain release navigation and unify offline presentation |
| 2.3.14-beta | 28 | [b60a1fe](https://github.com/arookiecoder-ip/MusicBox-APP/commit/b60a1fe6dc3ec8a01c46ce66bfb99b06c53d7fb9) (2026-10-05) | Synchronize playing identity and artwork; cache discovery names and refine loading and cards |
| 2.3.13-beta | 27 | [0452b8c](https://github.com/arookiecoder-ip/MusicBox-APP/commit/0452b8c41bfee177d6fb051a71efc2b902fd3857) (2026-10-05) | Fix collection routes and discovery layouts; add Alexa media controls and device history reporting |
| 2.3.12-beta | 26 | [5af7a28](https://github.com/arookiecoder-ip/MusicBox-APP/commit/5af7a2875b0bfb9b374e7e3d775ae7ed711fddd0) (2026-10-05) | Fix device playlist startup, stable update signing and library/player actions |
| 2.3.11-beta | 25 | [08414c1](https://github.com/arookiecoder-ip/MusicBox-APP/commit/08414c102dd92741b5d11c0d125b9cdf0ab52b2d) (2026-10-05) | Correct swipe actions and add PWA branding, discovery and paged artist browsing |
| 2.3.10-beta | 24 | [111ece0](https://github.com/arookiecoder-ip/MusicBox-APP/commit/111ece02ab33deae81488685ce1b8d0d4c98be09) (2026-10-05) | Anchor queue and refine controls, collection actions and network feedback |
| 2.3.9-beta | 23 | [86f3129](https://github.com/arookiecoder-ip/MusicBox-APP/commit/86f3129a77fa638b0b13dae62b3078f6e2c88333) (2026-10-05) | Redesign account library with collection filters and list/grid views |
| 2.3.8-beta | 22 | [27870ba](https://github.com/arookiecoder-ip/MusicBox-APP/commit/27870ba7c7363744c3dfb7593262feb5f9561d42) (2026-10-05) | Bleed artwork into status bar and refine player, web queue and artist pages |
| 2.3.7-beta | 21 | [42beb80](https://github.com/arookiecoder-ip/MusicBox-APP/commit/42beb805a97b29f36857708dde42c7bc78f2a213) (2026-10-05) | Match web song layouts and refine player, menus, collections and loading |
| 2.3.6-beta | 20 | [86a49ad](https://github.com/arookiecoder-ip/MusicBox-APP/commit/86a49ad0860b64ca5fd0a16d7d48b292b0e939c9) (2026-10-05) | Refine Home spacing and player controls with web-style sliders |
| 2.3.5-beta | 18 | [06611fb](https://github.com/arookiecoder-ip/MusicBox-APP/commit/06611fb02942d159a4590248ba367b5dd1e0018b) (2026-10-04) | Release v2.3.5-beta: version 2.3.5-beta (18), release notes, APK |
| 2.3.4-beta | 17 | [3579bb8](https://github.com/arookiecoder-ip/MusicBox-APP/commit/3579bb8d10207f485c194efd71105039ab085219) (2026-10-04) | Release v2.3.4-beta: version 2.3.4-beta (17), crash and navigation fixes, smarter recommendations, release notes |
| 2.3.2-beta-unreleased | 16 | [5a72b3d](https://github.com/arookiecoder-ip/MusicBox-APP/commit/5a72b3d71eb43e120fa1651b7697258b1c8ee758) (2026-04-30) | feat: add download track functionality in PlaybackService, update UI accordingly in MusicViewModel, and include download icon resource |
| 2.3.2-beta | 16 | [4eaa05e](https://github.com/arookiecoder-ip/MusicBox-APP/commit/4eaa05ee4eeb57235dace875a60ef7fe4168ca88) (2026-04-27) | feat: update release notes for v2.3.2-beta with new features, enhancements, and bug fixes; improve README and changelog for versioning and search experience |
| 2.3.1-beta-unreleased | 15 | [4ebe185](https://github.com/arookiecoder-ip/MusicBox-APP/commit/4ebe185962d003ca559689375f6dfbb6f6d5f77f) (2026-04-16) | feat: implement Spotmate queue-aware download recovery with fallback logic |
| 2.3.1-beta | 15 | [32983fc](https://github.com/arookiecoder-ip/MusicBox-APP/commit/32983fcf03ec50c978896c0225cae6cc1784665e) (2026-04-15) | feat: update versionCode to 15 and versionName to "2.3.1-beta" for release preparation |
| 2.3.0-beta_unreleased | 14 | [d448a89](https://github.com/arookiecoder-ip/MusicBox-APP/commit/d448a898020cd3aa2563c39f6d1708bed6d13569) (2026-04-13) | feat: implement MediaLibraryService with ExoPlayer and add LyricsOverlay component |
| 2.3.0-beta | 14 | [b5a7135](https://github.com/arookiecoder-ip/MusicBox-APP/commit/b5a713527fa9c0a5e328fb4794f72c770b1c1de9) (2026-04-12) | feat: implement core music database, Spotify API integration, playback services, and UI components |
| 2.2.0-beta | 13 | [601db94](https://github.com/arookiecoder-ip/MusicBox-APP/commit/601db9498ef91200eadee369783027f79f987738) (2026-03-18) | feat: add v2.2.0-beta release notes with new features and bug fixes |
| 2.1.1-beta | 12 | [1b72c0e](https://github.com/arookiecoder-ip/MusicBox-APP/commit/1b72c0e30a2ffa541bdeeeac850cd3f1b1be40c4) (2026-03-03) | release: v2.1.1-beta |
| 2.1.0-beta | 11 | [df83a8a](https://github.com/arookiecoder-ip/MusicBox-APP/commit/df83a8ab2355ea1d16a76542100d4e82e1f55c46) (2026-03-02) | feat: Implement music download and playback functionality with a dedicated service and player UI. |
| 2.0.0-stable | 10 | [7cd68dd](https://github.com/arookiecoder-ip/MusicBox-APP/commit/7cd68dddbeb7a2a708b6b9817ab2808543a8c2eb) (2026-02-20) | feat: enhance HomeViewModel to include favorites and dynamic greeting generation |
| 1.0.8-beta | 9 | [23d7dc7](https://github.com/arookiecoder-ip/MusicBox-APP/commit/23d7dc7d542cbea00e498dfe897f4bc94dd4cce1) (2026-02-19) | feat: update version to 1.0.8-beta and enhance playback queue management with shuffle mode preservation |
| 1.0.7-beta | 8 | [8a4c98c](https://github.com/arookiecoder-ip/MusicBox-APP/commit/8a4c98cca1badb73eb292936733941f18f54174d) (2026-02-15) | feat: Implement the initial Android application with Compose UI, navigation, music playback, update checking, and documentation updates. |
| 1.0.6-beta | 7 | [3d7e1a2](https://github.com/arookiecoder-ip/MusicBox-APP/commit/3d7e1a2cd3bbb310bada7b8cb66010c28e8118a1) (2026-01-08) | feat: Introduce QueueManager and MusicViewModel for enhanced queue management and recommendations, update build configuration, and document v1.0.6-beta features. |
| 1.0.5-beta | 6 | [835ba85](https://github.com/arookiecoder-ip/MusicBox-APP/commit/835ba8569350f3cbe81aa98cd6dc96ba0eb828f1) (2026-01-07) | feat: introduce comprehensive music playback system including service, viewmodel, UI, and download management. |
| 1.0.4-beta | 5 | [2304ee6](https://github.com/arookiecoder-ip/MusicBox-APP/commit/2304ee6bcf07789ea9cad66da2ac481364cf079d) (2026-01-04) | feat: Implement core music player functionality including playback, audio effects, playlist management, library, and search. |
| 1.0.3-beta | 4 | [12deff6](https://github.com/arookiecoder-ip/MusicBox-APP/commit/12deff6071d40c15c9ec8f9cfe9086820ea498c2) (2025-12-30) | feat: Introduce multi-selection bulk delete, AI/OCR modules, Spotify navigation, and service lifecycle improvements, alongside fixes for notification sync and various crashes. |
| 1.0.2-beta | 3 | [9d2e272](https://github.com/arookiecoder-ip/MusicBox-APP/commit/9d2e272d91daafec7d2c2e3c559936dece75602a) (2025-12-28) | feat: Update version to 1.0.2-beta and implement auto-resume after call for improved playback experience |
| 1.0.1-beta | 2 | [c943cda](https://github.com/arookiecoder-ip/MusicBox-APP/commit/c943cdaf826044dda9def9661e1ad478289bc4b7) (2025-12-26) | docs: Add v1.0.1-beta release notes and bump version |
| 1.0 | 1 | [afe51e4](https://github.com/arookiecoder-ip/MusicBox-APP/commit/afe51e4e6b35f97902cd5ca56e2da2718c49da05) (2025-12-20) | first commit |

## Pull request register

All 13 PRs returned for this repository were reviewed as of 2026-10-25; none was open. A merged PR is historical provenance, not proof every part of its implementation remains current. PR numbers mentioned in inherited commit subjects can belong to an upstream repository.

| PR | Title | State | Current relevance |
| --- | --- | --- | --- |
| [#1](https://github.com/arookiecoder-ip/MusicBox-APP/pull/1) | feat: rebuild Echo remote and YouTube Music features natively | Closed without merge | Native rebuild proposal; closed without a PR merge. |
| [#2](https://github.com/arookiecoder-ip/MusicBox-APP/pull/2) | Beta | Merged | History/reference; verify current behavior in ARCHITECTURE.md. |
| [#4](https://github.com/arookiecoder-ip/MusicBox-APP/pull/4) | Fix shared playback handoffs, lifecycle, queues and account UI | Closed without merge | Closed without a PR merge; use the current source for delivered behavior. |
| [#5](https://github.com/arookiecoder-ip/MusicBox-APP/pull/5) | Prepare Music Box 2.4.0 stable release with update-compatible signing | Merged | History/reference; verify current behavior in ARCHITECTURE.md. |
| [#6](https://github.com/arookiecoder-ip/MusicBox-APP/pull/6) | Fix SDK 37 stable APK certificate verification | Merged | History/reference; verify current behavior in ARCHITECTURE.md. |
| [#7](https://github.com/arookiecoder-ip/MusicBox-APP/pull/7) | Add one-off encrypted signing-key export workflow | Merged | History/reference; verify current behavior in ARCHITECTURE.md. |
| [#8](https://github.com/arookiecoder-ip/MusicBox-APP/pull/8) | Feat/alexa backend | Closed without merge | Integration proposal closed without merge. |
| [#9](https://github.com/arookiecoder-ip/MusicBox-APP/pull/9) | Feat/alexa backend | Merged | History/reference; verify current behavior in ARCHITECTURE.md. |
| [#10](https://github.com/arookiecoder-ip/MusicBox-APP/pull/10) | Fast playlist starts + suppress background offline toasts | Merged | History/reference; ve…29463 tokens truncated…ommit/8b6223ee641db40854fed17c412481e9b967e7de) | 2026-03-08 | feat: Create new Android application module with Media3 playback service and Gradle dependency management. |
| [4d4bae2](https://github.com/arookiecoder-ip/MusicBox-APP/commit/4d4bae2eef2c1344a8fc79b8234bb2580f072b4c) | 2026-03-08 | feat: Implement robust audio playback with Media3 ExoPlayer, including a playback service, queue management, and UI integration. |
| [c75b723](https://github.com/arookiecoder-ip/MusicBox-APP/commit/c75b72366084c1c87edeb801f0f3118af79765db) | 2026-03-08 | feat: Implement Media3-based \`PlaybackService\` for robust audio playback |
| [7fe8bda](https://github.com/arookiecoder-ip/MusicBox-APP/commit/7fe8bdaaa9129c7a72f3123f1f55e6dccc7ff07d) | 2026-03-08 | feat: Implement media playback service with queue management and next/previous icons. |
| [b348905](https://github.com/arookiecoder-ip/MusicBox-APP/commit/b348905abbc3b9e0ee19ce05ad9216b991a304ae) | 2026-03-08 | feat: Introduce PlayerScreen, BlacklistManager, QueueManager, ArtistDetailScreen, and AudioSettingsScreen. |
| [193a74d](https://github.com/arookiecoder-ip/MusicBox-APP/commit/193a74d2f0e81d8191e29d378deccc89c83a833c) | 2026-03-08 | feat: implement PlaybackService for media playback using Media3 ExoPlayer. |
| [b28c7e0](https://github.com/arookiecoder-ip/MusicBox-APP/commit/b28c7e097b6a69b60ce3f73d08fa2401fdbf63ee) | 2026-03-08 | feat: implement initial SearchScreen with search functionality and result display. |
| [1b72c0e](https://github.com/arookiecoder-ip/MusicBox-APP/commit/1b72c0e30a2ffa541bdeeeac850cd3f1b1be40c4) | 2026-03-03 | release: v2.1.1-beta |
| [b64a12b](https://github.com/arookiecoder-ip/MusicBox-APP/commit/b64a12bce384d26947cb80bddb5446c7bb7ad707) | 2026-03-03 | feat: Implement MediaLibraryService for audio playback using Media3, including queue management, audio effects, and Android Auto support. |
| [56ad6ce](https://github.com/arookiecoder-ip/MusicBox-APP/commit/56ad6cee83cd9a8eddfece73b80657a34742e3e8) | 2026-03-03 | feat: Implement a \`SwipeToAddNextContainer\` composable for swipe-to-add-next and optional swipe-to-delete functionality with visual and haptic feedback. |
| [d194a53](https://github.com/arookiecoder-ip/MusicBox-APP/commit/d194a53635015e8c527f2df3262aae6e3ab84edd) | 2026-03-03 | feat: Add type designation for emergency release in release notes |
| [ed36df4](https://github.com/arookiecoder-ip/MusicBox-APP/commit/ed36df4384bf4e4f3da6cb99cadc4d33564c27cc) | 2026-03-03 | lint |
| [63aa293](https://github.com/arookiecoder-ip/MusicBox-APP/commit/63aa29373f7154df762c82e1318da218d0ba5c92) | 2026-03-03 | feat: Implement the main player screen with track display, controls, progress, immersive background, and queue/lyrics/playlist management. |
| [df83a8a](https://github.com/arookiecoder-ip/MusicBox-APP/commit/df83a8ab2355ea1d16a76542100d4e82e1f55c46) | 2026-03-02 | feat: Implement music download and playback functionality with a dedicated service and player UI. |
| [cc765a7](https://github.com/arookiecoder-ip/MusicBox-APP/commit/cc765a706d062e92bcc78af4ea19073789179350) | 2026-03-02 | feat: Implement new Spotify API client with Spotdown integration for downloads, LRCLib for lyrics, and a Cloudflare Worker for API key management. |
| [55e39d6](https://github.com/arookiecoder-ip/MusicBox-APP/commit/55e39d6067bbb21fac6ebaba7a8c3fade244a01d) | 2026-02-20 | Merge pull request #1 from rajeet-04/beta |
| [3e2483d](https://github.com/arookiecoder-ip/MusicBox-APP/commit/3e2483d002eefbe2bc412c1fa635d2b22cebf818) | 2026-02-20 | Merge branch 'main' into beta |
| [7cd68dd](https://github.com/arookiecoder-ip/MusicBox-APP/commit/7cd68dddbeb7a2a708b6b9817ab2808543a8c2eb) | 2026-02-20 | feat: enhance HomeViewModel to include favorites and dynamic greeting generation |
| [3256690](https://github.com/arookiecoder-ip/MusicBox-APP/commit/3256690794b9abfc3cac7dbc7497990646ccd092) | 2026-02-20 | feat: enhance HomeViewModel to include favorites and dynamic greeting generation |
| [83c4931](https://github.com/arookiecoder-ip/MusicBox-APP/commit/83c493108fa139dc0064996d2355f10b4c911389) | 2026-02-20 | feat: implement play count tracking for tracks reaching 50% duration during playback session |
| [bdfebea](https://github.com/arookiecoder-ip/MusicBox-APP/commit/bdfebea734cfc34532ae383e5b2369214d663a17) | 2026-02-19 | feat: add track stream URL update functionality and handle null track counts gracefully |
| [23d7dc7](https://github.com/arookiecoder-ip/MusicBox-APP/commit/23d7dc7d542cbea00e498dfe897f4bc94dd4cce1) | 2026-02-19 | feat: update version to 1.0.8-beta and enhance playback queue management with shuffle mode preservation |
| [9989e8e](https://github.com/arookiecoder-ip/MusicBox-APP/commit/9989e8eb1da580216957f5805c0a4910a76bd4db) | 2026-02-18 | chore: update release notes for v1.0.8-beta, adding new features and fixes |
| [9ab1a20](https://github.com/arookiecoder-ip/MusicBox-APP/commit/9ab1a20e3b12c155e1340e0235db3727ed96cc8f) | 2026-02-18 | Refactor UI components and improve code readability |
| [3a0caab](https://github.com/arookiecoder-ip/MusicBox-APP/commit/3a0caab4d6088d3e516a1d11b552ff08a13d82e9) | 2026-02-18 | Refactor UI components and improve code readability |
| [03d565d](https://github.com/arookiecoder-ip/MusicBox-APP/commit/03d565d536b368568b045d8455f34bb0069c6de2) | 2026-02-18 | feat: implement offline fallback for recommendations and enhance error handling for network issues |
| [ac921e3](https://github.com/arookiecoder-ip/MusicBox-APP/commit/ac921e3a913029b6839f43bb2b93538b7c1cb6dd) | 2026-02-18 | feat: add search functionality, music playback service, and artist detail screens. |
| [31a86fd](https://github.com/arookiecoder-ip/MusicBox-APP/commit/31a86fd9772f4ad1a6dc2304598d5942e76b7ffe) | 2026-02-18 | feat: Add album, artist, and playlist detail screens with track listing and music queuing capabilities. |
| [9e1548a](https://github.com/arookiecoder-ip/MusicBox-APP/commit/9e1548a6f4c967ab3e8d11110fc39d9f92c60079) | 2026-02-18 | feat: Introduce \`QueueManager\` for smart music queueing and recommendations, exposed through \`MusicViewModel\`. |
| [ad4294b](https://github.com/arookiecoder-ip/MusicBox-APP/commit/ad4294b6be15e4db45b95cc7c001e9ad2ecdab8c) | 2026-02-18 | feat: Introduce local Room database for music, Spotify API integration, and corresponding ViewModels. |
| [7f520e4](https://github.com/arookiecoder-ip/MusicBox-APP/commit/7f520e4c3560918adb5ff9a1d3a3c73299cdcbc8) | 2026-02-18 | feat: Enhance color theme and add ExtractedColors data class for better theming support |
| [814ac4d](https://github.com/arookiecoder-ip/MusicBox-APP/commit/814ac4d4e5ec7852a361eaf8c11f0ccde4a06827) | 2026-02-18 | feat: Add initial Android application with Jetpack Compose UI, navigation, and build configurations. |
| [8a4c98c](https://github.com/arookiecoder-ip/MusicBox-APP/commit/8a4c98cca1badb73eb292936733941f18f54174d) | 2026-02-15 | feat: Implement the initial Android application with Compose UI, navigation, music playback, update checking, and documentation updates. |
| [253fc2e](https://github.com/arookiecoder-ip/MusicBox-APP/commit/253fc2e49a07ee00d6e5097260d253b6b4ad42b7) | 2026-02-15 | feat: Introduce initial application structure with main activity, navigation, data management, and core UI screens. |
| [336da1a](https://github.com/arookiecoder-ip/MusicBox-APP/commit/336da1a970513ce9df170d32f29fb9b05461a55b) | 2026-02-15 | feat: Initialize core application structure, navigation, music playback, and update checking. |
| [362602e](https://github.com/arookiecoder-ip/MusicBox-APP/commit/362602e215e1ebba4f8f87e4b93dd30f114ba0ef) | 2026-02-15 | feat: Implement Media3 PlaybackService for robust audio playback, managing audio focus, phone calls, and Android Auto integration. |
| [31904cd](https://github.com/arookiecoder-ip/MusicBox-APP/commit/31904cde2313f2ab5f08f35837165650d227f29d) | 2026-02-15 | feat: Implement music playback system with dedicated ViewModel, UI controls, and background service, along with new drawable assets. |
| [5c102fc](https://github.com/arookiecoder-ip/MusicBox-APP/commit/5c102fc336b13e63d75749bdd1988618bce8115a) | 2026-02-15 | feat: Add Spotify API integration with track models and initial music services, alongside Kotlin compiler configuration and gitignore updates. |
| [1021f7c](https://github.com/arookiecoder-ip/MusicBox-APP/commit/1021f7c7ac0b4ae421a3e4ba6f86da25e2b52c48) | 2026-01-26 | feat: Implement core music playback service, intelligent queue management with recommendations, and main application activity. |
| [f5a2ec1](https://github.com/arookiecoder-ip/MusicBox-APP/commit/f5a2ec1451a234b18e2c870ad919d787abc276df) | 2026-01-26 | feat: add MusicViewModel to manage music playback, queue, and UI state. |
| [456b5eb](https://github.com/arookiecoder-ip/MusicBox-APP/commit/456b5ebc1671afc7a5758b75a0621c3999435efc) | 2026-01-26 | update |
| [3d7e1a2](https://github.com/arookiecoder-ip/MusicBox-APP/commit/3d7e1a2cd3bbb310bada7b8cb66010c28e8118a1) | 2026-01-08 | feat: Introduce QueueManager and MusicViewModel for enhanced queue management and recommendations, update build configuration, and document v1.0.6-beta features. |
| [131e194](https://github.com/arookiecoder-ip/MusicBox-APP/commit/131e19495522c8f15cfaf1e0e88dee98cdf6e2be) | 2026-01-08 | feat: Add advanced audio controls including skip silence and stable volume, implemented via a new playback service with call and audio focus handling. |
| [a49b9aa](https://github.com/arookiecoder-ip/MusicBox-APP/commit/a49b9aa94df6ae9ed41d5e91fcaa07de10bfc7f0) | 2026-01-08 | feat: Implement music playback service, music and library view models, and library and player UI screens, updating changelog and release notes. |
| [eb00722](https://github.com/arookiecoder-ip/MusicBox-APP/commit/eb00722a442071229877d50ed22bb110f7656619) | 2026-01-08 | feat: Add Spotify API service for authentication, search, and various data retrieval operations. |
| [389b514](https://github.com/arookiecoder-ip/MusicBox-APP/commit/389b514a9ee88f8e80f79597584c03559e85625f) | 2026-01-08 | feat: implement HTTP server for music search API with error handling |
| [835ba85](https://github.com/arookiecoder-ip/MusicBox-APP/commit/835ba8569350f3cbe81aa98cd6dc96ba0eb828f1) | 2026-01-07 | feat: introduce comprehensive music playback system including service, viewmodel, UI, and download management. |
| [b4a45f0](https://github.com/arookiecoder-ip/MusicBox-APP/commit/b4a45f063c1de838403b76950ca5deee5e672f3e) | 2026-01-07 | feat: Update RELEASE_NOTES.md for v1.0.4-beta release |
| [225a69c](https://github.com/arookiecoder-ip/MusicBox-APP/commit/225a69c472ec3f833a37fabc9f8e46bd3beb2a5c) | 2026-01-05 | feat: add initial ProGuard/R8 rules for release build optimization and serialization. |
| [1ff09f7](https://github.com/arookiecoder-ip/MusicBox-APP/commit/1ff09f7a74eac8448fb777dcd834bfc66c1b8e71) | 2026-01-04 | feat: add SearchScreen UI with search input, filters, and result display |
| [c7ead9a](https://github.com/arookiecoder-ip/MusicBox-APP/commit/c7ead9a84e83aeaaea0118331d2af15f5852d3b6) | 2026-01-04 | docs: update README.md |
| [2304ee6](https://github.com/arookiecoder-ip/MusicBox-APP/commit/2304ee6bcf07789ea9cad66da2ac481364cf079d) | 2026-01-04 | feat: Implement core music player functionality including playback, audio effects, playlist management, library, and search. |
| [0c99e10](https://github.com/arookiecoder-ip/MusicBox-APP/commit/0c99e10b784b8bc084db736efa8e608c25ebfb8c) | 2026-01-01 | feat: Implement Spotify Web API integration and refine lyrics selection logic. |
| [e4a75e6](https://github.com/arookiecoder-ip/MusicBox-APP/commit/e4a75e6f2bd655ac8be3081f7137dafd60ca266a) | 2025-12-31 | feat: Implement Spotify API integration, music playback service, and core player UI components. |
| [68aaf2f](https://github.com/arookiecoder-ip/MusicBox-APP/commit/68aaf2f17b50354cc5f55f1bbcf09e73532bb5be) | 2025-12-31 | feat: Implement a new PlayerScreen with immersive UI, playback controls, lyrics, and queue management, adding new UI components and ProGuard rules. |
| [9f8dd70](https://github.com/arookiecoder-ip/MusicBox-APP/commit/9f8dd70678dd7f8d9831f8092f4458c7da1f73ec) | 2025-12-31 | feat: implement core music player UI components, view models, and playback service. |
| [48b9ea7](https://github.com/arookiecoder-ip/MusicBox-APP/commit/48b9ea705f87a8ddaffd32c60899208425df40eb) | 2025-12-31 | feat: Implement Spotify API integration, music and playback services, and player documentation, while updating release notes. |
| [12deff6](https://github.com/arookiecoder-ip/MusicBox-APP/commit/12deff6071d40c15c9ec8f9cfe9086820ea498c2) | 2025-12-30 | feat: Introduce multi-selection bulk delete, AI/OCR modules, Spotify navigation, and service lifecycle improvements, alongside fixes for notification sync and various crashes. |
| [33a0248](https://github.com/arookiecoder-ip/MusicBox-APP/commit/33a024827a1cdd22c46b1d2840ddc2c90810a588) | 2025-12-30 | Refactor ViewModels for improved state management and error handling |
| [b38df17](https://github.com/arookiecoder-ip/MusicBox-APP/commit/b38df175c8aaf63b53eaedf98ed068a814169c19) | 2025-12-29 | feat: Enhance media playback service with delayed resume functionality and update notification handling for background restrictions |
| [2dcd506](https://github.com/arookiecoder-ip/MusicBox-APP/commit/2dcd506cb79fbc82172da9a9df5b8391cced163f) | 2025-12-29 | feat: Update deployment target configuration and enhance notification channel setup in PlaybackService for improved media playback experience |
| [b5c9afe](https://github.com/arookiecoder-ip/MusicBox-APP/commit/b5c9afe37bfab7bc5a0869c001bf95fecdf9d1cc) | 2025-12-29 | feat: Implement core Android app structure, audio effects, library screen, and track undo delete functionality. |
| [eb1cef3](https://github.com/arookiecoder-ip/MusicBox-APP/commit/eb1cef397f5e9c7ed4291fd47b8908d339f311b8) | 2025-12-29 | feat: Implement core music playback and library management features with database, services, UI, and build optimizations. |
| [4f22d8a](https://github.com/arookiecoder-ip/MusicBox-APP/commit/4f22d8ac6469674928e29e94b40ae322bd47675e) | 2025-12-28 | feat: Implement Room database with Track entity, migrations, and core music service and view model. |
| [ce2b797](https://github.com/arookiecoder-ip/MusicBox-APP/commit/ce2b7976c8c1c52d6421f980793d8d3350a1337b) | 2025-12-28 | Newer Release |
| [9d2e272](https://github.com/arookiecoder-ip/MusicBox-APP/commit/9d2e272d91daafec7d2c2e3c559936dece75602a) | 2025-12-28 | feat: Update version to 1.0.2-beta and implement auto-resume after call for improved playback experience |
| [0380fb3](https://github.com/arookiecoder-ip/MusicBox-APP/commit/0380fb346c6ba0e91c1a57115fec6116d53fef02) | 2025-12-28 | feat: Implement Media3 PlaybackService with foreground service management, call state handling, and fixes for notification visibility and queue duplication. |
| [b2e7d0c](https://github.com/arookiecoder-ip/MusicBox-APP/commit/b2e7d0c2813053b9fbb84951ec359d323b61a0e9) | 2025-12-28 | feat: Implement Media3 \`PlaybackService\` with call state handling and \`MusicViewModel\` for robust media playback and UI integration. |
| [7551cd0](https://github.com/arookiecoder-ip/MusicBox-APP/commit/7551cd0de55bb13304d0ac6df078dc41bece17d7) | 2025-12-28 | feat: Implement core media playback service, phone call handling for smart pause, and initial app structure with navigation. |
| [0412a0c](https://github.com/arookiecoder-ip/MusicBox-APP/commit/0412a0cb8039b6ef664a9573f8e73a7ea46209b2) | 2025-12-28 | feat: Introduce core application structure with navigation, music playback, and in-app update checking. |
| [ca5e913](https://github.com/arookiecoder-ip/MusicBox-APP/commit/ca5e913c5cd431ab528c1bcc008b4c3e725de597) | 2025-12-28 | feat: Introduce new release with comprehensive features, UI/UX enhancements, and bug fixes, and clean up build artifacts. |
| [b25cb53](https://github.com/arookiecoder-ip/MusicBox-APP/commit/b25cb537332c44f2edca1b19ae198f44a4e8077d) | 2025-12-27 | feat: Implement core music playback, library management, and download functionalities across various components. |
| [c749e9f](https://github.com/arookiecoder-ip/MusicBox-APP/commit/c749e9fe8024ff7c3a131cc2eddfe6fb75f6cad7) | 2025-12-27 | feat: implement music playback management with \`MusicViewModel\` and \`MusicService\` to handle playback, queue, and recommendations. |
| [a01fb78](https://github.com/arookiecoder-ip/MusicBox-APP/commit/a01fb78a699f43fb59eb58291c2f939b9c7b0363) | 2025-12-27 | Merge branch 'beta' of https://github.com/rajeet-04/JUKES into beta |
| [eb1ab5b](https://github.com/arookiecoder-ip/MusicBox-APP/commit/eb1ab5be78df6dd814c6adfb588fc8073f663c9e) | 2025-12-27 | feat: Implement initial Juke music player application with core UI, playback, and audio features. |
| [ef46a15](https://github.com/arookiecoder-ip/MusicBox-APP/commit/ef46a15c31c503063ed38dfad9155af5d2c7c91a) | 2025-12-27 | feat: Implement initial Juke music player application with core UI, playback, and audio features. |
| [c943cda](https://github.com/arookiecoder-ip/MusicBox-APP/commit/c943cdaf826044dda9def9661e1ad478289bc4b7) | 2025-12-26 | docs: Add v1.0.1-beta release notes and bump version |
| [d12b4be](https://github.com/arookiecoder-ip/MusicBox-APP/commit/d12b4be9378fa5d090f882f0cb711cd40ad31341) | 2025-12-26 | feat: Update .gitignore to include JukeApplication.kt and ANALYTICS*.md |
| [05d1f89](https://github.com/arookiecoder-ip/MusicBox-APP/commit/05d1f89e8116b30c99badced5c81e928fadf6a14) | 2025-12-26 | Feat: Upgrade audio equalizer to 10 bands and update UI |
| [e49cb62](https://github.com/arookiecoder-ip/MusicBox-APP/commit/e49cb62ce42bb525ab1f37c5a433966baed2224b) | 2025-12-26 | Refactor: Enhance Media3 Playback Service with robust artwork validation |
| [0f45139](https://github.com/arookiecoder-ip/MusicBox-APP/commit/0f451392fc1c09f69a77086b7e5ce19cb991c195) | 2025-12-26 | Feat: Integrate analytics with PostHog |
| [001acde](https://github.com/arookiecoder-ip/MusicBox-APP/commit/001acde3f26221983a85ba0ab7e1dd14f78775d5) | 2025-12-26 | Refactor: General code cleanups and lint suppressions |
| [8ef161e](https://github.com/arookiecoder-ip/MusicBox-APP/commit/8ef161e42015ad34a4ac9bcc7d9f410538b0a318) | 2025-12-26 | Bump target SDK to 36 |
| [b908f16](https://github.com/arookiecoder-ip/MusicBox-APP/commit/b908f167295f72bdbbe80600db860b769a5fc020) | 2025-12-25 | feat: Enhance audio settings with equalizer, volume booster, and normalization features |
| [1802ca8](https://github.com/arookiecoder-ip/MusicBox-APP/commit/1802ca8c21bc1041464f82fac82c92dcbbf74e53) | 2025-12-24 | feat: Enhance library and search functionality with album support and UI improvements |
| [5dd531c](https://github.com/arookiecoder-ip/MusicBox-APP/commit/5dd531c20bfde94c0547da53bdd3bbc24e1500a7) | 2025-12-23 | Update issue templates |
| [848c624](https://github.com/arookiecoder-ip/MusicBox-APP/commit/848c6242935286620e547beace151ff532eedba2) | 2025-12-23 | feat: Update .gitignore to include LINKEDIN_*.md and retain *.salive |
| [3a475e5](https://github.com/arookiecoder-ip/MusicBox-APP/commit/3a475e54879c2fcc79bf54eac8da7ade005cd786) | 2025-12-23 | Update license badge in README.md to AGPL v3 |
| [edbd3bb](https://github.com/arookiecoder-ip/MusicBox-APP/commit/edbd3bb7a2a0b69a91f7ddf150de9fe4fbae9401) | 2025-12-23 | Update copyright year and author in LICENSE file |
| [17ceedf](https://github.com/arookiecoder-ip/MusicBox-APP/commit/17ceedfc012a6e76d498ea101e39c42c0547fc38) | 2025-12-23 | feat: Update README.md to link to RELEASE_NOTES.md for detailed release information |
| [cf5e5c5](https://github.com/arookiecoder-ip/MusicBox-APP/commit/cf5e5c52ba583ab7a62474f442bd40e977cd48fa) | 2025-12-23 | feat: Add seekToIndex method and update playTrack to play from queue in MusicViewModel |
| [8836958](https://github.com/arookiecoder-ip/MusicBox-APP/commit/883695895653e6cde2ea67ff46507c677a18b28e) | 2025-12-23 | feat: Update README.md with latest release details and installation instructions |
| [1c0aa57](https://github.com/arookiecoder-ip/MusicBox-APP/commit/1c0aa57c82a9313967a719bfdbdecb3e6b0f32df) | 2025-12-23 | Remove log file and add README.md for JUKE Music Player with detailed features, installation instructions, architecture, and API usage. |
| [6f3d429](https://github.com/arookiecoder-ip/MusicBox-APP/commit/6f3d42956be44c04d465708e1e8e99b6ff61a0a5) | 2025-12-23 | feat: Implement playlist import functionality and UI updates |
| [4a45efc](https://github.com/arookiecoder-ip/MusicBox-APP/commit/4a45efc6a5f0f665381068a59d006a0a3fb9a591) | 2025-12-23 | feat: Update build configuration and enhance UI components; enable ProGuard minification, add signing config, and improve track item displays across various screens |
| [38a65bb](https://github.com/arookiecoder-ip/MusicBox-APP/commit/38a65bb2500d30d1223b588729a509aa97e058c9) | 2025-12-23 | feat: Implement swipe-to-add-next functionality across various screens; add new SwipeToAddNextContainer component for enhanced user interaction |
| [7742811](https://github.com/arookiecoder-ip/MusicBox-APP/commit/7742811f6b89b0409f6924ab29cf810653f8a49e) | 2025-12-23 | feat: Add track removal and reordering functionality in PlaybackManager; update MusicViewModel and PlayerScreen for queue management |
| [54d8785](https://github.com/arookiecoder-ip/MusicBox-APP/commit/54d8785c50ab21d5abaeeda5a03945a1be8e3fe9) | 2025-12-23 | feat: Implement current track ID flow in PlaybackManager and update MusicViewModel to observe track changes; enhance PlayerScreen with full-screen queue display |
| [b41b788](https://github.com/arookiecoder-ip/MusicBox-APP/commit/b41b788b1367a1e73173daa6b38fb60c4a118e94) | 2025-12-23 | feat: Enhance search functionality with keyboard actions and IME options; improve player screen layout for better responsiveness |
| [6ec7ecf](https://github.com/arookiecoder-ip/MusicBox-APP/commit/6ec7ecf3030c5a199ce9e74a36324ea99788d5b5) | 2025-12-23 | chore: Add kotlin_shift to .gitignore to exclude generated files |
| [f3f06e5](https://github.com/arookiecoder-ip/MusicBox-APP/commit/f3f06e565fe3ae3dd450440385a2b6295b566213) | 2025-12-23 | Refactor: Remove unused network and service classes |
| [5ab3985](https://github.com/arookiecoder-ip/MusicBox-APP/commit/5ab398558afd9dc6c1407028eb41cdc14209d222) | 2025-12-23 | Refactor code structure and remove redundant code blocks for improved readability and maintainability |
| [01b15a9](https://github.com/arookiecoder-ip/MusicBox-APP/commit/01b15a9418bfd4e8e7e1da51492cc7f101ed818c) | 2025-12-23 | feat: Add app insights settings, deployment target selector, and default inspection profile; include launcher icons and background resources |
| [797f9e5](https://github.com/arookiecoder-ip/MusicBox-APP/commit/797f9e542a94ab2b227f09e6e910d983e81527ae) | 2025-12-21 | feat: Refactor media playback handling to use MediaController and improve UI responsiveness in PlayerScreen |
| [ffce8bf](https://github.com/arookiecoder-ip/MusicBox-APP/commit/ffce8bfb53dd9002b6b293c437a1a48c95cddee2) | 2025-12-21 | chore: Revert Android Gradle Plugin version to 8.13.1 |
| [7dc0731](https://github.com/arookiecoder-ip/MusicBox-APP/commit/7dc0731505e80e6f24db3e1889d1c9a2f07a6ac4) | 2025-12-21 | chore: Update Android Gradle Plugin version to 8.13.2 |
| [ca5b50f](https://github.com/arookiecoder-ip/MusicBox-APP/commit/ca5b50f9e238e41bd471c75f77c764234ed918e8) | 2025-12-21 | feat: Improve image selection logic in ArtistDetailScreen and reorganize sections in SearchScreen |
| [d2d6f76](https://github.com/arookiecoder-ip/MusicBox-APP/commit/d2d6f76fa3c3bb03f14aa8489e06f04851c8cebb) | 2025-12-21 | feat: Enhance album display in ArtistDetailScreen with a 2x2 grid layout and improved item styling |
| [45359b7](https://github.com/arookiecoder-ip/MusicBox-APP/commit/45359b71fbddc4f1621a7519694b4a2563687013) | 2025-12-21 | feat: Replace icon resources with vector drawables for improved scalability and maintainability |
| [2343ba2](https://github.com/arookiecoder-ip/MusicBox-APP/commit/2343ba24d730dac50385f0d9ae3df6f518cb82f9) | 2025-12-21 | feat: Add vector drawable resources for pause, next, and previous icons |
| [1679b4c](https://github.com/arookiecoder-ip/MusicBox-APP/commit/1679b4cf3cad08f819522983e31dc197a1422927) | 2025-12-21 | feat: Implement download management with UI components for tracking downloads |
| [35c5737](https://github.com/arookiecoder-ip/MusicBox-APP/commit/35c57379bb61cdc5b893ca3ed281192f8a6494bc) | 2025-12-20 | feat: Enhance Search Functionality with Artist and Playlist Support |
| [81fc70e](https://github.com/arookiecoder-ip/MusicBox-APP/commit/81fc70e992ee90a5a19e1d4d0185dfa6aad8ff2f) | 2025-12-20 | Refactor code structure for improved readability and maintainability |
| [732d5ff](https://github.com/arookiecoder-ip/MusicBox-APP/commit/732d5ffa744c83d96973101c7717cbc0cdbcadeb) | 2025-12-20 | add initial VCS configuration for Git |
| [afe51e4](https://github.com/arookiecoder-ip/MusicBox-APP/commit/afe51e4e6b35f97902cd5ca56e2da2718c49da05) | 2025-12-20 | first commit |

</details>
