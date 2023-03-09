package com.domanskii.homealarmbot.messagebus

import mu.KotlinLogging

private val log = KotlinLogging.logger {}

class MessageBus {
    private val subscribers = mutableSetOf<Pair<Observer, String>>()

    fun publish(topic: String, message: String) {
        log.debug { "Notifying '$topic' topic subscribers about '$message' message" }
        for (subscriber in subscribers.filter { it.second == topic }) {
            log.debug { "Notifying '${subscriber.first}' observer about '$message' message in '$topic' topic" }
            subscriber.first.onMessage(topic, message)

        }
    }

    fun subscribe(topic: String, observer: Observer) {
        log.info { "'$observer' observer subscribing to '$topic' topic" }
        subscribers.add(Pair(observer, topic))
    }

    fun unsubscribe(topic: String, observer: Observer) {
        log.info { "'$observer' observer unsubscribing from '$topic' topic" }
        subscribers.remove(Pair(observer, topic))
    }
}
