package com.lunivore.chimmer.binary

import com.lunivore.chimmer.testheplers.Hex
import com.lunivore.chimmer.testheplers.fakeConsistencyRecorder
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
        val modBinary = ModBinary.parse(modBytes, ::fakeConsistencyRecorder)

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
        val modBinary = ModBinary.parse(modBytes, ::fakeConsistencyRecorder)

        // When we ask it to replace the weapons with a Dawnguard crossbow
        val crossbowRecord = Record.parseAll(Hex.CROSSBOW_WEAPON.fromHexStringToByteList(), listOf("Dawnguard.esm"), ::fakeConsistencyRecorder).parsed[0]
        val crossbow = GenericRecordWrapper(crossbowRecord)
        val newModBinary = modBinary.createOrReplaceGrup("WEAP", listOf(crossbow))

        // Then it should have the new crossbow in its records and not the old sword
        assertEquals(crossbowRecord, newModBinary.first { it.isType("WEAP") }.first())
    }

    @Test
    fun `should be able to add grups in the right place when they do not already exist`() {
        // Given a mod with grups TREE, FLOR and AMMO (WEAP belonging between FLOR and AMMO)
        val modBinary = ModBinary.create(::fakeConsistencyRecorder).copy(grups = listOf(
                Grup("TREE", listOf(), listOf()),
                Grup("FLOR", listOf(), listOf()),
                Grup("AMMO", listOf(), listOf())))

        // When we add the Dawnguard crossbow
        val crossbowRecord = Record.parseAll(Hex.CROSSBOW_WEAPON.fromHexStringToByteList(), listOf("Dawnguard.esm"), ::fakeConsistencyRecorder).parsed[0]
        val crossbow = GenericRecordWrapper(crossbowRecord)
        val newModBinary = modBinary.createOrReplaceGrup("WEAP", listOf(crossbow))

        // Then the weapons group should be in the right place with the Dawnguard crossbow added
        assertEquals("TREE, FLOR, WEAP, AMMO".split(", "), newModBinary.map { it.type })
        assertEquals(crossbowRecord, newModBinary[2].first())
    }

    @Test
    fun `should be able to create a new ModBinary with an appropriate header`() {
        // When we create a new mod binary
        val modBinary = ModBinary.create(::fakeConsistencyRecorder)

        // Then it should have all the appropriate fields set correctly in the TES4 record's header
        assertEquals("TES4", modBinary.header.type)
        assertEquals(0, modBinary.header.flags)
        assertEquals(0, modBinary.header.formId)
        assertEquals(43, modBinary.header.formVersion) // Oldrim
        assertTrue(modBinary.grups.isEmpty())

        // And appropriate fields set in the TES4 record itself too.
        assertEquals(1.7f, modBinary.header.find("HEDR")?.asFloat())
        assertEquals("Chimmer", modBinary.header.find("CNAM")?.asString())
        assertNull(modBinary.header.find("MAST"))
    }

    @Test
    fun `should be able to find a grup of a particular type`() {
        // Given a mod which contains an iron sword
        val binary = (Hex.CHIMMER_MOD_HEADER + Hex.IRON_SWORD_WEAPON_GROUP).replace(" ", "")
        val modBytes = DatatypeConverter.parseHexBinary(binary)
        val modBinary = ModBinary.parse(modBytes, ::fakeConsistencyRecorder)

        // Then we should be able to get the weapons group
        assertEquals("WEAP", modBinary.find("WEAP")?.type)

        // But not the Wibble group which doesn't exist
        assertNull(modBinary.find("WIBB"))
    }

    data class GenericRecordWrapper(override val record: Record) : RecordWrapper<GenericRecordWrapper>
}