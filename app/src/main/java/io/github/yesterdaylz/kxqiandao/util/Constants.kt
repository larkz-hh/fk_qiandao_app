package io.github.yesterdaylz.kxqiandao.util

object Constants {
    const val BASE_URL = "https://at.kexie.space"
    const val KNOWLEDGE_BASE_URL = "https://ccn80b5bgw86.feishu.cn/wiki/CKD8wrIVyi1E9VkdUVGclVFxnlb"
    const val CAMPUS_RUN_URL = "http://10.60.58.102:81/"
    /// 科协网网段
    val CAMPUS_LAN_PREFIXES = listOf("172.16.22.")
    ///本版暂不开放定时签退
    const val ENABLE_SCHEDULED_SIGN_OUT = false
    const val QIANDAO_SUCCEED = "qiandaoSucceed"
    const val QIANDAO_OUT_TIME = "qiandaoOutTime"
    const val QIANTUI_SUCCEED = "qiantuiSucceed"
    const val QIANDAO_FAIL = "qiandaoFail"
    const val QIANTUI_FAIL = "qiantuiFail"
    const val QIANDAO_REPEAT = "qiantuiRepeat"
    const val QIANTUI_NO = "qiantuiNo"
    const val OP_TOO_FASTER = "opTooFaster"
    const val NETWORK_ERROR = "连的是科协网吗，你就签？(也可能是网炸了)🤣"
    const val INIT_ONLINE_OK = "状态加载完成"
    const val SUCCEED = "succeed"
    const val FAIL = "fail"
    const val SIGN_OUT_AUTO ="已自动完成签退"
    const val QUAN_XIAN = "你还未开启精确闹钟权限，请授权后再重试"
    const val DIAN_CHI = "请在列表中点击本应用，选择「不优化」或「允许后台运行」"
    const val CHANNEL_ID = "auto_signout_channel"
    const val CHANNEL_NAME = "定时签退"
    const val NOTIFICATION_ID = 1001
}
