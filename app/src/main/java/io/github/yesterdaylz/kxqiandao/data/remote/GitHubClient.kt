package io.github.yesterdaylz.kxqiandao.data.remote

import io.github.yesterdaylz.kxqiandao.data.model.GitHubRelease
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** GitHub API*/
object GitHubClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val releaseAdapter = RetrofitClient.moshi.adapter(GitHubRelease::class.java)

    /** repo 形如 owner/name */
    suspend fun latestRelease(repo: String): Result<GitHubRelease> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://api.github.com/repos/$repo/releases/latest")
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "KXqiandao")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException("HTTP ${response.code}"))
                }
                val body = response.body?.string().orEmpty()
                val release = releaseAdapter.fromJson(body)
                    ?: return@withContext Result.failure(IllegalStateException("空响应"))
                Result.success(release)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
