package versionUp.config

// 更新模式
enum class ActionModel {
    // 启动时候
    START,

    // 从后台切换到前台时候
    RESUME,

    // 立即
    NOW,

    // 未设置
    NONE;

    companion object {
        // 把字符串转为枚举值
        fun into(str: String): ActionModel {
            return when (str.uppercase()) {
                "START" -> START
                "RESUME" -> RESUME
                "NOW" -> NOW
                "NONE" -> NONE
                else -> NONE
            }
        }
    }

    // 把枚举值转化成字符串
    override fun toString(): String {
        return this.name.lowercase()
    }
}
