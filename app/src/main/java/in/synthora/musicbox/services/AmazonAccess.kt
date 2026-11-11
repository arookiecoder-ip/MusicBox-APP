package `in`.synthora.musicbox.services

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Fresh server policy shared by account and device UI; never persisted as a grant. */
object AmazonAccess {
    private val mutableEnabled = MutableStateFlow(false)
    val enabled = mutableEnabled.asStateFlow()
    fun update(enabled: Boolean) { mutableEnabled.value = enabled }
}
