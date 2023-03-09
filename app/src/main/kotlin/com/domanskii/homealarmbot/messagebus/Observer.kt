package com.domanskii.homealarmbot.messagebus

interface Observer {
    fun onMessage(topic: String, message: String)
}