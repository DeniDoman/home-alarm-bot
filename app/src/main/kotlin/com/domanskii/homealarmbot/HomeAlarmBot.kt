package com.domanskii.homealarmbot

import com.domanskii.homealarmbot.messagebus.MessageBus
import com.domanskii.homealarmbot.messagebus.Observer
import mu.KotlinLogging
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import org.telegram.telegrambots.meta.exceptions.TelegramApiException

private val log = KotlinLogging.logger {}

class HomeAlarmBot(botToken: String, private val usersList: List<String>, private val messageBus: MessageBus) :
    TelegramLongPollingBot(botToken), Observer {
    private val messageBusIncomeTopic = "messageBus/eventsFromMqtt"
    private val messageBusOutcomeTopic = "messageBus/messagesFromHomeAlarmBot"

    init {
        log.info { "Subscribing to '$messageBusIncomeTopic' MessageBus topic" }
        messageBus.subscribe(messageBusIncomeTopic, this)
    }

    override fun getBotUsername(): String {
        return "HomeAlarmBot"
    }

    override fun onUpdateReceived(update: Update) {
        if (!update.hasMessage() || !update.message.hasText()) {
            log.debug { "Update without a message received" }
            return
        }
        if (!usersList.contains(update.message.from.id.toString())) {
            log.info { "Message from an unknown user '${update.message.from.id}' received" }
            return
        }

        handleTgMessage(update)
    }

    private fun handleTgMessage(update: Update) {
        log.debug { "Handling '${update.message.text}' message from '${update.message.from.userName}' user" }

        val tgMessage = SendMessage()
        tgMessage.text = "Unknown command. Please use one of the commands below."

        if (BotCommands.values().any { it.name == update.message.text }) {
            sendCommandToMqtt(BotCommands.valueOf(update.message.text))
            tgMessage.text = "Message sent to MQTT!"
        } else if (update.message.text === "/start") {
            tgMessage.text = "Welcome to HomeAlarm bot!"
        }

        tgMessage.chatId = update.message.chatId.toString()
        tgMessage.replyMarkup = getMarkup()

        try {
            log.debug { "Sending confirmation message to '${update.message.from.userName}' user" }
            execute(tgMessage)
        } catch (e: TelegramApiException) {
            log.error(e) { "Error while sending confirmation message to '${update.message.from.userName}' user" }
        }
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
        val tgMessages = List(usersList.size) { SendMessage() }
        tgMessages.forEachIndexed { idx, it ->
            it.chatId = usersList[idx]
            it.text = "Event: $message"
            it.replyMarkup = getMarkup()

            try {
                log.debug { "Sending notification about '$message' MQTT message to '${it.chatId}' Telegram chat" }
                execute(it)
            } catch (e: TelegramApiException) {
                log.error(e) { "Error while notifying in '${it.chatId}' chat about incoming MQTT message" }
            }
        }
    }

    private fun sendCommandToMqtt(command: BotCommands) {
        log.debug { "Sending '${command.name}' command to '$messageBusOutcomeTopic' MessageBus topic" }
        messageBus.publish(messageBusOutcomeTopic, command.name)
    }

    private fun getMarkup(): ReplyKeyboardMarkup {
        val buttons = listOf(
            KeyboardRow(
                listOf(
                    KeyboardButton(BotCommands.ENABLED_AUTO.name), KeyboardButton(BotCommands.ENABLED_MANUAL.name)
                )
            ), KeyboardRow(
                listOf(
                    KeyboardButton(BotCommands.DISABLED_AUTO.name), KeyboardButton(BotCommands.DISABLED_MANUAL.name)
                )
            ), KeyboardRow(
                listOf(
                    KeyboardButton(BotCommands.ALARM_AUTO.name), KeyboardButton(BotCommands.ALARM_MANUAL.name)
                )
            )
        )

        val markup = ReplyKeyboardMarkup()
        markup.isPersistent = true
        markup.resizeKeyboard = true
        markup.inputFieldPlaceholder = "Select option"
        markup.keyboard = buttons

        return markup
    }
}
