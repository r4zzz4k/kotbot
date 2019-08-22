package io.heapy.kotbot.bot

import org.telegram.telegrambots.meta.api.methods.GetMe
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember
import org.telegram.telegrambots.meta.api.objects.Chat
import org.telegram.telegrambots.meta.bots.AbsSender

/**
 * Provides [BotQueries] implementation for Telegram API.
 */
class TelegramApiQueries(private val api: AbsSender): BotQueries {
    override fun getBotUser(): Pair<Int, String> {
        val bot = api.execute(GetMe())
        return bot.id to bot.userName
    }

    override fun isAdminUser(chatId: Long, userId: Int): Boolean {
        val member = api.execute(GetChatMember().also {
            it.chatId = chatId.toString()
            it.userId = userId
        })
        val status = member.status
        return status == "creator" || status == "administrator"
    }

    override fun getChat(chatId: Long): Chat = api.execute(GetChat(chatId))
}
