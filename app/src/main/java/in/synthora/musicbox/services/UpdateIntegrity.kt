package `in`.synthora.musicbox.services

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.File
import java.security.MessageDigest

internal fun trustedUpdateUrl(value: String): Boolean {
    val url = value.toHttpUrlOrNull() ?: return false
    return url.scheme == "https" && url.host == "github.com" && url.port == 443 &&
        url.username.isEmpty() && url.password.isEmpty() &&
        url.encodedPath.startsWith("/arookiecoder-ip/MusicBox-APP/releases/download/")
}

internal fun verifyUpdateDigest(file: File, expected: String?) {
    if (expected == null) return // Older GitHub releases omit digests; APK signer verification remains mandatory.
    require(expected.matches(Regex("sha256:[a-fA-F0-9]{64}"))) { "Unsupported update digest" }
    val hash = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
        val buffer = ByteArray(64 * 1024)
        while (true) { val count = input.read(buffer); if (count < 0) break; hash.update(buffer, 0, count) }
    }
    val actual = hash.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
    check(MessageDigest.isEqual(actual.toByteArray(), expected.substringAfter(':').lowercase().toByteArray())) { "Update digest mismatch" }
}

@Suppress("DEPRECATION")
internal fun verifyUpdateApk(context: Context, file: File, digest: String?) {
    verifyUpdateDigest(file, digest)
    val manager = context.packageManager
    val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
    val installed = manager.getPackageInfo(context.packageName, flags)
    val candidate = manager.getPackageArchiveInfo(file.absolutePath, flags) ?: error("Invalid APK")
    check(candidate.packageName == context.packageName) { "Unexpected update package" }
    val currentVersion = if (Build.VERSION.SDK_INT >= 28) installed.longVersionCode else installed.versionCode.toLong()
    val nextVersion = if (Build.VERSION.SDK_INT >= 28) candidate.longVersionCode else candidate.versionCode.toLong()
    check(nextVersion > currentVersion) { "Update is not newer than the installed app" }
    val trusted = if (Build.VERSION.SDK_INT >= 28) {
        val signers = candidate.signingInfo?.apkContentsSigners.orEmpty()
        signers.isNotEmpty() && signers.all { manager.hasSigningCertificate(context.packageName,
            MessageDigest.getInstance("SHA-256").digest(it.toByteArray()), PackageManager.CERT_INPUT_SHA256) }
    } else {
        val signatures = candidate.signatures.orEmpty()
        signatures.isNotEmpty() && signatures.toSet() == installed.signatures.orEmpty().toSet()
    }
    check(trusted) { "Update signing identity is not trusted" }
}
