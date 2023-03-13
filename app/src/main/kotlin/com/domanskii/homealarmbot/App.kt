package com.domanskii.homealarmbot

import com.domanskii.homealarmbot.messagebus.MessageBus
import mu.KotlinLogging
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession

private val log = KotlinLogging.logger {}

class App {
    fun start() {
        assertEnvVariables()
        val botToken: String = System.getenv("TELEGRAM_TOKEN")

        val messageBus = MessageBus()
        val botsApi = TelegramBotsApi(DefaultBotSession::class.java)
        val alarmBot = HomeAlarmBot(botToken, messageBus)
        val mqtt = MqttCustomClient(messageBus)

        mqtt.connect()
        botsApi.registerBot(alarmBot)
    }
}

fun main() {
    log.info { "Application starting..." }
    App().start()
    log.info { "Application started" }
}

fun assertEnvVariables() {
    val tgToken = System.getenv("TELEGRAM_TOKEN")
    val imageUrl = System.getenv("IMAGE_URL")
    val imageUser = System.getenv("IMAGE_USER")
    val imagePassword = System.getenv("IMAGE_PASSWORD")
    val imageAuth = System.getenv("IMAGE_AUTH")
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
}