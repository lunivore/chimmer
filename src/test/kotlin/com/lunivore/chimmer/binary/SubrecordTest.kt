package com.lunivore.chimmer.binary

import com.lunivore.chimmer.testheplers.toReadableHexString
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test

@UseExperimental(ExperimentalUnsignedTypes::class)
class SubrecordTest {

    @Test
    fun `should parse itself from raw bytes`() {
        // Given an EDID field with a length of 10 (0A 00 -> littleEndian so 00 0A = 10)
        val field = "45 44 49 44 0A 00 49 72 6F 6E 53 77 6F 72 64 00"

        // And some more bytes
        val rest = "FF EE DD CC"

        // When we render a subrecord from the bytes
        val parseResult = Subrecord.parse(SkyrimSubrecordMenu(), "WEAP", (field + rest).fromHexStringToByteList())
        val subrecord = parseResult.parsed[0]

        // Then it should read the type from the first four digits and parse the length appropriately
        assertEquals("EDID", subrecord.type)
        assertEquals("49 72 6F 6E 53 77 6F 72 64 00", parseResult.parsed[0].toReadableHexString())

        // And it should give us back the rest
        Assert.assertEquals(rest, parseResult.rest.toReadableHexString())
    }

    @Test
    fun `should fail if hex is malformed`() {
        // Given an EDID field with a length of 10 (0A 00 -> littleEndian so 00 0A = 10)
        // but which has a couple of bytes removed (so it's too short)
        val field = "45 44 49 44 0A 00 49 72 6F 6E 53 72 64 00"

        // When we render a subrecord from the bytes
        val parseResult = Subrecord.parse(SkyrimSubrecordMenu(), "WEAP", field.fromHexStringToByteList())

        // Then the parse result should be a failure
        assertEquals(false, parseResult.succeeded)
    }
}

class ByteSubTest {
    @Test
    fun `should be able to turn contents into a string`() {
        // Given a CNAM record with content "Chimmer"
        val cnam = ByteSub.create("CNAM", "43 68 69 6D 6D 65 72 00".fromHexStringToByteList())

        // When we ask for it as a string
        val text = cnam.asString()

        // Then we should get the contents back without the null terminator
        assertEquals("Chimmer", text)
    }
}