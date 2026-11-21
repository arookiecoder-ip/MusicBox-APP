package `in`.synthora.musicbox.network

/** Preserve an explicit language across retry without retaining a signed media URL. */
internal fun selectedAudioLanguage(source: String?): String? = runCatching {
    val uri = java.net.URI(source ?: return null)
    if (uri.scheme !in setOf("https", "http")) return null
    uri.rawQuery.orEmpty().split('&').filter { it.startsWith("audio_track=") }.singleOrNull()
        ?.substringAfter('=')?.takeIf { it.matches(Regex("[A-Za-z0-9_.-]{1,80}")) }
}.getOrNull()
