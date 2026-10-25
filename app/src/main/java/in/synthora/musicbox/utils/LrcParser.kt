package `in`.synthora.musicbox.utils

data class TimedLyricLine(val timeMs: Long, val text: String)
internal val lrcTimeTag = Regex("""\[(\d{1,3}):(\d{2})(?:[.,](\d{1,3}))?]""")
private val offsetTag = Regex("""(?im)^\s*\[offset:([+-]?\d+)]\s*$""")
private fun addTime(a: Long, b: Long): Long = try { Math.addExact(a,b) }
    catch (_: ArithmeticException) { if(a>=0 && b>=0) Long.MAX_VALUE else Long.MIN_VALUE }

/** LRC supports repeated timestamps, whole seconds, fractions and an optional millisecond offset. */
fun parseLrc(source: String, offsetMs: Long = 0): List<TimedLyricLine> {
    val sourceOffset=offsetTag.find(source)?.groupValues?.get(1)?.toLongOrNull() ?: 0
    val offset=addTime(offsetMs,sourceOffset)
    return source.lineSequence().flatMap { line ->
        val tags=lrcTimeTag.findAll(line).toList()
        val text=tags.lastOrNull()?.let { line.substring(it.range.last+1).trim() }.orEmpty()
        if(text.isBlank()) emptySequence() else tags.asSequence().mapNotNull { tag ->
            val seconds=tag.groupValues[2].toLong()
            if(seconds>59) null else {
                val fraction=tag.groupValues[3].padEnd(3,'0').ifEmpty { "000" }.toLong()
                val time=tag.groupValues[1].toLong()*60_000+seconds*1000+fraction
                TimedLyricLine(addTime(time,offset).coerceAtLeast(0),text)
            }
        }
    }.sortedBy { it.timeMs }.toList()
}
fun hasTimedLyrics(source: String?): Boolean = !source.isNullOrBlank() && parseLrc(source).isNotEmpty()
