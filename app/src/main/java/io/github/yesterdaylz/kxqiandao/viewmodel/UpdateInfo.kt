package io.github.yesterdaylz.kxqiandao.viewmodel

/** 发现的新版本 */
data class UpdateInfo(
    val version: String,
    val url: String,// release 页面
    val apkUrl: String?,// release 里挂的 apk，没有就是 null
)
