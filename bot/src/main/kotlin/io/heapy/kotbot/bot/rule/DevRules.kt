package io.heapy.kotbot.bot.rule

import io.heapy.kotbot.bot.*

/**
 * Command `/getid` (admin): responds with current chat id.
 */
fun <C : Chat, F: Family<C>> getIdRule(state: State<C, F>) : Rule = adminCommandRule("/getid", state) { _, message, _ ->
    val chat = message.chat
    val chatId = chat.id
    listOf(
        DeleteMessageAction(chatId, message.messageId),
        SendMessageAction(chatId, "Chat id for \"${chat.title}\" is $chatId")
    )
}

/**
 * Command `/adm message`: sends the message to family administrator chat.
 */
fun <C : Chat, F: Family<C>> admRule(store: BotStore<C, F>, state: State<C, F>) : Rule = commandRule("/adm", state) { args, message, _ ->
    val admMsg = "@${message.from.userName}: $args"
    val chatId = message.chat.id
    listOf(DeleteMessageAction(chatId, message.messageId)) +
            store.families
                .filter { chatId in it.chats.map(Chat::id) }
                .map { SendMessageAction(it.adminChat.id, admMsg) }
}

/**
 * A group of commands useful for development purposes.
 */
fun <C : Chat, F: Family<C>> devRules(store: BotStore<C, F>, state: State<C, F>) = compositeRule(
    getIdRule(state),
    admRule(store, state)
)
