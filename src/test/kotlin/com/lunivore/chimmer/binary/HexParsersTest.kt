package com.lunivore.chimmer.binary

import com.lunivore.chimmer.testheplers.fromHexStringToByteList
import com.lunivore.chimmer.testheplers.toReadableHexString
import org.junit.Assert.assertEquals
import org.junit.Test

class HexParsersTest {

    @Test
    fun `Can convert 4 little-endian bytes to an int`() {
        assertEquals(1, "01 00 00 00".fromHexStringToByteList().toLittleEndianInt())
        assertEquals(851970, "02 00 0D 00".fromHexStringToByteList().toLittleEndianInt())
    }

    @Test
    fun `Can convert 2 little-endian bytes to an int`() {
        assertEquals(15, "0F 00".fromHexStringToByteList().toLittleEndianInt())
    }

    @Test
    fun `Can convert a short to 2 little-endian bytes`() {
        assertEquals("0F 00", 15.toShort().toLittleEndianBytes().toReadableHexString())
    }

    @Test
    fun `Can convert an int to 4 little-endian bytes`() {
        assertEquals("01 02 00 00", 513.toLittleEndianBytes().toReadableHexString())
    }


}