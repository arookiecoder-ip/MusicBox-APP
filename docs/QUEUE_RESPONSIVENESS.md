# Queue and player responsiveness — debug 2.4.40 (92)

The tablet capture recorded 374 missed frame deadlines out of 1,440 frames
(25.97%), 313 slow UI-thread events and a 93 ms frame-time 99th percentile.
GPU timing was 4 ms median and 10 ms at the 99th percentile. This supports
reducing work on the UI thread; it does not isolate a specific Home/Library
function. The CPU sample predates the rendering capture and thermal status was
light, so neither establishes the cause of individual slow taps.

Queue taps now preview the selected index together with song metadata. The
requested occurrence is retained for duplicate songs. If a queue edit removes
that index, matching identity is rechecked; an absent song gets no false row
highlight. Clearing a pending request restores the receiver's actual state.

Queue row keys and lookup maps are built on a bounded background dispatcher
when the queue changes, and retained by the ViewModel between sheet openings.
Clock, selection and loading changes reuse the same prepared rows. During a
replacement snapshot, reorder/removal is disabled until the displayed rows
match the live queue. Empty/loading queues have a bounded skeleton. This
adds no polling timer or service.

The main player no longer observes queue-only changes; artwork and queue
observe their own queue/index slices. Large programmatic artwork jumps snap
to the selected page rather than animating through intervening queue pages;
adjacent transitions and user swipes retain animation. Entrance durations
remain 200 ms for the player and 160 ms for the queue.

Every backend request previously created an eight-second warning timer,
including routine presence and queue reads. Those timers are removed.
Existing actual network errors and feature loading/retry states remain.
Playback confirmation warnings ignore superseded requests and use foreground
message deduplication. The local startup timeout does not warn about a player
that is already playing or no longer buffering.

Validation includes immediate selection/rollback, duplicate occurrences,
removed indices, queue replacement/empty queues, and reuse across 30 state
updates with 10,000 songs. A Compose regression checks selection before any
playback confirmation. Compilation alone is not a device UI test pass.

Repeat the same tablet capture and variant after installing this build, using
the journeys in PERFORMANCE_MEASUREMENT.md. Physical-device frame timing,
Home/Library traces and long-duration tablet scrolling remain device checks;
no measured improvement or elimination of all tablet lag is claimed here.
