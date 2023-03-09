package com.domanskii.homealarmbot

import com.domanskii.homealarmbot.messagebus.MessageBus
import com.domanskii.homealarmbot.messagebus.Observer
import mu.KotlinLogging
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

private val log = KotlinLogging.logger {}

class MqttCustomClient(
    address: String,
    clientId: String,
    username: String,
    password: String,
    private val messageBus: MessageBus
) : Observer {
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
    }

    fun connect() {
        log.info { "Connecting to MQTT server" }
        client.setCallback(object : MqttCallback {
            override fun connectionLost(cause: Throwable?) {
                log.error { "Connection to MQTT server has been lost with '$cause' cause" }
                // TODO("Not yet implemented")
            }

            override fun messageArrived(topic: String, message: MqttMessage) {
                log.debug { "Handling arrived '${String(message.payload)}' message from '$topic' topic" }
                log.debug { "Publishing '${String(message.payload)}' message to '$messageBusOutcomeTopic' MessageBus topic" }
                messageBus.publish(messageBusOutcomeTopic, String(message.payload))
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                log.info { "Message delivery has completed" }
                // TODO("Not yet implemented")
            }

        })
        client.connect(options)
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

        val mqttMessage = MqttMessage()
        mqttMessage.payload = message.toByteArray()
        mqttMessage.qos = 2

        log.debug { "Publishing '${String(mqttMessage.payload)}' message to '$mqttOutcomeTopic' topic" }
        client.publish(mqttOutcomeTopic, mqttMessage)
    }
}