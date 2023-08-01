package com.domanskii.homealarmbot

import com.domanskii.homealarmbot.messagebus.MessageBus
import com.domanskii.homealarmbot.messagebus.Observer
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession
import mu.KotlinLogging


private val log = KotlinLogging.logger {}

class Main(botToken: String, private val messageBus: MessageBus) : Observer {
    private val usersList: List<String> = System.getenv("USERS_LIST").split(",")
    private val imageUrl = System.getenv("IMAGE_URL") ?: ""
    private val imageUser = System.getenv("IMAGE_USER") ?: ""
    private val imagePassword = System.getenv("IMAGE_PASSWORD") ?: ""
    private val imageAuth = System.getenv("IMAGE_AUTH") ?: ""

    private val videoUrl = System.getenv("VIDEO_URL") ?: ""
    private val videoUser = System.getenv("VIDEO_USER") ?: ""
    private val videoPassword = System.getenv("VIDEO_PASSWORD") ?: ""

    private val tgClient = TelegramClient(botToken, usersList, ::handleTgMessage)

    private val sendPhotoRunner = SendPhotoRunner(tgClient, imageUrl, imageUser, imagePassword, imageAuth)
    private val sendVideoRunner = SendVideoRunner(tgClient, videoUrl, videoUser, videoPassword)

    private val messageBusIncomeTopic = "messageBus/eventsFromMqtt"
    private val messageBusOutcomeTopic = "messageBus/messagesFromHomeAlarmBot"

    init {
        log.info { "Subscribing to '$messageBusIncomeTopic' MessageBus topic" }
        messageBus.subscribe(messageBusIncomeTopic, this)
    }

    fun registerBot() {
        TelegramBotsApi(DefaultBotSession::class.java).registerBot(tgClient)
    }

    private fun handleTgMessage(id: String, userName: String, chatId: String, message: String) {
        if (!usersList.contains(id)) {
            log.info { "Message from an unknown user '${id}' ignored" }
            return
        }

        log.debug { "Handling message '${message}' from '${userName}' user" }

        var text = "Unknown command. Please use one of the commands below"
        val command = BotCommands.from(message)
        if (command != null) {
            sendCommandToMqtt(command)
            text = "Command sent to MQTT!"
        } else if (message == "/start") {
            text = "Welcome to HomeAlarm bot!"
        }

        log.debug { "Sending confirmation message to '${userName}' user" }
        tgClient.sendMessage(text, listOf(chatId))
    }

    override fun onMessage(topic: String, message: String) {
        if (topic != messageBusIncomeTopic) {
            log.debug { "Ignoring incoming message with irrelevant '$topic' topic" }
            return
        }
        handleMqttMessage(message)
    }

    private fun handleMqttMessage(message: String) {
        log.debug { "Handling incoming MQTT message '$message'" }
        when (message) {
            BotCommands.ALARM_AUTO.name, BotCommands.ALARM_MANUAL.name -> handleAlarmStart()
            BotCommands.DISABLED_AUTO.name, BotCommands.DISABLED_MANUAL.name, BotCommands.ENABLED_AUTO.name, BotCommands.ENABLED_MANUAL.name -> handleAlarmStop()
            else -> tgClient.sendMessage(message)
        }
    }

    private fun handleAlarmStart() {
        sendPhotoRunner.start()
        sendVideoRunner.start()
    }

    private fun handleAlarmStop() {
        sendPhotoRunner.stop()
        sendVideoRunner.stop()
    }

    private fun sendCommandToMqtt(command: BotCommands) {
        log.debug { "Sending command '${command.name}' to '$messageBusOutcomeTopic' MessageBus topic" }
        messageBus.publish(messageBusOutcomeTopic, command.name)
    }
}
