package `in`.synthora.musicbox.services

/** Bounded lifecycle evidence; contains no account, song or device identifiers. */
internal object RemoteNotificationDiagnostics {
    private val events = ArrayDeque<String>()
    @Synchronized fun record(event: String) {
        if (events.size == 60) events.removeFirst()
        events.addLast("${android.os.SystemClock.elapsedRealtime()}:$event")
    }
    @Synchronized fun snapshot(): List<String> = events.toList()
}
