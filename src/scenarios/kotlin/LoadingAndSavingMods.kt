package com.lunivore.chimmer

import com.lunivore.chimmer.scenariohelpers.asResourceFile
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileNotFoundException

class LoadingAndSavingMods {

    @get:Rule
    val outputFolder = TemporaryFolder()

    @Test
    fun `should be able to load mods from a directory`() {

        // Given a plugins list and a folder with some mods in
        val plugins = asResourceFile("plugins.txt")
        val modDirectory = plugins.parentFile

        // When we load them using the order provided
        val mods = Chimmer(outputFolder.root).load(modDirectory, plugins)

        // Then we should have them in a list
        assertEquals(2, mods.size)
        assertEquals(setOf("IronSword.esp", "ArmorBootsSwordCrossbow.esp"), mods.map { it.name }.toSet())
    }

    @Test
    fun `should be able to load them as determined by the load order`() {

        // Given we provide the load order with the iron sword first
        val plugins = listOf("IronSword.esp", "ArmorBootsSwordCrossbow.esp")
        val modDirectory = asResourceFile("plugins.txt").parentFile

        // When we load them using that order
        var mods = Chimmer(outputFolder.root).load(modDirectory, plugins)

        // Then we should have them in that order
        assertEquals(listOf("IronSword.esp", "ArmorBootsSwordCrossbow.esp"), mods.map { it.name }.toList())

        // When we load them using a reverse order
        mods = Chimmer(outputFolder.root).load(modDirectory, plugins.reversed())

        // Then we should have them in the other order
        assertEquals(listOf("ArmorBootsSwordCrossbow.esp", "IronSword.esp"), mods.map { it.name }.toList())
    }

    @Test
    fun `should tell us if any file is not found`() {
        // Given a file that's not found
        val plugins = listOf("IDoNotExist.esp")
        val modFolder = asResourceFile("plugins.txt").parentFile

        // When we try to load it
        try {
            var mods = Chimmer(outputFolder.root).load(modFolder, plugins)
            fail("Should have thrown an exception")
        } catch (e: FileNotFoundException) {
            // Expected
        }

        // Then it should throw an exception
    }

    @Test
    fun `should tell us if the mod directory can't be found`() {
        // Given a mod folder that's not found
        val plugins = listOf("IronSword.esp")
        val modFolder = File("IDoNotExist")

        // When we try to load it
        try {
            Chimmer(outputFolder.root).load(modFolder, plugins)
            fail("Should have thrown an exception")
        } catch (e: FileNotFoundException) {
            // Expected
        }

        // Then it should throw an exception
    }

    @Test
    fun `should be able to save a mod`() {
        // Given we already loaded a mod
        val plugins = listOf("Skyrim.esm", "IronSword.esp")
        val modDirectory = asResourceFile("plugins.txt").parentFile
        val chimmer = Chimmer(outputFolder.root, loadRealSkyrimFiles = false)
        var mods = chimmer.load(modDirectory, plugins)

        // When we save it as a new mod
        val filename = "NewIronSword_${System.currentTimeMillis()}.esp"
        chimmer.save(mods[0].copy(name = filename))

        // Then it should be available to us
        assertTrue(File(outputFolder.root, "$filename").exists())
    }
}