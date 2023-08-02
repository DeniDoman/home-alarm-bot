package com.domanskii.homealarmbot

import com.domanskii.homealarmbot.messagebus.MessageBus
import com.domanskii.homealarmbot.messagebus.Observer
import mu.KotlinLogging
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

private val log = KotlinLogging.logger {}

class MqttCustomClient(
    private val messageBus: MessageBus
) : Observer {
    private val address: String = System.getenv("MQTT_ADDRESS")
    private val username: String = System.getenv("MQTT_USER")
    private val password: String = System.getenv("MQTT_PASSWORD")
    private val clientId: String = System.getenv("MQTT_CLIENT_ID")

    private val mqttIncomeTopic = "securityAlarm/event"
    private val mqttOutcomeTopic = "homeAlarmBot/message"
    private val messageBusIncomeTopic = "messageBus/messagesFromHomeAlarmBot"
    private val messageBusOutcomeTopic = "messageBus/eventsFromMqtt"
    private val client = MqttClient(address, clientId, MemoryPersistence())
    private val options: MqttConnectOptions

    init {
        log.info { "Subscribing to '$messageBusIncomeTopic' MessageBus topic" }
        messageBus.subscribe(messageBusIncomeTopic, this)

        options = MqttConnectOptions()
        options.userName = username
        options.password = password.toCharArray()
        options.connectionTimeout = 60
        options.keepAliveInterval = 60
        options.isAutomaticReconnect = true
    }

    fun connect() {
        log.info { "Connecting to MQTT server" }
        client.setCallback(object : MqttCallbackExtended {
            override fun connectionLost(cause: Throwable?) {
                log.error { "Connection to MQTT server has been lost with '$cause' cause" }
            }

            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                log.info { "${if (reconnect) "Reconnect" else "Connect"} to MQTT server completed" }
                subscribe()
            }

            override fun messageArrived(topic: String, message: MqttMessage) {
                log.debug { "Handling arrived '${String(message.payload)}' message from '$topic' topic" }
                log.debug { "Publishing '${String(message.payload)}' message to '$messageBusOutcomeTopic' MessageBus topic" }
                messageBus.publish(messageBusOutcomeTopic, String(message.payload))
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                log.debug { "Message delivery has completed" }
            }
        })
        client.connect(options)
    }

    private fun subscribe() {
        log.info { "Subscribing to '$mqttIncomeTopic' MQTT topic" }
        client.subscribe(mqttIncomeTopic, 2)
    }

    fun disconnect() {
        log.info { "Disconnecting from MQTT server" }
        client.disconnect()
        log.info { "Closing MQTT client" }
        client.close()
    }

    override fun onMessage(topic: String, message: String) {
        if (topic != messageBusIncomeTopic) {
            log.debug { "Ignoring incoming message with irrelevant '$topic' topic" }
            return
        }
        sendMqttMessage(message)
    }

    private fun sendMqttMessage(message: String) {
        val mqttMessage = MqttMessage()
        mqttMessage.payload = message.toByteArray()
        mqttMessage.qos = 2

        try {
            log.debug { "Publishing '${String(mqttMessage.payload)}' message to '$mqttOutcomeTopic' topic" }
            client.publish(mqttOutcomeTopic, mqttMessage)
        } catch (e: Exception) {
            log.error(e) { "Error while sending MQTT '$message' message to '$mqttOutcomeTopic' topic" }
        }
    }
}
