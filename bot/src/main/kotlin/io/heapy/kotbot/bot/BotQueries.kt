package io.heapy.kotbot.bot

import org.telegram.telegrambots.meta.api.objects.Chat
import org.telegram.telegrambots.meta.api.objects.ChatMember

/**
 * Provides a way to execute idempotent queries against messenger authority.
 */
interface BotQueries {
    suspend fun getBotUser(): Pair<Int, String>
    suspend fun isAdminUser(chatId: Long, userId: Int): Boolean
    suspend fun getChat(chatId: Long): Chat
    suspend fun getChatMember(chatId: Long, userId: Int): ChatMember

    suspend fun isCasBanned(userId: Int): Boolean
    fun getCasStatusUrl(userId: Int): String
}
