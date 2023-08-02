package com.domanskii.homealarmbot

import com.domanskii.homealarmbot.clients.HttpImageAuth
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

    val tgToken = System.getenv("TELEGRAM_TOKEN")
    val imageUrl = System.getenv("IMAGE_URL")
    val imageUser = System.getenv("IMAGE_USER")
    val imagePassword = System.getenv("IMAGE_PASSWORD")
    val imageAuth = System.getenv("IMAGE_AUTH")
    val rtspUrl = System.getenv("RTSP_URL")
    val rtspUser = System.getenv("RTSP_USER")
    val rtspPassword = System.getenv("RTSP_PASSWORD")
    val rtspClipLength = System.getenv("RTSP_CLIP_LENGTH")
    val mqttAddress = System.getenv("MQTT_ADDRESS")
    val mqttUser = System.getenv("MQTT_USER")
    val mqttPassword = System.getenv("MQTT_PASSWORD")
    val mqttClientId = System.getenv("MQTT_CLIENT_ID")
    val usersList = System.getenv("USERS_LIST").split(",")

    assert(tgToken.isNotBlank())
    assert(mqttAddress.isNotBlank())
    assert(mqttUser.isNotBlank())
    assert(mqttPassword.isNotBlank())
    assert(mqttClientId.isNotBlank())
    assert(usersList.isNotEmpty())

    if (imageUrl.isNotBlank()) {
        assert(HttpImageAuth.values().any { it.name == imageAuth })
    }

    if (imageAuth != HttpImageAuth.NONE.name) {
        assert(imageUser.isNotBlank())
        assert(imagePassword.isNotBlank())
    }

    if (rtspUser.isNotBlank() || rtspPassword.isNotBlank()) {
        assert(rtspUser.isNotBlank())
        assert(rtspPassword.isNotBlank())
    }

    if (rtspClipLength.isNotBlank()) {
        assert(rtspClipLength.toIntOrNull() != null)
    }
}