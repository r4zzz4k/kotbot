package io.heapy.kotbot.bot

import com.jetbrains.teamsys.dnq.database.TransientEntityStoreImpl
import com.jetbrains.teamsys.dnq.database.TransientSortEngineImpl
import jetbrains.exodus.database.TransientEntityStore
import jetbrains.exodus.database.TransientStoreSession
import jetbrains.exodus.entitystore.*
import jetbrains.exodus.env.EnvironmentConfig
import jetbrains.exodus.env.Environments
import jetbrains.exodus.io.inMemory.*
import jetbrains.exodus.log.LogConfig
import jetbrains.exodus.query.metadata.ModelMetaDataImpl
import kotlinx.dnq.*
import kotlinx.dnq.query.*
import kotlinx.dnq.store.DummyEventsMultiplexer
import kotlinx.dnq.store.XdQueryEngine
import kotlinx.dnq.store.container.StaticStoreContainer
import kotlinx.dnq.store.container.createTransientEntityStore
import kotlinx.dnq.util.*
import java.io.File

fun fileStore(location: File) : BotStore =
    xodusStore(createTransientEntityStore(location, ENV_NAME))

fun inMemoryStore() : BotStore = xodusStore(TransientEntityStoreImpl().apply {
    val store = this
    val memory = Memory()
    val logConfig = LogConfig.create(MemoryDataReader(memory), MemoryDataWriter(memory))
    val environmentConfig = EnvironmentConfig().apply {
        logDataReaderWriterProvider = "jetbrains.exodus.io.inMemory.MemoryDataReaderWriterProvider"
    }

    val environment = Environments.newInstance(logConfig, environmentConfig)
    this.persistentStore = PersistentEntityStoreImpl(environment, ENV_NAME)
    this.modelMetaData = ModelMetaDataImpl()
    this.eventsMultiplexer = DummyEventsMultiplexer
    this.queryEngine = XdQueryEngine(store).apply {
        this.sortEngine = TransientSortEngineImpl(store, this)
    }
})

private val ENV_NAME = "kotbot"

private fun xodusStore(store: TransientEntityStoreImpl): BotStore {
    XdModel.registerNodes(XdFamily, XdChat)
    StaticStoreContainer.store = store
    initMetaData(XdModel.hierarchy, store)
    return BotStore(store)
}

/**
 * Holds essential part of bot state, which should persist between restarts.
 */
class BotStore(private val store: TransientEntityStore) {
    private fun findXdChatById(chatId: ChatId): XdChat? = store.transactional {
        XdChat.filter { it.id eq chatId }.firstOrNull()
    }

    fun findChatById(chatId: ChatId): Chat? = findXdChatById(chatId)?.toModel()

    fun findFamilyByAdminChat(adminChatId: ChatId): Family? = store.transactional {
        val chat = findXdChatById(adminChatId) ?: return@transactional null
        XdFamily.filter { it.adminChat eq chat }.firstOrNull()?.toModel()
    }

    fun findFamilyByChat(chatId: ChatId): Family? = store.transactional {
        val chat = findXdChatById(chatId) ?: return@transactional null
        XdFamily.filter { it.chats contains chat }.firstOrNull()?.toModel()
    }

    fun addFamily(adminChatId: ChatId): Family = store.transactional {
        XdFamily.new {
            adminChat = XdChat.new { id = adminChatId }
        }.toModel()
    }

    fun addChatToFamily(family: Family, chatId: ChatId): Chat = store.transactional {
        XdChat.new {
            id = chatId
        }.also {
            XdFamily.findById(family.id).chats.add(it)
        }.toModel()
    }

    fun removeChatFromFamily(family: Family, chatId: ChatId) = store.transactional {
        val xdChat = findXdChatById(chatId) ?: return@transactional
        val xdFamily = xdChat.family
        if(xdFamily?.xdId != family.id) return@transactional
        xdFamily.chats.remove(xdChat)
    }
}

// TODO decide if we want to replace this with inline class considering usage of `Set<ChatId>`
typealias FamilyId = String
typealias ChatId = Long
typealias MessageId = Int
typealias UserId = Int

/**
 * Represents a family of chats. Chat family is a group of chats sharing policies, restrictions, admin list.
 * Family contains a list of corresponding [chats] and an [adminChat].
 */
data class Family(val id: FamilyId, val chats: Set<ChatId>, val adminChat: ChatId)
data class Chat(val id: ChatId)

class XdFamily(entity: Entity) : XdEntity(entity) {
    companion object : XdNaturalEntityType<XdFamily>()

    val chats by xdLink0_N(XdChat)
    var adminChat: XdChat by xdLink1(XdChat::family)

    fun toModel(): Family = Family(
        xdId,
        chats.asSequence().map(XdChat::id).toSet(),
        adminChat.id)
}

class XdChat(entity: Entity) : XdEntity(entity) {
    companion object : XdNaturalEntityType<XdChat>()

    var id: ChatId by xdRequiredLongProp()
    val family: XdFamily? by xdLink0_1(XdFamily::adminChat)

    fun toModel(): Chat = Chat(id)
}
