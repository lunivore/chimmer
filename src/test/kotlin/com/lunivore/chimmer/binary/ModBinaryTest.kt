package com.lunivore.chimmer.binary

import com.lunivore.chimmer.FormId
import com.lunivore.chimmer.skyrim.SkyrimObject
import com.lunivore.chimmer.testheplers.Hex
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import javax.xml.bind.DatatypeConverter

class ModBinaryTest {
    companion object {
        data class SkyrimThing(override val record: Record) : SkyrimObject<SkyrimThing>(record) {
            override fun create(record: Record): SkyrimThing {
                return SkyrimThing(record)
            }
        }
    }

    @Test
    fun `should separate bytes into TES4 header and grups`() {
        // Given a mod which contains an iron sword
        val binary = (Hex.CHIMMER_MOD_HEADER + Hex.IRON_SWORD_WEAPON_GROUP).replace(" ", "")
        val modBytes = DatatypeConverter.parseHexBinary(binary)

        // When we parse a ModBinary from it
        val modBinary = ModBinary.parse("Wibble.esp", modBytes)

        // Then it should have a header with the correct author
        assertEquals("Chimmer", modBinary.header.find("CNAM")?.asString())

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
        val modBinary = ModBinary.parse("Wibble.esp", modBytes)

        // When we ask it to replace the weapons with a Dawnguard crossbow
        val crossbowRecord = Record.parseAll("Wibble.esp", Hex.CROSSBOW_WEAPON.fromHexStringToByteList(), listOf("Dawnguard.esm")).parsed[0]
        val crossbow = SkyrimThing(crossbowRecord)
        val newModBinary = modBinary.createOrReplaceGrup("WEAP", listOf(crossbow))

        // Then it should have the new crossbow in its records and not the old sword
        assertEquals(crossbowRecord, newModBinary.first { it.isType("WEAP") }.first())
    }

    @Test
    fun `should be able to add grups in the right place when they do not already exist`() {
        // Given a mod with grups TREE, FLOR and AMMO (WEAP belonging between FLOR and AMMO)
        val modBinary = ModBinary.create().copy(grups = listOf(
                Grup("TREE", listOf(), listOf()),
                Grup("FLOR", listOf(), listOf()),
                Grup("AMMO", listOf(), listOf())))

        // When we add the Dawnguard crossbow
        val crossbowRecord = Record.parseAll("Wibble.esp", Hex.CROSSBOW_WEAPON.fromHexStringToByteList(), listOf("Dawnguard.esm")).parsed[0]
        val crossbow = SkyrimThing(crossbowRecord)
        val newModBinary = modBinary.createOrReplaceGrup("WEAP", listOf(crossbow))

        // Then the weapons group should be in the right place with the Dawnguard crossbow added
        assertEquals("TREE, FLOR, WEAP, AMMO".split(", "), newModBinary.map { it.type })
        assertEquals(crossbowRecord, newModBinary[2].first())
    }

    @Test
    fun `should be able to create a new ModBinary with an appropriate header`() {
        // When we create a new mod binary
        val modBinary = ModBinary.create()

        // Then it should have all the appropriate fields set correctly in the TES4 record's header
        assertEquals("TES4", modBinary.header.type)
        assertEquals(0u, modBinary.header.flags)
        assertEquals(FormId.TES4, modBinary.header.formId)
        assertEquals(43.toUShort(), modBinary.header.formVersion) // Oldrim
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
        val modBinary = ModBinary.parse("Wibble.esp", modBytes)

        // Then we should be able to get the weapons group
        assertEquals("WEAP", modBinary.find("WEAP")?.type)

        // But not the Wibble group which doesn't exist
        assertNull(modBinary.find("WIBB"))
    }

    @Test
    fun `should be able to render the appropriate masters to a new TES4 header`() {
        // Given a new mod, copying an iron sword as a new record
        val binary = (Hex.CHIMMER_MOD_HEADER + Hex.IRON_SWORD_WEAPON_GROUP).replace(" ", "")
        val modBytes = DatatypeConverter.parseHexBinary(binary)
        val ironSwordModBinary = ModBinary.parse("Whatever.esp", modBytes) // Name doesn't matter as Iron Sword still
                                                                           // has Skyrim as a master right now

        val firstNewModBinary = ModBinary.create().copy(grups = listOf(ironSwordModBinary.grups[0].copy(
                records = listOf(ironSwordModBinary.grups[0].records[0].copyAsNew())
        )))

        // Which is saved
        val firstBytes = ByteArrayOutputStream()
        val unindexedKeywordforNewSwordConsistency = 0xabcdefu
        firstNewModBinary.render(listOf("Skyrim.esm"), { unindexedKeywordforNewSwordConsistency }) {firstBytes.write(it)}

        // Then reloaded, and the iron sword copied to another mod (taking its master from whatever it's loaded from)
        val originalFirstBinary = ModBinary.parse("MyMod.esp", firstBytes.toByteArray())
        val secondNewModBinary = ModBinary.create().copy(grups = listOf(originalFirstBinary.grups[0].copy(
                records = listOf(originalFirstBinary.grups[0].records[0]) // Note not copying this as new
        )))

        // Then saved again
        val secondBytes = ByteArrayOutputStream()
        secondNewModBinary.render(listOf("Skyrim.esm", "MyMod.esp"), { throw Exception("Should not be needed") })
            {secondBytes.write(it)}

        // Then the TES4 header should have been rendered out with the appropriate masters
        val reloadedMod = ModBinary.parse("MySecondMod.esp", secondBytes.toByteArray())
        assertEquals(listOf("Skyrim.esm","MyMod.esp"), reloadedMod.header.masters)

        // (including the new "origin" of the new Iron Sword.
        assertEquals("MyMod.esp", reloadedMod.grups[0].records[0].formId.master)
    }
}