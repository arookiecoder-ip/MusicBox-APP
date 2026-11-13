package `in`.synthora.musicbox.utils

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Small, app-private values such as session cookies are encrypted with an
 * Android-Keystore-backed AES-GCM key before they reach SharedPreferences.
 *
 * Jetpack Security's EncryptedSharedPreferences is deprecated, so this keeps
 * the same property without an unmaintained dependency: the key never leaves
 * the Keystore, every write uses a fresh IV, and a ciphertext from another
 * install or a wiped Keystore simply fails closed.
 */
internal object SecureStore {
    private const val PROVIDER = "AndroidKeyStore"
    private const val ALIAS = "musicbox.session.v1"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_BYTES = 12
    private const val TAG_BITS = 128

    fun encrypt(plain: String): String? = runCatching {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val body = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        val packed = ByteArray(cipher.iv.size + body.size)
        cipher.iv.copyInto(packed)
        body.copyInto(packed, cipher.iv.size)
        Base64.encodeToString(packed, Base64.NO_WRAP)
    }.getOrNull()

    fun decrypt(encoded: String): String? = runCatching {
        val packed = Base64.decode(encoded, Base64.NO_WRAP)
        require(packed.size > IV_BYTES)
        val iv = packed.copyOfRange(0, IV_BYTES)
        val body = packed.copyOfRange(IV_BYTES, packed.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, iv))
        String(cipher.doFinal(body), Charsets.UTF_8)
    }.getOrNull()

    /** Drop the wrapping key after a failed decrypt; the next write creates a new one. */
    fun forgetKey() {
        runCatching { KeyStore.getInstance(PROVIDER).apply { load(null) }.deleteEntry(ALIAS) }
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance(PROVIDER).apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
        generator.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .setUserAuthenticationRequired(false)
                .build()
        )
        return generator.generateKey()
    }
}
