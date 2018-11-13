package com.lunivore.chimmer

import com.lunivore.chimmer.scenariohelpers.asResourceFile
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@UseExperimental(ExperimentalUnsignedTypes::class)
class CreatingANewMod {

    @get:Rule
    val outputFolder = TemporaryFolder()


    @Test
    fun `should be able to create a new mod with the contents of an old one`() {
        // Given we loaded a mod with an iron sword in
        val plugins = listOf("Skyrim.esm", "IronSword.esp")
        val modDirectory = asResourceFile("plugins.txt").parentFile
        val chimmer = Chimmer(outputFolder.root)
        var mods = chimmer.load(modDirectory, plugins, false)

        // When we parse a new mod with the same iron sword
        val modName = "NewIronSword_${System.currentTimeMillis()}.esp"
        val mod = chimmer.createMod(modName).withWeapons(mods[0].weapons)

        // Then we should be able to save it successfully
        chimmer.save(mod, plugins)

        // NB: Do not automate the checks for file contents - load it up in TES5Edit and make sure it's good!
        Assert.assertTrue(File(outputFolder.root, modName).exists())

        // Cleaning up
        File(modName).delete()
    }

    @Test
    fun `should be able to create a new item rather than an override and keep it consistent`() {
        // Given we loaded a mod with an iron sword in it
        // along with a consistency file that will enable us to create new records and keep them consistent

        val plugins = listOf("Skyrim.esm", "IronSword.esp")
        val modDirectory = asResourceFile("plugins.txt").parentFile
        val chimmer = Chimmer(outputFolder.root)
        var mods = chimmer.load(modDirectory, plugins, false)

        // When we create a new mod with a copy of that sword that's not an override
        val sword = mods[0].weapons.first().copyAsNew()
        val modName = "NewIronSword_${System.currentTimeMillis()}.esp"
        val newMod = chimmer.createMod(modName).withWeapons(listOf(sword))

        // Then the sword should be given an appropriate ID
        val formId = newMod.weapons.first().formId

        // When we save the mod
        chimmer.save(newMod, plugins)

        // Then the consistency file should have been created with the new mod name
        assertTrue(File(outputFolder.root, "chimmer/${modName}_consistency.txt").exists())

        // When we reload the mod again
        mods = Chimmer(outputFolder.root).load(outputFolder.root, listOf("Skyrim.esm", modName), false)

        // Then the sword should have the same ID
        val reloadedSwordId = newMod.weapons.first().formId
        assertEquals(formId.toBigEndianHexString(), reloadedSwordId.toBigEndianHexString())
    }

}