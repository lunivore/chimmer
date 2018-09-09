package com.lunivore.chimmer.binary

import com.lunivore.chimmer.testheplers.Hex
import com.lunivore.chimmer.testheplers.fromHexStringToByteList
import com.lunivore.chimmer.testheplers.toReadableHexString
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayOutputStream

class GrupTest {

    @Test
    fun `should parse all groups and the records of any we're interested in from bytes`() {
        // Given some groups, some of which we're interested in
        val hex = Hex.IRON_SWORD_WEAPON_GROUP + Hex.UNINTERESTING_COLOUR_GROUP

        // When we parseAll the group
        val grups = Grup.parseAll(hex.fromHexStringToByteList(), listOf("Skyrim.esm"))

        // Then we should be able to get the top-level records from it
        val weaponGrup = grups.first()
        val weaps = weaponGrup.records

        assertEquals(1, weaps.size)
    }

    @Test
    fun `should render group to bytes`() {
        // Given a group that we constructed (so the size wasn't parsed in from bytes)
        val groupHeaderBytes = Hex.IRON_SWORD_GROUP_HEADER.fromHexStringToByteList().subList(8, 24)
        val recordHex = Hex.IRON_SWORD_WEAPON + " " + Hex.CROSSBOW_WEAPON
        val recordBytes = recordHex.fromHexStringToByteList()
        val masters = listOf("Skyrim.esm", "Dawnguard.esm")
        val grup = Grup(groupHeaderBytes, Record.parseAll(recordBytes, masters).parsed)

        // When we render the group back to bytes again
        val renderer = ByteArrayOutputStream()
        grup.renderTo {renderer.write(it)}

        // Then it should contain the same bytes as the original
        val result = renderer.toByteArray()
        val actualContents = result.toList().subList(24, result.size).toReadableHexString()
        assertEquals(recordHex, actualContents)

        // And it should have a size field of 24 + record data size
        val expectedSize = 24 + recordBytes.size
        val actualSize = result.toList().subList(4, 8).toLittleEndianInt()

        assertEquals(expectedSize, actualSize)
    }
}