package `in`.synthora.musicbox

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.util.DebugLogger

class JukeApplication : Application(), ImageLoaderFactory, androidx.lifecycle.ViewModelStoreOwner {
    override val viewModelStore = androidx.lifecycle.ViewModelStore()
    fun playbackModel(): `in`.synthora.musicbox.viewmodels.MusicViewModel =
        androidx.lifecycle.ViewModelProvider(viewModelStore,
            androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(this))[
                `in`.synthora.musicbox.viewmodels.MusicViewModel::class.java]

    override fun onCreate() {
        super.onCreate()
        `in`.synthora.musicbox.services.PlaybackErrorRecorder.init(this)
        `in`.synthora.musicbox.utils.Diagnostics.installCrashRecorder(this)
        `in`.synthora.musicbox.network.Backend.init(this)
        `in`.synthora.musicbox.services.PhonePlaybackOwnership.init(this)
        `in`.synthora.musicbox.network.NetworkFeedback.observe(this)
        `in`.synthora.musicbox.services.DownloadRepository.get(this)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun newImageLoader(): ImageLoader {
        val lowMemory = getSystemService(android.app.ActivityManager::class.java).isLowRamDevice
        val imageBudget = minOf(Runtime.getRuntime().maxMemory() / 8, (if (lowMemory) 16 else 64) * 1024L * 1024L)
        return ImageLoader.Builder(this)
            // Limit simultaneous decoding so dense shelves do not compete with UI/render threads.
            .decoderDispatcher(kotlinx.coroutines.Dispatchers.Default.limitedParallelism(if (lowMemory) 1 else 2))
            .memoryCache {
                MemoryCache.Builder(this)
                    // Bound artwork memory on small heaps; disk cache still preserves reuse.
                    .maxSizeBytes(imageBudget.toInt())
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    // Increased from 2% → 5% so album thumbnails survive across sessions
                    .maxSizePercent(0.05)
                    .build()
            }
            // Always read from / write to both caches
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            // Respect server Cache-Control but still serve stale from disk while revalidating
            .networkCachePolicy(CachePolicy.ENABLED)
            .crossfade(false)
            .build()
    }
}
