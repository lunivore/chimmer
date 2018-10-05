package com.lunivore.chimmer

import com.lunivore.chimmer.scenariohelpers.asResourceFile
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class CreatingANewMod {

    @get:Rule
    val outputFolder = TemporaryFolder()


    @Test
    fun `should be able to create a new mod with the contents of an old one`() {
        // Given we loaded a mod with an iron sword in
        val plugins = listOf("Skyrim.esm", "IronSword.esp")
        val modDirectory = asResourceFile("plugins.txt").parentFile
        val chimmer = Chimmer(outputFolder.root, loadRealSkyrimFiles = false)
        var mods = chimmer.load(modDirectory, plugins)

        // When we parse a new mod with the same iron sword
        val modName = "NewIronSword_${System.currentTimeMillis()}.esp"
        val mod = chimmer.createMod(modName).withWeapons(mods[0].weapons)

        // Then we should be able to save it successfully
        chimmer.save(mod)

        // NB: Do not automate the checks for file contents - load it up in TES5Edit and make sure it's good!
        Assert.assertTrue(File(outputFolder.root, modName).exists())

        // Cleaning up
        File(modName).delete()
    }

    @Test
    fun `should be able to create a new item rather than an override and keep it consistent`() {
        // Given we loaded a mod with an iron sword in it
        // along with a consistency file that will enable us to create new records and keep them consistent

        val plugins = listOf("IronSword.esp")
        val modDirectory = asResourceFile("plugins.txt").parentFile
        val chimmer = Chimmer(outputFolder.root)
        var mods = chimmer.load(modDirectory, plugins)

        // When we create a new mod with a copy of that sword that's not an override
        val modName = "NewIronSword_${System.currentTimeMillis()}.esp"
        val sword = mods[0].weapons.first().copyAsNew(modName)

        // TODO Looks as if this is using the consistency recorder from the original mod,
        // rather than a new one with the new mod name. It's working,
        // but I suspect that may be coincidental - need to check!

        val newMod = chimmer.createMod(modName).withWeapons(listOf(sword))

        // Then the sword should be given an appropriate ID
        val formId = newMod.weapons.first().formId()

        // When we save the mod
        chimmer.save(newMod)

        // And we reload the mod again (in a new instance of Chimmer)
        mods = Chimmer(outputFolder.root).load(outputFolder.root, listOf(modName))

        // Then the sword should have the same ID
        val reloadedSwordId = mods[0].weapons.first().formId()
        assertEquals(formId, reloadedSwordId)
    }

    @Test
    fun `Just a reminder to also check that created mods are fine when loaded with TES5Edit`(){}

}