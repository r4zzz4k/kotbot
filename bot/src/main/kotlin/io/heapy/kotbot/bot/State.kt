package io.heapy.kotbot.bot

import io.heapy.kotbot.bot.rule.Action

/**
 * Holds nonessential part of current bot state. Should be mostly used to store information
 * needed between processing a sequence of interconnected updates.
 */
class State {
    var botUserId: UserId = 0
    lateinit var botUserName: String
    val familyAddRequests: MutableMap<String, Family> = mutableMapOf()
    val deferredActions: MutableMap<ChatId, MutableList<Action>> = mutableMapOf()

    fun deferAction(chatId: ChatId, action: Action) {
        deferredActions.getOrPut(chatId) { mutableListOf() } += action
    }
}
