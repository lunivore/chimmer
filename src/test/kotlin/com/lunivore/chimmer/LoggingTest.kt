package com.lunivore.chimmer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class LoggingTest {

    @Test
    fun `should return the same logger for each instance of a class`() {
        // Given two instances of a class with a logger
        val foo = Thingy()
        val bar = Thingy()

        // Then the logger should be the same
        assertSame(foo.logger, bar.logger)
    }

    @Test
    fun `should return the logger for a class when using the Companion Object`() {
        // Given an instance with a logger and a companion object with a logger
        val thingyLogger = Thingy().logger
        val thingyCoLogger = Thingy.coLogger

        // Then they should be the same
        assertSame(thingyLogger, thingyCoLogger)

        // And the name should be the canonical name of the class without any mention of companion objects.
        assertEquals(Thingy::class.java.canonicalName, thingyCoLogger.name)
    }

    class Thingy {
        companion object {
            val coLogger by Logging()
        }

        val logger by Logging()
    }
}