package io.heapy.kotbot.bot.rule

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.heapy.kotbot.bot.BotStore
import io.heapy.kotbot.bot.State
import io.heapy.kotbot.bot.utils.fullRef
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton

class Vote(val chatId: Long, val userId: Int, val action: Action) {
    fun data(): String = json.writeValueAsString(this)

    companion object {
        private val json = jacksonObjectMapper()
        fun parse(data: String): Vote? = json.readValue<Vote>(data)
    }

    enum class Action { Kick, Mute, Nothing }
}

/**
 * A rule which posts vote-kick message and kicks a person if vote was successful.
 */
suspend fun voteKickRule(store: BotStore, state: State) = commandRule("votekick", state) { args, message, queries ->
    val chatId = message.chatId

    val mention = message.entities.firstOrNull { it.type == "mention" }
    val textMention = message.entities.firstOrNull { it.type == "text_mention" }

    val userId = when {
        message.isReply -> message.replyToMessage.from.id
        textMention != null -> {
            textMention.user.id
        }
        mention != null -> {
            LOGGER.error("voteKick mention: [${mention.text}]")
            store.knownUsers.values.firstOrNull { it.userName == mention.text }?.userId
        }
        else -> null
    }
    if(userId == null) {
        listOf(SendMessageAction(chatId, "Sorry, unrecognized mention. Please try again."))
    } else {
        val user = queries.getChatMember(chatId, userId).user

        listOf(SendMessageAction(
            chatId,
            "Do you agree to restrict ${user.fullRef}?",
            listOf(listOf(
                InlineKeyboardButton("Yes, kick").apply {
                    callbackData = Vote(chatId, userId, Vote.Action.Kick).data()
                },
                InlineKeyboardButton("Yes, mute (7 days)").apply {
                    callbackData = Vote(chatId, userId, Vote.Action.Mute).data()
                },
                InlineKeyboardButton("No").apply {
                    callbackData = Vote(chatId, userId, Vote.Action.Nothing).data()
                }
            ))
        ))
    }

    emptyList()
}

suspend fun voteKickCallback(state: State) = callbackQueryRule rule@ { callback, queries ->
    val cb = Vote.parse(callback.data)
    if(cb == null) return@rule emptyList()

    state.votes[cb.chatId][cb.userId][cb.action]
}

/**
 * A group of commands and rules related to community votings..
 */
suspend fun voteRules(store: BotStore, state: State) = compositeRule(
    voteKickRule(store, state),
    voteKickCallback(state)
)
