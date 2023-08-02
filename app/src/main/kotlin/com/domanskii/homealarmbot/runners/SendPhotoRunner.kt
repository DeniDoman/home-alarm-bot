package com.domanskii.homealarmbot.runners

import com.domanskii.homealarmbot.clients.HttpImageAuth
import com.domanskii.homealarmbot.clients.HttpImageClient
import com.domanskii.homealarmbot.clients.TelegramClient
import kotlinx.coroutines.*
import mu.KotlinLogging
import java.util.concurrent.atomic.AtomicBoolean

private val log = KotlinLogging.logger {}

class SendPhotoRunner(private val tgClient: TelegramClient, private val imageUrl: String, private val imageUser: String, private val imagePassword: String, private val imageAuth: String) {
    
    private val photoTimeout = 5000L;
    private val scope = CoroutineScope(Dispatchers.IO)
    private var sendPhotoJob: Job? = null
    private val isRunning = AtomicBoolean(true)
    
    
    fun start() {
        log.debug { "Start taking photos..." }
        if (imageUrl.isBlank()) {
            log.debug { "imageUrl is not defined, taking photos is skipped" }
            return
        }
        if (sendPhotoJob?.isActive == true) {
            log.debug { "sendPhotoJob is already active" }
            return
        }
        
        sendPhotoJob = scope.launch {
            while (isRunning.get()) {
                log.debug { "Sending photo; isRunning == ${isRunning.get()}" }
                sendPhoto()
                delay(photoTimeout)
            }
        }
    }
    
    fun stop() {
        log.debug { "Stop taking photos..." }
        isRunning.set(false)
    }

    private fun sendPhoto() {
        val imageData: ByteArray
        try {
            imageData = HttpImageClient.getImage(imageUrl, imageUser, imagePassword, HttpImageAuth.valueOf(imageAuth))
        } catch (e: Exception) {
            log.error(e) { "Error while getting image from camera" }
            tgClient.sendMessage("Error while getting image from camera")
            return
        }

        tgClient.sendPhoto(imageData)
    }
}