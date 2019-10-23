package io.heapy.kotbot.bot

import io.heapy.kotbot.bot.rule.*
import org.telegram.telegrambots.meta.api.objects.Message

/**
 * Holds nonessential part of current bot state. Should be mostly used to store information
 * needed between processing a sequence of interconnected updates.
 */
class State {
    var botUserId: Int = 0
    lateinit var botUserName: String
    val familyAddRequests: MutableMap<String, Family> = mutableMapOf()
    val deferredActions: MutableMap<Long, MutableList<Action>> = mutableMapOf()
    val votes: VotesState = VotesState()

    fun deferAction(chatId: Long, action: Action) {
        deferredActions.getOrPut(chatId) { mutableListOf() } += action
    }
}

class VotesState {
    val votes: MutableMap<Long, MutableMap<Int, VoteState>> = mutableMapOf()

    fun createVote(chatId: Long, userId: Int, message: Message) {
        votes.getOrPut(chatId) { mutableMapOf() }[userId] = VoteState(message)
    }

    fun addVote(vote: Vote): Action {
        val actions = votes[vote.chatId]!![vote.userId]!!
        actions.addVote(vote.action)
        return EditMessageReplyMarkupAction(vote.chatId, actions.message,
            listOf())
    }
}

class VoteState(val message: Message) {
    private val results: MutableMap<Vote.Action, Int> = mutableMapOf()

    fun addVote(action: Vote.Action) {
        results[action] = results.getOrDefault(action, 0)
    }
}
