package com.domanskii.homealarmbot

import com.domanskii.homealarmbot.clients.MqttCustomClient
import com.domanskii.homealarmbot.messagebus.MessageBus
import mu.KotlinLogging


private val log = KotlinLogging.logger {}

class App {
    fun start() {
        assertEnvVariables()
        val botToken: String = System.getenv("TELEGRAM_TOKEN")

        val messageBus = MessageBus()
        val main = HomeAlarm(botToken, messageBus)
        val mqtt = MqttCustomClient(messageBus)

        mqtt.connect()
        main.registerBot()
    }
}

fun main() {
    log.info { "Application starting..." }
    App().start()
    log.info { "Application started" }
}

fun assertEnvVariables() {
    log.debug { "Asserting ENV variables..." }

    val tgToken = System.getenv("TELEGRAM_TOKEN") ?: ""
    val chatsList = System.getenv("CHATS_LIST").split(",")
    val alertInterval = System.getenv("ALERT_INTERVAL") ?: ""
    val mqttAddress = System.getenv("MQTT_ADDRESS") ?: ""
    val mqttUser = System.getenv("MQTT_USER") ?: ""
    val mqttPassword = System.getenv("MQTT_PASSWORD") ?: ""
    val mqttClientId = System.getenv("MQTT_CLIENT_ID") ?: ""
    val rtspUrl = System.getenv("RTSP_URL") ?: ""
    val rtspUser = System.getenv("RTSP_USER") ?: ""
    val rtspPassword = System.getenv("RTSP_PASSWORD") ?: ""
    val rtspClipLength = System.getenv("RTSP_CLIP_LENGTH") ?: ""
    val rtspImageInterval = System.getenv("RTSP_IMAGE_INTERVAL") ?: ""

    assert(tgToken.isNotBlank())
    assert(chatsList.isNotEmpty())
    assert(mqttAddress.isNotBlank())
    assert(mqttUser.isNotBlank())
    assert(mqttPassword.isNotBlank())
    assert(mqttClientId.isNotBlank())

    if (alertInterval.isNotBlank()) {
        assert(alertInterval.toIntOrNull() != null)
    }

    if (rtspUrl.isNotBlank()) {
        if (rtspUser.isNotBlank() || rtspPassword.isNotBlank()) {
            assert(rtspUser.isNotBlank())
            assert(rtspPassword.isNotBlank())
        }

        if (rtspClipLength.isNotBlank()) {
            assert(rtspClipLength.toIntOrNull() != null)
        }

        if (rtspImageInterval.isNotBlank()) {
            assert(rtspImageInterval.toIntOrNull() != null)
        }
    }
}