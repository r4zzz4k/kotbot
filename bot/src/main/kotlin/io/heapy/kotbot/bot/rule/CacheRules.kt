package io.heapy.kotbot.bot.rule

import io.heapy.kotbot.bot.BotStore
import io.heapy.kotbot.bot.UserInfo
import io.heapy.kotbot.bot.utils.fullName
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.User

/**
 * A rule which caches user information from incoming messages.
 */
suspend fun cacheUserInfo(store: BotStore) = rule { update, _ ->
    fun User.cache() {
        store.knownUsers[id] = UserInfo(id, userName, fullName)
    }
    fun Message.cache() {
        from.cache()
        replyToMessage.cache()
        newChatMembers.forEach { it.cache() }
        leftChatMember.cache()
    }

    update.message?.cache()
    update.editedMessage?.cache()

    emptyList()
}

/**
 * A group of rules to cache information otherwise unavailable via Bot API.
 */
suspend fun cacheRules(store: BotStore) = compositeRule(
    cacheUserInfo(store)
)
