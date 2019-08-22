package io.heapy.kotbot.bot.rule

import io.heapy.kotbot.bot.*
import io.heapy.kotbot.bot.utils.*
import kotlinx.dnq.query.*
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton

/**
 * Callback query ids for family functionality
 */
enum class FamilyCallbacks {
    /**
     * Instructs bot to check if it's an admin of the chats in the family.
     */
    RefreshAdminPermissions
}

/**
 * Command `/report`: pings family admin chat for them to pay attention to events happening in current chat.
 * Family chat message mentions chat, report sender and, if `/report` message is a reply to another one, report target.
 * It also provides a reference to report or report target if possible.
 */
fun reportRule(store: BotStore, state: State) = commandRule("/report", state) { _, message, _ ->
    val chat = message.chat
    val family = store.findFamilyByChat(chat.id) ?: return@commandRule emptyList()
    val reportMessage = if(message.isReply) message.replyToMessage else message

    val chatTitle = "\"${chat.title}\""
    val chatMention = chat.userName?.let { "$chatTitle (@$it)" } ?: chatTitle

    val senderText = "\nSent by @${message.from.fullRef}"
    val reportText = if(message.isReply) "\nReported user ${reportMessage.from.fullRef}" else ""
    val reportRef = reportMessage.publicLink?.let { "\n$it" } ?: ""

    listOf(
        SendMessageAction(
            family.adminChat.id,
            "Report in $chatMention:$senderText$reportText$reportRef"
        )
    )
}

/**
 * Callback query processing [FamilyCallbacks.RefreshAdminPermissions].
 */
fun refreshPermissionsCallbackRule(state: State) = callbackQueryRule { callback, _ ->
    if(callback.data != FamilyCallbacks.RefreshAdminPermissions.name)
        emptyList()
    else
        // TODO should definitely get family and retry actions only for it
        state.deferredActions.values.flatten()
}

/**
 * Command `/start`: connects new chat to existing family.
 * TODO: process bot addition to chat without prior request from family admin chat.
 */
fun familyStartRule(store: BotStore, state: State) = commandRule("/start", state) { args, message, queries ->
    val messageId = message.messageId
    val chatId = message.chat.id
    val chatTitle = "\"${message.chat.title}\""
    val from = message.from
    val family = state.familyAddRequests[args]

    store.transactional {
        val chat = store.findChatById(chatId)
        when {
            family == null -> listOf(
                SendMessageAction(chatId, "The request is expired, please try again!")
            )
            chat in family.chats -> listOf(
                DeleteMessageAction(chatId, messageId),
                SendMessageAction(
                    family.adminChat.id,
                    "Picked chat $chatTitle is already a member " +
                            "of this family, please pick another one"
                )
            )
            !queries.isAdminUser(chatId, from.id) -> listOf(
                SendMessageAction(
                    family.adminChat.id,
                    "Sorry, @${from.userName}, you are not an admin in $chatTitle"
                )
            )
            else -> {
                store.addChatToFamily(family, chatId)
                val deleteAction = DeleteMessageAction(chatId, messageId)
                if (!queries.isAdminUser(chatId, state.botUserId)) {
                    state.deferAction(chatId, deleteAction)
                    listOf(
                        SendMessageAction(
                            family.adminChat.id,
                            "Chat $chatTitle is now a member of the family! Don't forget " +
                                    "to give the bot admin rights :)",
                            listOf(
                                listOf(
                                    InlineKeyboardButton("Admin rights granted").apply {
                                        callbackData = FamilyCallbacks.RefreshAdminPermissions.name
                                    }
                                )
                            )
                        )
                    )
                } else {
                    listOf(
                        deleteAction,
                        SendMessageAction(
                            family.adminChat.id,
                            "Chat $chatTitle is now a member of the family!"
                        )
                    )
                }
            }
        }
    }
}

/**
 * Processes bot leave messages by disconnecting chat from the family.
 */
fun familyLeaveRule(store: BotStore, state: State) = rule { update, _ ->
    if(!update.hasMessage()) return@rule emptyList()
    val message = update.message
    val chat = message.chat
    val chatId = chat.id
    val leftChatMember = message.leftChatMember
    if(leftChatMember == null || leftChatMember.id != state.botUserId)
        return@rule emptyList()
    val family = store.findFamilyByChat(chatId) ?: return@rule emptyList()
    store.removeChatFromFamily(family, chatId)
    listOf(
        SendMessageAction(
            family.adminChat.id,
            "Chat \"${chat.title}\" left the family as the bot was removed from it :("
        )
    )
}

/**
 * Admin command `/family`: makes current chat a family admin chat.
 */
fun familyCreateRule(store: BotStore, state: State) = adminCommandRule("/family", state) { _, message, queries ->
    val chatId = message.chatId
    when {
        store.findFamilyByChat(chatId) != null -> listOf(
            SendMessageAction(chatId, "This chat is already bound to the family")
        )
        else -> {
            store.addFamily(chatId)
            listOf(
                SendMessageAction(chatId,
                    "This chat is now a family admin!\n" +
                            "Use /family_add to add other chats to the family")
            )
        }
    }
}

/**
 * Admin command `/family_list`: if invoked from family admin chat, shows a list of chats present in current family.
 */
fun familyListRule(store: BotStore, state: State) = adminCommandRule("/family_list", state) { _, message, queries ->
    val chatId = message.chatId
    val family = store.findFamilyByAdminChat(chatId)
    if (family == null) {
        listOf(
            SendMessageAction(
                chatId,
                "Didn't find family of this chat :(\n" +
                        "Use `/family` to create a new one!"
            )
        )
    } else {
        listOf(
            SendMessageAction(
                chatId,
                store.transactional { family.chats.asSequence().map(Chat::id).toList() }
                    .map(queries::getChat)
                    .map { "${it.title}: ${it.publicLink}" }
                    .joinToString(
                        prefix = "Chats in our family:\n- ",
                        separator = "\n-")
            )
        )
    }
}

/**
 * Admin command `/family_add`: if invoked from family admin chat, creates a request to add a new chat to the family.
 * By following a link posted by the bot and picking a chat, one adds the bot to that chat, adds the chat to the family.
 * The procedure should be finished by manually granting admin permissions to the bot and clicking refresh button here.
 */
fun familyAddRule(store: BotStore, state: State) = adminCommandRule("/family_add", state) { _, message, queries ->
    val chatId = message.chatId
    val family = store.findFamilyByAdminChat(chatId)
    if (family == null) {
        listOf(
            SendMessageAction(
                chatId,
                "Didn't find family of this chat :(\n" +
                        "Use `/family` to create a new one!"
            )
        )
    } else {
        val hash = randomString(32)
        state.familyAddRequests[hash] = family
        listOf(
            SendMessageAction(
                chatId,
                "Please pick the chat to add: https://telegram.me/${state.botUserName}?startgroup=$hash\n" +
                        "Note that you must be an admin in that chat!"
                /*listOf(listOf(
                    InlineKeyboardButton("Cancel").apply {
                        callbackData = "..."
                    }
                ))*/
            )
        )
    }
}

/**
 * A group of commands and rules related to family functionality.
 */
fun familyRules(store: BotStore, state: State) = compositeRule(
    reportRule(store, state),
    refreshPermissionsCallbackRule(state),
    familyStartRule(store, state),
    familyLeaveRule(store, state),

    familyCreateRule(store, state),
    familyListRule(store, state),
    familyAddRule(store, state)
)
