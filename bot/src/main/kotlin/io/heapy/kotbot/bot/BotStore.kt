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
import kotlinx.dnq.util.initMetaData
import java.io.File

fun fileStore(location: File) : BotStore =
    xodusStore(createTransientEntityStore(location, ENV_NAME))

fun inMemoryStore() : BotStore = xodusStore(TransientEntityStoreImpl().apply {
    val store = this
    val memory = Memory()

    val environment = Environments.newInstance(
        LogConfig.create(MemoryDataReader(memory), MemoryDataWriter(memory)),
        EnvironmentConfig().apply {
            logDataReaderWriterProvider = "jetbrains.exodus.io.inMemory.MemoryDataReaderWriterProvider"
        })
    this.persistentStore = PersistentEntityStoreImpl(environment, ENV_NAME)
    this.modelMetaData = ModelMetaDataImpl()
    this.eventsMultiplexer = DummyEventsMultiplexer
    this.queryEngine = XdQueryEngine(store).apply {
        this.sortEngine = TransientSortEngineImpl(store, this)
    }
})

private val ENV_NAME = "kotbot"

private fun xodusStore(store: TransientEntityStoreImpl): BotStore {
    XdModel.registerNodes(Family, Chat)
    StaticStoreContainer.store = store
    initMetaData(XdModel.hierarchy, store)
    return BotStore(store)
}

/**
 * Holds essential part of bot state, which should persist between restarts.
 */
class BotStore(private val store: TransientEntityStore) {
    fun <T> transactional(
        readonly: Boolean = false,
        queryCancellingPolicy: QueryCancellingPolicy? = null,
        isNew: Boolean = false,
        block: (TransientStoreSession) -> T
    ) = store.transactional(readonly, queryCancellingPolicy, isNew, block)

    fun findChatById(chatId: Long): Chat? = transactional {
        Chat.filter { it.id eq chatId }.firstOrNull()
    }

    fun findFamilyByAdminChat(adminChatId: Long): Family? = transactional {
        val chat = findChatById(adminChatId) ?: return@transactional null
        Family.filter { it.adminChat eq chat }.firstOrNull()
    }

    fun findFamilyByChat(chatId: Long): Family? = transactional {
        val chat = findChatById(chatId) ?: return@transactional null
        Family.filter { it.chats contains chat }.firstOrNull()
    }

    fun addFamily(adminChatId: Long): Family = store.transactional {
        Family.new {
            adminChat = Chat.new { id = adminChatId }
        }
    }

    fun addChatToFamily(family: Family, chatId: Long): Chat = transactional {
        Chat.new {
            id = chatId
        }.also {
            family.chats.add(it)
        }
    }

    fun removeChatFromFamily(family: Family, chatId: Long) = store.transactional {
        Chat.query((Chat::id eq chatId) and (Chat::family eq family))
        //family.chats.removeAll(Chat.query(Chat::id eq chatId))
    }
}

/**
 * Represents a family of chats. Chat family is a group of chats sharing policies, restrictions, admin list.
 * Family contains a list of corresponding [chats] and an [adminChat].
 */
class Family(entity: Entity) : XdEntity(entity) {
    companion object : XdNaturalEntityType<Family>()

    val chats by xdLink0_N(Chat)
    var adminChat: Chat by xdLink1(Chat::family)
}

class Chat(entity: Entity) : XdEntity(entity) {
    companion object : XdNaturalEntityType<Chat>()

    var id: Long by xdRequiredLongProp()
    val family: Family? by xdLink0_1(Family::adminChat)
}
