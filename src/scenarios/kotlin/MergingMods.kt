package com.lunivore.chimmer

import com.lunivore.chimmer.testheplers.asResourceFile
import org.junit.Assert.assertEquals
import org.junit.Test

class MergingMods : ChimmerScenario() {

    @Test
    fun `should be able to merge mods, taking the last loaded record`() {
        // Given a list of mods with iron swords
        val plugins = listOf("Skyrim.esm", "Dawnguard.esm", "ArmorBootsSwordCrossbow.esp", "IronSword.esp")
        val modDirectory = asResourceFile("plugins.txt").parentFile

        // When we load them using that order, then merge them
        val chimmer = Chimmer(fileHandler())
        var mods = chimmer.load(modDirectory, plugins,  ModsToLoad.SKIP_BETHESDA_MODS)
        val newMod = chimmer.merge("NewMod.esp", mods)

        // Then we should have all the records present
        assertEquals(2, newMod.weapons.count())
        assertEquals(2, newMod.armors.count())

        // And the last-loaded iron sword should be the "winner"
        assertEquals("IronSword.esp", (newMod.weapons.first {it.editorId == "IronSword"}.formId as ExistingFormId).loadingMod)
    }
}

