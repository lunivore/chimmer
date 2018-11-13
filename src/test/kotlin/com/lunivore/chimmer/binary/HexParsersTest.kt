package com.lunivore.chimmer.binary

import com.lunivore.chimmer.testheplers.toReadableHexString
import org.junit.Assert.assertEquals
import org.junit.Test

@UseExperimental(ExperimentalUnsignedTypes::class)
class HexParsersTest {

    @Test
    fun `Can convert 4 little-endian bytes to an int (sizes, counts etc)`() {
        assertEquals(1, "01 00 00 00".fromHexStringToByteList().toLittleEndianInt())
        assertEquals(0x000d0002, "02 00 0D 00".fromHexStringToByteList().toLittleEndianInt())
    }

    @Test
    fun `Can convert 4 little-endian bytes to a uint (formIds, flags etc)`() {
        assertEquals(1u, "01 00 00 00".fromHexStringToByteList().toLittleEndianUInt())
        assertEquals(0x000d0002u, "02 00 0D 00".fromHexStringToByteList().toLittleEndianUInt())
    }

    @Test
    fun `Can convert 2 little-endian bytes to a ushort (version, small ids)`() {
        assertEquals(15u.toUShort(), "0F 00".fromHexStringToByteList().toLittleEndianUShort())
    }

    @Test
    fun `Can convert 2 little-endian bytes to an int (for size of small data fields)`() {
        assertEquals(15, "0F 00".fromHexStringToByteList().toLittleEndianInt())
    }

    @Test
    fun `Can convert a ushort to 2 little-endian bytes`() {
        assertEquals("0F 00", 15u.toUShort().toLittleEndianBytes().toReadableHexString())
    }

    @Test
    fun `Can convert an int to 4 little-endian bytes`() {
        assertEquals("01 02 00 00", 513.toLittleEndianBytes().toReadableHexString())
    }

    @Test
    fun `Can convert a uint to 4 little-endian bytes`() {
        assertEquals("01 02 00 00", 513u.toLittleEndianBytes().toReadableHexString())
    }

    @Test
    fun `Can convert a float to 4 little-endian bytes`() {
        val converted = 1.7f.toLittleEndianBytes()
        assertEquals("9A 99 D9 3F", converted.toReadableHexString()) // Found using TES5Edit
    }

    @Test
    fun `Can convert a hex string to a byte list`() {
        assertEquals("0102000A".fromHexStringToByteList(), listOf(1, 2, 0, 10).map { it.toByte() })
    }
}

