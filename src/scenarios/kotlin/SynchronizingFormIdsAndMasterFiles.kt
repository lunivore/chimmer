package com.lunivore.chimmer

import com.lunivore.chimmer.binary.ModBinary
import com.lunivore.chimmer.scenariohelpers.asResourceFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File


class SynchronizingFormIdsAndMasterFiles() {

    @get:Rule
    val outputFolder = TemporaryFolder()

    @Test
    fun `should provide the masterlist of a new mod`() {
        // Given a mod with Skyrim's iron sword and Dawnguard's crossbow as overrides
        val plugins = listOf("IronSword.esp", "ArmorBootsSwordCrossbow.esp")
        val modDirectory = asResourceFile("IronSword.esp").parentFile
        val chimmer = Chimmer(outputFolder.root)
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
        val plugins = listOf("IronSword.esp")
        val modDirectory = asResourceFile("plugins.txt").parentFile
        val chimmer = Chimmer(outputFolder.root)
        var mods = chimmer.load(modDirectory, plugins)

        val sword1 = mods[0].weapons.first().copyAsNew()
        val modName1 = "IronSword1_${System.currentTimeMillis()}.esp"
        val newMod1 = chimmer.createMod(modName1).withWeapons(listOf(sword1))

        val sword2 = mods[0].weapons.first().copyAsNew()
        val modName2 = "IronSword2_${System.currentTimeMillis()}.esp"
        val newMod2 = chimmer.createMod(modName2).withWeapons(listOf(sword2))

        chimmer.save(newMod1)
        chimmer.save(newMod2)

        // When we load them and merge those two weapons into one new mod which overrides them
        val reloadedMods = chimmer.load(outputFolder.root, listOf(modName1, modName2))

        val mergedModName = "IronSwords_${System.currentTimeMillis()}.esp"
        val mergedMod = chimmer.createMod(mergedModName)
                .withWeapons(listOf(reloadedMods[0].weapons[0], reloadedMods[1].weapons[0]))

        // And we save that mod
        chimmer.save(mergedMod)

        // Then the hex representing the two form ids should have the index of the appropriate masters
        val mergedModBinary = ModBinary.parse(mergedModName, File(outputFolder.root, mergedModName).readBytes())

        val weaps = mergedModBinary.grups[0]
        val formIds = weaps.map { it.formId }

        assertEquals(modName1, formIds[0].master)
        assertEquals(modName2, formIds[1].master)
    }
}