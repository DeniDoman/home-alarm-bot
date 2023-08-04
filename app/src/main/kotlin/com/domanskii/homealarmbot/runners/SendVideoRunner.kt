package com.domanskii.homealarmbot.runners

import com.domanskii.homealarmbot.clients.RTSPClient
import com.domanskii.homealarmbot.clients.TelegramClient
import kotlinx.coroutines.*
import mu.KotlinLogging


private val log = KotlinLogging.logger {}

class SendVideoRunner(private val tgClient: TelegramClient, private val rtspUrl: String, private val rtspUser: String, private val rtspPassword: String, private val rtspClipLength: Int) {

    private val scope = CoroutineScope(Dispatchers.Default)
    private var sendVideoJob: Job? = null
    @Volatile private var isRunning = true
    fun start() {
        log.debug { "Starting taking video..." }
        if (rtspUrl.isBlank()) {
            log.debug { "rtspUrl is not defined, taking video is skipped" }
            return
        }
        if (rtspClipLength == 0) {
            log.debug { "rtspClipLength is 0, taking video is skipped" }
            return
        }
        if (sendVideoJob?.isActive == true) {
            log.debug { "sendVideoJob is already active" }
            return
        }

        isRunning = true
        sendVideoJob = scope.launch {
            while (isRunning) {
                log.debug { "Sending video; isRunning == $isRunning" }
                sendVideo()
            }
        }
    }

    fun stop() {
        log.debug { "Stop video taking..." }
        isRunning = false
    }

    private fun sendVideo() {
        val url = rtspUrl.replace("rtsp://", "")
        val user = rtspUser
        val password = if (rtspPassword.isNotBlank()) ":${rtspPassword}@" else ""
        val finalUrl = "rtsp://$user$password$url"
        
        val videoData: ByteArray
        try {
            log.debug { "Getting video from RTSP url $url" }
            videoData = RTSPClient.getVideo(finalUrl, rtspClipLength)
        } catch (e: Exception) {
            log.error(e) { "Error while taking video from camera" }
            tgClient.sendText("Error while taking video from camera")
            return
        }

        tgClient.sendVideo(videoData)
    }
}