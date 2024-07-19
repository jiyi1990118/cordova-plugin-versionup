package versionUp.config

import kotlinx.coroutines.*
import org.apache.cordova.LOG
import org.json.JSONArray
import versionUp.utils.*
import java.io.File
import java.io.IOException


// 本地静态资源管理
object LocalSourceManage {

    // 本地最新版本配置
    val localConfig: VersionConfigData?
        get() = localExternalConfig ?: localAssetsConfig

    // 获取本地静态资源中的版本配置
    val localAssetsConfig =
        AssetsHelper.configFromAssets(SourceNameConfig.configFileBasePath)
            ?.takeIf { it.isNotEmpty() }
            ?.let { VersionConfigData.new(it) }

    // 获取本地扩展存储中的版本配置
    val localExternalConfig: VersionConfigData?
        get() {
            // 检查外部存储中www目录是否安装
            if (isWwwFolderExists) {
                try {
                    return VersionConfigData.new(FilesHelper.readFile(SourceNameConfig.externalConfigPath))
                } catch (e: IOException) {
                    LOG.e(
                        "versionUp",
                        "localExternalConfig Err:${e.message} currentVersion：${ApplicationConfig.currentVersion}"
                    )
                }
            }
            return null
        }

    // 检查 外部存储 www目录是否存在
    val isWwwFolderExists: Boolean
        get() = File(SourceNameConfig.contentFolder).exists()

    init {
        // 检查www是否存在,创建协程进行www目录文件安装（第一次打开app执行处理）
        if (!isWwwFolderExists) CoroutineScope(Dispatchers.Main).launch {
            try {
                withContext(Dispatchers.IO) {
                    installWwwFolder()
                }
            } catch (e: Exception) {
                LOG.e("versionUp", "installWwwFolder ErrMsg: ${e.message}")
            }
        }
    }

    // 将assets中www文件夹安装到外部存储
    fun installWwwFolder() {
        // 检查本地静态资源中www目录不存在，且当前版本与本地配置版本一致（用于区分第一次app安装时候初始化www目录）
        if (!isWwwFolderExists && ApplicationConfig.currentVersion === localConfig?.version) {
            // 拷贝asset目录到应用目录中
            AssetsHelper.copyAssets(
                SourceNameConfig.assetsWwwFolder,
                SourceNameConfig.externalWwwFolder
            )
        }
    }

    // 合并基础文件（避免cordova相关文件缺失）
    fun mergeBaseFile(version: String) {
        // 版本www文件夹路径
        val versionPath = PathsHelper.join(
            SourceNameConfig.externalFolder,
            version,
            SourceNameConfig.wwwContentFolder
        )

        // 拷贝asset目录到应用目录中
        AssetsHelper.copyAssets(
            SourceNameConfig.assetsWwwFolder,
            versionPath,
            false
        )
    }

    fun getLocalManifest(): JSONArray? {
        if (isWwwFolderExists) {
            // 从本地扩展存储中获取版本清单
            try {
                return FilesHelper.readFile(SourceNameConfig.externalManifestPath)
                    .let { it.toJson() }
            } catch (e: IOException) {
                LOG.e("versionUp", "getLocalManifest Err:${e.message}")
                return null
            }
        } else {
            // 获取本地静态资源中的版本清单
            return AssetsHelper.configFromAssets(SourceNameConfig.manifestFileBasePath)
                ?.let { it.toJson() }
        }

    }

    // 清理旧版本缓存
    fun cleanOldVersionCache() {
        // 删除外部存储中的www目录
        FilesHelper.delete(
            PathsHelper.join(
                SourceNameConfig.externalFolder,
                ApplicationConfig.previousVersion
            )
        )
    }

}

