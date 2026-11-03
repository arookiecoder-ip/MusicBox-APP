package `in`.synthora.musicbox.services

import java.io.File
import java.security.MessageDigest
import org.junit.Assert.*
import org.junit.Test

class UpdateIntegrityTest {
    @Test fun updatesMustComeFromTheOfficialHttpsReleasePath() {
        assertTrue(trustedUpdateUrl("https://github.com/arookiecoder-ip/MusicBox-APP/releases/download/v2.5/app.apk"))
        for (url in listOf("http://github.com/arookiecoder-ip/MusicBox-APP/releases/download/v2.5/app.apk",
            "https://github.com.evil.test/arookiecoder-ip/MusicBox-APP/releases/download/v2/app.apk",
            "https://github.com/other/app/releases/download/v2/app.apk", "https://evil.test/app.apk")) assertFalse(url, trustedUpdateUrl(url))
    }
    @Test fun tamperingAndInvalidDigestFormatsAreRejected() {
        val file = File.createTempFile("update-integrity", ".apk")
        try {
            file.writeText("expected release bytes")
            val digest = "sha256:" + MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it.toInt() and 255) }
            verifyUpdateDigest(file, digest)
            file.appendText("tampered")
            assertTrue(runCatching { verifyUpdateDigest(file, digest) }.isFailure)
            assertTrue(runCatching { verifyUpdateDigest(file, "sha1:untrusted") }.isFailure)
        } finally { file.delete() }
    }
}
