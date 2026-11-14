package `in`.synthora.musicbox.network

/** Feature/provider authorization is separate from the Music Box login cookie. */
internal suspend fun verifiedAccountFailure(path:String,error:BackendAuthException,validate:suspend ()->Unit):Exception {
    if(path.trimEnd('/') in setOf("/api/account","/login"))return error
    if(confirmSessionExpired(validate))return error
    return BackendHttpException(error.statusCode,error.message ?: "Reconnect this feature. Your Music Box account is still signed in.")
}
