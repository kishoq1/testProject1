package com.example.testproject.util

import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.io.IOException

class DownloaderImpl private constructor(
    private val client: OkHttpClient
) : Downloader() {

    companion object {
        private var instance: DownloaderImpl? = null

        fun init(client: OkHttpClient?): DownloaderImpl {
            return instance ?: DownloaderImpl(
                client ?: OkHttpClient.Builder().build()
            ).also { instance = it }
        }
    }

    override fun execute(request: Request): Response {
        val requestBody = request.dataToSend()?.toRequestBody()

        val okHttpRequest = okhttp3.Request.Builder()
            .url(request.url())
            .method(request.httpMethod(), requestBody)
            .headers(request.headers().toOkHttpHeaders())
            .build()

        try {
            val response = client.newCall(okHttpRequest).execute()

            if (!response.isSuccessful) {
                if (response.code == 429) {
                    response.close()
                    throw ReCaptchaException("ReCaptcha Challenge requested", request.url())
                }

                val errorBody = response.body?.string() ?: "Unknown error"
                response.close()
                throw IOException("Request failed with code ${response.code}: $errorBody")
            }

            // Lấy URL cuối cùng sau khi đã chuyển hướng
            val latestUrl = response.request.url.toString()

            return Response(
                response.code,
                response.message,
                response.headers.toNewPipeHeaders(),
                response.body?.string(),
                latestUrl
            )

        } catch (e: IOException) {
            throw IOException("Network error: ${e.message}", e)
        }
    }

    private fun Map<String, List<String>>.toOkHttpHeaders(): Headers {
        val builder = Headers.Builder()
        for ((name, values) in this) {
            for (value in values) {
                builder.add(name, value)
            }
        }
        return builder.build()
    }

    private fun Headers.toNewPipeHeaders(): Map<String, List<String>> {
        return this.toMultimap()
    }
}