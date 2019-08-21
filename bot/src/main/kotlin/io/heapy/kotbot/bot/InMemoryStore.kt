package io.heapy.kotbot.bot

/**
 * Represents [BotStore] backed by in-memory structure. It is not persistent.
 */
class InMemoryStore : BotStore<InMemoryChat, InMemoryFamily> {
    override val families: MutableSet<InMemoryFamily> = mutableSetOf()

    override fun addFamily(adminChatId: Long) =
        InMemoryFamily(mutableSetOf(), InMemoryChat(adminChatId)).also { families.add(it) }

    override fun removeFamily(family: InMemoryFamily) {
        families.remove(family)
    }

    override fun addChatToFamily(family: InMemoryFamily, chatId: Long): InMemoryChat =
        InMemoryChat(chatId).also { family.chats.add(it) }

    override fun removeChatFromFamily(family: InMemoryFamily, chatId: Long) {
        family.chats.removeIf { it.id == chatId }
    }
}

class InMemoryFamily(
    override val chats: MutableSet<InMemoryChat>,
    override var adminChat: InMemoryChat
) : Family<InMemoryChat>

class InMemoryChat(
    override var id: Long
) : Chat
