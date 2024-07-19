package versionUp


// 使用kotlin协程

// inner lib
// cordova相关

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import org.apache.cordova.CallbackContext
import org.apache.cordova.CordovaInterface
import org.apache.cordova.CordovaPlugin
import org.apache.cordova.CordovaWebView
import org.apache.cordova.LOG
import org.apache.cordova.PluginResult
import org.json.JSONArray
import org.json.JSONObject
import versionUp.config.ApplicationConfig
import versionUp.config.VersionDiffData
import versionUp.utils.Global


/**
 * 这个类说明
 */
class VersionUp : CordovaPlugin() {

    // 变量懒加载
    private val activity: AppCompatActivity by lazy {
        cordova.activity
    }

    // 生命周期顺序 1
    init {
        LOG.i("versionUp", "插件 init");
    }

    // 生命周期顺序 2
    // initialize 方法是 Cordova 中的一个生命周期方法，用于初始化 Cordova 的 CordovaWebView 和 CordovaInterface。该方法通常在插件类的基类 CordovaPlugin 中被调用。
    // 主要特点：
    // 在每次创建 WebView 时调用。
    // 适合用于需要在每次 WebView 创建时执行的初始化代码。
    override fun initialize(cordova: CordovaInterface?, webView: CordovaWebView?) {
        super.initialize(cordova, webView)

        LOG.i("versionUp", "插件 initialize");
    }

    // 生命周期顺序 3
    // pluginInitialize 是 Cordova 插件类中的一个方法。这个方法会在插件实例化时调用，通常用于插件的初始化操作。
    // 根目录下plugin.xml的文件中 新增属性 onload 并且设置为true,会自动初始化
    // 主要特点：
    // 仅在插件首次创建时调用一次。
    // 适合用于执行一次性的初始化代码。
    override fun pluginInitialize() {
        super.pluginInitialize()
        // 初始化全局数据
        Global.init(this);

        // 获取config_file、request_headers 参数
        // 获取安装参数，如果未提供则使用默认值
        var requestHeaders = preferences.getString("request_headers", "");
        var configFileUrl = preferences.getString("config_file", "");

        // 对应用配置进行保存
        ApplicationConfig.setConfigFileUrl(configFileUrl);
        ApplicationConfig.setRequestHeaders(requestHeaders);

        // 应用配置同步到本地存储中
        ApplicationConfig.syncMemoryToStore()
    }


    // 生命周期顺序 4
    // 这个方法在Activity 由不可见变为可见的时候调用。
    override fun onStart() {
        super.onStart()
        LOG.i("versionUp", "插件 start");

        // 将用户重定向到位于外部存储而非 assets 文件夹中的页面
        ApplicationConfig.redirectToLocalStorageIndexPage();
    }

    // 前台生存期
    // 这个方法在Activity 准备好和用户进行交互的时候调用。
    // 此时的Activity 定位于返回栈的栈顶，并且处于运行状态
    override fun onResume(multitasking: Boolean) {
        super.onResume(multitasking)
        LOG.i("versionUp", "插件 Resume");
    }

    // 执行插件
    override fun execute(
        action: String, args: JSONArray, callbackContext: CallbackContext
    ): Boolean {

        // 插件方法执行
        LOG.i("versionUp", "$action 方法被执行");

        return when (action) {
            // 获取版信息
            "getVersionInfo" -> getVersionInfo(callbackContext)
            // 检查更新
            "checkUpdate" -> {
                val isForceUpdate = args.optString(0, "").let {
                    when (it) {
                        "true" -> true
                        "false" -> false
                        else -> null
                    }
                }
                val isForceInstall = args.optString(1, "").let {
                    when (it) {
                        "true" -> true
                        "false" -> false
                        else -> null
                    }
                }

                LOG.i(
                    "versionUp",
                    "checkUpdate isForceUpdate:$isForceUpdate isForceInstall:$isForceInstall"
                )
                // 检查是否有更新
                ApplicationConfig.checkUpdate { isUpdate, diffData ->
                    // 返回是否有更新
                    callbackContext.sendPluginResult(
                        PluginResult(
                            PluginResult.Status.OK,
                            JSONObject(
                                """{
                                        "cmd": "checkUpdate",
                                        "isUpdate": $isUpdate
                                    }"""
                            )
                        ).also { it.keepCallback = true }
                    )

                    if (diffData != null) {
                        if (isUpdate && (isForceUpdate == true || isForceInstall == true)) {
                            LOG.i(
                                "versionUp",
                                "checkUpdate isUpdate:$isUpdate localVersionData:${diffData.localVersionData.version} serverVersionData:${diffData.serverVersionData.version}"
                            );
                            // 进行更新下载
                            diffData.update(
                                isForceInstall = isForceInstall,
                                progressCallback = { responseData, _ ->
                                    // 返回更新进度
                                    callbackContext.sendPluginResult(
                                        PluginResult(
                                            PluginResult.Status.OK,
                                            JSONObject(
                                                """{
                                                "cmd": "progress",
                                                "progress": ${responseData.progress},
                                                "updateAmount": ${responseData.taskSize},
                                                "completedAmount": ${responseData.progressSize},
                                                "localVersionData":${diffData.localVersionData.version},
                                                "serverVersionData":${diffData.serverVersionData.version}
                                            }"""
                                            )
                                        ).also { it.keepCallback = true }
                                    )
                                },
                                updateCallback = { updateStatus, responseData ->
                                    // 返回更新状态
                                    callbackContext.sendPluginResult(
                                        PluginResult(
                                            PluginResult.Status.OK,
                                            JSONObject(
                                                """{
                                                "cmd": "update",
                                                "updateStatus": ${updateStatus >= VersionDiffData.UpdateStatus.Success},
                                                "updateCode": $updateStatus,
                                                "progress": ${responseData.progress},
                                                "updateAmount": ${responseData.taskSize},
                                                "completedAmount": ${responseData.progressSize},
                                                "localVersionData":${diffData.localVersionData.version},
                                                "serverVersionData":${diffData.serverVersionData.version}
                                            }"""
                                            )
                                        ).also { it.keepCallback = true }
                                    )
                                },
                                installCallback = { installStatus, responseData ->
                                    LOG.i(
                                        "versionUp",
                                        "installCallback: ${installStatus.toString()}"
                                    )
                                    // 返回安装状态
                                    callbackContext.sendPluginResult(
                                        PluginResult(
                                            PluginResult.Status.OK,
                                            JSONObject(
                                                """{
                                                "cmd": "install",
                                                "installStatus": ${installStatus == VersionDiffData.UpdateStatus.Installed},
                                                "installCode": $installStatus,
                                                "progress": ${responseData.progress},
                                                "updateAmount": ${responseData.taskSize},
                                                "completedAmount": ${responseData.progressSize},
                                                "localVersionData":${diffData.localVersionData.version},
                                                "serverVersionData":${diffData.serverVersionData.version}
                                            }"""
                                            )
                                        ).also { it.keepCallback = true }
                                    )
                                })

                            // 检查是否强制更新
                        } else if (isForceUpdate == true) {
                            var updateStatus = diffData.updateStatus;
                            // 返回更新状态
                            callbackContext.sendPluginResult(
                                PluginResult(
                                    PluginResult.Status.OK,
                                    JSONObject(
                                        """{
                                                "cmd": "update",
                                                "updateStatus": ${updateStatus >= VersionDiffData.UpdateStatus.Success},
                                                "updateCode": $updateStatus,
                                                "progress": ${diffData.progress},
                                                "updateAmount": ${diffData.taskSize},
                                                "completedAmount": ${diffData.progressSize},
                                                "localVersionData":${diffData.localVersionData.version},
                                                "serverVersionData":${diffData.serverVersionData.version}
                                            }"""
                                    )
                                ).also { it.keepCallback = true }
                            )

                            // 检查是否强制安装
                        } else if (isForceInstall == true) {

                            // 设置默认状态
                            var updateStatus = diffData.updateStatus;

                            // 检查是否有待更新的版本
                            if (ApplicationConfig.readyInstallVersion == ApplicationConfig.currentVersion) {
                                diffData.restart()
                                updateStatus = VersionDiffData.UpdateStatus.Success
                            }

                            // 返回安装状态
                            callbackContext.sendPluginResult(
                                PluginResult(
                                    PluginResult.Status.OK,
                                    JSONObject(
                                        """{
                                                "cmd": "install",
                                                "installStatus": ${updateStatus >= VersionDiffData.UpdateStatus.Success},
                                                "installCode": ${diffData.updateStatus},
                                                "progress": ${diffData.progress},
                                                "updateAmount": ${diffData.taskSize},
                                                "completedAmount": ${diffData.progressSize},
                                                "localVersionData":${diffData.localVersionData.version},
                                                "serverVersionData":${diffData.serverVersionData.version}
                                            }"""
                                    )
                                ).also { it.keepCallback = true }
                            )
                        }

                    }


                }
                true
            }

            else -> {
                callbackContext.error("versionUp插件中未找到 [ $action ] 方法！")
                false
            }
        }
    }

    // 获取版本信息
    private fun getVersionInfo(callbackContext: CallbackContext): Boolean {
        var applicationInfo = activity.applicationInfo;
        // 获取包信息
        var packageInfo = activity.packageManager.getPackageInfo(activity.packageName, 0)
        // 获取应用权限信息
        val permissions = activity.packageManager.getPackageInfo(
            activity.applicationInfo.packageName, PackageManager.GET_PERMISSIONS
        ).requestedPermissions?.let { JSONArray(it) } ?: JSONArray()


        // 返回版本相关信息
        callbackContext.success(
            JSONObject(
                """
                    {
                        "versionCode": ${packageInfo.versionCode},
                        "versionName": ${packageInfo.versionName},
                        "className": ${applicationInfo.className},
                        "packageName": ${applicationInfo.packageName},
                        "sourceDir": "${applicationInfo.sourceDir}",
                        "dataDir": "${applicationInfo.dataDir}",
                        "nativeLibraryDir": "${applicationInfo.nativeLibraryDir}",
                        "publicSourceDir": "${applicationInfo.publicSourceDir}",
                        "targetSdkVersion": ${applicationInfo.targetSdkVersion},
                        "filesDir": "${activity.filesDir.absolutePath}",
                        "localVersion": ${ApplicationConfig.currentVersion},
                        "previousVersion": ${ApplicationConfig.previousVersion}
                    }
                    """
            ).also {
                it.put("permissions", permissions)
            }
        )
        return true
    }


    override fun onDestroy() {
        super.onDestroy()
    }
}
