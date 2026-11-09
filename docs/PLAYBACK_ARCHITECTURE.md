# Playback architecture

The current, code-verified architecture is maintained in [../ARCHITECTURE.md](../ARCHITECTURE.md).

It covers phone/remote controllers, queue occurrence identity and revisions, output leases and handoffs, progressive audio, separate current/next-five caching and explicit downloads, presence-triggered observation, calls, lyrics, signing, tests and physical-device limits.

This compatibility entry point replaces the earlier checklist: its download-first playback, partial-byte-only preloading, older poll intervals and “UI tests disabled” descriptions are historical rather than the current contract. For dated changes and PR provenance, see [../CHANGELOG.md](../CHANGELOG.md). For profiling procedures, see [PERFORMANCE_MEASUREMENT.md](PERFORMANCE_MEASUREMENT.md).
