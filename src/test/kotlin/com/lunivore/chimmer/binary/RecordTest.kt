package com.lunivore.chimmer.binary

import com.lunivore.chimmer.ConsistencyRecorder
import com.lunivore.chimmer.testheplers.Hex
import com.lunivore.chimmer.testheplers.fakeConsistencyRecorder
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
        val result = Record.parseAll((hex + rest).fromHexStringToByteList(), listOf("Skyrim.esm"), ::fakeConsistencyRecorder)

        // Then we should have back a record of type "WEAP"
        assertEquals("WEAP", result.parsed[0].type)

        // And the rest should be returned too for further parsing
        assertEquals(rest, result.rest.toReadableHexString())
    }

    @Test
    fun `should render itself and its subrecords as bytes on request`() {
        // Given a record containing an iron sword
        val hex = Hex.IRON_SWORD_WEAPON
        val record = Record.parseAll(hex.fromHexStringToByteList(), listOf("Skyrim.esm"), ::fakeConsistencyRecorder).parsed[0]

        // When we turn it back into bytes

        val rendered = ByteArrayOutputStream()
        record.renderTo { rendered.write(it) }

        // Then we should have the bytes back again identically
        assertEquals(hex, rendered.toByteArray().toReadableHexString())
    }

    @Test
    fun `should be able to copy itself as new, looking up any existing form id in the consistency accessor where appropriate`() {

        // Given a record containing an iron sword
        // And a new sword that was generated last time with a new EDID that was tracked for consistency
        val consistencyRecorder: ConsistencyRecorder = {
            if (it == "MY_MOD_Editor_Id_123456") "00ABCD".fromHexStringToByteList().toLittleEndianInt()
            else throw IllegalArgumentException("This will not happen in this test code.")
        }

        val hex = Hex.IRON_SWORD_WEAPON
        val record = Record.parseAll(hex.fromHexStringToByteList(), listOf("Skyrim.esm"), consistencyRecorder).parsed[0]

        // When we copy the record with a new editor id as new
        val newRecord = record.with(Subrecord("EDID", "MY_MOD_Editor_Id_123456\u0000".toByteList())).copyAsNew()

        // Then the form Id should be the newRecord but with an index of 01 (because 00 is Skyrim.esm)
        // (Remembering little Endian is reversed so the "01" is at the end, not the beginning of the field in binary)
        assertEquals("00 AB CD 01", newRecord.formId.toLittleEndianBytes().toReadableHexString())
    }
}


