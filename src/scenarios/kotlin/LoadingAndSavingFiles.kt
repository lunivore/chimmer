package com.lunivore.chimmer

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class LoadingAndSavingFiles {

    @Test
    fun `should be able to load mods from a directory`() {

        // Given a plugins list and a directory with some mods in
        val plugins = asResourceFile("plugins.txt")
        val modDirectory = plugins.parentFile

        // When we load them using the order provided
        val mods = Chimmer().load(modDirectory, plugins, null)

        // Then we should have them in a list
        assertEquals(2, mods.size)
        assertEquals(setOf("IronSword.esp", "ArmorBootsSwordCrossbow.esp"), mods.map { it.name }.toSet())
    }

    private fun asResourceFile(filename: String): File {
        val resourceEsp = LoadingAndSavingFiles::class.java.getResource("/$filename").file
        return File(resourceEsp)
    }
}