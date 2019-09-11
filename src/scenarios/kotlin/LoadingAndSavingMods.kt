package com.lunivore.chimmer

import com.lunivore.chimmer.binary.toBigEndianHexString
import com.lunivore.chimmer.testheplers.asResourceFile
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.io.FileNotFoundException

class LoadingAndSavingMods  : ChimmerScenario() {

    @Test
    fun `should be able to load mods from a directory`() {

        // Given a plugins list and a folder with some mods in
        val plugins = asResourceFile("plugins.txt")
        val modDirectory = plugins.parentFile

        // When we load them using the order provided
        val mods = Chimmer(fileHandler()).load(modDirectory, plugins)

        // Then we should have them in a list
        assertEquals(2, mods.size)
        assertEquals(setOf("IronSword.esp", "ArmorBootsSwordCrossbow.esp"), mods.map { it.name }.toSet())
    }

    @Test
    fun `should be able to filter on particular groups`() {

        // Given several mods, some of which have weapons
        val plugins = listOf("IronSword.esp", "ArmorBootsSwordCrossbow.esp", "CompressedDunTransmogrifyDremora.esp")
        val modDirectory = asResourceFile("plugins.txt").parentFile
        val chimmer = Chimmer(fileHandler())

        // When we load them using the order provided
        val mods = Chimmer(fileHandler()).load(modDirectory, plugins, ModsToLoad.LOAD_ALL, listOf(Group.Weapon))

        // Then we should have only the ones with weapons
        assertEquals(2, mods.size)
        assertEquals(listOf("IronSword.esp", "ArmorBootsSwordCrossbow.esp"), mods.map { it.name })

        // And only the weapons should have been loaded
        assertEquals(2, mods[1].weapons.size)
        assertEquals(0, mods[1].armors.size)
    }

    @Test
    fun `should be able to load them as determined by the load order`() {

        // Given we provide the load order with the iron sword first
        val plugins = listOf("IronSword.esp", "ArmorBootsSwordCrossbow.esp")
        val modDirectory = asResourceFile("plugins.txt").parentFile

        // When we load them using that order
        var mods = Chimmer(fileHandler()).load(modDirectory, plugins)

        // Then we should have them in that order
        assertEquals(listOf("IronSword.esp", "ArmorBootsSwordCrossbow.esp"), mods.map { it.name }.toList())

        // When we load them using a reverse order
        mods = Chimmer(fileHandler()).load(modDirectory, plugins.reversed())

        // Then we should have them in the other order
        assertEquals(listOf("ArmorBootsSwordCrossbow.esp", "IronSword.esp"), mods.map { it.name }.toList())
    }

    @Test
    fun `should tell us if any mod is not found`() {
        // Given a file that's not found
        val plugins = listOf("IDoNotExist.esp")
        val modFolder = asResourceFile("plugins.txt").parentFile

        // When we try to load it
        try {
            var mods = Chimmer(fileHandler()).load(modFolder, plugins)
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
            var mods = Chimmer(fileHandler()).load(modFolder, plugins)
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
        val chimmer = Chimmer(fileHandler())
        var mods = chimmer.load(modDirectory, plugins,  ModsToLoad.SKIP_BETHESDA_MODS)

        // When we save it as a new mod
        val filename = "NewIronSword_${System.currentTimeMillis()}.esp"
        chimmer.save(mods[0].copy(name = filename), plugins)

        // Then it should be available to us
        assertTrue(File(outputFolder.root, "$filename").exists())
    }

    /**
     * Note that this test will fail if Skyrim isn't installed with Steam, or if
     * any of the usual .esm files are missing!
     */
    @Test
    fun `should use plugins txt and Skyrim mod dir from the right places by default`() {
        // Given Chimmer
        val chimmer = Chimmer()

        // When we tell it to load all
        val mods = chimmer.load(ModsToLoad.LOAD_ALL)

        // Then it should have found things successfully
        assertEquals("Skyrim.esm", mods[0].name)
    }

    @Test
    fun `should be able to load compressed records like NPCs`() {
        // Given a mod with compressed records
        val plugins = listOf("Skyrim.esm", "CompressedDunTransmogrifyDremora.esp")
        val modDirectory = asResourceFile("plugins.txt").parentFile
        val chimmer = Chimmer(fileHandler())

        // When we load the mods
        var mods = chimmer.load(modDirectory, plugins,  ModsToLoad.SKIP_BETHESDA_MODS)

        // Then we should be able to see those records too
        assertEquals("00000EB4", (mods[0].npcs[0].formId as ExistingFormId).unindexed.toBigEndianHexString())
        assertEquals("dunTransmogrifyDremora", mods[0].npcs[0].editorId)

        // When we copy the record and save it
        val newMod = chimmer.createMod("MyMod.esp").withNpcs(listOf(mods[0].npcs[0]))
        chimmer.save(newMod)

        // Then we should be able to load it again (because it got zipped back up on save)
        val reloadedMod = chimmer.load(outputFolder.root, listOf("MyMod.esp"))[0]
        assertEquals("dunTransmogrifyDremora", reloadedMod.npcs[0].editorId)
    }
}