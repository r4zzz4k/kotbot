package io.heapy.kotbot.bot

import jetbrains.exodus.database.TransientEntityStore
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.query.*
import kotlinx.dnq.store.container.StaticStoreContainer
import kotlinx.dnq.util.initMetaData
import java.io.File

class XodusStore(private val store: TransientEntityStore) : BotStore<XdChat, XdFamily> {
    override val families: Set<XdFamily>
        get() = store.transactional {
            XdFamily.all().toSet()
        }

    override fun addFamily(adminChatId: Long): XdFamily = store.transactional {
        XdFamily.new {
            adminChat = XdChat.new { id = adminChatId }
        }
    }

    override fun removeFamily(family: XdFamily) = store.transactional {
        family.delete()
    }

    override fun addChatToFamily(family: XdFamily, chatId: Long): XdChat = store.transactional {
        XdChat.new {
            id = chatId
        }.also {
            family.chatsLink.add(it)
        }
    }

    override fun removeChatFromFamily(family: XdFamily, chatId: Long) = store.transactional {
        family.chatsLink.removeAll(XdChat.query(XdChat::id eq chatId))
    }
}

fun xodusStore(location: File) : BotStore<XdChat, XdFamily> {
    XdModel.registerNodes(XdFamily, XdChat)
    val store = StaticStoreContainer.init(location, "kotbot")
    initMetaData(XdModel.hierarchy, store)
    return XodusStore(store)
}

class XdFamily(entity: Entity) : XdEntity(entity), Family<XdChat> {
    companion object : XdNaturalEntityType<XdFamily>()

    internal val chatsLink by xdLink0_N(XdChat)
    override val chats: Set<XdChat>
        get() = chatsLink.toSet()

    override var adminChat by xdLink1(XdChat)
}

class XdChat(entity: Entity) : XdEntity(entity), Chat {
    companion object : XdNaturalEntityType<XdChat>()

    override var id: Long by xdRequiredLongProp()
}
