package versionUp.config

import org.json.JSONArray
import org.json.JSONObject
import versionUp.utils.HttpHelper
import versionUp.utils.toJson

object ServerSourceManage {

    val serverConfigUrl = ApplicationConfig.configFileUrl
        ?: LocalSourceManage.localConfig?.configUrl

    // 获取服务端版本配置
    suspend fun getServerConfig(customUrl: String? = null): VersionConfigData? {
        val url = customUrl ?: serverConfigUrl
        return url?.let {
            HttpHelper.getContentString(url)?.let { VersionConfigData.new(it) }
        }
    }

    // 获取服务端版本配置
    fun getServerConfig(customUrl: String? = null, callback: (VersionConfigData?) -> Unit) {
        val url = customUrl ?: serverConfigUrl
        if (url !== null) {
            HttpHelper.get(url) { responseData ->
                callback(responseData.response?.body?.let {
                    VersionConfigData.new(it.string())
                })
            }
        } else {
            callback(null)
        }
    }

    // 回调的方式获取服务端文件清单
    fun getServerManifest(manifestUrl: String, callback: (JSONObject?) -> Unit) {
        // 队列的方式请求
        HttpHelper.get(manifestUrl) { responseData ->
            callback(responseData.response?.body?.let {
                it.string().toJson()
            })
        }
    }

    // 协程方式获取服务端文件清单
    suspend fun getServerManifest(manifestUrl: String): JSONArray? =
        HttpHelper.getContentString(manifestUrl)?.let { it.toJson() }
}