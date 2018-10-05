package com.lunivore.chimmer

import com.lunivore.chimmer.scenariohelpers.asResourceFile
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class MergingAndFiltering {

    @get:Rule
    val outputFolder = TemporaryFolder()

    @Test
    fun `should be able to filter a mod on relevant groups and records within them`() {
        val chimmer = Chimmer(outputFolder.root)

        // Given we have a mod with armor, boots, a sword and a crossbow
        // And another one with an iron sword
        val plugins = asResourceFile("plugins.txt")
        val modDirectory = plugins.parentFile
        val mods = chimmer.load(modDirectory, listOf("ArmorBootsSwordCrossbow.esp", "IronSword.esp"))

        // When we filter them for just the iron sword
        val newMod = chimmer.createMod("NewMod.esp")
                .withWeapons(mods.weapons { it.name == "Iron Sword" })
        // TODO This syntax isn't nice; we should provide some easy way to make a mod from merged mods

        // Then we should get back a mod with just the last iron sword in it
        // TODO - need to change something in the iron sword so we can check this

    }
}




