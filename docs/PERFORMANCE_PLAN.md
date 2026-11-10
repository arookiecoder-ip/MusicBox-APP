# Music Box performance plan

## Evidence and scope

The supplied one-minute foreground capture has 878 frames, 253 missed deadlines
(28.82%), and 248 slow UI-thread events. GPU median/99th percentile are 3/8 ms.
Music Box averages 20.2% of one core on a device reporting 1000% total CPU
capacity. Current thermal status is normal. These establish a responsiveness
problem; they do not establish the cause of overheating.

Deliver debug builds only. Preserve playback ownership, immediate stream start,
complete current-song caching, background recovery, and notification controls.
Do not trade correctness for lower polling frequency.

## Implementation order

1. A single subscriber-driven playback clock. Progress and lyric consumers share
   it, and ordinary screen state does not emit every 300 ms. Draw mini-player
   progress without recomposing artwork/buttons; emit lyric changes only when
   the displayed line changes.
2. Remove repeated queue equality/mapping on clock ticks. Prepare large lyric
   lists and derived lists off the main thread, use stable keys/content types,
   and retain appropriately sized image requests. Audit existing optimizations
   before introducing redundant caches.
3. Deduplicate overlapping remote-state reads. Presence receives output/commands;
   unchanged ownership must not launch redundant queue refreshes. Ownership
   leases retain their existing deadlines and checks. Queue and cursor changes
   must still propagate promptly.
4. Cache current audio with priority. Upcoming downloads remain sequential and
   lazy; pause speculative work on low battery, battery saver or thermal stress.
   Current-song caching and explicit downloads remain available. Release locks
   on completion/cancellation and retain bounded network retry/backoff.
5. Reuse cursor metadata, avoid duplicate periodic disk saves and queue work,
   and stop UI-only loops as soon as the last visible consumer leaves. Keep
   background ownership, playback and locked-device command delivery alive.
6. Add a debug-signed optimized/profileable performance variant and repeatable
   startup/navigation/scroll benchmarks and Baseline Profile generation. Do not
   invent a profile: generate it from real journeys on a device/emulator, then
   review and validate the resulting artifact before shipping it.
7. Review diffs, run unit/regression suites, build debug and performance targets,
   verify manifest/package entry points, and publish the debug APK through CI.

## Edge-case matrix

| Area | Cases | Required behavior |
|---|---|---|
| Clock | Multiple consumers, rotation, close/reopen, background/lock | One clock; no UI sampling with zero consumers; fresh first sample on return |
| Transport | Pause, rapid seeks, end/repeat, loading/error, unknown duration | Freeze while paused/loading; clamp valid cursor; never advance old track/output |
| Output | Phone, Alexa, remote phone, paused switch, handoff pending | Keep output selection; use the destination clock only after confirmation |
| Ownership | Lease expiry, slow HTTP, stale replies, device close, two phones | Preserve existing fences and never start on the wrong phone |
| Connectivity | Offline cache, partial cache, loss/restore, 429/server outage | Healthy cached audio continues; bounded retry; no stale representation splice |
| Prefetch | Current incomplete, next becomes current, cache eviction/budget | Current first; promotion cancels obsolete speculative work; one writer |
| Power | Battery saver, low battery, charging, severe heat, old Android API | Speculative policy only; no polling of sensors every frame; compatible fallback |
| UI | Long queues/lyrics, duplicate IDs/timestamps, missing artwork, resize | Stable row identity; bounded decoding; no recomposition of full lists per tick |
| Lifecycle | Task removed, service restart, call, earbuds, notifications | Honor intentional stop; retain allowed background recovery and call pause |
| Measurements | Debug vs optimized debug, baseline vs new, local vs remote | Compare same journeys/device/state; separate generated and measured results |

## Validation and acceptance

Use cancellation/subscription tests for the clock; stale/output/queue tests for
remote refresh; preloader tests for demand priority and resource cleanup; and the
existing call, cache-integrity, resumption, handoff and ownership regression suite.
Compile instrumentation benchmarks even when no device is attached.

On the Samsung, repeat cold startup, Home/Library scrolling, player/queue opening,
remote control, and locked local playback. Compare frame deadlines, UI-thread
work, CPU, network request counts, and battery/thermal readings under the same
conditions. Do not claim a measured FPS/battery improvement from unit tests.

Baseline Profile generation and physical-device battery/thermal validation are
explicit device steps; retain their pending status until actually run.
