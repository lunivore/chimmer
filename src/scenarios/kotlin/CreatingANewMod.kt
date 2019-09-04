package com.lunivore.chimmer

import com.lunivore.chimmer.testheplers.asResourceFile
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

@UseExperimental(ExperimentalUnsignedTypes::class)
class CreatingANewMod : ChimmerScenario() {

    @Test
    fun `should be able to create a new mod with the contents of an old one`() {
        // Given we loaded a mod with an iron sword in
        val plugins = listOf("Skyrim.esm", "IronSword.esp")
        val modDirectory = asResourceFile("plugins.txt").parentFile
        val chimmer = Chimmer(fileHandler())
        var mods = chimmer.load(modDirectory, plugins,  ModsToLoad.SKIP_BETHESDA_MODS)

        // When we parseAll a new mod with the same iron sword
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
        val chimmer = Chimmer(fileHandler())
        var mods = chimmer.load(modDirectory, plugins,  ModsToLoad.SKIP_BETHESDA_MODS)

        // When we create a new mod with a copy of that sword that's not an override
        val modName = "NewIronSword_${System.currentTimeMillis()}.esp"
        val sword = mods[0].weapons.first().copyAsNew(modName, "MyMod_IronSword")
        val newMod1 = chimmer.createMod(modName).withWeapons(listOf(sword))

        // And we save the mod then reload it
        chimmer.save(newMod1, plugins)
        val reloadedMod1 = chimmer.load(outputFolder.root, listOf("Skyrim.esm", modName),  ModsToLoad.SKIP_BETHESDA_MODS)[0]

        // And trash and recreate the sword, then save and load again
        val newMod2 = chimmer.createMod(modName).withWeapons(listOf(sword))
        chimmer.save(newMod2, plugins)
        val reloadedMod2 = chimmer.load(outputFolder.root, listOf("Skyrim.esm", modName),  ModsToLoad.SKIP_BETHESDA_MODS)[0]


        // Then the sword should have the same ID
        val reloadedSwordId1 = reloadedMod1.weapons.first().formId
        val reloadedSwordId2 = reloadedMod2.weapons.first().formId
        assertEquals(reloadedSwordId1, reloadedSwordId2)
    }

}