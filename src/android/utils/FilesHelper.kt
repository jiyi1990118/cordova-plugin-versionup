package versionUp.utils

import java.io.*
import java.security.MessageDigest

object FilesHelper {
    // 确保文件目录存在
    fun ensureDirectoryExists(dirPath: String) = ensureDirectoryExists(File(dirPath))

    // 通过文件对象来确保文件目录存在
    fun ensureDirectoryExists(file: File) {
        // 检查文件是否存在或文件不是目录则进行创建
        if (!file.exists() || !file.isDirectory) {
            file.mkdirs()
        }
    }

    // 拷贝文件或者文件夹
    @Throws(IOException::class)
    fun copy(fromFile: String, toFile: String) = copy(File(fromFile), File(toFile))

    // 拷贝文件或者文件夹
    @Throws(IOException::class)
    fun copy(fromFile: File, toFile: File) {
        if (fromFile.isDirectory) {
            // 通过文件对象来确保文件目录存在
            ensureDirectoryExists(toFile);
            // 获取目录下所有文件
            val filesList = fromFile.list();
            for (file in filesList) {
                copy(File(fromFile, file), File(toFile, file));
            }
        } else {
            copyFile(fromFile, toFile)
        }
    }

    // 对文件进行拷贝
    @Throws(IOException::class)
    fun copyFile(fromFile: String, toFile: String) = copyFile(File(fromFile), File(toFile))

    // 对文件进行拷贝
    @Throws(IOException::class)
    fun copyFile(fromFile: File, toFile: File) {
        FileInputStream(fromFile).use { input ->
            // 通过文件对象来确保文件目录存在
            ensureDirectoryExists(toFile.parentFile);
            // BufferedOutputStream 通常比直接使用 FileOutputStream 效率更高，特别是在写入大量数据时，因为减少了对底层文件系统的频繁访问。
            BufferedOutputStream(FileOutputStream(toFile)).use { output ->
                val buf = ByteArray(DEFAULT_BUFFER_SIZE)
                var bytesRead: Int
                while (input.read(buf).also { bytesRead = it } != -1) {
                    output.write(buf, 0, bytesRead)
                }
            }
        }
    }

    // 从文件路径中以字符串形式读取数据
    @Throws(IOException::class)
    fun readFile(filePath: String) = readFile(File(filePath))

    // 从文件中以字符串形式读取数据
    @Throws(IOException::class)
    fun readFile(file: File): String {
        // 根据文件创建文件、缓冲读取器
        return BufferedReader(FileReader(file)).use { bufferedReader ->
            val content = StringBuilder()
            var line: String?
            while (bufferedReader.readLine().also { line = it } != null) {
                content.append(line).append("\n")
            }
            content.toString().trim()
        }
    }

    // 把字符串写入文件
    @Throws(IOException::class)
    fun writeFile(content: String, filePath: String) = writeFile(content, File(filePath))

    // 把字符串写入文件
    @Throws(IOException::class)
    fun writeFile(content: String, file: File) {
        BufferedWriter(FileWriter(file)).use {
            it.write(content)
        }
    }

    // 删除指定目录及其中所有文件
    fun delete(directoryPath: String) = delete(File(directoryPath))

    // 删除目录及其中所有文件
    fun delete(directoryFile: File) {
        if (!directoryFile.exists()) {
            return
        }

        if (directoryFile.isDirectory) {
            val filesList = directoryFile.listFiles()
            if (filesList != null) {
                for (child in filesList) {
                    delete(child)
                }
            }
        }

        // 可以在某些情况下减少文件被其他进程锁定的风险
        val to = File(directoryFile.absolutePath + System.currentTimeMillis())
        directoryFile.renameTo(to)
        to.delete()
    }

    // 计算指定路径的文件Md5值
    fun calculateFileHash(filePath: String) = calculateFileHash(File(filePath))

    // 计算文件Md5值
    fun calculateFileHash(file: File): String {
        val md5 = MessageDigest.getInstance("MD5")
        val inputStream = FileInputStream(file)
        try {
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var bytesRead = inputStream.read(buffer)
            while (bytesRead != -1) {
                md5.update(buffer, 0, bytesRead)
                bytesRead = inputStream.read(buffer)
            }
        } finally {
            inputStream.close()
        }

        val hashBytes = md5.digest()
        val hexString = StringBuilder(hashBytes.size * 2)
        for (byte in hashBytes) {
            val hex = String.format("%02x", byte)
            hexString.append(hex)
        }
        return hexString.toString()
    }


}