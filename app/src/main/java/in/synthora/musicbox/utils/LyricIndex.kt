package `in`.synthora.musicbox.utils

/** Last timestamp at/before the cursor, including duplicate timestamps and negative offsets. */
fun lyricIndexAt(count: Int, position: Long, timeAt: (Int) -> Long): Int {
    var low = 0
    var high = count
    while (low < high) {
        val middle = (low + high) ushr 1
        if (timeAt(middle) <= position) low = middle + 1 else high = middle
    }
    return low - 1
}
