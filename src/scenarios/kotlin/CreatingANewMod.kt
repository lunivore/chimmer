package com.lunivore.chimmer

import com.lunivore.chimmer.scenariohelpers.asResourceFile
import org.junit.Assert
import org.junit.Test
import java.io.File

class CreatingANewMod {

    @Test
    fun `should be able to create a new mod with the contents of an old one`() {
        // Given we loaded a mod with an iron sword in
        val plugins = listOf("IronSword.esp")
        val modDirectory = asResourceFile("plugins.txt").parentFile
        var mods = Chimmer().load(modDirectory, plugins, null)

        // When we parseAll a new mod with the same iron sword
        val mod = Chimmer().createMod("Chimmer.esp").withWeapons(mods[0].weapons)

        // Then we should be able to save it successfully
        val filename = "NewIronSword_${System.currentTimeMillis()}.esp"
        Chimmer().save(mods[0], File("out"), filename)

        // NB: Do not automate the checks for file contents - load it up in TES5Edit and make sure it's good!
        Assert.assertTrue(File("out/$filename").exists())
    }

}