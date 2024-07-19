package versionUp.config

import android.content.Context
import org.apache.cordova.ConfigXmlParser
import org.apache.cordova.LOG
import org.json.JSONObject
import versionUp.utils.Global
import versionUp.utils.Global.context
import versionUp.utils.PathsHelper
import versionUp.utils.toJson
import java.io.File

object ApplicationConfig {

    // 存储文件名称
    private var PREF_FILE_NAME = "ApplicationConfig"

    // 配置存储key
    private var PREF_KEY = "versionInfo"

    // 启动文件路径
    val launchBaseFilePath: String
        get() = ConfigXmlParser().let {
            it.parse(context)
            it.launchUrl.replace("file:///android_asset/www", "")
                .replace("https://localhost", "")
        }

    // 外部存储启动地址
    val externalLaunchUrl: String
        get() = "file://" + PathsHelper.join(SourceNameConfig.externalWwwFolder, launchBaseFilePath)

    // 配置字符串
    var configString: String? = null
        private set

    // 更新配置文件 `chcp.json` 路径
    var configFileUrl: String? = null
        private set

    // 请求的header
    var requestHeader: JSONObject? = null
        private set


    // 更新模式
    var updateModel: ActionModel? = null
        private set

    // 安装模式
    var installModel: ActionModel? = null
        private set

    // 当前版本
    var currentVersion: String? = null
        private set

    // 上次版本
    var previousVersion: String? = null
        private set

    // 准备安装的版本
    var readyInstallVersion: String? = null
        private set

    // 应用配置数据存储
    var sharedPreferences = context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)

    // 初始化 获取缓存中的版本信息
    init {
        // 同步存储数据到内存中
        syncStoreToMemory()
    }

    // 同步存储数据到内存中
    private fun syncStoreToMemory() {
        // 获取版本信息
        sharedPreferences.getString(PREF_KEY, null).takeIf {
            // 如果为空，则使用本地应用静态目录中的配置
            if (it.isNullOrEmpty()) {
                // 存储中无数据时候则取静态文件中的版本
                currentVersion = LocalSourceManage.localAssetsConfig?.version
                false
            } else {
                true
            }
        }?.also {
            it.toJson<JSONObject>()?.apply {
                // 更新模式
                updateModel = optString("updateModel").takeIf { it.isNotEmpty() }
                    ?.let { ActionModel.into(it) }
                // 安装模式
                installModel = optString("installModel").takeIf { it.isNotEmpty() }
                    ?.let { ActionModel.into(it) }
                // 当前版本
                currentVersion = optString("currentVersion").takeIf { it.isNotEmpty() }
                    ?: LocalSourceManage.localConfig?.version
                // 上次版本
                previousVersion = optString("previousVersion").takeIf { it.isNotEmpty() }
                // 准备安装的版本
                readyInstallVersion = optString("readyInstallVersion").takeIf { it.isNotEmpty() }
            }
        }

    }

    // 同步内存中数据到存储
    fun syncMemoryToStore() {
        sharedPreferences.edit().apply {
            putString(PREF_KEY, ApplicationConfig.toString())
        }.commit()
    }

    // 设置更新模式
    fun setUpdateModel(model: ActionModel? = null) {
        updateModel = model
    }

    // 设置安装模式
    fun setInstallModel(model: ActionModel? = null) {
        installModel = model
    }

    // 设置当前版本
    fun setCurrentVersion(version: String? = null) {
        currentVersion = version
    }

    // 设置上次版本
    fun setPreviousVersion(version: String? = null) {
        previousVersion = version
    }

    // 设置准备安装的版本
    fun setReadyInstallVersion(version: String? = null) {
        readyInstallVersion = version
    }

    override fun toString(): String {
        // 整合数据
        var jsonObject = JSONObject();
        // 转换数据
        return jsonObject.apply {
            // 更新模式
            put("updateModel", updateModel)
            // 安装模式
            put("installModel", installModel)
            // 当前版本
            put("currentVersion", currentVersion)
            // 上次版本
            put("previousVersion", previousVersion)
            // 准备安装的版本
            put("readyInstallVersion", readyInstallVersion)
        }.toString()
    }

    // 设置请求header
    fun setRequestHeaders(headers: String?) {
        if (!headers.isNullOrEmpty()) {
            requestHeader = headers.toJson();
        }
    }

    // 设置configFileUrl (chcp.json)
    fun setConfigFileUrl(url: String?) {
        if (!url.isNullOrEmpty()) {
            configFileUrl = url
        }
    }

    // 将用户重定向到位于外部存储而非 assets 文件夹中的页面
    fun redirectToLocalStorageIndexPage() {
        // 检查www目录是否安装
        if (!LocalSourceManage.isWwwFolderExists) return

        // 对路径进行提取检查，并进行重新跳转到本地对应版本目录中
        externalLaunchUrl.replace("file://", "").also {
            // 检查即将需要跳转的路径文件是否存在
            if (!File(it).exists()) {
                // 找不到外部起始页。正在中止页面更改。
                LOG.d(
                    "versionUp",
                    "External starting page not found. Aborting page change.$it"
                );
            } else {
                LOG.i("versionUp", "Redirecting to local starting page: $externalLaunchUrl");
                // 跳转到本地存储中的启动页面
                Global.webView.loadUrlIntoView(externalLaunchUrl, false);
            }
        }
    }

    // 检查是否有更新
    fun checkUpdate(resCallback: ((Boolean, VersionDiffData?) -> Unit)? = null) {
        // 获取服务端配置
        ServerSourceManage.getServerConfig { content ->
            // 服务端配置不为空,且本地配置不为空
            if (content !== null && LocalSourceManage.localConfig !== null) {
                // 服务端与本地配置diff
                content.diffToLocal(LocalSourceManage.localConfig!!).also {
                    resCallback?.invoke(it.isUpdate(), it)
                }
            } else {
                resCallback?.invoke(false, null)
            }
        }
    }
}

