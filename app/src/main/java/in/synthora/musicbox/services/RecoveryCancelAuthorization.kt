package `in`.synthora.musicbox.services

/** Only the immutable notification PendingIntent may invoke the private recovery action. */
internal class RecoveryCancelAuthorization {
    val proof: String = java.util.UUID.randomUUID().toString()
    fun accepts(candidate: String?): Boolean = candidate != null && java.security.MessageDigest.isEqual(
        proof.toByteArray(Charsets.UTF_8), candidate.toByteArray(Charsets.UTF_8))
}
