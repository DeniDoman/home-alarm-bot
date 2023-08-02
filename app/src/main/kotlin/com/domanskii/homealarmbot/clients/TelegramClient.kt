package com.domanskii.homealarmbot.clients

import com.domanskii.homealarmbot.BotCommands
import mu.KotlinLogging
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto
import org.telegram.telegrambots.meta.api.methods.send.SendVideo
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import org.telegram.telegrambots.meta.exceptions.TelegramApiException


private val log = KotlinLogging.logger {}

class TelegramClient(botToken: String, private val usersList: List<String>, private val onTelegramMessage: (id: String, userName: String, chatId: String, message: String) -> Unit) : TelegramLongPollingBot(botToken) {
    override fun getBotUsername(): String {
        return "HomeAlarmBot"
    }

    override fun onUpdateReceived(update: Update) {
        if (!update.hasMessage() || !update.message.hasText()) {
            log.debug { "Update without a message received" }
            return
        }
        
        onTelegramMessage(update.message.from.id.toString(), update.message.from.userName, update.message.chatId.toString(), update.message.text)
    }

    fun sendMessage(message: String, users: List<String>? = null) {
        val sendList = users ?: this.usersList
        val tgMessages = List(sendList.size) { SendMessage() }
        tgMessages.forEachIndexed { idx, it ->
            it.chatId = sendList[idx]
            it.replyMarkup = getMarkup()
            it.text = "Event: $message"

            try {
                log.debug { "Sending a message '$message' to '${it.chatId}' Telegram chat" }
                executeAsync(it)
            } catch (e: TelegramApiException) {
                log.error(e) { "Error while sending a message to '${it.chatId}' chat" }
            }
        }
    }
    
    fun sendPhoto(imageData: ByteArray) {
        val tgMessages = List(usersList.size) { SendPhoto() }
        tgMessages.forEachIndexed { idx, it ->
            it.chatId = usersList[idx]
            it.replyMarkup = getMarkup()
            it.caption = "\uD83D\uDEA8 ALARM \uD83D\uDEA8"
            it.photo = InputFile(imageData.inputStream(), "photo.jpeg")

            try {
                log.debug { "Sending a photo to '${it.chatId}' Telegram chat" }
                executeAsync(it)
            } catch (e: TelegramApiException) {
                log.error(e) { "Error while sending a foto to '${it.chatId}' chat" }
            }
        }
    }
    
    fun sendVideo(videoData: ByteArray) {
        val tgMessages = List(usersList.size) { SendVideo() }
        tgMessages.forEachIndexed { idx, it ->
            it.chatId = usersList[idx]
            it.replyMarkup = getMarkup()
            it.caption = "\uD83D\uDEA8 VIDEO \uD83D\uDEA8"
            it.video = InputFile(videoData.inputStream(), "video.mp4")

            try {
                log.debug { "Sending a video to '${it.chatId}' Telegram chat" }
                executeAsync(it)
            } catch (e: TelegramApiException) {
                log.error(e) { "Error while sending a video to '${it.chatId}' chat" }
            }
        }
    }

    private fun getMarkup(): ReplyKeyboardMarkup {
        val buttons = listOf(
                KeyboardRow(
                        listOf(
                                KeyboardButton(BotCommands.ENABLED_AUTO.message), KeyboardButton(BotCommands.DISABLED_AUTO.message)
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