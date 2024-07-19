package versionUp.config

import org.apache.cordova.LOG
import org.json.JSONObject
import versionUp.utils.*

// 版本配置数据
class VersionConfigData private constructor(
    var contentUrl: String,
    var version: String,
    var update: ActionModel,
    var install: ActionModel,
) {
    // 配置中的chcp.json配置
    var configUrl: String = "${this.contentUrl}/${SourceNameConfig.configFileName}"
        private set

    // manifest 文件清单
    var manifestUrl: String = "${this.contentUrl}/${SourceNameConfig.manifestFileName}"
        private set

    companion object {
        // 根据字符串生成VersionConfig数据对象
        fun new(content: String): VersionConfigData? = content.toJson<JSONObject>()?.run {
            optString("content_url")
            // 检查参数值是否存在
            if (has("content_url") && has("release")) {
                return VersionConfigData(
                    getString("content_url"),
                    getString("release"),
                    ActionModel.into(optString("update")),
                    ActionModel.into(optString("install")),
                )
            } else {
                LOG.e("versionUp", "VersionConfigData Err: contentUrl or release is null")
            }
            return null
        }
    }

    // 版本对比
    fun diffToServer(serverVersionConfigData: VersionConfigData) =
        VersionDiffData(this, serverVersionConfigData)

    // 与本地版本信息进行对比
    fun diffToLocal(localVersionConfigData: VersionConfigData) =
        VersionDiffData(localVersionConfigData, this)

    // 获取服务器上的文件清单
    suspend fun getServerManifest() = ServerSourceManage.getServerManifest(manifestUrl)

    // 获取本地版本的文件清单
    fun getLocalManifest() = LocalSourceManage.getLocalManifest()

}

