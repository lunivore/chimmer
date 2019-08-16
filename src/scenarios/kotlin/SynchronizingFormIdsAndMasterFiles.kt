package com.lunivore.chimmer

import com.lunivore.chimmer.binary.ModBinary
import com.lunivore.chimmer.testheplers.asResourceFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File


class SynchronizingFormIdsAndMasterFiles()  : ChimmerScenario() {

    @Test
    fun `should provide the masterlist of a new mod`() {
        // Given a mod with Skyrim's iron sword and Dawnguard's crossbow as overrides
        val plugins = listOf("IronSword.esp", "ArmorBootsSwordCrossbow.esp")
        val modDirectory = asResourceFile("IronSword.esp").parentFile
        val chimmer = Chimmer(fileHandler())
        val mods = chimmer.load(modDirectory, plugins)
        val newMod = chimmer.createMod("NewMod.esp").withWeapons(listOf(
                mods[0].weapons[0],
                mods[1].weapons.first { it.formId.master == "Dawnguard.esm" }))

        // When we ask that mod for its masterlist
        val masters = newMod.masters

        // Then it should contain both Skyrim and Dawnguard
        assertTrue(masters.contains("Skyrim.esm"))
        assertTrue(masters.contains("Dawnguard.esm"))
    }

    @Test
    fun `should change the indexed FormId of an overridden record so that it matches the new masterlist`() {

        // Given two mods which both have an iron sword copied as a new record
        val plugins = listOf("Skyrim.esm", "IronSword.esp")
        val modDirectory = asResourceFile("plugins.txt").parentFile
        val chimmer = Chimmer(fileHandler())
        var mods = chimmer.load(modDirectory, plugins,  ModsToLoad.SKIP_BETHESDA_MODS)

        val modName1 = "IronSword1_${System.currentTimeMillis()}.esp"
        val sword1 = mods[0].weapons.first().copyAsNew(modName1, "MyMod1_IronSword")
        val newMod1 = chimmer.createMod(modName1).withWeapons(listOf(sword1))

        val modName2 = "IronSword2_${System.currentTimeMillis()}.esp"
        val sword2 = mods[0].weapons.first().copyAsNew(modName2, "MyMod2_IronSword")
        val newMod2 = chimmer.createMod(modName2).withWeapons(listOf(sword2))

        chimmer.save(newMod1)
        chimmer.save(newMod2)

        // When we load them and merge those two weapons into one new mod which overrides them
        // (we need to copy the Iron Sword across too)
        asResourceFile("IronSword.esp").copyTo(File(outputFolder.root, "IronSword.esp"))
        val reloadedMods = chimmer.load(outputFolder.root, listOf("Skyrim.esm", "IronSword.esp", modName1, modName2),  ModsToLoad.SKIP_BETHESDA_MODS)


        val mergedModName = "IronSwords_${System.currentTimeMillis()}.esp"
        val mergedMod = chimmer.createMod(mergedModName)
                .withWeapons(listOf(reloadedMods[1].weapons[0], reloadedMods[2].weapons[0]))

        // And we save that mod
        chimmer.save(mergedMod)

        // Then the hex representing the two form ids should have the index of the appropriate masters
        val mergedModBinary = ModBinary.parse(mergedModName, File(outputFolder.root, mergedModName).readBytes())

        val weaps = mergedModBinary.grups[0]
        val formIds = weaps.map { it.formId }

        assertEquals(modName1, formIds[0].master)
        assertEquals(modName2, formIds[1].master)
    }

    @Test
    fun `should change FormIds within an overridden record so that they match the new masterlist`() {
        // Given a mod with an Iron Sword in it and another with a crossbow
        val plugins = listOf("Skyrim.esm", "Dawnguard.esm", "IronSword.esp", "ArmorBootsSwordCrossbow.esp", "MiscellaneousKeyword.esp")
        val modDirectory = asResourceFile("plugins.txt").parentFile
        var chimmer = Chimmer(fileHandler())
        val mods = chimmer.load(modDirectory, plugins,  ModsToLoad.SKIP_BETHESDA_MODS)

        // When we add the crossbow, then the sword with a keyword from another mod and save it as a new mod
        // (we need to add the crossbow to force update the ids)
        val oldSword = mods[0].weapons.first()
        val crossbow = mods[1].weapons[1]

        val keywordToAdd = mods[2].keywords.first().formId as ExistingFormId
        val newModName = "NewMod_${System.currentTimeMillis()}.esp"
        val newSword = oldSword.copyAsNew(newModName, "MyMod_IronSword").plusKeyword(keywordToAdd)

        val newMod = chimmer.createMod(newModName).withWeapons(listOf(crossbow, newSword))

        chimmer.save(newMod)

        // And reload it along with Dawnguard (we need to copy the old mods to the new folder too)
        File(modDirectory, "IronSword.esp").copyTo(File(outputFolder.root, "IronSword.esp"))
        File(modDirectory, "MiscellaneousKeyword.esp").copyTo(File(outputFolder.root, "MiscellaneousKeyword.esp"))

        val newModList = listOf("Skyrim.esm", "Dawnguard.esm", "IronSword.esp", "MiscellaneousKeyword.esp", newModName)
        chimmer = Chimmer(fileHandler())
        val reloadedMods = chimmer.load(outputFolder.root, newModList,  ModsToLoad.SKIP_BETHESDA_MODS)

        // Then the keyword should be updated to reflect the position in the new masterlist
        // (Remember we don't actually load Skyrim or Dawnguard!)
        val expectedKeyword = FormId.create(newModName, 0x01000000u or (keywordToAdd).unindexed, listOf("Skyrim.esm", "Dawnguard.esm", "MiscellaneousKeyword.esp")) as ExistingFormId
        val reloadedSword = reloadedMods[2].weapons[1]
        val actualKeyword = reloadedSword.keywords[3] as ExistingFormId

        // (This first line is just easier to read if it goes wrong)
        assertEquals(expectedKeyword.toBigEndianHexString(), actualKeyword.toBigEndianHexString())
        assertEquals(expectedKeyword.key, actualKeyword.key)
    }
}