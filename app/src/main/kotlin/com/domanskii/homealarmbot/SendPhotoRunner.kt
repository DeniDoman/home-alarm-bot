package com.domanskii.homealarmbot

import kotlinx.coroutines.*
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

class SendPhotoRunner(private val tgClient: TelegramClient, private val imageUrl: String, private val imageUser: String, private val imagePassword: String, private val imageAuth: String) {
    
    private val photoTimeout = 5000L;
    private val scope = CoroutineScope(Dispatchers.IO)
    private var sendPhotoJob: Job? = null
    
    fun start() {
        log.debug { "Starting photo taking..." }
        if (sendPhotoJob?.isActive == true) return
        
        sendPhotoJob = scope.launch {
            while (isActive) {
                sendPhoto()
                delay(photoTimeout)
            }
        }
    }
    
    fun stop() {
        sendPhotoJob?.cancel()
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