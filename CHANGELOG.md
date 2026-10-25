# Music Box changelog

Coverage reviewed 2026-10-25: Android `feat/alexa-backend` through `22aa629d15a8d6f362d36b20c3dc81695eb7efb0`, plus companion backend `08139f16aac382d80703d59104838c4ecd1fb40a`. [Current architecture](ARCHITECTURE.md) · [Backend changelog](https://github.com/arookiecoder-ip/youtube-music-alexa-skill/blob/master/CHANGELOG.md).

Entries below distinguish source/debug builds from published releases. Dates follow commit metadata; they are not evidence that an APK was published on that date. The version-introduction ledger, PR register and full reachable commit index preserve detailed provenance. Existing inherited release notes are retained as historical records.

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
| [#10](https://github.com/arookiecoder-ip/MusicBox-APP/pull/10) | Fast playlist starts + suppress background offline toasts | Merged | History/reference; verify current behavior in ARCHITECTURE.md. |
| [#11](https://github.com/arookiecoder-ip/MusicBox-APP/pull/11) | CI verification: feat/alexa-backend merge (fast playlist starts) | Closed without merge | Temporary CI-verification PR, not a product release. |
| [#12](https://github.com/arookiecoder-ip/MusicBox-APP/pull/12) | Detect silent-but-advancing phone playback and recover in place | Merged | History/reference; verify current behavior in ARCHITECTURE.md. |
| [#13](https://github.com/arookiecoder-ip/MusicBox-APP/pull/13) | Feat/alexa backend | Merged | History/reference; verify current behavior in ARCHITECTURE.md. |
| [#14](https://github.com/arookiecoder-ip/MusicBox-APP/pull/14) | Fix playback snapshot ordering and compile review APKs in CI | Closed without merge | Closed without merge; equivalent lifecycle fix is on this branch as 53903fe. |

## Inherited release notes

The following original records describe their dated releases, including earlier cache/provider designs. They are not the current architecture or a fresh production-readiness guarantee.

## [2.4.0] - 2026-10-06

### Added

- Shared phone/Alexa playback and queue ownership with fenced handoffs, lease deadlines, and queue-identity preservation.
- Update-compatible signed stable release: minified/resource-shrunk, non-debuggable, pinned signing certificate, and SHA-256 checksums.
- In-app update channel pointed at MusicBox-APP: stable ignores prereleases, beta can upgrade to stable, releases without an APK are skipped.
- Release-only logging policy (errors only) and stable-APK verification script gating publishing.

See full release notes: [v2.4.0-RELEASE_NOTES.md](docs/v2.4.0-RELEASE_NOTES.md)

## [2.3.5-beta] - 2026-10-04

### Added

- Power Tools (locked until the version line is tapped 7 times): preferred audio source, repeat threshold, mini-player gestures, diagnostics and JSON export.
- Queue tools (shuffle upcoming, sort A–Z, clear played, save as playlist) and a Search tab re-tap that selects the query.

### Fixed

- Player backdrop reaches the bottom edge; Power Tools list scrolls.
- 30-second previews and wrong-length audio are rejected: fetched length is verified against Spotify and the source is switched and remembered.

See full release notes: [v2.3.5-beta-RELEASE_NOTES.md](docs/v2.3.5-beta-RELEASE_NOTES.md)

## [2.3.4-beta] - 2026-10-04

### Added

- Liquid glass UI revamp with redesigned player, search, and library screens; reserve-based recommendations.
- In-pipeline volume/bass boost with stable volume AGC, edge-only skip silence, playback speed, and queue status.
- Call handling via audio focus and a shared audio controller.
- Resumable Spotify playlist imports that survive app close and network loss.
- Repeat-aware recommendations (daily plays), reseeding from recent songs when a radio runs dry, and offline recommendations scored from play statistics.

### Fixed

- Forward seeking while a track is still streaming now switches to the completed local file at the requested position.
- Crashes opening Search and during playback with idle audio processors; tab navigation returns to the tab root.

See full release notes: [v2.3.4-beta-RELEASE_NOTES.md](docs/v2.3.4-beta-RELEASE_NOTES.md)

## [2.3.2-beta] - 2026-04-27

### Added

- Three-stage lyrics fallback chain: LRCLib primary lookup with YouTube Music lyrics and YouTube captions fallbacks.
- Search UX upgrade with YouTube Music autocomplete, debounced typing suggestions, and submit-only full Spotify metadata searches.
- Search tab multi-tap behavior in `MainActivity` (single tap navigate, double tap reset-and-focus, triple tap focus-without-clear).
- Mini-player lyrics intelligence: music-symbol filtering and adaptive gap threshold fallback.

### Fixed

- Spotmate queue responses (`queued`/`processing`) now trigger typed queue handling instead of hard-failing convert requests.
- Playback/download resilience improved through immediate provider fallback and deferred Spotmate task polling recovery.
- Search keyboard handling refined with scroll-to-dismiss and reliable delayed focus triggers.

See full release notes: [v2.3.2-beta-RELEASE_NOTES.md](docs/v2.3.2-beta-RELEASE_NOTES.md)

## [2.3.1-beta] - 2026-04-15

### Added

- Ensemble Recommendation Engine improvements (robust seed pool, parallel network intersections, offline fallback scoring).
- Streaming playback & download resilience (seamless stream promotion, optimized source routing, async lyrics and YouTube ID hydration).
- UI & UX polish: bottom action pill, mini-player redesign, animated play/pause transition, Material 3 player menu refresh, Search/Home/AudioSettings M3 updates.
- Haptics engine upgrade with semantic haptics and `VIBRATE` permission.

### Fixed

- Queue race condition and duplication resolved via atomic `replaceMediaItem()` swaps.
- LRU streaming stability fixes (eviction protection, redundant download prevention).
- Duplicate stream request avoidance and related stability improvements.

See full release notes: [v2.3.1-beta-RELEASE_NOTES.md](docs/v2.3.1-beta-RELEASE_NOTES.md)

## [2.3.0-beta] - 2026-04-12
 
### Added
 
- **LRU Streaming Cache**: Implemented a 15-file LRU disk cache for streaming playback. Temporary stream files are automatically evicted as the limit is reached, optimizing storage usage.
- **Dual Download Sources**: Integrated "Gamepvz" as a secondary download source alongside "Spotmate". Includes load-balanced source selection and automatic fallback on errors or duration mismatches.
- **Stream/DB Decoupling**: Streaming tracks are no longer persisted to the database by default, keeping the user's library clean and focused on permanent downloads.
 
### Fixed
 
- **Lyrics Auto-Scroll**: Fixed a bug where synced lyrics wouldn't scroll to the active line when toggled on during paused playback.
- **Library Bloat**: Updated library queries to filter out streaming tracks, ensuring an organized view across the app.
- **Startup Cleanup**: Added a background process to purge legacy stale stream entries from the persistent database.
 
### See full release notes
 
[v2.3.0-beta-RELEASE_NOTES.md](docs/v2.3.0-beta-RELEASE_NOTES.md)
 
---

## [2.2.0-beta] - 2026-03-18

### Added

- **Stream Playback Caching**: Integrated ExoPlayer `SimpleCache` with a 256MB LRU disk cache. Seeking back to already-buffered parts of a stream is now instant and doesn't require re-fetching from the network.
- **Artist Blacklist UI**:
  - Added "Blacklist Artist" option to the Player Screen more options menu.
  - Added a dedicated Blacklist toggle button on the Artist Detail screen.
  - Added a "Blacklisted Artists" management section in Audio Settings.

### Fixed

- **Radio Mode Pause**: Optimized `startRadio` to surgically trim the playback queue using `removeMediaItem` instead of resetting the entire player. This prevents the noticeable audio drop when starting radio mode.
- **Foreground Service Crash**: Added robust exception handling for `ForegroundServiceStartNotAllowedException` when the app is in the background on Android 12+.
- **Shuffle Logic**: Fixed a bug where the shuffle toggle failed to correctly update the playback queue or track sequencing.
- **Search Screen Crash**: Resolved `IllegalArgumentException` in the search results list caused by duplicate keys in `LazyColumn`.
- **Stream Seek Crash**: Fixed `MalformedURLException` (no protocol) when playing local files through the new `CacheDataSource` by using `DefaultDataSource` as the upstream factory.

### See full release notes

[v2.2.0-beta-RELEASE_NOTES.md](docs/v2.2.0-beta-RELEASE_NOTES.md)

---

## [2.1.1-beta] - 2026-03-03

### Fixed

- Swipe-to-Add-Next firing multiple times per gesture
- Player pausing/freezing on 403 HTTP error

### See full release notes

[v2.1.1-beta-RELEASE_NOTES.md](docs/v2.1.1-beta-RELEASE_NOTES.md)

---

## [2.1.0-beta] - 2026-03-02

### Added

- **Secure API Key via Cloudflare KV**: Spotdown API key is no longer bundled in the APK. Fetched at runtime from a Cloudflare Worker backed by KV storage. Key can be rotated without an app update.
- **Referer Header for Spotdown**: All Spotdown download/check requests now include `Referer: https://spotdown.org/` as required by the API.
- **Automatic Download Fallback**: Spotdown ↔ Spotmate auto-fallback on any error or duration mismatch exceeding 10%. Silent, no user action required.
- **Emergency Update Dialogs**: Update checker now distinguishes emergency (`hotfix`/`critical`/`emergency` tags) from normal updates — red themed, non-dismissible for emergencies.
- **Scrollable Update Changelog**: Release notes inside the update dialog are now fully scrollable.
- **Fully Responsive Player Screen**: `BoxWithConstraints` used to scale all player elements (artwork, icons, spacers, typography) based on 3 device height breakpoints — compact, normal, and large/tablet.
- **HomeScreen Settings Button Fix**: `Modifier.weight(1f)` on greeting column prevents the settings button from being pushed off-screen by long greetings.

### Fixed

- Spotdown API 403 due to missing Referer and API key headers
- Download silently failing when Spotdown rejected request
- Duration mismatch not triggering download source fallback
- Update dialog text unreadable on dynamic dark/light themes
- Emergency updates dismissible (should be forced)
- Player album art overflowing into controls on small-screen phones
- Lyrics close button displaced due to wrong `statusBarsPadding` in clipped lyrics frame
- Player controls using fixed sizes regardless of device screen size

### See full release notes

[v2.1.0-beta-RELEASE_NOTES.md](docs/v2.1.0-beta-RELEASE_NOTES.md)

---

## [2.0.0-stable] - 2026-02-20

### Overview

**First Major Stable Release** - JUKE v2.0.0-stable consolidates all features from v1.0.0 through v1.0.8-beta into a production-ready, battle-tested music streaming application.

### Status: Production Ready

- ✅ Proven stability through 8 beta releases
- ✅ Zero breaking changes from v1.0.x
- ✅ Seamless upgrade path for all users
- ✅ Complete feature parity with v1.0.8-beta

### Key Achievements

- **50+ Features**: Comprehensive music streaming platform
- **4 API Integrations**: Spotify, YouTube Music, Spotdown, LRCLib
- **Zero Crashes**: < 0.1% crash rate from beta testing
- **Professional Audio**: 10-band equalizer, volume booster, sleep timer
- **Offline-First**: Download and keep music forever
- **Beautiful UI**: Material Design 3 with glassmorphism

---

## [1.0.8-beta] - 2026-02-18

### See full release notes

[release-v1.0.8-beta.md](docs/release-v1.0.8-beta.md)

---

## [1.0.7-beta] - 2026-02-15

### Added

- **Purge Redundant Tracks**: New storage cleanup feature in Audio Settings. Identifies and deletes unused songs based on: low play count (< 5 plays, > 14 days old), never-played downloads (> 30 days), short audio (< 60s), and incomplete metadata. Excludes Favorites and playlisted tracks. Shows estimated storage savings with multi-select deletion.
- **Radio Mode**: One-tap radio button on the Player Screen. Resets the current queue to only the playing song, clears recommendation history, and fetches fresh recommendations based on the current track for an infinite, discovery-driven listening session.
- **Repeat Mode**: Added Repeat One, Repeat All, and Repeat Off modes. Cycle through modes via a new Repeat button in the player controls next to the Next button.
- **Notification Favorites**: Added a Heart (Favorite) toggle button to the media notification. Tapping it toggles the current track's favorite status in real-time with filled/border heart icon updates.
- **Shuffle Button in Player Controls**: Shuffle toggle now available directly in the player controls for easier access.

### Fixed

- **Shuffle Queue Premature End**: Fixed critical bug where shuffle mode would stop playing after ~14 tracks. Root cause was `player.replaceMediaItem()` in `onMediaItemTransition` which reset ExoPlayer's internal shuffle order/seed. Removed the offending call; metadata is now correctly set when tracks are initially added to the queue.
- **Spotdown API Key Requirement**: Updated `SpotifyApi` to include the required `x-api-key` header for `checkDirectDownload` and `downloadSong` requests. Added `SpotdownCheckResponse` data class for structured error handling. Prevents `Unexpected JSON token` parsing failures.
- **Spotdown Error Handling**: Implemented robust JSON parsing error handling and network exception catching in `SpotifyApi` to gracefully handle API failures and fall through to Spotmate.
- **Shuffle Recommendation Trigger**: Fixed recommendations triggering prematurely during shuffle play. Added `getRemainingTracksCount()` to `PlaybackManager` which traverses ExoPlayer's timeline using `getNextWindowIndex` to accurately count remaining songs in the shuffle order instead of relying on the linear queue index.

---

## [1.0.6-beta] - 2026-01-08

### Added

- **Local Audio Import**: Seamlessly import audio files from device storage. Copied to internal storage `imported_music/` with cover art extraction.
- **Batch Operations**: Multi-select songs in Library to "Add to Queue", "Play Next", or "Add to Playlist".
- **Playlist Management**: Added "Add to Queue" for entire playlists and inline "Rename" functionality.
- **Library UI Refinements**: Updated Library header to display active playlist name and integrated quick action buttons.
- **Player Visuals**: Solid black background for tracks with missing album art.
- **Audio Settings**: Added "Skip Silence" option to skip silent parts at start/end of tracks.

### Fixed

- **Queue Synchronization**: Fixed critical bug where deleting the playing track desynchronized the UI. `MusicViewModel` now observes `queueFlow` for real-time updates.
- **LRCLib Source**: Updated lyrics API endpoint to `https://lrclib.meek.workers.dev` for improved reliability.
- **Batch Deletion**: Optimized deletion logic to use batch database operations.
- **Compilation**: Fixed `PlayerScreen` crash by updating `AddToPlaylistDialog` signature.
- **Queue Duplication**: Implemented intelligent deduplication logic—new tracks are added and duplicates are moved to the end while excluding the currently playing song.
- **Playback Continuity**: Fixed playback restarts and random skips when updating queue structure by precisely maintaining current position and calculating shifted indices.

---

## [1.0.5-beta] - January 2026 (Emergency Release)

> ⚠️ **Emergency Release**: Critical fixes for queue, player, and lyrics issues.

### Added

- **Spotify Region Selector**: New option in Audio Settings to choose your Spotify market region (ISO 3166-1 alpha-2). Includes 20 popular markets (IN, US, GB, PK, NP, BD, LK, etc.) with a searchable dialog. All Spotify API searches now use the selected region for personalized results.
- **Instant Playback with Concurrent Lyrics**: Songs now start playing immediately via streaming while downloading in the background. Lyrics are fetched concurrently and appear within 1-2 seconds of playback start.
- **Queue Persistence for Streaming**: Streaming tracks are now saved to the database immediately, allowing queue restoration across app restarts. When downloads complete, the same track record is updated with the local file path.
- **Deleted Track Safety**: Critical safety features to prevent crashes when tracks are deleted:
  - Deleted tracks are automatically removed from the playback queue
  - If a deleted track is playing, playback automatically skips to the next song
  - File-not-found errors gracefully skip to next track instead of crashing
- **Play Statistics Preservation**: Play count and last played time are now preserved when streaming tracks are downloaded, maintaining accurate listening history.
- **Create Playlist from Player**: Added "Create Playlist" option to the "Add to Playlist" dialog on both Player Screen and Library Screen for quick playlist creation.
- **Improved Delete UX**: When deleting multiple tracks rapidly, pending deletes are now committed immediately before starting a new countdown, allowing true one-swipe deletion.
- **Player Screen Swipe Gestures**: Swipe left/right on the album art to skip to next/previous track.
- **Swipe-to-Open Queue**: Swipe up from the Player Screen bottom controls to open the queue.
- **Refresh Lyrics**: Added "Refresh Lyrics" option to the player 3-dots menu to re-fetch lyrics from LRCLib.
- **Smart Lyrics Fallback**: If primary lyrics search fails for multi-artist tracks, the app now retries with each artist individually (5% duration tolerance for strict matching).
- **Add to Playlist on Player**: Added a "+" button on the Player Screen to manage playlist membership for the current track.
- **Spotmate Fallback**: Added Spotmate as a secondary download source if Spotdown fails, with CSRF token handling.
- **Search Screen Redesign**: Premium redesign with glassmorphic search bar, filter chips (All, Tracks, Artists, Playlists, Albums), and frosted glass track cards. Enhanced empty states and polished result list.
- **Save Playlist Offline**: Added "Save Playlist Offline" button to Playlist Detail screens to download all tracks with progress indicator.

### Fixed

- **Refined Lyrics Selection**: Improved lyrics selection logic to prioritize synced lyrics and tie-break equal scores using duration proximity. This ensures the most accurate version (e.g., standard vs. extended/lo-fi) is selected.
- **Queue Synchronization Fix**: Fixed UI desynchronization when duplicate tracks exist in the queue. The player now uses the exact queue index for state updates instead of `indexOfFirst`, ensuring correct track highlighting.
- **Rapid Skip Race Condition**: Fixed a race condition where quickly skipping tracks caused metadata updates to overwrite the wrong track's info. The player now validates the current track before applying delayed updates.

## [Beta-Released]

### Added

- **Multi-Selection Bulk Delete**: Long-press any track to enter selection mode; features "Select All", batched deletion, and haptic feedback.
- **Enhanced Haptics**: Integrated tactile feedback for track selection, long-press gestures, and destructive confirmations.
- **Undo Timeout Update**: Increased deletion undo window from 3 seconds to 5 seconds for better recoverability.
- **Service Termination Lifecycle**: Application now explicitly stops playback and terminates background service when swiped away from recents.
- **Spotify Navigation Integration**:
  - Clickable artist names in Player Screen with a selection dialog for multi-artist tracks.
  - "Go to Album" and "Share Track" options added to the player context menu.
- **AI & OCR Modules**:
  - Integrated OpenRouter Gemma 3 model for image processing.
  - Local Image-to-Text extraction using Ollama and DeepSeek model.
  - On-device offline OCR using Google ML Kit with CameraX and Text-to-Speech support.
- **Playlist Deletion**: Added option to delete playlists via a context menu on playlist chips.
- **Robust Artist Matching**: Improved duplicate detection for playlist imports and smart downloads (handles multi-artists/case variations).
- **Library Screen Rework**: New UI layout with 70% search field and 10% sort button; includes persistent sort order and shuffle/play controls.
- **Tap-to-Seek**: Added tap gesture to seek bar for instant position changes.
- **Audio Focus Handling**: Music now pauses for other apps and auto-resumes correctly.

### Fixed

- **Samsung Media Notification Sync**: Fixed issue on Samsung devices where playback controls disappeared. Synchronized Notification IDs between manual `startForeground` and Media3 `DefaultMediaNotificationProvider` (ID 1).
- **Playlist Sync**: Fixed `track_count` desynchronization when tracks are deleted from the library.
- **UI Crash (Duplicate Keys)**: Fixed `IllegalArgumentException` in playlist `LazyColumn` by implementing a unique key strategy for duplicate tracks.
- **Background Resume Crash**: Resolved `ForegroundServiceStartNotAllowedException` when resuming playback from background on Android 12+.
- **Library UI**: Fixed Sort button height alignment with search field.
- **Queue Shuffle Logic**: Queue now correctly respects shuffle state when adding tracks.
- **Duplicate Download Bug**: Fixed race condition and cross-path duplication issues when adding songs to the download queue via different methods (swipe vs. click).
- **Lyrics Persistence Bug**: Refreshed lyrics now persist correctly when navigating between queue tracks.
- **Notification Thumbnail Repaint**: Added 500ms delay to notification metadata update to force thumbnail refresh on track change.

### Performance & Build

- **Queue Recommendation Reliability**: Fixed critical issue where recommendations stopped generating after prolonged use or app restart.
  - Implemented proper lifecycle reset for `QueueManager` to prevent "zombie" instance state.
- **ProGuard Optimization**: Updated rules to safely strip logging for battery saving while preserving critical data models and Room database classes.

## [1.0.1-beta_2] - December 2025

This release brings significant UI/UX improvements, enhanced queue management, and several bug fixes for a more polished music experience.

---

## ✨ New Features

### Playlist Management

- **Create Playlists**: Create custom playlists directly from the Library screen via the "New" chip button.
- **Add/Remove Tracks**: Easily add tracks to playlists using the **+** icon, or remove them with the **-** icon when viewing a playlist.
- **Context-Aware Dialog**: The "Add to Playlist" dialog now shows which playlists a track is already in and allows removing from those playlists directly.
- **Shuffle Play**: Shuffle any list of tracks (All Tracks, Favorites, or a specific Playlist) with a single tap from the Library header.

### Smart Recommendation System

- **Intelligent Queue Generation**: Automatic recommendations based on currently playing tracks using YouTube Music API integration.
- **Spotify Validation**: All recommendations are validated against Spotify to ensure high-quality matches (70% minimum confidence).
- **Background Downloads**: Concurrent download management with pre-buffering of next 2 songs for seamless playback.
- **Infinite Queue**: Automatic queue replenishment when songs drop to ≤2 remaining.
- **Spam Filtering**: Automatic filtering of remixes, covers, karaoke, and low-quality variants.
- See [RECOMMENDATION_SYSTEM.md](docs/RECOMMENDATION_SYSTEM.md) for comprehensive documentation and [QUICK_START_RECOMMENDATIONS.md](docs/QUICK_START_RECOMMENDATIONS.md) for integration guide.

### Enhanced Artist Matching

- **Multi-Artist Parsing**: Intelligent parsing of artist strings to handle collaborations and featured artists.
- **Order-Independent Matching**: Artist order no longer affects recommendation matching (e.g., "Drake, The Weeknd" matches "The Weeknd, Drake").
- **Flexible Similarity Scoring**: Uses Levenshtein distance for name matching with >60% threshold for valid matches.
- **Weighting System**: 70% average artist similarity + 30% matched artist ratio for balanced scoring.
- **Confidence Boosts**: Contextual confidence boosts based on artist match quality (0.05 to 0.15).
- See [ARTIST_MATCHING_ENHANCEMENT.md](docs/ARTIST_MATCHING_ENHANCEMENT.md) for technical details and [ARTIST_MATCHING_QUICK_REFERENCE.md](docs/ARTIST_MATCHING_QUICK_REFERENCE.md) for quick reference.

### App Update Checker

- **Automatic Updates**: Checks for new releases (including betas) on app launch.
- **In-App Notification**: Alerts the user with a dialog if a newer version is available on GitHub.
- **Direct Download**: Provides a direct link to the release page for easy downloading.

### Call Handling

- **Smart Pause**: Playback automatically pauses on incoming calls (`RINGING`) via `BroadcastReceiver`.
- **Manual Control**: Users can manually resume playback during a call (`OFFHOOK`). This is enabled by disabling ExoPlayer's automatic Audio Focus handling, preventing the system from blocking manual play requests during active calls.
- **Runtime Permissions**: Integrated `READ_PHONE_STATE` permission request in `MainActivity` on app launch.

### Notification Thumbnails

- **Optimized Thumbnails**: Separate 64x64 thumbnails are now stored for media notifications, reducing memory usage while maintaining 640x640 images for the player screen.
- **Database Migration**: Existing songs are gracefully handled; notifications fall back to the larger image if the new thumbnail is unavailable.

---

## 🎨 UI/UX Enhancements

### Search Screen Redesign

- **Modern Search Bar**: Visually refreshed search input with improved clarity and feedback.
- **Enhanced Track Display**: Better visual indicators for download status and improved spacing/typography.
- **Polished Empty States**: More informative and visually appealing empty and error states.

### MiniPlayer Redesign

- **Progress Line**: The MiniPlayer now displays a progress line indicating the current track position.
- **Swipe Gestures**: Swipe left to skip to the next track, swipe right for the previous track.

### Library Screen Redesign

- **Unified Filter Row**: All filter options (All Tracks, Favorites, Playlists) are now in a single, horizontally scrollable row.
- **Lazy Loading**: Optimized performance with "load when needed" patterns.
- **Centered Empty States**: Improved text alignment for empty library messages.

---

## 🔧 Improvements & Bug Fixes

### Queue Management Enhancements

- **Recommendation Variety**: Recently played artists are now tracked to influence future song recommendations, preventing repetitive suggestions.
- **Duplicate Prevention**: The system now prevents duplicate songs from being added to the queue, whether through recommendations or manual user additions.
- **"Move" Functionality**: Adding a song already in the queue now moves it to the new position instead of creating a duplicate.
- **Queue Hydration Safety**: Fixed queue contamination issue where selecting a new song while recommendations were downloading would pollute the new queue with old recommendations.
- **App Restart Duplication Fix**: Fixed issue where tracks from index 0 were duplicated and added below the last track when app was restarted. Removed duplicate `QueueManager.initializeQueue()` call in `MusicViewModel.loadRestoredQueue()`.
- **Auto-Resume After Call**: Implemented smart playback handling for phone calls. Music now explicitly pauses on incoming/outgoing calls and automatically resumes when the call ends, provided it was playing before the interruption.
- See [QUEUE_HYDRATION_FIX.md](docs/QUEUE_HYDRATION_FIX.md) for technical details on the fix.

### Track Matching Accuracy

- **Exact Match Logic**: Database lookups for tracks now use exact matching for title and artist (case-insensitive) and include a duration check (±2 seconds tolerance). This prevents issues like "Jhol" incorrectly playing "Jhol - Acoustic".

### Equalizer Fix

- **Sound Output Debugging**: Addressed issues where adjusting the equalizer in Audio Settings did not affect sound output. Ensured equalizer settings are correctly applied to the audio player's session.

### Notification Visibility Fix (Samsung One UI 6.x+)

- **Media Notification Fix**: Fixed notification not appearing on Samsung Galaxy S25 and Tab S11+ devices (Android 14/15). Uses platform MediaStyle notification formatting to ensure Samsung devices recognize it as a valid media player notification.
- **Background Playback Crash Fix**: Fixed `ForegroundServiceStartNotAllowedException` crash when resuming playback from notification while app is in background. Added manual `startForeground()` call with proper Android 14+ service type (`FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK`).

### Navigation & State Fixes

- **Search Tab Behavior**: Detail screens (Artist, Playlist, Album) now correctly navigate back to the main Search screen when the Search tab is clicked, and the Search tab remains highlighted on these screens.
- **Playlist URL Flag**: Fixed a bug where the "Import Playlist" button could incorrectly appear for non-playlist URLs.
- **Library Scroll Reset**: Prevented unwanted scroll reset in the Library when deleting tracks or toggling favorites.
- **Lyrics Reset**: Lyrics now correctly reset to the beginning when a new track starts playing.

---

## 📦 Technical Notes

- `PlaylistDao` updated with `getPlaylistsForTrack` for reverse lookup.
- `LibraryViewModel` methods (`addToPlaylist`, `removeFromPlaylist`) are now `suspend` functions for better state synchronization.
- `TrackDao.findTrackByTitleArtist` signature updated to include `durationSec` for accurate matching.
- `ValidatedRecommendation` now includes `durationSec`.

## Complete reachable commit index

This index covers all **550 commits reachable from `22aa629`** on the documented branch, including imported/merged history. It is a provenance ledger, not a claim that each historical implementation is active. Documentation-only changes after this functional snapshot are outside the index.

<details>
<summary>Expand the complete commit history</summary>

| Commit | Commit date | Subject |
| --- | --- | --- |
| [22aa629](https://github.com/arookiecoder-ip/MusicBox-APP/commit/22aa629d15a8d6f362d36b20c3dc81695eb7efb0) | 2026-10-25 | Preserve remote retry backoff when presence changes arrive |
| [0ce230e](https://github.com/arookiecoder-ip/MusicBox-APP/commit/0ce230eff10fcc4fe1674062051d57346c4b497d) | 2026-10-25 | Keep the active search input session alive across navigation re-taps |
| [08c8fe1](https://github.com/arookiecoder-ip/MusicBox-APP/commit/08c8fe1f9567d1c95463d090ddd202e5bd12cd98) | 2026-10-25 | Use presence events for immediate remote updates with fewer idle polls |
| [518dea4](https://github.com/arookiecoder-ip/MusicBox-APP/commit/518dea457dfe82e2ef17b6ff404daa26031d424f) | 2026-10-25 | Test artist video preview navigation and fence pagination responses |
| [d5d7a43](https://github.com/arookiecoder-ip/MusicBox-APP/commit/d5d7a43b092bd31fdfba4709c89eb8b11cdb6673) | 2026-10-25 | Reduce rendering and idle work; restore top status bar and artist video navigation |
| [8326f7b](https://github.com/arookiecoder-ip/MusicBox-APP/commit/8326f7bbb4cfb47ac2d4af80e63892e5739667eb) | 2026-10-25 | Fence keyboard cleanup and recover search focus across window changes |
| [684e0bd](https://github.com/arookiecoder-ip/MusicBox-APP/commit/684e0bd981dbe2a8b04354c51819b345d130cdbd) | 2026-10-25 | Measure artist banner coverage against a full-width scroll fixture |
| [98fd7aa](https://github.com/arookiecoder-ip/MusicBox-APP/commit/98fd7aa1fe32045adee5e416ea67e111ef13606f) | 2026-10-25 | Wake call recovery checks immediately and retain video-only artist pages |
| [69cac22](https://github.com/arookiecoder-ip/MusicBox-APP/commit/69cac229f668a7771e9e0a2024433db7158de787) | 2026-10-25 | Preserve regional search characters and limit idle call monitoring |
| [13f7931](https://github.com/arookiecoder-ip/MusicBox-APP/commit/13f7931caaf9ea440ecf272099f1b3c3646de7f5) | 2026-10-25 | Fix call recovery, search navigation and artist presentation |
| [f8afa09](https://github.com/arookiecoder-ip/MusicBox-APP/commit/f8afa0974dbd106026757f7146f7b56decb8a356) | 2026-10-24 | Cancel stale search keyboard requests on close and submission |
| [2a75133](https://github.com/arookiecoder-ip/MusicBox-APP/commit/2a7513329ba44e6f52dee3d06235004afdb67144) | 2026-10-24 | Refresh timed lyrics when playback output changes and cancel superseded lookups |
| [8cfbb9e](https://github.com/arookiecoder-ip/MusicBox-APP/commit/8cfbb9ecd01e5acc9626df2b44a6f07171ccaae9) | 2026-10-24 | Handle call focus arriving before mode changes and improve lyric query metadata |
| [009e9b1](https://github.com/arookiecoder-ip/MusicBox-APP/commit/009e9b1ffe56b69765331a5d75c591340d29f2e3) | 2026-10-24 | Restore timed lyrics, add smooth artwork and home blends, compact the dock and fix call playback intent |
| [2492aa5](https://github.com/arookiecoder-ip/MusicBox-APP/commit/2492aa5a2bc4237ef935ac28206ed57b8c5688eb) | 2026-10-24 | Keep Compose field focus while retrying search IME activation after actual touch |
| [4ed8345](https://github.com/arookiecoder-ip/MusicBox-APP/commit/4ed83459c51566738e839bb4f2c0057922ced743) | 2026-10-24 | Exercise offline song failure and retry through the app model and reset explicit retry budget |
| [d28334c](https://github.com/arookiecoder-ip/MusicBox-APP/commit/d28334c278e4518bef529ce3a070b1a79ed41950) | 2026-10-24 | Clear failed selection on explicit navigation and prevent seeking a superseded song |
| [ef8ed31](https://github.com/arookiecoder-ip/MusicBox-APP/commit/ef8ed31778a07df639642ba5fdc11756bcc5750e) | 2026-10-24 | Fix explicit queue playback, retain failed song selection, open search IME and refresh radio mixes |
| [d086fd0](https://github.com/arookiecoder-ip/MusicBox-APP/commit/d086fd0567e9c74908f2e975c0abdc9a57580912) | 2026-10-24 | Fix test tag import for playlist download badge emulator regression |
| [3fcf13a](https://github.com/arookiecoder-ip/MusicBox-APP/commit/3fcf13a1af9f78f67a95e12d743e7b577bb787ca) | 2026-10-24 | Fix playlist download badge crash when progress disappears before semantics reads |
| [0983606](https://github.com/arookiecoder-ip/MusicBox-APP/commit/0983606ac4442f212a357695d7a9561577455993) | 2026-10-24 | Fence delayed call resumes and controller starts after a new song or pause |
| [2b260f8](https://github.com/arookiecoder-ip/MusicBox-APP/commit/2b260f817090ea15a219f0cf2675d9975f8e7579) | 2026-10-24 | Fix queue gesture continuity, offline Echo selection, radio refresh and rolling audio recovery |
| [9e25a24](https://github.com/arookiecoder-ip/MusicBox-APP/commit/9e25a2424268e43d2dc202a078ccf0fa76db09a7) | 2026-10-24 | Preserve queue row metadata across playback transitions and verify intermediate frames |
| [c301e1e](https://github.com/arookiecoder-ip/MusicBox-APP/commit/c301e1eb8adbb88b926d1653dea0298d5bf1228a) | 2026-10-24 | Reproduce old song metadata leaking into newly selected queue rows |
| [225a76d](https://github.com/arookiecoder-ip/MusicBox-APP/commit/225a76dc054f8dc2cc7cbf72d38b38cb0c2c62dc) | 2026-10-24 | Recover offline Echo selections when server restarts without an output serial |
| [8bd7f5f](https://github.com/arookiecoder-ip/MusicBox-APP/commit/8bd7f5fc72e27752d38e1b5589031916db5dcc92) | 2026-10-24 | Report lifecycle visibility promptly and fence delayed foreground updates |
| [d5b1a5b](https://github.com/arookiecoder-ip/MusicBox-APP/commit/d5b1a5b858aac79398222447515ad5466609aecf) | 2026-10-24 | Keep tap callbacks fresh and navigation gestures stable during UI updates |
| [1f7698c](https://github.com/arookiecoder-ip/MusicBox-APP/commit/1f7698cb6cc57106f94d2924b40000c4e0c119cd) | 2026-10-24 | Bind queue metadata to entries, play selected rows and correct song menus and device fallback |
| [22994fd](https://github.com/arookiecoder-ip/MusicBox-APP/commit/22994fd23ad7ad1bba37220fa9e3b24d373265ea) | 2026-10-24 | Cancel superseded YouTube link requests on transport changes |
| [29c7a36](https://github.com/arookiecoder-ip/MusicBox-APP/commit/29c7a36ccfad2b9a0576e0b2a261ae73835d6b5f) | 2026-10-24 | Wait for YouTube link submission and fence pending playback; fix remote queue removal |
| [3d02ec9](https://github.com/arookiecoder-ip/MusicBox-APP/commit/3d02ec9745d20cbda28256f2c66763c6f08b82c7) | 2026-10-24 | Correct portrait metadata fixture syntax |
| [233d415](https://github.com/arookiecoder-ip/MusicBox-APP/commit/233d415c140a8b4d8a387f749a87ee5a804a9b84) | 2026-10-24 | Fix search IME focus, load artist portraits on demand and improve cover controls |
| [e32f3de](https://github.com/arookiecoder-ip/MusicBox-APP/commit/e32f3dee8157657a626b55bf1ede269ee9763951) | 2026-10-24 | Keep queue selection until its underlying presentation catches up |
| [0b22468](https://github.com/arookiecoder-ip/MusicBox-APP/commit/0b2246899278076c9b44e570dcacd44acec5f448) | 2026-10-24 | Transfer paused phone cursor to Echo atomically and connect destination renderer early |
| [5877c9c](https://github.com/arookiecoder-ip/MusicBox-APP/commit/5877c9c8cae95e89c951a4bddc1760e4842de144) | 2026-10-24 | Fix queue selection, diagonal scrolling and handoff stalls; polish speed dial and playlist header |
| [247592f](https://github.com/arookiecoder-ip/MusicBox-APP/commit/247592ff99da22a5b8f0a3008f06c85b583fbbb3) | 2026-10-24 | Bump debug update to version 2.4.41 (93) |
| [bd86117](https://github.com/arookiecoder-ip/MusicBox-APP/commit/bd86117c0661e9e8f8fbcc4d140948363d8de8a5) | 2026-10-24 | Report playback controls immediately and stabilize device handoff feedback |
| [5ad3d03](https://github.com/arookiecoder-ip/MusicBox-APP/commit/5ad3d03a83b439749bf324278a6bea5d1cdeb021) | 2026-10-22 | Keep dismissed output controllable without restarting its saved queue |
| [2f3fab7](https://github.com/arookiecoder-ip/MusicBox-APP/commit/2f3fab7b44b5300084014a43a33c2d5ddf24fec5) | 2026-10-22 | Refine collection controls and prevent playback resuming after task dismissal |
| [53903fe](https://github.com/arookiecoder-ip/MusicBox-APP/commit/53903feac0fc7d0afa15a48516e963c1ff97b50e) | 2026-10-22 | Discard poll side effects when snapshot decoding is invalidated |
| [ee2cfb8](https://github.com/arookiecoder-ip/MusicBox-APP/commit/ee2cfb8c4d9fee1fc4549237f1b634a2fbf64ef6) | 2026-10-22 | Improve playlist controls and reduce phone playback startup delays |
| [f00994e](https://github.com/arookiecoder-ip/MusicBox-APP/commit/f00994e1f424fce162a0518b370b00b3ee1f02ac) | 2026-10-18 | Integrate playback guards with current feature branch and CI |
| [3822b0f](https://github.com/arookiecoder-ip/MusicBox-APP/commit/3822b0f556042a37d24939c42bd5a91c39b9ced1) | 2026-10-18 | Test remote controller lifecycle with injectable transport |
| [08c6c60](https://github.com/arookiecoder-ip/MusicBox-APP/commit/08c6c601bc14005cedc53ff4004b771c93e4344a) | 2026-10-18 | Guard playback snapshots across output changes and validate APKs in CI |
| [08c71a3](https://github.com/arookiecoder-ip/MusicBox-APP/commit/08c71a33cacf3f92685b8771ddc0a7d01de78695) | 2026-10-12 | Measure adaptive collection artwork directly in parity regression |
| [08bc58a](https://github.com/arookiecoder-ip/MusicBox-APP/commit/08bc58a2070afd138904a62fe20c8e3a7f666182) | 2026-10-12 | Fix padded lyrics seeking and validate local phone and tablet UI fixtures |
| [3f93fef](https://github.com/arookiecoder-ip/MusicBox-APP/commit/3f93fef1bd79d73c352aa1c6dfa88360b2389641) | 2026-10-12 | Improve queue selection and player responsiveness; silence routine slow polls |
| [0730716](https://github.com/arookiecoder-ip/MusicBox-APP/commit/07307166d8386abc7e9502236f3d5e30dfa09fa6) | 2026-10-12 | Reduce playback UI and background overhead in debug 2.4.39 |
| [382aff3](https://github.com/arookiecoder-ip/MusicBox-APP/commit/382aff3250e05a892f14a78ede14de8e30a2d78f) | 2026-10-11 | Fix Alexa control reconciliation and call-held playback in debug 2.4.38 |
| [30edd3f](https://github.com/arookiecoder-ip/MusicBox-APP/commit/30edd3f2c1125cc7f78619b1428615f361eeabb4) | 2026-10-10 | Check cache generation across each read including error fallback |
| [715b9ca](https://github.com/arookiecoder-ip/MusicBox-APP/commit/715b9cadbc3d85c4e6b975b986d2bf5d44c7eedc) | 2026-10-10 | Fence cached audio handoffs and repair EOF from completed streams |
| [f6837c1](https://github.com/arookiecoder-ip/MusicBox-APP/commit/f6837c12c87e5db1c97adc7d23f05020d60e8af0) | 2026-10-10 | Persist automatic playback error reports for debug diagnostics |
| [8f0a90b](https://github.com/arookiecoder-ip/MusicBox-APP/commit/8f0a90bf42a3c4a09f709fbba672514490843dca) | 2026-10-09 | Batch cache integrity reads and support long fragmented audio |
| [57f411f](https://github.com/arookiecoder-ip/MusicBox-APP/commit/57f411f3751d72689201fdba0014b480d1261163) | 2026-10-09 | Forward HTTP headers explicitly through audio validation |
| [d9c5244](https://github.com/arookiecoder-ip/MusicBox-APP/commit/d9c52448d781d0f30e007b6ade5c7b6ff327516a) | 2026-10-09 | Keep background audio continuous across truncated transfers and cache completion |
| [d845d41](https://github.com/arookiecoder-ip/MusicBox-APP/commit/d845d412a88e1a0db1f69f8494e62a788b8e29da) | 2026-10-09 | Avoid repeating server queue preparation for local-only songs |
| [b6d554e](https://github.com/arookiecoder-ip/MusicBox-APP/commit/b6d554ef3a4a949f50dc2bb6e6e8dc1c32dcb5a2) | 2026-10-09 | Ignore stale resume responses after a newer device lease takes ownership |
| [0d07d64](https://github.com/arookiecoder-ip/MusicBox-APP/commit/0d07d64fb401bbec9b84828cae9ce1f26afab3b6) | 2026-10-09 | Suppress late framework Play after any explicit resumption cancellation |
| [5bed213](https://github.com/arookiecoder-ip/MusicBox-APP/commit/5bed213bc4ea1c64fe1c3dbbc3519acc28840a80) | 2026-10-09 | Show actionable background resume errors and test notification feedback |
| [1d05937](https://github.com/arookiecoder-ip/MusicBox-APP/commit/1d05937027b4e643f009adec621c0c47f19351ad) | 2026-10-09 | Reject resuming a deleted current song instead of starting its neighbor |
| [e3a256c](https://github.com/arookiecoder-ip/MusicBox-APP/commit/e3a256caec415664de96786a3974169809ccd417) | 2026-10-09 | Fence overlapping resumption jobs and retain pending playback on screen reopen |
| [ed3188a](https://github.com/arookiecoder-ip/MusicBox-APP/commit/ed3188a4a12b0e668fe54aee799044f44abeb719) | 2026-10-09 | Restore notification and earbud playback safely without opening the app |
| [36b3717](https://github.com/arookiecoder-ip/MusicBox-APP/commit/36b3717a031b8a697a1be7fd3bd80275744902bf) | 2026-10-09 | Preserve structured artist credits in queue metadata |
| [94b2ddf](https://github.com/arookiecoder-ip/MusicBox-APP/commit/94b2ddfc6b48a7ad8e7b62203b3a9f856f50e9f1) | 2026-10-09 | Match web artist credit parsing and preserve structured band names |
| [9611414](https://github.com/arookiecoder-ip/MusicBox-APP/commit/9611414f899699772119c92026d762a4dab7cef8) | 2026-10-09 | Use existing position listener for interruption diagnostics |
| [45f59d5](https://github.com/arookiecoder-ip/MusicBox-APP/commit/45f59d5467e88e844873fec2b64e60b6b7c3eb2f) | 2026-10-09 | Avoid interrupting cached audio while bounded ownership renewal is pending |
| [28d5fcb](https://github.com/arookiecoder-ip/MusicBox-APP/commit/28d5fcbd2dfb6feabf03df939c337cd4e2652d26) | 2026-10-09 | Keep cached phone audio running when ownership server is unreachable |
| [a96eb62](https://github.com/arookiecoder-ip/MusicBox-APP/commit/a96eb623f27ef2de3e26dffb2d1be4a20a128154) | 2026-10-09 | End reconnect grace only on confirmed server ownership |
| [abbe141](https://github.com/arookiecoder-ip/MusicBox-APP/commit/abbe141f679c81cf42ca2f604b9b25547bba4a65) | 2026-10-09 | Bound reconnect ownership reconciliation without interrupting available audio |
| [0017a9c](https://github.com/arookiecoder-ip/MusicBox-APP/commit/0017a9c8a108ec796abf9935e78253c868d4d49a) | 2026-10-09 | Let available buffered local audio finish during network outages |
| [2954479](https://github.com/arookiecoder-ip/MusicBox-APP/commit/295447935043bbdcbd982feec49dd647ae800173) | 2026-10-09 | Retain cache identity when reopening cached audio |
| [562dc4a](https://github.com/arookiecoder-ip/MusicBox-APP/commit/562dc4a1371ad53a9883843a5ce922dce9ad6804) | 2026-10-09 | Recover failed sockets directly from complete cache without server lookup |
| [9c6c44a](https://github.com/arookiecoder-ip/MusicBox-APP/commit/9c6c44aa663ff722e10966e51e1a1f83e4a83ea8) | 2026-10-09 | Verify reconnect retry rate limits and identify debug build 83 |
| [543ae04](https://github.com/arookiecoder-ip/MusicBox-APP/commit/543ae04bedd9e3e90457431a2b64a28eeefefe38) | 2026-10-09 | Preserve healthy mobile audio across connectivity interruptions |
| [874e233](https://github.com/arookiecoder-ip/MusicBox-APP/commit/874e233708441c57a1800d969016966ec200332b) | 2026-10-09 | Handle missing downloaded copy safely during audio recovery |
| [062721a](https://github.com/arookiecoder-ip/MusicBox-APP/commit/062721a2873265170dad1245709bb9df452bcdf1) | 2026-10-09 | Preserve healthy downloads when repairing a failed network stream |
| [a43417d](https://github.com/arookiecoder-ip/MusicBox-APP/commit/a43417dd02e72f92d14466d8c803a08ab6f0d645) | 2026-10-09 | Repair invalid audio responses and downloads; refine song dial and notification radio |
| [3e2f232](https://github.com/arookiecoder-ip/MusicBox-APP/commit/3e2f232cecd5d03c5d1ab8db7ba6ff4e287ea15a) | 2026-10-09 | Preview remote notification track transitions and reconcile confirmed playback |
| [83b2d31](https://github.com/arookiecoder-ip/MusicBox-APP/commit/83b2d313fcef00fc30c4d1b5882cdacdb3bd855b) | 2026-10-09 | Promote failed speculative audio requests without bypassing rate limits |
| [c21a8ae](https://github.com/arookiecoder-ip/MusicBox-APP/commit/c21a8ae9ae89ea1112bd2de3231f678aa5eaa0a0) | 2026-10-09 | Add personalized Home speed dial and protect current audio caching |
| [ccf0eeb](https://github.com/arookiecoder-ip/MusicBox-APP/commit/ccf0eebb4760a90bc2bf350b65cd7fd6157b2d31) | 2026-10-09 | Persist Home feed for thirty minutes with background refresh |
| [bfaa4d5](https://github.com/arookiecoder-ip/MusicBox-APP/commit/bfaa4d5c3cd8ff74a9e6e9f114878dcd846b4f0c) | 2026-10-09 | Fix offline Home crash and downloaded playback restoration |
| [4ba196a](https://github.com/arookiecoder-ip/MusicBox-APP/commit/4ba196a60959f8472394fa842c676be56dc4cd8c) | 2026-10-09 | Hold transport previews until confirmed state reaches UI presentation |
| [d211212](https://github.com/arookiecoder-ip/MusicBox-APP/commit/d2112121156fac5ef5b1f0a0fde79e99de93b32d) | 2026-10-09 | Preserve sign-in expiry handling during remote confirmation recovery |
| [637fc98](https://github.com/arookiecoder-ip/MusicBox-APP/commit/637fc98bb74d5bf988c43120c8fa23cfcb4e60ae) | 2026-10-09 | Bind remote transport retries to the confirmed receiver session |
| [cf8e623](https://github.com/arookiecoder-ip/MusicBox-APP/commit/cf8e62370b1b5fe254748ad56c9cead496780223) | 2026-10-09 | Match receiver previous-song restart and paused skip semantics |
| [47a17f4](https://github.com/arookiecoder-ip/MusicBox-APP/commit/47a17f4177fad4959cc4992bf056466b2a99f5b6) | 2026-10-09 | Allow receiver confirmations after backend revision epoch resets |
| [1f2a402](https://github.com/arookiecoder-ip/MusicBox-APP/commit/1f2a402c035addcdb38b1496ba1d2ef7a434c2c6) | 2026-10-09 | Require fresh receiver revisions and reject delayed phone status snapshots |
| [81b7439](https://github.com/arookiecoder-ip/MusicBox-APP/commit/81b74392fc3c6899d6cdacb268678fefceae1ebe) | 2026-10-09 | Preview remote phone transport instantly and reconcile bounded confirmations |
| [1a0d30e](https://github.com/arookiecoder-ip/MusicBox-APP/commit/1a0d30e6789ab1d17a4664142caa443ede054634) | 2026-10-09 | Present mobile and tablet queues immediately with a short entrance |
| [1c75561](https://github.com/arookiecoder-ip/MusicBox-APP/commit/1c7556171a75568ea32d14799663733fe5a24e08) | 2026-10-09 | Separate playback cache reads from full-song background writes |
| [16b2c39](https://github.com/arookiecoder-ip/MusicBox-APP/commit/16b2c393012d30913fab631d821e57a03af4c79a) | 2026-10-09 | Load ordinary songs past the two-minute ceiling with bounded sample memory |
| [45ce499](https://github.com/arookiecoder-ip/MusicBox-APP/commit/45ce4990dec3c2ba83975f5d1cdaa84f8a169ead) | 2026-10-09 | Verify full current-song caching and lazily cache five upcoming streams |
| [3bb8ab6](https://github.com/arookiecoder-ip/MusicBox-APP/commit/3bb8ab6bd2c1ad94cb89b0df55d1ecfd96d24cb9) | 2026-10-09 | Use current player metadata and scope remote loading to remote outputs |
| [40a24dd](https://github.com/arookiecoder-ip/MusicBox-APP/commit/40a24dd430e213b1bd3f0f825d938addd47283cc) | 2026-10-09 | Test metadata refresh and missing current-song database fallback |
| [7c7b27a](https://github.com/arookiecoder-ip/MusicBox-APP/commit/7c7b27a3102e53663d191ccb4fe1f436bbc84a08) | 2026-10-09 | Refresh live song metadata and expose loading for notification skips |
| [649b8fb](https://github.com/arookiecoder-ip/MusicBox-APP/commit/649b8fbe9855a612269204104dde05d4b9bbfe11) | 2026-10-08 | Use supported Robolectric application accessor in service regression |
| [d698906](https://github.com/arookiecoder-ip/MusicBox-APP/commit/d6989065266c6bb6c67f6540684a13add1c5be58) | 2026-10-08 | Promote every connection service start and refresh truncated audio on retry |
| [f1cd079](https://github.com/arookiecoder-ip/MusicBox-APP/commit/f1cd079da2e5f3852025d6d93de77071e2ec866f) | 2026-10-08 | Meet foreground deadline before remote restart waits for server metadata |
| [dfc0b82](https://github.com/arookiecoder-ip/MusicBox-APP/commit/dfc0b82affc3beab40c9be7c3597bf935374065e) | 2026-10-08 | Explain unconfirmed remote skips after the bounded loading wait |
| [2f57eb6](https://github.com/arookiecoder-ip/MusicBox-APP/commit/2f57eb6b5eff245bdc7a2abe6c59b6f8ed57ce7e) | 2026-10-08 | Show immediate loading for remote next and previous until playback confirms |
| [c33bfd4](https://github.com/arookiecoder-ip/MusicBox-APP/commit/c33bfd4df7921c48c3f1769f1a4179c0d3c35534) | 2026-10-08 | Keep playback command lifecycle independent of Activity recreation |
| [676c59f](https://github.com/arookiecoder-ip/MusicBox-APP/commit/676c59f4f843ba381552c615838a3b9c369f04b6) | 2026-10-08 | Allow Android to restore active device connection and remote controls |
| [1ca7017](https://github.com/arookiecoder-ip/MusicBox-APP/commit/1ca701734a0cfbf403dad8a7b8fc085eca223e64) | 2026-10-08 | Retain buffering notifications and show loading during remote phone recovery |
| [5f2a853](https://github.com/arookiecoder-ip/MusicBox-APP/commit/5f2a853309c96390c2865d0be09182c0dadd58c6) | 2026-10-08 | Keep restored playback cursor aligned when saved tracks are missing |
| [a6fe9b9](https://github.com/arookiecoder-ip/MusicBox-APP/commit/a6fe9b9f9d0a6f9c0d91edec48e4d61022d79054) | 2026-10-08 | Fix volume feedback expiry and restore playback with current stream caching |
| [b308e59](https://github.com/arookiecoder-ip/MusicBox-APP/commit/b308e59ad4fe18f06ef67e3ef2c6d857f669fcda) | 2026-10-08 | Include application bootstrap and reject APKs missing manifest entry points |
| [daac938](https://github.com/arookiecoder-ip/MusicBox-APP/commit/daac93839f9de5646d5accad039ea39425c7ec2b) | 2026-10-08 | Rename Music Box to in.synthora.musicbox and confirm Alexa transport UI promptly |
| [fca5285](https://github.com/arookiecoder-ip/MusicBox-APP/commit/fca5285d88b158bc7538288c62208ee7e1fd0fa2) | 2026-10-08 | Retry malformed cached audio once and report exact safe player failure details |
| [ddf28b8](https://github.com/arookiecoder-ip/MusicBox-APP/commit/ddf28b8171e06b700be588969b5550e6a97c10ad) | 2026-10-08 | Refresh saved-cache progress only while the player UI is visible |
| [b67a268](https://github.com/arookiecoder-ip/MusicBox-APP/commit/b67a2689b749552348a5d50a149dc7ae7024d5b1) | 2026-10-08 | Keep readiness loading while recoverable audio requests retry |
| [3b16ce5](https://github.com/arookiecoder-ip/MusicBox-APP/commit/3b16ce5cd4d6c4bdc6ab2394c05bf6302c8e6f5e) | 2026-10-08 | Show persistent contiguous stream cache and complete downloaded audio |
| [e6eb7bc](https://github.com/arookiecoder-ip/MusicBox-APP/commit/e6eb7bc97415bf4d143d3d82579d397afc012997) | 2026-10-08 | Read buffer visibility from the public playback output state |
| [5099632](https://github.com/arookiecoder-ip/MusicBox-APP/commit/50996328934c43026693ab0823d84f4033d69839) | 2026-10-08 | Reload failed phone audio before toggling play and show buffered progress |
| [be5d81b](https://github.com/arookiecoder-ip/MusicBox-APP/commit/be5d81bf3fc2ec34edcbc82e33e636a9ecbf490a) | 2026-10-08 | Release the remote service anchor through the guarded binding context |
| [61e48d8](https://github.com/arookiecoder-ip/MusicBox-APP/commit/61e48d8196273883d2eecf4a81f744ef4e565a19) | 2026-10-08 | Fix asynchronous Media3 binding cleanup and controller reconnection |
| [6365d4c](https://github.com/arookiecoder-ip/MusicBox-APP/commit/6365d4cbda438877743a8738c8c8e279f69ba139) | 2026-10-08 | Use exact-track readiness once and preserve cancellation diagnostics |
| [46aa5e4](https://github.com/arookiecoder-ip/MusicBox-APP/commit/46aa5e4c623f3abd229832722ea7427183f609e8) | 2026-10-08 | Keep seed preparation silent until the full queue timeline is installed |
| [00659ac](https://github.com/arookiecoder-ip/MusicBox-APP/commit/00659ac82e8a805ffb82391917d6db714719a305) | 2026-10-08 | Remove redundant handoff requests and prepare selected audio before bulk queue work |
| [7e51956](https://github.com/arookiecoder-ip/MusicBox-APP/commit/7e51956edfcb33043aa204c1d0bcde64559b194f) | 2026-10-08 | Do not reuse an expired rate-limit delay after a later timeout |
| [8f2ee2a](https://github.com/arookiecoder-ip/MusicBox-APP/commit/8f2ee2a0abfe9140a0e2a88d7d3692ab3241a229) | 2026-10-08 | Preserve idle polling on legacy servers and avoid offline ownership requests |
| [1cc1def](https://github.com/arookiecoder-ip/MusicBox-APP/commit/1cc1def47835fd83d76985641bfce9c2db47c8ea) | 2026-10-08 | Reduce background presence traffic and coalesce unchanged playback cursors |
| [414e7ef](https://github.com/arookiecoder-ip/MusicBox-APP/commit/414e7efd6b4ee323c82a22c65094c248bb791411) | 2026-10-08 | Handle connection foreground promotion restrictions without crashing |
| [cccf25d](https://github.com/arookiecoder-ip/MusicBox-APP/commit/cccf25d729fe3b0fa2241409ceacb2a91d7fdf2e) | 2026-10-08 | Keep remote device connection foreground while locked and meet media button startup deadline |
| [8920647](https://github.com/arookiecoder-ip/MusicBox-APP/commit/8920647b79ba0daad4fcb6b5f6aa3266278c7b1c) | 2026-10-08 | fix: preserve destination foreground authorization across background phone handoffs |
| [84eaaf1](https://github.com/arookiecoder-ip/MusicBox-APP/commit/84eaaf1daa9d8eb628f8fa7f15dae20bc856b1a2) | 2026-10-07 | fix: retry remote play intents after the same phone renews its song token |
| [a362fe2](https://github.com/arookiecoder-ip/MusicBox-APP/commit/a362fe22dbda752b0f8e8849fec3e4ad0c6a7ead) | 2026-10-07 | fix: freeze mirrored playback clocks until the phone handoff is ready |
| [b11d2ab](https://github.com/arookiecoder-ip/MusicBox-APP/commit/b11d2abf590f1561e1bfdcb98862d5470f6a2123) | 2026-10-07 | fix: retain authorized background playback until remote songs are prepared |
| [c2f686a](https://github.com/arookiecoder-ip/MusicBox-APP/commit/c2f686a07ff78f1f86269c8b924172a7a0defafe) | 2026-10-07 | fix: serialize remote song preparation and preserve phone handoff presentation |
| [ba5bd73](https://github.com/arookiecoder-ip/MusicBox-APP/commit/ba5bd73e75d914427b6e536fda33b4abff4db954) | 2026-10-07 | fix: keep notification seek and volume routed to the handoff destination |
| [23716e4](https://github.com/arookiecoder-ip/MusicBox-APP/commit/23716e48fb6bb0698c91a30816e1206fa1210848) | 2026-10-07 | fix: reconcile accepted artist subscription changes in the library |
| [acfcee9](https://github.com/arookiecoder-ip/MusicBox-APP/commit/acfcee9e933f69a545582218929540307e19cd9f) | 2026-10-07 | fix: restore player interactions and reliable background device handoffs |
| [53a4467](https://github.com/arookiecoder-ip/MusicBox-APP/commit/53a4467aef24c0af3abd4fd2134aef51fd7b3445) | 2026-10-07 | fix: keep presence and recovery alive during slow background handoffs |
| [3640485](https://github.com/arookiecoder-ip/MusicBox-APP/commit/3640485855e159d522555cf20f0f8c6c231db3ab) | 2026-10-07 | fix: resume closed phone sessions and avoid false silence restarts |
| [0752733](https://github.com/arookiecoder-ip/MusicBox-APP/commit/075273361e1a2ce1f5ee5ec1634bef485d882a25) | 2026-10-07 | fix: display cached covers immediately and preserve duplicate shelf ordering |
| [b689632](https://github.com/arookiecoder-ip/MusicBox-APP/commit/b689632c6d68fcbd5cd5048ecc65f7bb20eb1f3d) | 2026-10-07 | fix: blend and center landscape heroes and speed up player presentation |
| [0705c4f](https://github.com/arookiecoder-ip/MusicBox-APP/commit/0705c4fae1871225d6a0cb4d31269fd1c50a1956) | 2026-10-07 | perf: isolate root playback observers and bound lyrics backdrop decoding |
| [5b028f1](https://github.com/arookiecoder-ip/MusicBox-APP/commit/5b028f1a3c16b308ddd99ec5f91ab213991ca013) | 2026-10-07 | fix: center landscape artwork and lyrics and route remote volume keys |
| [88c77f0](https://github.com/arookiecoder-ip/MusicBox-APP/commit/88c77f0a640cb840d77b05f0c176c3567dd6639b) | 2026-10-07 | Use target volume levels and complete genre and downloads skeleton states |
| [c5676c6](https://github.com/arookiecoder-ip/MusicBox-APP/commit/c5676c6e8f3f4fbf27521aa139e89d15869a28e5) | 2026-10-07 | Round remote phone volume commands to Android stream steps |
| [f995955](https://github.com/arookiecoder-ip/MusicBox-APP/commit/f995955a9497e66100204f98b55b2702b12a59b0) | 2026-10-07 | Bound idle-device reconciliation and preserve login volume key behavior |
| [790eaba](https://github.com/arookiecoder-ip/MusicBox-APP/commit/790eaba427fb410a1646fb1f23539b1bc1954016) | 2026-10-07 | Reconcile idle device preference before adopting a paused Alexa output |
| [15e87a4](https://github.com/arookiecoder-ip/MusicBox-APP/commit/15e87a4e988996af0f919d696e7c3d574e645046) | 2026-10-07 | Route remote volume keys and select silent local defaults with responsive loaders |
| [a1a4f37](https://github.com/arookiecoder-ip/MusicBox-APP/commit/a1a4f3784dd8723383f095062df2331132978c2c) | 2026-10-07 | Verify late artwork callbacks cannot restart a switched output |
| [a640f5d](https://github.com/arookiecoder-ip/MusicBox-APP/commit/a640f5daecb24517ddff2add872a658baaa4ac00) | 2026-10-07 | Cancel automatic resumes after Android denies foreground playback |
| [53684a7](https://github.com/arookiecoder-ip/MusicBox-APP/commit/53684a71f596a470cf8ebe5131270268019c60c9) | 2026-10-07 | Guard delayed notification artwork against Android foreground start rejection |
| [bb8f301](https://github.com/arookiecoder-ip/MusicBox-APP/commit/bb8f30172e07ff0816e88b456c47bf04dfc8fe29) | 2026-10-07 | Avoid busy lease checks after a disconnected session expires |
| [6f47f8e](https://github.com/arookiecoder-ip/MusicBox-APP/commit/6f47f8ed0c5998d5b24b33b11af2dacd0ebb5949) | 2026-10-07 | Retry pending player connections and reduce idle playback maintenance |
| [32c23e3](https://github.com/arookiecoder-ip/MusicBox-APP/commit/32c23e345c3638202da12681791e3e2ee01d4ab1) | 2026-10-07 | Preserve tablet-only profile popup behavior across rotation |
| [cc35784](https://github.com/arookiecoder-ip/MusicBox-APP/commit/cc35784ebf8441e36e7ac1622f025e438f7dd1f3) | 2026-10-07 | Fence player reconnection callbacks and contain background worker failures |
| [a2d59b1](https://github.com/arookiecoder-ip/MusicBox-APP/commit/a2d59b1981a8db2b6b22370334e2bf60aa4fee95) | 2026-10-07 | Fix remote phone preparation, player reconnects and landscape presentation |
| [6e609ad](https://github.com/arookiecoder-ip/MusicBox-APP/commit/6e609ad2863f2e301778a980b54fd0855cfa23da) | 2026-10-07 | Suspend profile presence refresh when app is backgrounded |
| [fe379c1](https://github.com/arookiecoder-ip/MusicBox-APP/commit/fe379c11c01ecdea3a1e69453969a70b4d296486) | 2026-10-07 | Refresh Echo presence promptly without changing playback |
| [4d04018](https://github.com/arookiecoder-ip/MusicBox-APP/commit/4d04018a697013aea4500dbcf4a75c5bd2216693) | 2026-10-07 | Use phone-style player and song shelves in tablet portrait |
| [e7e2f53](https://github.com/arookiecoder-ip/MusicBox-APP/commit/e7e2f539327673365c511657beb3cdf1c2789724) | 2026-10-07 | Cover pause acknowledgment and real buffering presentation regressions |
| [9dfd24d](https://github.com/arookiecoder-ip/MusicBox-APP/commit/9dfd24d549e22b55f74eb8ecf5dc4b969a1a33d0) | 2026-10-07 | Preserve a single navigation inset beneath the tablet dock |
| [760bb16](https://github.com/arookiecoder-ip/MusicBox-APP/commit/760bb1684d3ed00ba51ab94822d27607369bcdb6) | 2026-10-07 | Show profile as blurred-background popup and keep tablet navigation below player |
| [0246d30](https://github.com/arookiecoder-ip/MusicBox-APP/commit/0246d30eecb93f3cfd0aaf9647bd83eeca2f6540) | 2026-10-07 | Fix transport presentation, swipe intent and tablet landscape layouts |
| [053cce8](https://github.com/arookiecoder-ip/MusicBox-APP/commit/053cce8a8724d4e130bb83b9f7b7bc505f266068) | 2026-10-07 | Preserve latest transport intent across rapid repeated taps |
| [687576e](https://github.com/arookiecoder-ip/MusicBox-APP/commit/687576e01570828f42db6eff217eddbbc0773877) | 2026-10-07 | Order Alexa transport commands and resume owned phone audio without a fresh claim |
| [0c2ff32](https://github.com/arookiecoder-ip/MusicBox-APP/commit/0c2ff321d0a68b0dbb540f1b9def017bd2e112db) | 2026-10-07 | Rebuild the audio loader on reconnect and cancel source recovery during handoff |
| [a61211f](https://github.com/arookiecoder-ip/MusicBox-APP/commit/a61211f233f2b6fabd0a8c9d3ec793cf46644718) | 2026-10-07 | Restart broken stream connections on network recovery instead of waiting three minutes |
| [d91cd7d](https://github.com/arookiecoder-ip/MusicBox-APP/commit/d91cd7d24852d3d417e77115cd50c3a696740b83) | 2026-10-07 | Clear interrupted playback intent when the active queue item is removed or replaced |
| [ed7bbbb](https://github.com/arookiecoder-ip/MusicBox-APP/commit/ed7bbbb2b6e98ffc10d4279b4b6b88cf000a58e1) | 2026-10-07 | Guard stale restored leases and Android background notification restarts |
| [556d5ac](https://github.com/arookiecoder-ip/MusicBox-APP/commit/556d5ac0036d17e76957f0608e08309b6dec7e4f) | 2026-10-07 | Exercise sustained server outages in the real cache writer regression test |
| [9db587e](https://github.com/arookiecoder-ip/MusicBox-APP/commit/9db587e1986a2cefd37716b212cb6b9fe766f23c) | 2026-10-07 | Preserve outage lease intent and handle output events without a live phone service |
| [1e9e43a](https://github.com/arookiecoder-ip/MusicBox-APP/commit/1e9e43a1d4a5bfcba96fc9f1d58a73809c01ef43) | 2026-10-07 | Recover background playback and preloading after network outages |
| [02e0fa6](https://github.com/arookiecoder-ip/MusicBox-APP/commit/02e0fa63a8e6ab0c9279084660ed9c68baf57ff1) | 2026-10-07 | Retain legacy handoff compatibility and protect tablet settings from system bars |
| [ce57a3b](https://github.com/arookiecoder-ip/MusicBox-APP/commit/ce57a3b2e6401e4a98b687a62e49055d930eb79b) | 2026-10-07 | Reuse prepared audio and shared queues for fast cached handoffs |
| [aad11b7](https://github.com/arookiecoder-ip/MusicBox-APP/commit/aad11b73c7c115e4c6988b6ee51945a01f146deb) | 2026-10-07 | Fence delayed device snapshots and pause before presence removal |
| [18a5ba2](https://github.com/arookiecoder-ip/MusicBox-APP/commit/18a5ba29915b2faa0ab02bb375c2958a5776778d) | 2026-10-07 | Wake phone handoffs promptly and add remote volume with responsive tablet docks |
| [ff185e4](https://github.com/arookiecoder-ip/MusicBox-APP/commit/ff185e4ec56e5e09e7b8bcd59b23d2d0e2fdfaa8) | 2026-10-07 | Retain the latest mobile output selection during an active handoff |
| [89ca64d](https://github.com/arookiecoder-ip/MusicBox-APP/commit/89ca64d8c7dd6a7f6b30fc499e2910019a668820) | 2026-10-07 | Reject preparing phone leases until source pause is acknowledged |
| [b225308](https://github.com/arookiecoder-ip/MusicBox-APP/commit/b225308da236087b5f2a8f158411cbac1941933a) | 2026-10-07 | Keep downloads local and route remote queue tools to their playback owner |
| [a2efb75](https://github.com/arookiecoder-ip/MusicBox-APP/commit/a2efb75b591cb3ce8cc34f5e3d1eed59fc708016) | 2026-10-07 | Avoid duplicate remote phone polling and unchanged queue downloads |
| [efd425d](https://github.com/arookiecoder-ip/MusicBox-APP/commit/efd425de3446cd2aa062065e7cfe77d80c692b4b) | 2026-10-07 | Publish focused device handoff test reports from CI |
| [58572f0](https://github.com/arookiecoder-ip/MusicBox-APP/commit/58572f0e8bba91894f45b6749fdca6dea16df56d) | 2026-10-07 | Respect the active phone after offline playback reconnects |
| [236fd21](https://github.com/arookiecoder-ip/MusicBox-APP/commit/236fd21191a61aeadfdb04a477a19325d1168b63) | 2026-10-07 | Keep complete local playlists through shared queue publication failures |
| [6c9f4ba](https://github.com/arookiecoder-ip/MusicBox-APP/commit/6c9f4ba7a33e7966b1df1e7b196f9fd0ef63ebd0) | 2026-10-07 | Carry exact paused cursor across mobile handoff acknowledgements |
| [d1eaea5](https://github.com/arookiecoder-ip/MusicBox-APP/commit/d1eaea5e30951768fb89103d5323fb90c630b18a) | 2026-10-07 | Integrate current Alexa feature branch for signed CI build |
| [d967888](https://github.com/arookiecoder-ip/MusicBox-APP/commit/d967888f780c9d83ef8b5d85dd7474a944c29408) | 2026-10-07 | Keep device identities unique across backups and support phone-only accounts |
| [66757b9](https://github.com/arookiecoder-ip/MusicBox-APP/commit/66757b9a960fcdad650daf3e784e470000b79141) | 2026-10-07 | Fix remote radio routing and fence stale presence close requests |
| [2850b84](https://github.com/arookiecoder-ip/MusicBox-APP/commit/2850b840a8b123883887de18ee2e322aa7c5e442) | 2026-10-07 | Validate device protocol independently of signing cache |
| [befd19f](https://github.com/arookiecoder-ip/MusicBox-APP/commit/befd19fce8583b3a9b22757e98951efc245dfd8e) | 2026-10-07 | Fix playlist sync and add exclusive online mobile device handoffs |
| [9b66ef3](https://github.com/arookiecoder-ip/MusicBox-APP/commit/9b66ef3f23020b6239c8815e65bdbf5ede1ab52e) | 2026-10-06 | Drop dead JetBrains-21 daemon toolchain pin |
| [bc73920](https://github.com/arookiecoder-ip/MusicBox-APP/commit/bc739203a3d5d1b1071e9a68c76efafc0c36e385) | 2026-10-07 | Merge pull request #12 from arookiecoder-ip/fix/silent-playback-watchdog |
| [eac5c16](https://github.com/arookiecoder-ip/MusicBox-APP/commit/eac5c1698efb0f2956d553e934107e79ad6339c9) | 2026-10-06 | Fix unsigned waveform byte conversion in silence detector |
| [2332b80](https://github.com/arookiecoder-ip/MusicBox-APP/commit/2332b805616a45d67867e8e8ad83bb234a4d52a1) | 2026-10-06 | Fix Byte literal type in silence test |
| [b1253f4](https://github.com/arookiecoder-ip/MusicBox-APP/commit/b1253f445e4156bc6e8011358fd5136749f87f18) | 2026-10-06 | Fix watchdog player type: audioSessionId needs ExoPlayer |
| [0ad79d9](https://github.com/arookiecoder-ip/MusicBox-APP/commit/0ad79d9130af7d6b781766ff6e2c3c776a491584) | 2026-10-06 | Detect silent-but-advancing phone playback and recover in place |
| [d0adba1](https://github.com/arookiecoder-ip/MusicBox-APP/commit/d0adba1e3190b50b855ef578f4bb675d69da08af) | 2026-10-06 | Merge pull request #10 from arookiecoder-ip/fix/fast-playlist-start |
| [cc6ba9d](https://github.com/arookiecoder-ip/MusicBox-APP/commit/cc6ba9d31ff89f24fa16f5f769842f336a5cd78c) | 2026-10-06 | Merge fast playlist starts + background toast fixes |
| [581e380](https://github.com/arookiecoder-ip/MusicBox-APP/commit/581e3803dbed43a716f586b74efaad6d9d355678) | 2026-10-06 | Fix shuffled backfill to include songs before the playing one |
| [02135c6](https://github.com/arookiecoder-ip/MusicBox-APP/commit/02135c68f88d5207572ac9df7fec977a1f2dd076) | 2026-10-06 | Fix nullable-type assertions in playlist remainder tests |
| [c08c5d3](https://github.com/arookiecoder-ip/MusicBox-APP/commit/c08c5d37d78494834d0d3239dff6cc38d22868ac) | 2026-10-06 | Remove duplicated backfill helpers left by a bad edit |
| [311e2db](https://github.com/arookiecoder-ip/MusicBox-APP/commit/311e2db4801892e8a25aa735d455c7215a564761) | 2026-10-06 | Fast playlist starts: play first, backfill the rest in chunks |
| [ce652ee](https://github.com/arookiecoder-ip/MusicBox-APP/commit/ce652ee04290cc7ff8db8f698b885b94ca56bb45) | 2026-10-06 | Suppress false offline toasts while the app is backgrounded |
| [cd75ac3](https://github.com/arookiecoder-ip/MusicBox-APP/commit/cd75ac304af0b8c9e0119167ebed4bd56a1e1c43) | 2026-10-06 | Merge pull request #9 from arookiecoder-ip/feat/alexa-backend |
| [88c32ca](https://github.com/arookiecoder-ip/MusicBox-APP/commit/88c32cae4514edc45ea21fb241dd17b03efa573b) | 2026-10-06 | Rewrite README from scratch for Music Box v2.4.0 |
| [59b705f](https://github.com/arookiecoder-ip/MusicBox-APP/commit/59b705fe5136b79983387a9379802ec581266ad6) | 2026-10-06 | Update planning structure map after architecture doc cleanup |
| [08d4d46](https://github.com/arookiecoder-ip/MusicBox-APP/commit/08d4d46b7130ce72ade07f8255d39f478f85a6ef) | 2026-10-06 | Remove obsolete architecture docs, IDE config and worker source |
| [9177dc1](https://github.com/arookiecoder-ip/MusicBox-APP/commit/9177dc16a61ad789c4febbf9b72b8ce53bf0a6ae) | 2026-10-06 | Remove unreferenced clutter from feat/alexa-backend |
| [b15333c](https://github.com/arookiecoder-ip/MusicBox-APP/commit/b15333cf8caa59b133e50bc74ac0335c00f15898) | 2026-10-06 | Point remaining README links at MusicBox-APP v2.4.0 |
| [8c1db90](https://github.com/arookiecoder-ip/MusicBox-APP/commit/8c1db904d4ed626ce0008c5a561ad6b63da6da41) | 2026-10-06 | Merge pull request #7 from arookiecoder-ip/feat/alexa-backend |
| [2fed518](https://github.com/arookiecoder-ip/MusicBox-APP/commit/2fed518199e1eb735f6d44e09cabf7abaf038e3e) | 2026-10-06 | Add one-off encrypted signing-key export workflow |
| [8edc100](https://github.com/arookiecoder-ip/MusicBox-APP/commit/8edc1000c361fd1ced926ecc3e9fa0d75494e43b) | 2026-10-06 | Merge pull request #6 from arookiecoder-ip/feat/alexa-backend |
| [d13ef25](https://github.com/arookiecoder-ip/MusicBox-APP/commit/d13ef252aa287d3cf9788cd65e25409a22320e7f) | 2026-10-06 | Parse SDK 37 APK signer labels without weakening certificate pin |
| [a60040e](https://github.com/arookiecoder-ip/MusicBox-APP/commit/a60040e86a1eff6b123fd140d84b08c99e1d6d97) | 2026-10-06 | Merge pull request #5 from arookiecoder-ip/feat/alexa-backend |
| [fff5638](https://github.com/arookiecoder-ip/MusicBox-APP/commit/fff56381e6963b529bdffcb62c40ff6f3f424b3d) | 2026-10-06 | Finalize Music Box 2.4.0 stable release docs and drop committed APK |
| [d2a41fd](https://github.com/arookiecoder-ip/MusicBox-APP/commit/d2a41fdd9cb13ff92d9b521e2479110656722272) | 2026-10-06 | Import File explicitly to avoid Gradle java extension shadowing |
| [8a7c977](https://github.com/arookiecoder-ip/MusicBox-APP/commit/8a7c9774df894b3313305cd4c51b99928371cce2) | 2026-10-06 | Fix release lint icon aliases and configuration-cache-safe signing validation |
| [d7db3bd](https://github.com/arookiecoder-ip/MusicBox-APP/commit/d7db3bdaa1a16b1177884eeb02655492197a944c) | 2026-10-06 | Merge main history while retaining the current Music Box branding |
| [f112e1f](https://github.com/arookiecoder-ip/MusicBox-APP/commit/f112e1f87ef9d236c3f7898ca79d2c0fe6415d04) | 2026-10-06 | Use block-bodied version parser for invalid-version early return |
| [612997c](https://github.com/arookiecoder-ip/MusicBox-APP/commit/612997cf12a17d5cfbe5043741ab1f0e4009e87a) | 2026-10-06 | Prepare signed stable 2.4.0 release and correct update channel |
| [a5890ff](https://github.com/arookiecoder-ip/MusicBox-APP/commit/a5890ff87b74ec585b58371f2fb49a841eb42aa1) | 2026-10-06 | Preserve real phone play intent and avoid duplicate Alexa handoff pause |
| [3171bb1](https://github.com/arookiecoder-ip/MusicBox-APP/commit/3171bb1cc971c1280119cd2ad53dd75756b3ffab) | 2026-10-06 | Start background audio preloading only after the selected track is ready |
| [3ee682c](https://github.com/arookiecoder-ip/MusicBox-APP/commit/3ee682c023c529e9e74fd7b8bcd45817119e474e) | 2026-10-06 | Verify download persistence and transaction rollback with focused JVM storage tests |
| [c2f3dce](https://github.com/arookiecoder-ip/MusicBox-APP/commit/c2f3dce407d0d96c34957b8014c8d116952d7634) | 2026-10-06 | Supply duration in duplicate queue regression fixture |
| [7b4ba31](https://github.com/arookiecoder-ip/MusicBox-APP/commit/7b4ba314557d5e5ac25fbbc32ca5c201816af5b0) | 2026-10-06 | Validate queue revisions and adapt preloads to network and lifecycle changes |
| [78a900a](https://github.com/arookiecoder-ip/MusicBox-APP/commit/78a900a5a4eee197638b1d1b9a806ee0700a26cc) | 2026-10-06 | Separate playback controller and lease sync, fence stale polls and reduce queue payloads |
| [fbf158f](https://github.com/arookiecoder-ip/MusicBox-APP/commit/fbf158fddb071b1ae6a3ac7aeae63057877158d9) | 2026-10-06 | Preserve queue identities across handoffs and share verified HD artwork |
| [07dacde](https://github.com/arookiecoder-ip/MusicBox-APP/commit/07dacde65b8e6baef83383eb739308f5ed0441ba) | 2026-10-06 | Centralize remote playback sync, persist downloads in Room and scope audio credentials |
| [cdde2eb](https://github.com/arookiecoder-ip/MusicBox-APP/commit/cdde2eb44011337217d6e30d08dc5afcdfd81604) | 2026-10-06 | Fix queue rollback coroutine import and reset row state after duplicate song deletion |
| [b52cd85](https://github.com/arookiecoder-ip/MusicBox-APP/commit/b52cd85edfaabbc322ae4e3a7b949d7bb1c0eb6c) | 2026-10-06 | Start radio from artist songs, animate optimistic queue removal, and refine profile and Jam states |
| [9f533c0](https://github.com/arookiecoder-ip/MusicBox-APP/commit/9f533c0dfcbf56b932c4412f7e13d9928ec54ff9) | 2026-10-06 | Opt into Compose stable system-bar inset APIs |
| [f0d4f6a](https://github.com/arookiecoder-ip/MusicBox-APP/commit/f0d4f6afded246905d2a3cd2735e636536858689) | 2026-10-06 | Clear dismissed remote session metadata and derive HD notification art from song identity |
| [9d09f9e](https://github.com/arookiecoder-ip/MusicBox-APP/commit/9d09f9ed44861deb890210fdcc77f9b47d848062) | 2026-10-06 | Fix stable share insets, queue dismissal, artwork recovery and playback restoration after task removal |
| [5838e78](https://github.com/arookiecoder-ip/MusicBox-APP/commit/5838e7893bd893b2a3d9ee9880fd9abf0b829264) | 2026-10-06 | Renew phone leases during target preparation without publishing stale handoff cursors |
| [2cfcb4b](https://github.com/arookiecoder-ip/MusicBox-APP/commit/2cfcb4b1397194ebb24287dd5c1ff6893bdb6db1) | 2026-10-06 | Apply the shared grey modal surface to playlist and artist pickers |
| [bfa8642](https://github.com/arookiecoder-ip/MusicBox-APP/commit/bfa864284474ca7b112c667c8e295ddb95d34057) | 2026-10-06 | Guard in-flight phone reports during repeated output transfers and test lease transitions |
| [e1b2d78](https://github.com/arookiecoder-ip/MusicBox-APP/commit/e1b2d78da48de7c6cd339af3de3c40b0dba3b3e1) | 2026-10-06 | Retry failed HD artwork and fix playback lifecycle and handoff reporting; refine Settings modals |
| [391e2ea](https://github.com/arookiecoder-ip/MusicBox-APP/commit/391e2eaac3f92d9a5998d2e355c46f1fc527fc66) | 2026-10-06 | Focus Search when its navigation button closes a detail page |
| [ca96b1e](https://github.com/arookiecoder-ip/MusicBox-APP/commit/ca96b1e373ff2c1fd9993a2e4ceebd490c579b47) | 2026-10-06 | Keep dragged queue row positioned during layout and include cookie probe checks |
| [2e158b5](https://github.com/arookiecoder-ip/MusicBox-APP/commit/2e158b56fd4e46056c03471c373e179cb14cff67) | 2026-10-06 | Fix queue drag placement, compact action sheets and reorganize settings and search focus |
| [39a1a6c](https://github.com/arookiecoder-ip/MusicBox-APP/commit/39a1a6c7a7af8552636d2195557424ee7a28da7f) | 2026-10-06 | Merge playlist JSON pages with explicit list insertion |
| [77aaa29](https://github.com/arookiecoder-ip/MusicBox-APP/commit/77aaa29bf4c69b3759f27aef0fb41d31c4a4a040) | 2026-10-06 | Preserve pause and native auto-advance during late queue continuation |
| [5bcf550](https://github.com/arookiecoder-ip/MusicBox-APP/commit/5bcf5502580bcf30e6a17f9cbb2de5f9648f0641) | 2026-10-06 | Fix complete playlist loading and native queue continuation; reconcile queue conflicts |
| [0e4b91b](https://github.com/arookiecoder-ip/MusicBox-APP/commit/0e4b91b965420ab0d38ae09a7aa3a7d83a14501f) | 2026-10-06 | Identify offline download traffic to preserve foreground playback budget |
| [75e658d](https://github.com/arookiecoder-ip/MusicBox-APP/commit/75e658da46289036e83d5b86a18675e0f6afa4df) | 2026-10-06 | Import resolved collection model in playback view model |
| [a98a77b](https://github.com/arookiecoder-ip/MusicBox-APP/commit/a98a77bdd6726bf55d057da90abe25a77a8b299c) | 2026-10-06 | Respect audio rate-limit cooldowns and retry downloads without a request storm |
| [ca8102e](https://github.com/arookiecoder-ip/MusicBox-APP/commit/ca8102ecf9d48629394a67854069330c4f8f9db9) | 2026-10-06 | Use the PWA bar logo for native and Alexa notification icons |
| [6c1809d](https://github.com/arookiecoder-ip/MusicBox-APP/commit/6c1809d6b1ce118ca463ffcc55131fc8f0046ab0) | 2026-10-06 | Allow Library grid to fit two columns on phone screens |
| [0e0d8ab](https://github.com/arookiecoder-ip/MusicBox-APP/commit/0e0d8ab4562ea26d6ce2eafcaa432e288a65176f) | 2026-10-06 | Fix download metadata, Library controls and icons; streamline playlist and offline playback |
| [42b473a](https://github.com/arookiecoder-ip/MusicBox-APP/commit/42b473aa2d31c65e3bd5f70626931e279b08c2e3) | 2026-10-06 | Add task-only handoff checks without emulator testing |
| [4bff626](https://github.com/arookiecoder-ip/MusicBox-APP/commit/4bff6267d13d96adf7edf5d915297518f3a8aa44) | 2026-10-06 | Preserve handoff failures across bounded destination waits |
| [6a6f882](https://github.com/arookiecoder-ip/MusicBox-APP/commit/6a6f882eaceeff7803a8a177a5401737f86a7df7) | 2026-10-06 | Bound output handoff and recovery waits so silent Echo cannot lock controls |
| [d90000f](https://github.com/arookiecoder-ip/MusicBox-APP/commit/d90000fd79221130622617e048b31dd678baf647) | 2026-10-06 | Acknowledge actual phone pause directly in Alexa handoff |
| [8179aa3](https://github.com/arookiecoder-ip/MusicBox-APP/commit/8179aa356976662c99657427559e3faaa1cda2ef) | 2026-10-06 | Keep Echo-to-Echo transfers independent of phone ownership |
| [f86913d](https://github.com/arookiecoder-ip/MusicBox-APP/commit/f86913d4ac0dd57995aa595b202916a40275c346) | 2026-10-06 | Guard handoffs against newer phone plays and isolate transport test ownership |
| [70bfe78](https://github.com/arookiecoder-ip/MusicBox-APP/commit/70bfe78020ddca40081d304472756900b5f6aa36) | 2026-10-05 | Resume phone-to-Alexa handoffs through the working web transport path |
| [0369975](https://github.com/arookiecoder-ip/MusicBox-APP/commit/0369975ae24cf40e29ba9fab4210367b67abba94) | 2026-10-05 | Publish phone song and cursor before waiting for radio recommendations |
| [1da2608](https://github.com/arookiecoder-ip/MusicBox-APP/commit/1da260856fa5fd58cc62690dce08ed44e6154e5f) | 2026-10-05 | Block prepared and delayed phone starts after output ownership changes |
| [e87cdd6](https://github.com/arookiecoder-ip/MusicBox-APP/commit/e87cdd60e5ca9dfbde40e687c7bcd2f0550093e5) | 2026-10-05 | Enforce phone lease deadlines independently of stalled network requests |
| [e61d771](https://github.com/arookiecoder-ip/MusicBox-APP/commit/e61d7715e2172e9c440de207b7767c3cec4e279e) | 2026-10-05 | Use the shared Echo snapshot parser during background handoff |
| [954f38e](https://github.com/arookiecoder-ip/MusicBox-APP/commit/954f38ea92f78cb5e1c3b2285088a5f3dda98e17) | 2026-10-05 | Coordinate shared phone and Alexa queue ownership and handoffs |
| [d467ab0](https://github.com/arookiecoder-ip/MusicBox-APP/commit/d467ab021d81d72a1ddafd4534bded90532a298b) | 2026-10-05 | Use production backend for feature APK audio and queues |
| [432be14](https://github.com/arookiecoder-ip/MusicBox-APP/commit/432be142eddf88cd5ef85b565628142e9bf9cc62) | 2026-10-05 | Recover phone queue cursor after remote reorder or restart |
| [76fa1c2](https://github.com/arookiecoder-ip/MusicBox-APP/commit/76fa1c2131b451b26931037e2c2a8bd493d3f0fb) | 2026-10-05 | Install and read phone queues atomically on the audio backend |
| [84504d5](https://github.com/arookiecoder-ip/MusicBox-APP/commit/84504d5405f4b9172134f29456aa063291037542) | 2026-10-05 | Authenticate phone queue writes against the audio backend |
| [a706e87](https://github.com/arookiecoder-ip/MusicBox-APP/commit/a706e87dbce64c785fa161fc18d9bb50b4c380e3) | 2026-10-05 | Expose rejected queue endpoint and HTTP status safely |
| [d0d5da2](https://github.com/arookiecoder-ip/MusicBox-APP/commit/d0d5da260d84bf2e0384ac0f94c548d225732e14) | 2026-10-05 | Keep valid login sessions when queue sync rejects access |
| [50e1023](https://github.com/arookiecoder-ip/MusicBox-APP/commit/50e1023a8347ba9ee1045f9b90a67df31dd162fd) | 2026-10-05 | Fix shared queue sync retries before phone queue is ready |
| [b36dd1f](https://github.com/arookiecoder-ip/MusicBox-APP/commit/b36dd1f8fe889985633aa306ed3160ed0b707ddb) | 2026-10-05 | Exercise the actual queue drag handle at device-scaled distances |
| [9f93af2](https://github.com/arookiecoder-ip/MusicBox-APP/commit/9f93af23a448b0d377f6c4693075828540b016e6) | 2026-10-05 | Discard queue drags invalidated by concurrent queue updates |
| [556f077](https://github.com/arookiecoder-ip/MusicBox-APP/commit/556f077b42f629a6b13118ac33d8ff700d038262) | 2026-10-05 | Close obsolete preload connections and avoid repeated queue error toasts |
| [3f13c1d](https://github.com/arookiecoder-ip/MusicBox-APP/commit/3f13c1d90baef63cc669196360c5aa5d357fb867) | 2026-10-05 | Reuse queue row identities and animate only neighboring rows during dragging |
| [9266df6](https://github.com/arookiecoder-ip/MusicBox-APP/commit/9266df62cbede6ecba37732f9c62d76cb3db5fce) | 2026-10-05 | Move Echo queue rows optimistically while preserving the current occurrence |
| [faba49b](https://github.com/arookiecoder-ip/MusicBox-APP/commit/faba49b16db069c00e77ce6855a258a8a79d3c9c) | 2026-10-05 | Bound preloads to five queue positions and respect repeat state |
| [131e928](https://github.com/arookiecoder-ip/MusicBox-APP/commit/131e9282891232b33796e2d80f9794c6c0e31eb7) | 2026-10-05 | Warm the next five streams and recover queues search songs and download controls |
| [e146ed0](https://github.com/arookiecoder-ip/MusicBox-APP/commit/e146ed0bba1b1bd7ad1c25b5819605f8c5514a6c) | 2026-10-05 | Keep downloaded collection routes usable when the device is offline |
| [e24701b](https://github.com/arookiecoder-ip/MusicBox-APP/commit/e24701b45fc247248a2fd035f8e91f30e4e3595e) | 2026-10-05 | Preserve public playlist share IDs for downloaded collections |
| [e28af62](https://github.com/arookiecoder-ip/MusicBox-APP/commit/e28af6283218938fde152ad88391ac5331a2a7dd) | 2026-10-05 | Unify downloaded collection pages and close prior UI and offline action gaps |
| [9991cda](https://github.com/arookiecoder-ip/MusicBox-APP/commit/9991cda0816d15a87c64c39caa57a3b0b0915e10) | 2026-10-05 | Animate artwork when replacing a queue as well as skipping songs |
| [79b60fb](https://github.com/arookiecoder-ip/MusicBox-APP/commit/79b60fbc4ef2e0325d8c455ccbcfc24b138dddd8) | 2026-10-05 | Fix responsive layout scope and navigation modifier |
| [e224aab](https://github.com/arookiecoder-ip/MusicBox-APP/commit/e224aaba26e649a75916cf20c09c2d5e1c28f4ec) | 2026-10-05 | Adapt tablet player layouts and fix Alexa notifications, lyrics and Library controls |
| [a85f6f5](https://github.com/arookiecoder-ip/MusicBox-APP/commit/a85f6f54268b7be6c5d2e0b9a1f3cc035a2e9176) | 2026-10-05 | Recover phone queue sync after interrupted handoffs |
| [ad84095](https://github.com/arookiecoder-ip/MusicBox-APP/commit/ad84095d9cacfad3af8660e167347581ee3fb5c3) | 2026-10-05 | Show active downloads and load artist top songs from account browse data |
| [0d1809f](https://github.com/arookiecoder-ip/MusicBox-APP/commit/0d1809f2c3211120f1b7cf409aa5f012695df9cf) | 2026-10-05 | Serialize phone queue updates and validate download notifications |
| [97a2341](https://github.com/arookiecoder-ip/MusicBox-APP/commit/97a234165dbb1d5f1a1736193e28e3211259edae) | 2026-10-05 | Fix shared playback queues and refresh downloads, Library and search interactions |
| [5dd559c](https://github.com/arookiecoder-ip/MusicBox-APP/commit/5dd559cf432ae825a4e5d22cceb010d6b50455a4) | 2026-10-05 | Authenticate configured audio routes and exercise real phone playback service |
| [c6e4436](https://github.com/arookiecoder-ip/MusicBox-APP/commit/c6e443656a4252311f5601ab2c52e94c0bce60be) | 2026-10-05 | Run native playback checks with emulator runner line execution |
| [00e2c99](https://github.com/arookiecoder-ip/MusicBox-APP/commit/00e2c99fab7efce0fdc0da020e3e190d3b8240d8) | 2026-10-05 | Exercise authenticated online audio through the actual Android player |
| [4d68f9c](https://github.com/arookiecoder-ip/MusicBox-APP/commit/4d68f9cbc9e206d72542a8182dcd335b4c36a7d6) | 2026-10-05 | Bypass legacy stream lookup for normal device playback |
| [2f2dad7](https://github.com/arookiecoder-ip/MusicBox-APP/commit/2f2dad7a2fe3b5238b19e9536c8568b3d0f15073) | 2026-10-05 | Make online phone audio seekable and allow cold-cache preparation |
| [99bb720](https://github.com/arookiecoder-ip/MusicBox-APP/commit/99bb72082c812675e6bc543885b8cf485ac10780) | 2026-10-05 | Order rapid output requests and avoid stale queue writes during handoff |
| [1e4bffd](https://github.com/arookiecoder-ip/MusicBox-APP/commit/1e4bffd80a01d477bdedd9a0e2024b2bbccc9455) | 2026-10-05 | Stabilize output handoff, improve artwork and startup, and add collection downloads |
| [da05a89](https://github.com/arookiecoder-ip/MusicBox-APP/commit/da05a89eefad68161cc29918119a569e4f622181) | 2026-10-05 | Unblock mobile cookie actions and preserve pasted export formatting |
| [e90e1b7](https://github.com/arookiecoder-ip/MusicBox-APP/commit/e90e1b7b09bfab8ea763ec4413c833d369fea78e) | 2026-10-05 | Cover native cookie replacement states with Compose UI tests |
| [dbf0166](https://github.com/arookiecoder-ip/MusicBox-APP/commit/dbf0166d33af6d130f5e805eed0f4e8b8d571f3c) | 2026-10-05 | Add native audio cookie validation and replacement |
| [9f07871](https://github.com/arookiecoder-ip/MusicBox-APP/commit/9f078713b4b1756f2f9d548c6091bea1ae70c91f) | 2026-10-05 | Add account song downloads and recover device playback with retained artist paging |
| [d62a77a](https://github.com/arookiecoder-ip/MusicBox-APP/commit/d62a77aeeb0d7c37547a746e73df051ee951dc14) | 2026-10-05 | Import alignment for the shared offline back control |
| [123cf86](https://github.com/arookiecoder-ip/MusicBox-APP/commit/123cf86662d6641ac0600a30c2940b337b65c350) | 2026-10-05 | Refine discovery shelves, retain release navigation and unify offline presentation |
| [05ca0c6](https://github.com/arookiecoder-ip/MusicBox-APP/commit/05ca0c6c2ba61d3103f30d479e8deb48e2b555a9) | 2026-10-05 | Prefer full resolution YouTube video artwork with existing source fallback |
| [b60a1fe](https://github.com/arookiecoder-ip/MusicBox-APP/commit/b60a1fe6dc3ec8a01c46ce66bfb99b06c53d7fb9) | 2026-10-05 | Synchronize playing identity and artwork; cache discovery names and refine loading and cards |
| [c1cb022](https://github.com/arookiecoder-ip/MusicBox-APP/commit/c1cb0220ee3b9a7cdc33362b28ff27bbec858db9) | 2026-10-05 | Expose the Echo queue timeline so Android next and previous controls work |
| [42273aa](https://github.com/arookiecoder-ip/MusicBox-APP/commit/42273aadc5adc3103fb0807fe50ffc20b752d33f) | 2026-10-05 | Keep remote notifications on the selected output and reset discovery through Search tab |
| [0452b8c](https://github.com/arookiecoder-ip/MusicBox-APP/commit/0452b8c41bfee177d6fb051a71efc2b902fd3857) | 2026-10-05 | Fix collection routes and discovery layouts; add Alexa media controls and device history reporting |
| [51524d0](https://github.com/arookiecoder-ip/MusicBox-APP/commit/51524d08fa2531bf3b611b5296e81a6559793e8e) | 2026-10-05 | Pin APK signing certificate for future update compatibility |
| [5af7a28](https://github.com/arookiecoder-ip/MusicBox-APP/commit/5af7a2875b0bfb9b374e7e3d775ae7ed711fddd0) | 2026-10-05 | Fix device playlist startup, stable update signing and library/player actions |
| [08414c1](https://github.com/arookiecoder-ip/MusicBox-APP/commit/08414c102dd92741b5d11c0d125b9cdf0ab52b2d) | 2026-10-05 | Correct swipe actions and add PWA branding, discovery and paged artist browsing |
| [80a4511](https://github.com/arookiecoder-ip/MusicBox-APP/commit/80a4511237a7750deba9da03c6dcb9c328f0d088) | 2026-10-05 | Apply collection play overlay to the search top result |
| [fd89b40](https://github.com/arookiecoder-ip/MusicBox-APP/commit/fd89b407fd55a0b11ca8e16e706cdf5a7a64c78b) | 2026-10-05 | Show playlist operation feedback on every page and keep retry visible |
| [111ece0](https://github.com/arookiecoder-ip/MusicBox-APP/commit/111ece02ab33deae81488685ce1b8d0d4c98be09) | 2026-10-05 | Anchor queue and refine controls, collection actions and network feedback |
| [86f3129](https://github.com/arookiecoder-ip/MusicBox-APP/commit/86f3129a77fa638b0b13dae62b3078f6e2c88333) | 2026-10-05 | Redesign account library with collection filters and list/grid views |
| [c930ea2](https://github.com/arookiecoder-ip/MusicBox-APP/commit/c930ea2b5c82244cde26baa2d7ce47ee9a59ffcb) | 2026-10-05 | Fix artist songs import and detail skeleton arguments |
| [27870ba](https://github.com/arookiecoder-ip/MusicBox-APP/commit/27870ba7c7363744c3dfb7593262feb5f9561d42) | 2026-10-05 | Bleed artwork into status bar and refine player, web queue and artist pages |
| [63209c9](https://github.com/arookiecoder-ip/MusicBox-APP/commit/63209c9c358576cf43ba3b0a2a7b4f6d6fba1abb) | 2026-10-05 | Capture expanded menu content for visual verification |
| [42beb80](https://github.com/arookiecoder-ip/MusicBox-APP/commit/42beb805a97b29f36857708dde42c7bc78f2a213) | 2026-10-05 | Match web song layouts and refine player, menus, collections and loading |
| [3975683](https://github.com/arookiecoder-ip/MusicBox-APP/commit/39756837807a74b8dbdce1422b8180e07fd926db) | 2026-10-05 | Measure unclipped artwork and slider semantics in UI checks |
| [bc249ed](https://github.com/arookiecoder-ip/MusicBox-APP/commit/bc249ed364ee9acabc0c9a16c72d11ceac6e9c16) | 2026-10-05 | Keep UI checks running and preserve full lint findings |
| [7fb43a3](https://github.com/arookiecoder-ip/MusicBox-APP/commit/7fb43a3830def200a3b3951f2289a36fbf538677) | 2026-10-05 | Fix player artwork height scope |
| [86a49ad](https://github.com/arookiecoder-ip/MusicBox-APP/commit/86a49ad0860b64ca5fd0a16d7d48b292b0e939c9) | 2026-10-05 | Refine Home spacing and player controls with web-style sliders |
| [e0a3fe1](https://github.com/arookiecoder-ip/MusicBox-APP/commit/e0a3fe138b53ee47c95389288aad6afda4dc60d2) | 2026-10-05 | Refine Music Box player menus queue and orange branding |
| [04282e1](https://github.com/arookiecoder-ip/MusicBox-APP/commit/04282e120971ff4e45ac4c972716b33e856389c5) | 2026-10-05 | Dock flat navigation and player and refine Home cards and media menus |
| [e3c83f9](https://github.com/arookiecoder-ip/MusicBox-APP/commit/e3c83f9423bdbf3917b05a04428b8a07f924a639) | 2026-10-05 | Fix UI compilation and load complete artist song lists |
| [bc0e7db](https://github.com/arookiecoder-ip/MusicBox-APP/commit/bc0e7db42e0e73e87d19638b4ecccac2150c766d) | 2026-10-05 | Restore audio settings and improve search artist and playback interactions |
| [90ea9e7](https://github.com/arookiecoder-ip/MusicBox-APP/commit/90ea9e769c76b443de7f9330b4d0e75340d1fcdd) | 2026-10-05 | Route account login to the enabled web-session server |
| [f23c9a3](https://github.com/arookiecoder-ip/MusicBox-APP/commit/f23c9a3551133996e45b89ca19704dbc8ef00b6a) | 2026-10-05 | Integrate account-backed library and Echo playback controls |
| [48c17b1](https://github.com/arookiecoder-ip/MusicBox-APP/commit/48c17b179c2122b47ca392d7472d69d5a7d36419) | 2026-10-04 | Complete native account login polling and callback handling |
| [97228ed](https://github.com/arookiecoder-ip/MusicBox-APP/commit/97228edbf04746ae228bd0b2b9c1a3e5429ab700) | 2026-10-04 | Support native staging configuration and scope credentials to each server |
| [16c8e5a](https://github.com/arookiecoder-ip/MusicBox-APP/commit/16c8e5a06d99325be04fb19180064e6def6e7ab4) | 2026-10-04 | Harden remote session lifecycle and complete native browsing details |
| [d7110fc](https://github.com/arookiecoder-ip/MusicBox-APP/commit/d7110fcb736e1726d3a95bfde2840ece0b9cffe7) | 2026-10-04 | Fix remote navigation and preserve collection playback context |
| [b79883a](https://github.com/arookiecoder-ip/MusicBox-APP/commit/b79883afb2487e6729fa4e0058b638d22342529a) | 2026-10-04 | Rebuild Alexa web controls and library as native Android screens |
| [72c3254](https://github.com/arookiecoder-ip/MusicBox-APP/commit/72c32546be6852310c1a4b0aa45d38870d27910c) | 2026-10-04 | Merge beta into feat/alexa-backend, preserving both backend integrations |
| [a5b9606](https://github.com/arookiecoder-ip/MusicBox-APP/commit/a5b9606e191d1dc8e34fcdd830c167f0ccf26e3d) | 2026-10-04 | fix: fetch radio without queue replacement and report phone playback |
| [b46de71](https://github.com/arookiecoder-ip/MusicBox-APP/commit/b46de71f834c072ae84c427cc808c4115566bc16) | 2026-10-04 | fix: align Alexa search, home shelves, and shared queue with backend |
| [b2109a7](https://github.com/arookiecoder-ip/MusicBox-APP/commit/b2109a79cbcab4ecbc93bb6d0fffd6dab87cdd04) | 2026-10-04 | Merge pull request #13 from rajeet-04/feat/alexa-backend-source |
| [1367e80](https://github.com/arookiecoder-ip/MusicBox-APP/commit/1367e80f88a2ca0290838ce9b79ffcb800fc8525) | 2026-10-04 | Merge pull request #12 from rajeet-04/perf/battery |
| [e9d53e9](https://github.com/arookiecoder-ip/MusicBox-APP/commit/e9d53e9f73cc8eeb8a16d8ed9495065ea2b1812e) | 2026-10-04 | Backend source: only accept a search hit that is the same song |
| [4ab8451](https://github.com/arookiecoder-ip/MusicBox-APP/commit/4ab8451ae8fb219d13d9be39f0eb7a2e32ad7453) | 2026-10-04 | Fix playlist imports: songs dropping out of playlists and imports never resuming |
| [8d5e3d3](https://github.com/arookiecoder-ip/MusicBox-APP/commit/8d5e3d392c5679d0dec43fe6d441f4828b53fdef) | 2026-10-04 | Use the Alexa skill backend as the first audio source |
| [17cd03f](https://github.com/arookiecoder-ip/MusicBox-APP/commit/17cd03f2d62a16a45701b8ea8d436112f22753f3) | 2026-10-04 | fix: move BackendTrack.toAppTrack to top level (object-member extensions are not visible to callers) |
| [1ce4a85](https://github.com/arookiecoder-ip/MusicBox-APP/commit/1ce4a855b48900540fb422695f90a08d263c1be6) | 2026-10-04 | Romanized lyrics: never save a partial result; redo saved results with non-Latin lines; retry failed lines |
| [75b1f5b](https://github.com/arookiecoder-ip/MusicBox-APP/commit/75b1f5bcdce6a58f49db50839cba0bc0aeef3935) | 2026-10-04 | feat: add Alexa backend as second music source behind Settings toggle |
| [1bbec4d](https://github.com/arookiecoder-ip/MusicBox-APP/commit/1bbec4d36e14b91874c96f42905a150d995459c3) | 2026-10-04 | Hold Wi-Fi/CPU locks while playing so screen-off streams don't stall |
| [ad6c34a](https://github.com/arookiecoder-ip/MusicBox-APP/commit/ad6c34a799fa7051707d2906a0a093ade029d49d) | 2026-10-04 | Battery and playback efficiency: adaptive service tick, allocation-free audio processors, opt-in offload playback, leaner R8 rules |
| [ad28116](https://github.com/arookiecoder-ip/MusicBox-APP/commit/ad28116fec45f2e3867396db59964a88c16e95d6) | 2026-10-04 | Merge pull request #11 from rajeet-04/fix/short-previews |
| [1044690](https://github.com/arookiecoder-ip/MusicBox-APP/commit/1044690694b263632e95b5368dac537db022e00f) | 2026-10-04 | Release notes: preview and wrong-length fix in 2.3.5-beta |
| [fda405c](https://github.com/arookiecoder-ip/MusicBox-APP/commit/fda405c38a9f39da5c322bcf98749655374d5e84) | 2026-10-04 | Reject 30s previews and wrong-length audio: verify fetched length against Spotify, switch source and remember it; refresh APK |
| [832af7a](https://github.com/arookiecoder-ip/MusicBox-APP/commit/832af7ae6ebe9a59d9469cb3b724865653eb2874) | 2026-10-04 | Merge pull request #10 from rajeet-04/superuser |
| [06611fb](https://github.com/arookiecoder-ip/MusicBox-APP/commit/06611fb02942d159a4590248ba367b5dd1e0018b) | 2026-10-04 | Release v2.3.5-beta: version 2.3.5-beta (18), release notes, APK |
| [99413af](https://github.com/arookiecoder-ip/MusicBox-APP/commit/99413aff4004b4cae229263ef54cdd385e2c382e) | 2026-10-04 | Player: extend the backdrop to the screen edge so nothing shows through under the controls; refresh APK |
| [545df35](https://github.com/arookiecoder-ip/MusicBox-APP/commit/545df35472809dd69847474521940cf253b27302) | 2026-10-04 | Power Tools: locked by default, unlocked by tapping the version 7 times; lock again from the page; refresh APK |
| [13063c8](https://github.com/arookiecoder-ip/MusicBox-APP/commit/13063c87cd8704fa662dc5c966c4e8e96e697f2b) | 2026-10-04 | Power Tools: give the list the remaining height so it scrolls, inset for the floating bars; refresh APK |
| [8360aae](https://github.com/arookiecoder-ip/MusicBox-APP/commit/8360aae3a32d64e7cb743c11a1a0fe3961525d5b) | 2026-10-04 | Search tab re-tap selects query; queue tools, Power Tools screen, gesture shortcuts, diagnostics and backup export |
| [30e4b1a](https://github.com/arookiecoder-ip/MusicBox-APP/commit/30e4b1a2643009944a161d9ecc250e35cb90a5c9) | 2026-10-04 | Refetch wrong songs from another source and remember rejected sources per song |
| [31dacfd](https://github.com/arookiecoder-ip/MusicBox-APP/commit/31dacfdff33a94506b4c6d69beeed3cb2313827c) | 2026-10-04 | Add seek-forward and Spotsaver debugging notes and results; update deployment target |
| [b2a553c](https://github.com/arookiecoder-ip/MusicBox-APP/commit/b2a553cea59678186926f6b940c945d887a848e8) | 2026-10-04 | Solid surfaces: dark panel in dark mode instead of near-opaque white; refresh release APK |
| [308748e](https://github.com/arookiecoder-ip/MusicBox-APP/commit/308748e0bbf29ddde2f7422a9f0d54301d92a37d) | 2026-10-04 | Mini player: gliding swipe to next/previous with the neighbouring track sliding in; refresh release APK |
| [306497e](https://github.com/arookiecoder-ip/MusicBox-APP/commit/306497e9efef24ec66d58e67010cdc88314c6cc5) | 2026-10-04 | Swipe cards: label only in the revealed strip, blurred glow behind frosted glass; refresh release APK |
| [4893072](https://github.com/arookiecoder-ip/MusicBox-APP/commit/4893072021ffe90b8f2fa8892a354102bb0cfbf2) | 2026-10-04 | Queue sheet swipe-to-remove: glass glow instead of opaque block; refresh release APK |
| [fd434ee](https://github.com/arookiecoder-ip/MusicBox-APP/commit/fd434ee611499f347a8d875e197b6380821f6f9e) | 2026-10-04 | Swipe cards: tinted glass row with a glow lighting up from behind; refresh release APK |
| [50565f3](https://github.com/arookiecoder-ip/MusicBox-APP/commit/50565f38c1d28bd76a18222ba93bcf15b655d057) | 2026-10-04 | Fix Library scrolling, back off LRCLib when busy; refresh release APK |
| [3579bb8](https://github.com/arookiecoder-ip/MusicBox-APP/commit/3579bb8d10207f485c194efd71105039ab085219) | 2026-10-04 | Release v2.3.4-beta: version 2.3.4-beta (17), crash and navigation fixes, smarter recommendations, release notes |
| [a9d5d75](https://github.com/arookiecoder-ip/MusicBox-APP/commit/a9d5d751789d1faf64519416405b2c1f0ec2104c) | 2026-10-04 | Add release APK build (beta) |
| [a20b61a](https://github.com/arookiecoder-ip/MusicBox-APP/commit/a20b61ade251208548d879c60bff92416cbaebd7) | 2026-10-04 | Resumable Spotify playlist imports that survive app close and network loss |
| [049ea4b](https://github.com/arookiecoder-ip/MusicBox-APP/commit/049ea4ba1e5f2ca40f19a3ae1b73dce62388dd8f) | 2026-10-04 | Call handling via audio focus, shared audio controller, queue status, speed, solid surfaces, CI |
| [47d09ad](https://github.com/arookiecoder-ip/MusicBox-APP/commit/47d09ad004faf96eaf48b10a97a53f1b5a19744c) | 2026-10-04 | Audio: in-pipeline volume/bass boost, stable volume AGC, edge-only skip silence; compact settings |
| [b5b52ec](https://github.com/arookiecoder-ip/MusicBox-APP/commit/b5b52ec6da14dc8d7bc2ffc562c4026ed2aef8ff) | 2026-10-04 | Liquid glass UI revamp, reserve-based recommendations, player/search/library redesign |
| [b3a2e69](https://github.com/arookiecoder-ip/MusicBox-APP/commit/b3a2e6982cdc6b0360421c969e727a0f8c5afc76) | 2026-10-04 | Polish beta UI and improve playback and lyrics hydration |
| [4b4f4c1](https://github.com/arookiecoder-ip/MusicBox-APP/commit/4b4f4c1d69aba872ae9a819e46150daa00b4c49e) | 2026-05-26 | feat(recommendations): add multi-layer deduplication to prevent duplicate tracks in queue |
| [cfee665](https://github.com/arookiecoder-ip/MusicBox-APP/commit/cfee6651170f4ad9b74bfacf2a64d8d79f3088b9) | 2026-05-24 | feat: add RecommenderApi for YouTube search and QueueManager for playback handling |
| [dfb889d](https://github.com/arookiecoder-ip/MusicBox-APP/commit/dfb889d79c2cc3d7676669f44aee2fc02b9cc501) | 2026-05-19 | feat(accessibility): enhance accessibility features and improve user interaction in HomeScreen |
| [2e0c92a](https://github.com/arookiecoder-ip/MusicBox-APP/commit/2e0c92a0c429c203907014668f34cb36343a6e5c) | 2026-05-19 | chore(.gitignore): add Kiro cache directory to gitignore |
| [f98f116](https://github.com/arookiecoder-ip/MusicBox-APP/commit/f98f11637baf03aae205817b76567b6714028e37) | 2026-05-19 | feat(image-caching): optimize Coil image loading with explicit cache keys and aggressive caching |
| [f67c839](https://github.com/arookiecoder-ip/MusicBox-APP/commit/f67c839527ef0106df4273f6e548da0f5caa0aa5) | 2026-05-19 | Update codebase map with structured documents from parallel mapper agents |
| [9c366d6](https://github.com/arookiecoder-ip/MusicBox-APP/commit/9c366d618d7276e27a35fcab12833134ec510d75) | 2026-05-19 | feat(playback): implement instant-play streaming with background download and local caching |
| [a3dc86b](https://github.com/arookiecoder-ip/MusicBox-APP/commit/a3dc86bfbcb0bf84e9e331d136141afc93307c93) | 2026-05-19 | feat(navigation): improve tab navigation handling and state management in MainActivity |
| [9882ff4](https://github.com/arookiecoder-ip/MusicBox-APP/commit/9882ff4df489fbb59ac2c674c6afd3653deaa9ca) | 2026-05-19 | feat(playback): enhance track change handling to resume playback after manual changes |
| [0e4c392](https://github.com/arookiecoder-ip/MusicBox-APP/commit/0e4c392ba76d8944eaacfe50e8d64833d86340b2) | 2026-05-19 | feat(database): add migration from version 8 to 9 for romanized lyrics |
| [e0de43b](https://github.com/arookiecoder-ip/MusicBox-APP/commit/e0de43ba4dd37204c51f287d5ab2a39384aade33) | 2026-05-19 | feat: add shimmer and skeleton loading effects for improved UI responsiveness in various screens |
| [423e208](https://github.com/arookiecoder-ip/MusicBox-APP/commit/423e208fe6b49faa0d50dd764bd54735adbb8a40) | 2026-05-19 | feat: implement update download functionality, add update handling in MainActivity, and create file paths for APK storage |
| [adb1b99](https://github.com/arookiecoder-ip/MusicBox-APP/commit/adb1b9987f23a191ff0c576b30499a1f0aa98b74) | 2026-05-19 | feat: implement horizontal paging for player artwork, sync playback with queue changes, and remove drag gesture handling |
| [0da59a2](https://github.com/arookiecoder-ip/MusicBox-APP/commit/0da59a2a669f31e217be1074609d97ae4a4e4f9a) | 2026-05-19 | feat: enhance AddToPlaylistDialog UI, improve playlist management, and add loading indicators in SearchScreen |
| [5eb12a8](https://github.com/arookiecoder-ip/MusicBox-APP/commit/5eb12a8b8d2be4ff7f9c5e5c940e0540cb5b1032) | 2026-05-19 | Merge pull request #9 from rajeet-04/imgbot |
| [0aa2ab5](https://github.com/arookiecoder-ip/MusicBox-APP/commit/0aa2ab56a94213f53a27c7a339380118b8a8b2ab) | 2026-05-19 | [ImgBot] Optimize images |
| [d08a6b4](https://github.com/arookiecoder-ip/MusicBox-APP/commit/d08a6b42bbdebab55ed0778d113578be36f005ed) | 2026-05-01 | docs: add codebase documentation and feature overview files |
| [5a72b3d](https://github.com/arookiecoder-ip/MusicBox-APP/commit/5a72b3d71eb43e120fa1651b7697258b1c8ee758) | 2026-04-30 | feat: add download track functionality in PlaybackService, update UI accordingly in MusicViewModel, and include download icon resource |
| [827b82e](https://github.com/arookiecoder-ip/MusicBox-APP/commit/827b82e6f17d8722a60da21beac676abc3cef55d) | 2026-04-29 | feat: enhance image loading in JukeApplication, optimize LyricsRomanizer for uppercase first character, improve MusicViewModel queue synchronization, and pre-warm thumbnail cache in SearchViewModel |
| [2fa86a0](https://github.com/arookiecoder-ip/MusicBox-APP/commit/2fa86a04d90cea841ee28e1041413ad1f925dd9b) | 2026-04-29 | feat: update release notes for v2.3.1-beta with new features, enhancements, and bug fixes; add macrobenchmark module and security hardening details |
| [30b539f](https://github.com/arookiecoder-ip/MusicBox-APP/commit/30b539fc1c5af4d2e21a6559dd4e4fcb9a110f3f) | 2026-04-29 | feat: refactor analytics integration, update PostHog initialization, and enhance library screen shuffle functionality; remove unused code and improve UI responsiveness |
| [0b879c4](https://github.com/arookiecoder-ip/MusicBox-APP/commit/0b879c44bd46d80673556cfe43efff5721e3d6fc) | 2026-04-27 | feat: update UI latency tracking in SearchViewModel, enhance color extraction in MusicViewModel, and improve greeting messages in HomeViewModel; adjust ProGuard rules and Gradle settings for performance |
| [e3d4249](https://github.com/arookiecoder-ip/MusicBox-APP/commit/e3d424982eb2f27d9a6b498f537130190f53bff7) | 2026-04-27 | feat: update Android build configuration, implement OkHttp engine for HttpClient, and add API latency tracking in SearchViewModel |
| [4eaa05e](https://github.com/arookiecoder-ip/MusicBox-APP/commit/4eaa05ee4eeb57235dace875a60ef7fe4168ca88) | 2026-04-27 | feat: update release notes for v2.3.2-beta with new features, enhancements, and bug fixes; improve README and changelog for versioning and search experience |
| [ce9c842](https://github.com/arookiecoder-ip/MusicBox-APP/commit/ce9c8425cd6083708c9e90966df787fadf8e6fd4) | 2026-04-17 | feat: implement three-stage lyrics fallback chain using LRCLib, YouTube Music, and YouTube Captions with automatic video ID resolution. |
| [180ea8a](https://github.com/arookiecoder-ip/MusicBox-APP/commit/180ea8a4ace8fe18fb6a2d2a19cd8e3ea7abfbc4) | 2026-04-17 | feat: implement search screen with interactive search bar and categorized results UI |
| [8c2e891](https://github.com/arookiecoder-ip/MusicBox-APP/commit/8c2e891bf5b75818987b0c79e3f9aea9a1343d4e) | 2026-04-17 | feat: implement search screen UI and viewmodel for Spotify content discovery |
| [4ecd518](https://github.com/arookiecoder-ip/MusicBox-APP/commit/4ecd518262ad3a738b2efd246038a7a4f675220b) | 2026-04-17 | feat: implement main navigation structure and add search screen UI |
| [61887f1](https://github.com/arookiecoder-ip/MusicBox-APP/commit/61887f1c0d90dd5c2efc74a5133387a248fd1cd9) | 2026-04-16 | feat: add AGENTS.md for AI agent quick reference and guidelines |
| [1942ff6](https://github.com/arookiecoder-ip/MusicBox-APP/commit/1942ff6a3a3e079462dade9823d8b227ade98bb7) | 2026-04-16 | feat: enhance documentation with detailed comments for various functions across multiple files |
| [cad650d](https://github.com/arookiecoder-ip/MusicBox-APP/commit/cad650dae696f338d24cba80bfa024d1c6d553f0) | 2026-04-16 | Update testing documentation and add codebase map |
| [54b7395](https://github.com/arookiecoder-ip/MusicBox-APP/commit/54b73957e29ffc964e786cfafc2abb262a617a69) | 2026-04-16 | Merge pull request #6 from rajeet-04/multi-part-download |
| [2fcabaf](https://github.com/arookiecoder-ip/MusicBox-APP/commit/2fcabaf4c27cc38a3fba3d6c6eda736ac4043fe2) | 2026-04-16 | Merge beta into multi-part-download and resolve download conflicts |
| [4ebe185](https://github.com/arookiecoder-ip/MusicBox-APP/commit/4ebe185962d003ca559689375f6dfbb6f6d5f77f) | 2026-04-16 | feat: implement Spotmate queue-aware download recovery with fallback logic |
| [6286226](https://github.com/arookiecoder-ip/MusicBox-APP/commit/6286226890574d7f875568fe57535e6616425f5d) | 2026-04-16 | feat: implement segmented downloading with FastDownloader utility and update MusicService to utilize new download methods |
| [eac0a6b](https://github.com/arookiecoder-ip/MusicBox-APP/commit/eac0a6b1647f76b0a70035c2d1682a89a5042336) | 2026-04-16 | feat: enhance track queuing with stream mode support in MusicViewModel and SearchScreen |
| [9db187e](https://github.com/arookiecoder-ip/MusicBox-APP/commit/9db187e3453964ae63c46baee30b9962328441ef) | 2026-04-16 | feat: implement MiniPlayer component with adaptive lyric display and gesture support |
| [747bbf6](https://github.com/arookiecoder-ip/MusicBox-APP/commit/747bbf6b891df011dd018018784a38de833f71c0) | 2026-04-15 | feat: add release notes for v2.3.0-beta detailing new features, improvements, and technical notes |
| [32983fc](https://github.com/arookiecoder-ip/MusicBox-APP/commit/32983fcf03ec50c978896c0225cae6cc1784665e) | 2026-04-15 | feat: update versionCode to 15 and versionName to "2.3.1-beta" for release preparation |
| [4b0e214](https://github.com/arookiecoder-ip/MusicBox-APP/commit/4b0e2141d2dd0be20130af11fe9ad2d03e6105f2) | 2026-04-15 | feat: add baseline_mix drawable and update PlayerScreen to use it for improved icon representation |
| [97dc05c](https://github.com/arookiecoder-ip/MusicBox-APP/commit/97dc05cb364886959241bc5fc09c8e679b797af3) | 2026-04-15 | feat: update PlayerScreen layout with safe drawing padding and improved bottom bar spacing for better UI consistency |
| [b09dae3](https://github.com/arookiecoder-ip/MusicBox-APP/commit/b09dae3f51a8e24d6957f73d1985cc4be15e8959) | 2026-04-15 | feat: update AlbumCard, ArtistCard, and PlaylistCard to use basicMarquee for improved text overflow handling |
| [7457c38](https://github.com/arookiecoder-ip/MusicBox-APP/commit/7457c3809d6a40481ca263a5ad4057026f994bb9) | 2026-04-15 | feat: implement mini-player synced lyrics feature with crossfade animation and user preference toggle |
| [54fc3ad](https://github.com/arookiecoder-ip/MusicBox-APP/commit/54fc3ad194a00565d84da605010b540f604061c1) | 2026-04-15 | feat: enhance LibraryScreen and PurgeSelectionScreen with modernized UI elements, including transparent TopAppBar, outlined filter chips, and refined selection rows for improved user experience |
| [386fa77](https://github.com/arookiecoder-ip/MusicBox-APP/commit/386fa77390fd410a6f4bf7f05ad75d0dda0a014a) | 2026-04-15 | feat: refactor SearchScreen to use Material 3 SearchBar and implement reusable SearchResultItemM3 for improved UI |
| [16ac978](https://github.com/arookiecoder-ip/MusicBox-APP/commit/16ac978791a28d74ee2b814fbae55c5ed6fc470e) | 2026-04-15 | feat: refresh HomeScreen with immersive header, improved spacing, and enhanced card interactions using JukeHaptics |
| [4e878a8](https://github.com/arookiecoder-ip/MusicBox-APP/commit/4e878a83ecb15a12a513a2f13fbcecc71b7de88e) | 2026-04-15 | feat: redesign AudioSettingsScreen with rounded Material 3 cards and enhanced UI elements |
| [edd0df1](https://github.com/arookiecoder-ip/MusicBox-APP/commit/edd0df18acbf93c3058cf69f7610811736bd8f01) | 2026-04-15 | feat: redesign MiniPlayer with rounded card style and integrate animated DancingGlassBackground for enhanced UI |
| [18f6298](https://github.com/arookiecoder-ip/MusicBox-APP/commit/18f629853f7ec96ed1e9002d58d0a9cfb88adfe8) | 2026-04-15 | feat: enhance LyricsOverlay and PlayerScreen with improved UI elements and haptic feedback |
| [8a1237b](https://github.com/arookiecoder-ip/MusicBox-APP/commit/8a1237bb829412f560e495bd25664e44448e80ca) | 2026-04-15 | feat: implement async lyrics and YouTube video ID hydration for improved playback experience |
| [b1c6e6c](https://github.com/arookiecoder-ip/MusicBox-APP/commit/b1c6e6ca624dfe940effe14ed485473a448450b3) | 2026-04-15 | feat: Implement haptic feedback across UI components |
| [0684f60](https://github.com/arookiecoder-ip/MusicBox-APP/commit/0684f6072f108a25c31abbb2a3716eb84d4e553a) | 2026-04-15 | feat: update PostHog dependency and enhance MiniPlayer and PlayerControls with animated play/pause icons |
| [89dbcbe](https://github.com/arookiecoder-ip/MusicBox-APP/commit/89dbcbe6ea9c292798bd351b4948880dd3029c18) | 2026-04-14 | feat: implement MusicViewModel and core playback services for track management and state synchronization |
| [7814644](https://github.com/arookiecoder-ip/MusicBox-APP/commit/78146448b42b80bc5aaf4eef78a028a7e77e6a26) | 2026-04-14 | feat: implement MediaLibraryService with ExoPlayer and audio focus management |
| [4e0c909](https://github.com/arookiecoder-ip/MusicBox-APP/commit/4e0c90903836a1a0f0849758815323e143cf137c) | 2026-04-14 | feat: implement LyricsOverlay component with synced lyric scrolling, blur effects, and associated viewmodel and queue management logic |
| [a4f7353](https://github.com/arookiecoder-ip/MusicBox-APP/commit/a4f7353257fe62bacf13bd155f9687fec14868b6) | 2026-04-14 | feat: enhance music streaming with title cleaning and improved cache management |
| [dc259fd](https://github.com/arookiecoder-ip/MusicBox-APP/commit/dc259fdf582b8743306054b73d4c3396cdaa248e) | 2026-04-14 | feat: implement music service, playback infrastructure, and track management utilities |
| [d448a89](https://github.com/arookiecoder-ip/MusicBox-APP/commit/d448a898020cd3aa2563c39f6d1708bed6d13569) | 2026-04-13 | feat: implement MediaLibraryService with ExoPlayer and add LyricsOverlay component |
| [45ade5f](https://github.com/arookiecoder-ip/MusicBox-APP/commit/45ade5feda2c032123f3cbf5eb3aba7025015f7f) | 2026-04-13 | feat: implement QueueManager service for automated music recommendations and background downloads |
| [9abbf0e](https://github.com/arookiecoder-ip/MusicBox-APP/commit/9abbf0e8c02b9f0d27d68476b3e42008ee1e1511) | 2026-04-13 | feat: implement music streaming, playback, and database management services |
| [e18327b](https://github.com/arookiecoder-ip/MusicBox-APP/commit/e18327b66c698f558fc3fea0731c806d04febc77) | 2026-04-12 | refactor: reorganize project documentation by moving all release notes and guides into the /docs directory. |
| [c4a7fe4](https://github.com/arookiecoder-ip/MusicBox-APP/commit/c4a7fe48884f7e38927307c64e87a0299223ab3a) | 2026-04-12 | RELEASE 2.3.0-beta feat: add LRU streaming cache, dual download sources, and decouple streaming from library database |
| [b5a7135](https://github.com/arookiecoder-ip/MusicBox-APP/commit/b5a713527fa9c0a5e328fb4794f72c770b1c1de9) | 2026-04-12 | feat: implement core music database, Spotify API integration, playback services, and UI components |
| [e4021b2](https://github.com/arookiecoder-ip/MusicBox-APP/commit/e4021b28dfa2a4687aada0016246ab4fb7b37d1e) | 2026-04-12 | feat: implement QueueManager service for automated music recommendation, download, and playback queue management |
| [d18440b](https://github.com/arookiecoder-ip/MusicBox-APP/commit/d18440bc6ccfcae4267141ff10bcf072166bc7ea) | 2026-04-12 | Enhance playback recovery and UI components |
| [5552dc7](https://github.com/arookiecoder-ip/MusicBox-APP/commit/5552dc750ac7b6fe5a9ea77e353581b2a29c3d3c) | 2026-03-19 | feat: enhance cache management in PlaybackService with new clearing methods and custom LoadControl |
| [263039e](https://github.com/arookiecoder-ip/MusicBox-APP/commit/263039eca4ec3a76e93e71203a874b613d5bde54) | 2026-03-18 | Merge branch 'beta' of https://github.com/rajeet-04/JUKES into beta |
| [601db94](https://github.com/arookiecoder-ip/MusicBox-APP/commit/601db9498ef91200eadee369783027f79f987738) | 2026-03-18 | feat: add v2.2.0-beta release notes with new features and bug fixes |
| [a982ae8](https://github.com/arookiecoder-ip/MusicBox-APP/commit/a982ae8d0b70e9167803d31f47ada2fec38b18f5) | 2026-03-18 | feat: add v2.2.0-beta release notes with new features and bug fixes |
| [020f1c3](https://github.com/arookiecoder-ip/MusicBox-APP/commit/020f1c31cad63cd0229c38469cc0abf377f8679b) | 2026-03-18 | feat: Update track preparation logic to ensure upcoming 3 songs are downloaded and validated |
| [371e061](https://github.com/arookiecoder-ip/MusicBox-APP/commit/371e061871cc9479e45650f8178fcdeca13b5ee4) | 2026-03-18 | feat: Implement lyrics offset functionality with migration and UI controls for synchronization |
| [a39f0f4](https://github.com/arookiecoder-ip/MusicBox-APP/commit/a39f0f4597ce5342d0b12d769e93ab200605bfed) | 2026-03-18 | feat: Enhance LyricsRomanizer with coroutine support for asynchronous romanization and caching |
| [036f001](https://github.com/arookiecoder-ip/MusicBox-APP/commit/036f00139765980ae6d2c56441acb3bf3df890be) | 2026-03-18 | feat: Add LyricsRomanizer utility for romanizing lyrics and synced lyrics |
| [b629a00](https://github.com/arookiecoder-ip/MusicBox-APP/commit/b629a00a647065f713f06fdaf82aad7fa0071d2b) | 2026-03-18 | feat: Add methods to delete playlist tracks before track deletion to prevent FK constraint errors and implement cache cleanup functionality |
| [97e7db2](https://github.com/arookiecoder-ip/MusicBox-APP/commit/97e7db2150dc67e6284ec3ecd533c8da29e40dc7) | 2026-03-13 | feat: Implement core Juke music player functionality including search, playback, Spotify API integration, and initial project structure. |
| [8b6223e](https://github.com/arookiecoder-ip/MusicBox-APP/commit/8b6223ee641db40854fed17c412481e9b967e7de) | 2026-03-08 | feat: Create new Android application module with Media3 playback service and Gradle dependency management. |
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
