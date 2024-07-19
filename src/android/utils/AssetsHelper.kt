package versionUp.utils


import org.apache.cordova.LOG
import java.io.*
import java.util.Enumeration
import java.util.jar.JarEntry
import java.util.jar.JarFile

// 静态资源处理工具
object AssetsHelper {

    // 拷贝指定静态资源到指定的目录中
    fun copyAssets(assetsDir: String, toDir: String, isOverwrite: Boolean = true) {
        val prefixLength = assetsDir.length;
        // 获取apk中资源文件
        val jarFile: JarFile = JarFile(Global.context.applicationInfo.sourceDir);
        // jar文件迭代器
        val filesEnumeration: Enumeration<JarEntry> = jarFile.entries();
        // 检查集合中是否还有更多元素
        while (filesEnumeration.hasMoreElements()) {
            filesEnumeration.nextElement().apply {
                // 检查是否不是目录 且是assetsDir开头的
                if (!isDirectory && name.startsWith(assetsDir)) {
                    val targetPath = PathsHelper.join(toDir, name.substring(prefixLength));
                    // 检查文件是否存在，如果不存在或使用覆盖 则拷贝
                    if (!File(targetPath).exists() || isOverwrite) {
                        // 进行文件拷贝
                        writeStreamToFile(
                            jarFile.getInputStream(this),
                            targetPath
                        )
                    }
                }
            }
        }
        jarFile.close()
    }

    // 流文件写入指定路径文件中
    fun writeStreamToFile(inputStream: InputStream, targetPath: String) {
        // 检查文件夹是否存在，不存在则子目录一起创建
        FilesHelper.ensureDirectoryExists(File(targetPath).parentFile)
        inputStream.use { input ->
            // 创建输出的文件流
            // (BufferedOutputStream 通常比直接使用 FileOutputStream 效率更高，特别是在写入大量数据时，因为减少了对底层文件系统的频繁访问。)
            BufferedOutputStream(FileOutputStream(targetPath)).use { output ->
                val buf = ByteArray(DEFAULT_BUFFER_SIZE)
                var bytesRead: Int
                while (input.read(buf).also { bytesRead = it } != -1) {
                    output.write(buf, 0, bytesRead)
                }
            }
        }
    }


    // 从本地加载静态资源
    fun configFromAssets(filePath: String): String? {
        var content: String? = null
        // 静态资源管理
        val assetManager = Global.context.resources.assets
        // 资源读取
        try {
            // 打开配置文件的资源流
            val inputStreamReader = InputStreamReader(assetManager.open(filePath))
            // 把流数据读取到缓冲区
            content = BufferedReader(inputStreamReader).let { reader ->
                val content = StringBuilder()
                // 读取流中一行数据
                var line = reader.readLine()
                // 逐行读取内容，如果到达流的末尾，返回 null。
                while (line != null) {
                    content.append(line).append('\n')
                    line = reader.readLine()
                }
                // 关闭读取器
                reader.close()
                content.toString()
            }
        } catch (e: Exception) {
            LOG.d("versionUp", "Failed to read $filePath from assets", e)
        }

        return content
    }

}