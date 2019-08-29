package io.heapy.kotbot.bot

import org.telegram.telegrambots.meta.api.objects.Chat

/**
 * Provides a way to execute idempotent queries against messenger authority.
 */
interface BotQueries {
    fun getBotUser(): Pair<Int, String>
    fun isAdminUser(chatId: ChatId, userId: UserId): Boolean
    fun getChat(chatId: ChatId): Chat
}
