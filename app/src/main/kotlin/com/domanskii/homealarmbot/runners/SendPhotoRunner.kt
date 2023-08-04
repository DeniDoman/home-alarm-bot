package com.domanskii.homealarmbot.runners

import com.domanskii.homealarmbot.clients.RTSPClient
import com.domanskii.homealarmbot.clients.TelegramClient
import kotlinx.coroutines.*
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

class SendPhotoRunner(private val tgClient: TelegramClient, private val rtspUrl: String, private val rtspUser: String, private val rtspPassword: String, private val rtspImageInterval: Int) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var sendPhotoJob: Job? = null
    @Volatile private var isRunning = true
    
    
    fun start() {
        log.debug { "Start taking photos each $rtspImageInterval seconds..." }
        if (rtspUrl.isBlank()) {
            log.debug { "rtspUrl is not defined, taking photo is skipped" }
            return
        }
        if (rtspImageInterval == 0) {
            log.debug { "rtspImageInterval is 0, taking photo is skipped" }
            return
        }
        if (sendPhotoJob?.isActive == true) {
            log.debug { "sendPhotoJob is already active" }
            return
        }
        
        isRunning = true
        sendPhotoJob = scope.launch {
            while (isRunning) {
                log.debug { "Sending photo; isRunning == $isRunning" }
                sendPhoto()
                delay(rtspImageInterval.toLong() * 1000)
            }
        }
    }
    
    fun stop() {
        log.debug { "Stop taking photos..." }
        isRunning = false
    }

    private fun sendPhoto() {
        val url = rtspUrl.replace("rtsp://", "")
        val user = rtspUser
        val password = if (rtspPassword.isNotBlank()) ":${rtspPassword}@" else ""
        val finalUrl = "rtsp://$user$password$url"

        val imageData: ByteArray
        try {
            imageData = RTSPClient.getImage(finalUrl)
        } catch (e: Exception) {
            log.error(e) { "Error while getting image from camera" }
            tgClient.sendText("Error while getting image from camera")
            return
        }

        tgClient.sendPhoto(imageData)
    }
}