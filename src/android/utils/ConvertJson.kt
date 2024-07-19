package versionUp.utils

import org.apache.cordova.LOG
import org.json.JSONArray
import org.json.JSONObject

// 字符串转换成JSON
inline fun <reified T> String.toJson(): T? {
    var content = this.trim();
    if (content.isEmpty()) return null;

    return try {
        when {
            T::class == JSONObject::class -> JSONObject(content) as T
            T::class == JSONArray::class -> JSONArray(content) as T
            else -> throw IllegalArgumentException("Unsupported type")
        }

    } catch (e: Exception) {
        LOG.e("versionUp", "Failed to parse JSON to JsonObject: ${e.message}")
        null
    }
}

