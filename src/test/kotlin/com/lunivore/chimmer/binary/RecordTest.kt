package com.lunivore.chimmer.binary

import com.lunivore.chimmer.testheplers.Hex
import com.lunivore.chimmer.testheplers.fromHexStringToByteList
import com.lunivore.chimmer.testheplers.toReadableHexString
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayOutputStream

class RecordTest {

    @Test
    fun `should be able to parse Tes4 from bytes`() {
        // Given a mod which contains an iron sword
        val binary = (Hex.CHIMMER_MOD_HEADER + Hex.IRON_SWORD_WEAPON_GROUP).replace(" ", "")

        // When we parse the record header from it
        val result = Record.parseTes4(binary.fromHexStringToByteList())

        // Then we should get back one record  with all the relevant subrecords in
        val record = result.parsed
        assertEquals("HEDR, CNAM, MAST, DATA".split(", "), record.map { it.type })

        // And the records should have the correct data
        assertEquals("Chimmer", record.find("CNAM")?.asString())

        // And the rest of the data should also be passed back in the result.
        val expectedHex = Hex.IRON_SWORD_WEAPON_GROUP
        val actualHex = result.rest.toByteArray().toReadableHexString()
        assertEquals(expectedHex, actualHex)
    }

    @Test
    fun `should be able to parse non-Tes4 records from bytes too`() {
        // Given an iron sword weapon as bytes
        val hex = Hex.IRON_SWORD_WEAPON

        // And some other random data
        val rest = "0E FF BB DD"

        // When we get the record from it
        val result = Record.parseAll((hex + rest).fromHexStringToByteList(), listOf("Skyrim.esm"))

        // Then we should have back a record of type "WEAP"
        assertEquals("WEAP", String(result.parsed[0].header.subList(0, 4).toByteArray()))

        // And the rest should be returned too for further parsing
        assertEquals(rest, result.rest.toReadableHexString())
    }

    @Test
    fun `should render itself and its subrecords as bytes on request`() {
        // Given a record containing an iron sword
        val hex = Hex.IRON_SWORD_WEAPON
        val record = Record.parseAll(hex.fromHexStringToByteList(), listOf("Skyrim.esm")).parsed[0]

        // When we turn it back into bytes

        val rendered = ByteArrayOutputStream()
        record.renderTo { rendered.write(it) }

        // Then we should have the bytes back again identically
        assertEquals(hex, rendered.toByteArray().toReadableHexString())

    }
}

