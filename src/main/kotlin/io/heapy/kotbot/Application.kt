package io.heapy.kotbot

import io.heapy.kotbot.bot.*
import io.heapy.kotbot.bot.rule.*
import io.heapy.kotbot.configuration.Configuration
import io.heapy.kotbot.metrics.createPrometheusMeterRegistry
import io.heapy.kotbot.web.startServer
import io.heapy.logging.logger
import java.io.File

/**
 * Entry point of bot.
 *
 * @author Ruslan Ibragimov
 */
object Application {
    @JvmStatic
    fun main(args: Array<String>) {
        val classLoader = Application::class.java.classLoader

        val configuration = Configuration()
        val metricsRegistry = createPrometheusMeterRegistry(configuration)
        val store = xodusStore(File("/tmp/kotbot-db"))
        val state = State<XdChat, XdFamily>()
        val rules = listOfNotNull(
            policyRules(classLoader.getResource("contains.txt")),
            devRules(store, state),
            familyRules(store, state)
        )

        startServer(
            metricsRegistry
        )

        startBot(
            configuration,
            rules,
            state
        )

        LOGGER.info("Application started.")
    }

    private val LOGGER = logger<Application>()
}
