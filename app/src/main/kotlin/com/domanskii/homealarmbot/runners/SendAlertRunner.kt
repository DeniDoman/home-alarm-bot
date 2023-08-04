package com.domanskii.homealarmbot.runners

import com.domanskii.homealarmbot.clients.TelegramClient
import kotlinx.coroutines.*
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

class SendAlertRunner(private val tgClient: TelegramClient, private val alertInterval: Int) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var sendAlertJob: Job? = null
    @Volatile private var isRunning = true
    
    
    fun start() {
        log.debug { "Start sending alerts each $alertInterval seconds..." }
        if (alertInterval == 0) {
            log.debug { "alertInterval is 0, sending alert is skipped" }
            return
        }
        if (sendAlertJob?.isActive == true) {
            log.debug { "sendAlertJob is already active" }
            return
        }
        
        isRunning = true
        sendAlertJob = scope.launch {
            while (isRunning) {
                log.debug { "Sending alert; isRunning == $isRunning" }
                sendAlert()
                delay(alertInterval.toLong() * 1000)
            }
        }
    }
    
    fun stop() {
        log.debug { "Stop sending alerts..." }
        isRunning = false
    }

    private fun sendAlert() {
        tgClient.sendText("🚨 ALARM 🚨")
    }
}