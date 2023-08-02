package com.domanskii.homealarmbot.runners

import com.domanskii.homealarmbot.clients.RTSPVideoClient
import com.domanskii.homealarmbot.clients.TelegramClient
import kotlinx.coroutines.*
import mu.KotlinLogging
import java.util.concurrent.atomic.AtomicBoolean


private val log = KotlinLogging.logger {}

class SendVideoRunner(private val tgClient: TelegramClient, private val rtspUrl: String, private val rtspUser: String, private val rtspPassword: String) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var sendVideoJob: Job? = null
    private val isRunning = AtomicBoolean(true)

    fun start() {
        log.debug { "Starting video recording..." }
        if (rtspUrl.isBlank()) {
            log.debug { "rtspUrl is not defined, video record is skipped" }
            return
        }
        if (sendVideoJob?.isActive == true) {
            log.debug { "sendVideoJob is already active" }
            return
        }

        sendVideoJob = scope.launch {
            while (isRunning.get()) {
                log.debug { "Sending video; isRunning == ${isRunning.get()}" }
                sendVideo()
            }
        }
    }

    fun stop() {
        log.debug { "Stop video recording..." }
        isRunning.set(false)
    }

    private fun sendVideo() {
        val url = rtspUrl.replace("rtsp://", "")
        val user = rtspUser
        val password = if (rtspPassword.isNotBlank()) ":${rtspPassword}@" else ""
        val finalUrl = "rtsp://$user$password$url"
        
        val videoData: ByteArray
        try {
            log.debug { "Getting video from RTSP url $url" }
            videoData = RTSPVideoClient.getVideo(finalUrl)
        } catch (e: Exception) {
            log.error(e) { "Error while recording video from camera" }
            tgClient.sendMessage("Error while recording video from camera")
            return
        }

        tgClient.sendVideo(videoData)
    }
}