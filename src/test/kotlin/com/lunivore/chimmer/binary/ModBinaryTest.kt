package com.lunivore.chimmer.binary

import com.lunivore.chimmer.testheplers.Hex
import org.junit.Assert.assertEquals
import org.junit.Test
import javax.xml.bind.DatatypeConverter

class ModBinaryTest {

    @Test
    fun `should separate bytes into TES4 header and grups`() {
        // Given a mod which contains an iron sword
        val binary = (Hex.CHIMMER_MOD_HEADER + Hex.IRON_SWORD_WEAPON_GROUP).replace(" ", "")
        val modBytes = DatatypeConverter.parseHexBinary(binary)

        // When we parseAll a ModBinary from it
        val modBinary = ModBinary.parse(modBytes)

        // Then it should have a header with the correct author
        assertEquals("Chimmer", modBinary.header.find { it.type == "CNAM" }?.asString())

        // And the correct masters
        assertEquals(listOf("Skyrim.esm"), modBinary.header.masters)

        // And one weapon group
        assertEquals(1, modBinary.grups.size)

        // (Yes, I know this is violating the Law of Demeter but it's how mods are organized.
        // We'll sort it all out in the Skyrim-equivalent wrappers.)
        assertEquals("WEAP", String(modBinary.first().first().header.subList(0, 4).toByteArray()))
    }
}
