package versionUp.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONObject
import java.io.IOException

// 定义一个数据类，包含多种类型的数据
data class ResponseData(
    val response: Response? = null,
    val err: Exception? = null,
    val success: Boolean
)


object HttpHelper {

    // http客户端
    private val httpClient: OkHttpClient = OkHttpClient();

    // 获取内容
    suspend fun getContentString(url: String, headers: JSONObject? = null): String? {
        return withContext(Dispatchers.IO) {
            get(
                url,
                headers
            ).let { responseData ->
                responseData.success.takeIf { it }.let { responseData.response?.body?.string() }
            }
        }
    }

    // 请求发送
    fun get(url: String, headers: JSONObject? = null, callback: (ResponseData) -> Unit) {
        try {
            // 创建请求
            Request.Builder()
                .url(url)
                .tag(url) // 添加标签以便取消
                .headers(jsonToHeaders(headers))
                .build().also { request ->
                    // 请求发生
                    httpClient.newCall(request).enqueue(object : Callback {

                        override fun onFailure(call: Call, e: IOException) {
                            // 处理请求失败
                            callback(
                                ResponseData(
                                    response = Response.Builder()
                                        .request(request)
                                        .code(0) // HTTP status code
                                        .message(e.message.toString()) // HTTP status message
                                        .body("""{"success":false}""".toResponseBody("application/json".toMediaType()))
                                        .addHeader("Content-Type", "application/json")
                                        .build(), err = e, success = false
                                )
                            )
                        }

                        override fun onResponse(call: Call, response: Response) {
                            response.use {
                                if (!response.isSuccessful) callback(
                                    ResponseData(
                                        response,
                                        err = throw IOException("Unexpected code $response"),
                                        success = false
                                    )
                                )
                                callback(ResponseData(it, success = it.isSuccessful))
                            }
                        }
                    })
                }
        } catch (e: Exception) {
            callback(ResponseData(err = e, success = false))
        }

    }

    // 同步请求发送
    fun get(url: String, headers: JSONObject? = null): ResponseData {
        return try {
            httpClient.newCall(
                Request.Builder()
                    .url(url)
                    .tag(url) // 添加标签以便取消
                    .headers(jsonToHeaders(headers))
                    .build()
            ).execute().let {
                ResponseData(it, success = it.isSuccessful)
            }
        } catch (e: IOException) {
            ResponseData(err = e, success = false)
        }
    }


    // 取消所有请求
    fun cancelAll() {
        httpClient.dispatcher.cancelAll()
    }

    // 根据标签取消特定请求
    fun cancel(tag: String) {
        // httpClient.dispatcher.cancel(tag)
    }

    // Json转为header
    fun jsonToHeaders(jsonObject: JSONObject?): Headers {
        val headersBuilder = Headers.Builder()
        jsonObject?.also {
            it.keys().forEach { key ->
                val value = it.getString(key)
                headersBuilder.add(key, value)
            }
        }
        return headersBuilder.build()
    }
}