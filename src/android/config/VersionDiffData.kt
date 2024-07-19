package versionUp.config


import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.cordova.LOG
import org.json.JSONArray
import versionUp.utils.FilesHelper
import versionUp.utils.FilesHelper.ensureDirectoryExists
import versionUp.utils.HttpHelper
import versionUp.utils.PathsHelper
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 版本差异数据
 * @param localVersionData 本地版本数据
 * @param serverVersionData 服务器版本数据
 */
class VersionDiffData constructor(
    val localVersionData: VersionConfigData,
    val serverVersionData: VersionConfigData
) {
    // 任务数
    var taskSize = 0
        private set

    // 更新进度
    var progressSize = 0
        private set

    // 更新进度
    var progress: Double = 0.0
        private set

    // 更新状态
    var updateStatus = UpdateStatus.None
        private set

    var tempVersionWwwFolders: String = ""
        private set

    /**
     * 更新状态
     */
    enum class UpdateStatus(val value: Int) {
        None(0),
        Processing(1),
        Fail(4),
        Success(6),
        Installing(7),
        Installed(8);

        // 把枚举值转化成字符串
        override fun toString(): String {
            return this.name.lowercase()
        }

        companion object {
            // 通过值获取枚举常量
            fun fromValue(value: Int): UpdateStatus? {
                return values().find { it.value == value }
            }
        }
    }

    /**
     * 任务类型
     */
    enum class TaskType {
        DOWNLOAD,
        None,
        COPY;

        // 把枚举值转化成字符串
        override fun toString(): String {
            return this.name.lowercase()
        }
    }

    /**
     * 进度项数据
     * @param requestType 请求类型
     * @param originFileUrl 原始文件url
     * @param fileItem 文件项
     * @param isSucceed 是否成功
     */
    data class ProgressItemData(
        val requestType: TaskType,
        val originFileUrl: String,
        val fileItem: FileItem,
        val isSucceed: Boolean = false,
    )

    /**
     * 文件项
     * @param file 文件名
     * @param hash 文件hash
     */
    data class FileItem(val file: String, val hash: String = "")

    /**
     * 版本号比较
     * 比较两个版本号的大小，正数表示“需要更新”，负数表示“本地版本是最新的/无需更新”，0表示版本相同“无需更新”
     * @param newVersionCode 新版本号
     * @param oldVersionCode 旧版本号
     * @return 比较结果
     */
    private fun versionCodeDiff(newVersionCode: String, oldVersionCode: String): Int {
        val pattern = "yyyy.MM.dd-HH.mm.ss"
        val formatter = SimpleDateFormat(pattern, Locale.getDefault())
        val newParsedDate: Date = formatter.parse(newVersionCode)
        val oldParsedDate: Date = formatter.parse(oldVersionCode)

        // 输出结果：正数表示 newParsedDate > oldParsedDate，负数表示 newParsedDate < oldParsedDate，0表示相等
        return newParsedDate.compareTo(oldParsedDate)
    }

    /**
     * 检查是否有更新
     * @param callback 回调函数
     * @param isForceUpdate 是否强制更新
     * @param progressCallback 更新进度回调
     * @param updateCallback 更新回调
     * @param installCallback 安装回调
     * @return 是否有更新
     */
    fun isUpdate(
        callback: ((isUpdate: Boolean) -> Unit)? = null,
        isForceUpdate: Boolean? = null,
        progressCallback: ((responseData: VersionDiffData, itemData: ProgressItemData) -> Unit)? = null,
        updateCallback: ((updateStatus: UpdateStatus, responseData: VersionDiffData) -> Unit)? = null,
        installCallback: ((updateStatus: UpdateStatus, responseData: VersionDiffData) -> Unit)? = null,
    ): Boolean {
        var isUpdate: Boolean = false;
        // 比较版本号是否相等
        if (localVersionData.version === serverVersionData.version) {
            isUpdate = false
        }
        // 比较版本号是否相等
        else if (versionCodeDiff(
                localVersionData.version,
                serverVersionData.version
            ) < 0
        ) {
            isUpdate = true
        }

        if (callback !== null) callback(isUpdate);

        // 发现更新，且开启更新模式为立即更新 或启用强制更新
        if (isUpdate == true && ((isForceUpdate == null && serverVersionData.update === ActionModel.NOW) || isForceUpdate == true)) {
            update(progressCallback, isForceUpdate, updateCallback, installCallback)
        }

        return isUpdate
    }

    /**
     * 内容更新
     * @param progressCallback 更新进度回调
     * @param isForceInstall 是否强制安装
     * @param updateCallback 更新回调
     * @param installCallback 安装回调
     */
    fun update(
        progressCallback: ((responseData: VersionDiffData, itemData: ProgressItemData) -> Unit)? = null,
        isForceInstall: Boolean? = null,
        updateCallback: ((updateStatus: UpdateStatus, responseData: VersionDiffData) -> Unit)? = null,
        installCallback: ((updateStatus: UpdateStatus, responseData: VersionDiffData) -> Unit)? = null,
    ) {
        // 更新状态
        updateStatus = UpdateStatus.Processing
        // 启动协程处理
        CoroutineScope(Dispatchers.Main).launch {
            // 获取服务器上的文件清单
            transformManifestStruct(serverVersionData.getServerManifest())?.let { getServerManifest ->
                // 获取本地的文件清单
                transformManifestStruct(LocalSourceManage.getLocalManifest())?.let { getLocalManifest ->
                    // 获取交集数据
                    val commonItems = getServerManifest.intersect(getLocalManifest).toMutableSet()
                    // 查找getLocalManifest中没有的item
                    var differentItems = (getServerManifest.minus(getLocalManifest)).toMutableSet()

                    // 添加版本相关文件到 differentItems
                    differentItems.add(FileItem(SourceNameConfig.configFileName))
                    differentItems.add(FileItem(SourceNameConfig.manifestFileName))

                    // 返回结果
                    Pair(commonItems, differentItems)
                }
            }?.let { (commonItems, differentItems) ->

                // 任务数量
                taskSize = commonItems.size + differentItems.size;

                // 新版本的www目录
                val newVersionWwwFolders = PathsHelper.join(
                    SourceNameConfig.externalFolder,
                    serverVersionData.version,
                    SourceNameConfig.wwwContentFolder
                )

                // 新版本临时文件的www目录
                tempVersionWwwFolders = PathsHelper.join(
                    SourceNameConfig.externalFolder,
                    serverVersionData.version,
                    "_temp",
                )

                // 使用IO线程进行处理
                withContext(Dispatchers.IO) {
                    // 对交集的文件进行copy
                    commonItems.forEach { fileItem ->
                        val newFile = File(
                            PathsHelper.join(
                                newVersionWwwFolders,
                                fileItem.file
                            )
                        )

                        val nowFile = File(
                            PathsHelper.join(
                                SourceNameConfig.externalWwwFolder,
                                fileItem.file
                            )
                        )

                        // 检查当前版本对应文件是否存在
                        if (!nowFile.exists()) {
                            // 把不存在的文件添加到待下载清单中
                            differentItems.add(fileItem)
                        } else {
                            // 把当前版本存储中的文件拷贝到新的版本存储中
                            FilesHelper.copyFile(nowFile, newFile)
                            // 进度反馈：文件拷贝成功
                            progressHandle(
                                ProgressItemData(
                                    TaskType.COPY,
                                    nowFile.absolutePath,
                                    fileItem,
                                    true
                                ),
                                progressCallback,
                                updateCallback,
                                installCallback,
                                isForceInstall
                            )
                        }
                    }

                    // 对不同的文件进行下载
                    differentItems.forEach { fileItem ->
                        // 拼装网络文件url
                        val fileUrl = PathsHelper.join(serverVersionData.contentUrl, fileItem.file)
                            .removePrefix("/");
                        // 新文件
                        val newFile = File(
                            PathsHelper.join(
                                newVersionWwwFolders,
                                fileItem.file
                            )
                        )

                        // 检查文件是否存在
                        if (!newFile.exists()) {
                            // 临时文件
                            val tempFile = File(
                                PathsHelper.join(
                                    tempVersionWwwFolders,
                                    fileItem.file
                                )
                            )

                            // 通过文件对象来确保文件目录存在
                            ensureDirectoryExists(newFile.parentFile);
                            // 通过文件对象来确保文件目录存在
                            ensureDirectoryExists(tempFile.parentFile);
                            // 下载文件
                            HttpHelper.get(fileUrl) { result ->
                                // 下载成功
                                if (result.success) {
                                    // 写入文件
                                    result.response?.body?.byteStream()?.use { inputStream ->
                                        tempFile.outputStream().use { outputStream ->
                                            inputStream.copyTo(outputStream)
                                        }
                                    }

                                    // 检查文件是否下载成功
                                    if (tempFile.exists()) {
                                        // 检查文件是否存在
                                        if (newFile.exists()) {
                                            // 删除文件
                                            newFile.delete()
                                        }
                                        // 重命名文件
                                        tempFile.renameTo(newFile)
                                        // 进度反馈：文件更新成功
                                        progressHandle(
                                            ProgressItemData(
                                                TaskType.DOWNLOAD,
                                                fileUrl,
                                                fileItem,
                                                true
                                            ),
                                            progressCallback,
                                            updateCallback,
                                            installCallback,
                                            isForceInstall
                                        )
                                    } else {
                                        // 进度反馈：文件创建失败
                                        progressHandle(
                                            ProgressItemData(
                                                TaskType.DOWNLOAD,
                                                fileUrl,
                                                fileItem,
                                                false,
                                            ),
                                            progressCallback,
                                            updateCallback,
                                            installCallback,
                                            isForceInstall
                                        )
                                    }
                                } else {
                                    // 进度反馈：文件下载失败
                                    progressHandle(
                                        ProgressItemData(
                                            TaskType.DOWNLOAD,
                                            fileUrl,
                                            fileItem,
                                            false
                                        ),
                                        progressCallback,
                                        updateCallback,
                                        installCallback,
                                        isForceInstall
                                    )
                                }
                            }

                        } else {
                            // 文件已存在
                            progressHandle(
                                ProgressItemData(
                                    TaskType.None,
                                    fileUrl,
                                    fileItem,
                                    true
                                ),
                                progressCallback,
                                updateCallback,
                                installCallback,
                                isForceInstall
                            )
                        }

                    }

                }
            }
            // end
        }

    }

    /**
     * 更新进度处理
     * @param itemData 进度项数据
     * @param progressCallback 更新进度回调
     * @param updateCallback 更新状态回调
     * @param installCallback 安装回调
     * @param isForceInstall 是否强制安装
     */
    private fun progressHandle(
        itemData: ProgressItemData,
        progressCallback: ((responseData: VersionDiffData, itemData: ProgressItemData) -> Unit)? = null,
        updateCallback: ((updateStatus: UpdateStatus, responseData: VersionDiffData) -> Unit)? = null,
        installCallback: ((updateStatus: UpdateStatus, responseData: VersionDiffData) -> Unit)? = null,
        isForceInstall: Boolean? = null,
    ) {
        if (itemData.isSucceed) {
            progress = if (taskSize > ++progressSize) {
                String.format("%.2f", ((progressSize.toDouble() / taskSize.toDouble()) * 100))
                    .toDouble()
            } else {
                // 更新进度
                updateStatus = UpdateStatus.Success
                // 成功后删除临时文件夹
                if (File(tempVersionWwwFolders).exists()) {
                    FilesHelper.delete(tempVersionWwwFolders)
                }

                // 合并基础文件（避免cordova相关文件缺失）
                LocalSourceManage.mergeBaseFile(serverVersionData.version);
                100.0
            }
        } else {
            // 更新进度
            updateStatus = UpdateStatus.Fail
            // 更新失败
            LOG.i("versionUp", "更新失败");
            updateCallback?.invoke(updateStatus, this)
        }

        // 更新进度回调
        progressCallback?.invoke(this, itemData)

        // 检查更新状态是否成功
        if (updateStatus >= UpdateStatus.Success) {
            // 更新回调
            updateCallback?.invoke(updateStatus, this)
            updateStatus = UpdateStatus.Installing
            // 版本定版
            finalVersion(isForceInstall)
            // 安装模式为立即安装
            if (isForceInstall == true || (isForceInstall == null && serverVersionData.install === ActionModel.NOW)) {
                // 安装回调
                installCallback?.invoke(updateStatus, this)
            }
        }

    }

    /**
     * 定版并更新版本信息
     * @param isForceInstall 是否强制安装
     */
    private fun finalVersion(isForceInstall: Boolean? = null) {
        // 判断是否更新成功
        if (updateStatus >= UpdateStatus.Success) {
            // 设置上次版本号
            ApplicationConfig.setPreviousVersion(localVersionData.version)
            // 设置当前版本号为最新版本号
            ApplicationConfig.setCurrentVersion(serverVersionData.version)
            // 设置即将安装的版本号
            ApplicationConfig.setReadyInstallVersion(serverVersionData.version)
            // 设置更新模式
            ApplicationConfig.setUpdateModel(serverVersionData.update)
            // 设置安装模式
            ApplicationConfig.setInstallModel(serverVersionData.install)

            // 同步本地文件配置
            if (isForceInstall == true || (isForceInstall == null && serverVersionData.install == ActionModel.NOW)) {
                updateStatus = UpdateStatus.Installing
                restart()
                updateStatus = UpdateStatus.Installed
            }
            // 保存配置到存储中
            ApplicationConfig.syncMemoryToStore();
        }
        return
    }

    // 重启应用
    fun restart() {
        // 将用户重定向到位于外部存储而非 assets 文件夹中的页面
        ApplicationConfig.redirectToLocalStorageIndexPage();

        // 检查是否有待更新的版本
        if (ApplicationConfig.readyInstallVersion == ApplicationConfig.currentVersion) {
            // 清理待更新的版本标识
            ApplicationConfig.setReadyInstallVersion()
            // 保存配置到存储中
            ApplicationConfig.syncMemoryToStore();
        }
        // 清理老版本文件
        LocalSourceManage.cleanOldVersionCache()
    }

    /**
     * 转换清单结构
     * @param manifest 清单
     * @return 文件项集合
     */
    private fun transformManifestStruct(manifest: JSONArray?): MutableSet<FileItem>? {
        if (manifest === null) return null;
        val fileItemSet = mutableSetOf<FileItem>()
        for (i in 0 until manifest.length()) {
            val jsonObject = manifest.getJSONObject(i)
            val file = jsonObject.getString("file")
            val hash = jsonObject.getString("hash")
            fileItemSet.add(FileItem(file, hash))
        }
        return fileItemSet
    }


}
