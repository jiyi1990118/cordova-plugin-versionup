package versionUp.config


import versionUp.utils.Global.context
import versionUp.utils.PathsHelper

object SourceNameConfig {
    // 插件目录（用于存储版本目录的文件夹）
    val pluginFolder = "versionUp"

    // 版本配置
    val configFileName = "chcp.json"

    // 版本文件清单
    val manifestFileName = "chcp.manifest"

    // 应用主要内容资源文件夹
    val wwwContentFolder = "www"

    // 本地静态目录下的www资源文件夹
    val assetsWwwFolder = "assets/$wwwContentFolder"

    // 版本json配置基于www基础路径
    val configFileBasePath: String
        get() = "$wwwContentFolder/$configFileName"

    // 版本文件清单基于www基础路径
    val manifestFileBasePath: String
        get() = "$wwwContentFolder/$manifestFileName"

    // 扩展存储中插件存储目录
    val externalFolder: String
        get() = PathsHelper.join(context.filesDir.absolutePath, pluginFolder);

    // 扩展存储中当前版本存储目录
    val contentFolder: String
        // 获取应用程序的内部存储文件夹的绝对路径
        get() = PathsHelper.join(externalFolder, ApplicationConfig.currentVersion);

    // 扩展存储中当前版本www存储目录
    val externalWwwFolder: String
        get() = PathsHelper.join(contentFolder, wwwContentFolder)

    // 扩展存储中当前版本的版本配置json文件路径
    val externalConfigPath: String
        get() = PathsHelper.join(externalWwwFolder, configFileName)

    // 扩展存储中当前版本的版本配置manifest文件路径
    val externalManifestPath: String
        get() = PathsHelper.join(externalWwwFolder, manifestFileName)


}
