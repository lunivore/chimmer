package com.lunivore.chimmer

import com.lunivore.chimmer.testheplers.asResourceFile
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class MergingMods {

    @get:Rule
    val outputFolder = TemporaryFolder()

    @Test
    fun `should be able to merge mods, taking the last loaded record`() {
        // Given a list of mods with iron swords
        val plugins = listOf("Skyrim.esm", "Dawnguard.esm", "ArmorBootsSwordCrossbow.esp", "IronSword.esp")
        val modDirectory = asResourceFile("plugins.txt").parentFile

        // When we load them using that order, then merge them
        val chimmer = Chimmer(outputFolder.root)
        var mods = chimmer.load(modDirectory, plugins, false)
        val newMod = chimmer.merge("NewMod.esp", mods)

        // Then we should have all the records present
        assertEquals(2, newMod.weapons.count())
        assertEquals(2, newMod.armors.count())

        // And the last-loaded iron sword should be the "winner"
        assertEquals("IronSword.esp", newMod.weapons.first {it.editorId == "IronSword"}.formId.loadingMod)
    }
}

