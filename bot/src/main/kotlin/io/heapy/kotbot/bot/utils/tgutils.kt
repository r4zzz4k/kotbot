package io.heapy.kotbot.bot.utils

import io.heapy.kotbot.bot.UserInfo
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.objects.*
import org.telegram.telegrambots.meta.bots.AbsSender
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException
import org.telegram.telegrambots.meta.updateshandlers.SentCallback
import java.io.Serializable
import kotlin.coroutines.*

private val T_ME = "https://t.me/"

val Chat.publicLink: String?
    get() = userName?.let { "$T_ME$it" }

val Message.publicLink: String?
    get() = if(!chat.isChannelChat && !chat.isSuperGroupChat) null else chat.userName?.let { "$T_ME$it/$messageId" }

val User.publicLink: String?
    get() = userName?.let { "$T_ME$it" }

val User.fullName: String
    get() = "$firstName $lastName"

val User.fullRef: String
    get() = userName?.let { "$fullName (@$it)" } ?: fullName

val UserInfo.fullRef: String
    get() = userName?.let { "$fullName (@$it)" } ?: fullName

suspend fun <T: Serializable, Method: BotApiMethod<T>> AbsSender.execAsync(method: Method): T = suspendCoroutine { cont ->
    executeAsync(method, object : SentCallback<T> {
        override fun onResult(method: BotApiMethod<T>, response: T) {
            cont.resume(response)
        }

        override fun onException(method: BotApiMethod<T>, exception: Exception) {
            cont.resumeWithException(exception)
        }

        override fun onError(method: BotApiMethod<T>, apiException: TelegramApiRequestException) {
            cont.resumeWithException(apiException)
        }
    })
}
