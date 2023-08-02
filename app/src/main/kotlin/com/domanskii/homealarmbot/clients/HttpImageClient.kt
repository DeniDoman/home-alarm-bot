package com.domanskii.homealarmbot.clients

import com.burgstaller.okhttp.AuthenticationCacheInterceptor
import com.burgstaller.okhttp.CachingAuthenticatorDecorator
import com.burgstaller.okhttp.digest.CachingAuthenticator
import com.burgstaller.okhttp.digest.DigestAuthenticator
import com.burgstaller.okhttp.digest.Credentials as DigestCredentials
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Credentials
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap


enum class HttpImageAuth {
    NONE, BASIC, DIGEST
}

class HttpImageClient {
    companion object {
        @Throws(IOException::class, IllegalStateException::class)
        fun getImage(url: String, user: String, password: String, auth: HttpImageAuth): ByteArray {
            when (auth) {
                HttpImageAuth.NONE -> {
                    val client = OkHttpClient()
                    val request = Request.Builder().url(url).build()
                    val response = client.newCall(request).execute()
                    return response.body!!.bytes()
                }

                HttpImageAuth.BASIC -> {
                    val client = OkHttpClient()
                    val credential: String = Credentials.basic(user, password)
                    val request = Request.Builder().url(url).header("Authorization", credential).build()
                    val response = client.newCall(request).execute()
                    return response.body!!.bytes()
                }

                HttpImageAuth.DIGEST -> {
                    val authenticator = DigestAuthenticator(DigestCredentials(user, password))
                    val authCache: Map<String, CachingAuthenticator> = ConcurrentHashMap()
                    val client: OkHttpClient =
                        OkHttpClient.Builder().authenticator(CachingAuthenticatorDecorator(authenticator, authCache))
                            .addInterceptor(AuthenticationCacheInterceptor(authCache)).build()

                    val request: Request = Request.Builder().url(url).get().build()
                    val response: Response = client.newCall(request).execute()
                    return response.body!!.bytes()
                }
            }
        }
    }
}
