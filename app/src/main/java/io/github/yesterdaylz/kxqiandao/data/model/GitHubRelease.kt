package io.github.yesterdaylz.kxqiandao.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** GitHub release 里用得到的字段 */
@JsonClass(generateAdapter = true)
data class GitHubRelease(
    @param:Json(name = "tag_name") val tagName: String? = null,
    @param:Json(name = "name") val name: String? = null,
    @param:Json(name = "html_url") val htmlUrl: String? = null,
    @param:Json(name = "body") val body: String? = null,
    @param:Json(name = "assets") val assets: List<ReleaseAsset>? = null
)

/** release 里挂的文件 */
@JsonClass(generateAdapter = true)
data class ReleaseAsset(
    @param:Json(name = "name") val name: String? = null,
    @param:Json(name = "browser_download_url") val browserDownloadUrl: String? = null
)
