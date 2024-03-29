package com.domanskii.homealarmbot

import com.domanskii.homealarmbot.clients.TelegramClient
import com.domanskii.homealarmbot.messagebus.MessageBus
import com.domanskii.homealarmbot.messagebus.Observer
import com.domanskii.homealarmbot.runners.SendAlertRunner
import com.domanskii.homealarmbot.runners.SendPhotoRunner
import com.domanskii.homealarmbot.runners.SendVideoRunner
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession
import mu.KotlinLogging


private val log = KotlinLogging.logger {}

class HomeAlarm(botToken: String, private val messageBus: MessageBus) : Observer {
    private val chatsList: List<String> = System.getenv("CHATS_LIST").split(",")
    private val tgClient = TelegramClient(botToken, chatsList, ::handleTgMessage)
    private val alertInterval = System.getenv("ALERT_INTERVAL") ?: "5"
    private val rtspUrl = System.getenv("RTSP_URL") ?: ""
    private val rtspUser = System.getenv("RTSP_USER") ?: ""
    private val rtspPassword = System.getenv("RTSP_PASSWORD") ?: ""
    private val rtspClipLength = System.getenv("RTSP_CLIP_LENGTH") ?: "15"
    private val rtspImageInterval = System.getenv("RTSP_IMAGE_INTERVAL") ?: "5"
    private val sendAlertRunner = SendAlertRunner(tgClient, alertInterval.toInt())
    private val sendPhotoRunner = SendPhotoRunner(tgClient, rtspUrl, rtspUser, rtspPassword, rtspImageInterval.toInt())
    private val sendVideoRunner = SendVideoRunner(tgClient, rtspUrl, rtspUser, rtspPassword, rtspClipLength.toInt())
    private val messageBusIncomeTopic = "messageBus/eventsFromMqtt"
    private val messageBusOutcomeTopic = "messageBus/messagesFromHomeAlarmBot"

    init {
        log.info { "Subscribing to '$messageBusIncomeTopic' MessageBus topic" }
        messageBus.subscribe(messageBusIncomeTopic, this)
    }

    fun registerBot() {
        log.info { "Registering Telegram bot" }
        TelegramBotsApi(DefaultBotSession::class.java).registerBot(tgClient)
    }

    private fun handleTgMessage(id: String, userName: String, chatId: String, message: String) {
        if (!chatsList.contains(id)) {
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
        tgClient.sendText(text, listOf(chatId))
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
        }
        tgClient.sendText(message)
    }

    private fun handleAlarmStart() {
        log.debug { "Handling alarm start" }
        sendAlertRunner.start()
        sendPhotoRunner.start()
        sendVideoRunner.start()
    }

    private fun handleAlarmStop() {
        log.debug { "Handling alarm stop" }
        sendAlertRunner.stop()
        sendPhotoRunner.stop()
        sendVideoRunner.stop()
    }

    private fun sendCommandToMqtt(command: BotCommands) {
        log.debug { "Sending command '${command.name}' to '$messageBusOutcomeTopic' MessageBus topic" }
        messageBus.publish(messageBusOutcomeTopic, command.name)
    }
}
