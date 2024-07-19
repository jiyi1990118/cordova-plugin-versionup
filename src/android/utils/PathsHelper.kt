package versionUp.utils

object PathsHelper {

    /**
     * Construct path from the given set of paths.
     * 路径连接
     * @param paths list of paths to concat
     * @return resulting path
     */
    fun join(vararg paths: String?): String {
        return paths.joinToString(separator = "") { normalize(it) }
    }

    fun normalize(path: String?): String {
        // 判断是否空
        if (path.isNullOrEmpty()) return ""
        var normalizedPath = path
        if (!normalizedPath.startsWith("/")) {
            normalizedPath = "/$normalizedPath"
        }

        if (normalizedPath.endsWith("/")) {
            normalizedPath = normalizedPath.dropLast(1)
        }

        return normalizedPath
    }
}