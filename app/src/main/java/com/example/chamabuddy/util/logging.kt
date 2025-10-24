package com.example.chamabuddy.util

import java.util.logging.*

fun setupLogging() {
    val logger = Logger.getLogger("")
    logger.handlers.forEach { logger.removeHandler(it) }

    val handler = ConsoleHandler().apply {
        formatter = SimpleFormatter()
        level = Level.ALL
    }

    logger.addHandler(handler)
    logger.level = Level.FINE
}