package io.heapy.kotbot.bot

/**
 * Holds essential part of bot state, which should persist between restarts.
 */
interface BotStore<C: Chat, F: Family<C>> {
    val families: Set<F>

    fun addFamily(adminChatId: Long): F
    fun removeFamily(family: F)

    fun addChatToFamily(family: F, chatId: Long): C
    fun removeChatFromFamily(family: F, chatId: Long)
}

/**
 * Represents a family of chats. Chat family is a group of chats sharing policies, restrictions, admin list.
 * Family contains a list of corresponding [chats] and an [adminChat].
 */
interface Family<C : Chat> {
    val chats: Set<C>
    var adminChat: C
}

interface Chat {
    var id: Long
}
