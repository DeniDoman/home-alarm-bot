package com.domanskii.homealarmbot

enum class BotCommands(val message: String) {
    ENABLED_AUTO("✅ ENABLED_AUTO"),
    ENABLED_MANUAL("ENABLED_MANUAL"),
    DISABLED_AUTO("❎ DISABLED_AUTO"),
    DISABLED_MANUAL("❌ DISABLED_MANUAL"),
    ALARM_AUTO("ALARM_AUTO"),
    ALARM_MANUAL("ALARM_MANUAL"),
    GET_STATE("GET_STATE");

    companion object {
        fun from(message: String): BotCommands? = BotCommands.values().firstOrNull { it.message == message }
    }
}
