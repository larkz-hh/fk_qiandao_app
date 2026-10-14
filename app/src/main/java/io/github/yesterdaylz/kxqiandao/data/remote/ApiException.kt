package io.github.yesterdaylz.kxqiandao.data.remote

/** 服务端返回了业务错误 */
class ApiException(val code: Int?, message: String) : Exception(message)
