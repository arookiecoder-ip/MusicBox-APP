# Remote playback events

The application uses one owner-authenticated WebSocket at `/api/app/events/`, shared by the process's playback controller and presence connection. Mini player, full player and media notification continue to observe the same controller state. Commands remain HTTP requests guarded by output tokens and existing command IDs; receiving a command acknowledgment does not establish that audio is playing.

The initial event contains the full queue. Later events omit it when both queue version and output token are unchanged. Position is an anchor for the existing shared local clock, not a network-driven animation. Snapshot decoding stays off the main thread. HTTP and event snapshots share a server epoch/revision; delayed responses from older revisions or a retired epoch are rejected. Changing the selected device cancels its subscription; signing out cancels it and clears ordering/error state.

A connected stream supplies state changes and a repair/authentication snapshot every 15 seconds. Ordinary state polling is suppressed while events are fresh. Failed or unsupported connections retry with bounded exponential backoff, jitter and Retry-After, while the existing adaptive HTTP/presence fallback remains available. Event consumers and pending state updates are conflated. Device commands and acknowledgments are kept on their separate existing path, so a state event cannot discard a command.

Presence uses immediate short HTTP requests at most eight seconds apart while events are live, waking early on device/command revisions. Without events it uses the backend's globally bounded long-poll budget and honors the returned retry interval. The mandatory phone ownership heartbeat stays independent of this synchronization; a live event connection is never permission to ignore an expired lease.

## Compatibility and verification

Deploy the backend's async gateway image to enable events; older backends remain usable through polling. No persistent connection is held in Waitress. The server's Docker port stays 5000 and the reverse proxy must support WebSocket upgrades (Caddy reverse_proxy does by default). No server build or deployment is performed by this change.

GitHub Actions runs all JVM tests and builds the APK. `PlaybackEventsTest` covers state delivery while HTTP is blocked, older responses, server restart epochs, device changes, sign-out, queue deltas, duplicate errors and backoff. `PlaybackPresenceDeliveryTest` verifies that events do not erase commands. Existing ownership, interruption, buffering, queue and notification suites remain required.

Physical-device acceptance still requires phone-to-Echo and phone-to-phone transfers, voice-driven Echo changes, background/Doze, loss/recovery of Wi-Fi, rapid transport taps and observing actual audio against the notification. Alexa callback latency is outside the event transport's control; no instant-update guarantee is made without receiver confirmation.
