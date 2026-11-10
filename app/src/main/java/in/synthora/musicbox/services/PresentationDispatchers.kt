package `in`.synthora.musicbox.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi

/** Canceled/rapid screen changes cannot occupy every CPU with stale presentation work. */
@OptIn(ExperimentalCoroutinesApi::class)
internal object PresentationDispatchers {
    val lyrics = Dispatchers.Default.limitedParallelism(1)
    val library = Dispatchers.Default.limitedParallelism(1)
}
