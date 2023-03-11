package com.domanskii.homealarmbot

import com.domanskii.homealarmbot.messagebus.MessageBus
import mu.KotlinLogging
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession

private val log = KotlinLogging.logger {}

class App {
    fun start() {
        val tgToken = System.getenv("TELEGRAM_TOKEN")
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

        val messageBus = MessageBus()
        val botsApi = TelegramBotsApi(DefaultBotSession::class.java)
        val alarmBot = HomeAlarmBot(tgToken, usersList, messageBus)
        val mqtt = MqttCustomClient(mqttAddress, mqttClientId, mqttUser, mqttPassword, messageBus)

        mqtt.connect()
        botsApi.registerBot(alarmBot)
    }
}

fun main() {
    log.info { "Application starting..." }
    App().start()
    log.info { "Application started" }
}
