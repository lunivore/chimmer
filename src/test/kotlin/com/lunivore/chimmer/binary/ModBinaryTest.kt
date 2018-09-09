package com.lunivore.chimmer.binary

import com.lunivore.chimmer.testheplers.Hex
import com.lunivore.chimmer.testheplers.fromHexStringToByteList
import org.junit.Assert.*
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

        assertEquals("WEAP", modBinary.first().first().type)
    }

    @Test
    fun `should be able to replace grups`() {
        // Given a mod which contains an iron sword
        val binary = (Hex.CHIMMER_MOD_HEADER + Hex.IRON_SWORD_WEAPON_GROUP).replace(" ", "")
        val modBytes = DatatypeConverter.parseHexBinary(binary)
        val modBinary = ModBinary.parse(modBytes)

        // When we ask it to replace the weapons with a Dawnguard crossbow
        val crossbowRecord = Record.parseAll(Hex.CROSSBOW_WEAPON.fromHexStringToByteList(), listOf("Dawnguard.esm")).parsed[0]
        val crossbow = GenericRecordWrapper(crossbowRecord)
        val newModBinary = modBinary.replaceGrup("WEAP", listOf(crossbow))

        // Then it should have the new crossbow in its records and not the old sword
        assertEquals(crossbowRecord, newModBinary.first { it.isType("WEAP") }.first())
    }

    @Test
    fun `should be able to create a new ModBinary with an appropriate header`() {
        // When we create a new mod binary
        val modBinary = ModBinary.create()

        // Then it should have all the appropriate fields set correctly in the TES4 record's header
        assertEquals("TES4", modBinary.header.type)
        assertEquals(0, modBinary.header.flags)
        assertEquals("00000000".toByteArray().toList(), modBinary.header.formId)
        assertEquals(43, modBinary.header.formVersion) // Oldrim
        assertTrue(modBinary.grups.isEmpty())

        // And appropriate fields set in the TES4 record itself too.
        assertEquals(1.7f, modBinary.header.find("HEDR")?.asFloat())
        assertEquals("Chimmer", modBinary.header.find("CNAM")?.asString())
        assertNull(modBinary.header.find("MAST"))
    }

    data class GenericRecordWrapper(override val record : Record) : RecordWrapper
}