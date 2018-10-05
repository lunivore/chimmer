package com.lunivore.chimmer

import com.lunivore.chimmer.binary.fromHexStringToByteList
import com.lunivore.chimmer.testheplers.toReadableHexString
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.lang.IllegalArgumentException

class FormIdTest {

    @Test
    fun `should be able to construct a FormId from an indexed FormId and a master list`() {
        // Given an indexed Form Id in bytes
        val bytes = "12 00 00 02".fromHexStringToByteList()

        // And a master list, of which the FormId references the 2nd (01)
        val masterList = listOf("Skyrim.esm", "Dawnguard.esm", "Dragonborn.esm", "Hearthfires.esm")

        // When we pass the bytes to a Form Id
        val formId = FormId(bytes, masterList)

        // Then it should parse out the unindexed form id and the master file
        assertEquals(0x12, formId.unindexFormId)
        assertEquals("Dragonborn.esm", formId.master)

        // And it should be equal to a formId composed with the same master but a different list
        val other = FormId("12 00 00 01".fromHexStringToByteList(), listOf("Skyrim.esm", "Dragonborn.esm"))

        assertEquals(other, formId)
    }

    @Test
    fun `should render itself to little endian bytes with the appropriate masterlist`() {
        // Given a form id from a new mod
        val formId = FormId(0x123456, "MyMod.esp")

        // When we turn it into bytes with a new masterlist
        val bytes = formId.toLittleEndianBytes(listOf(
                "Skyrim.esm",
                "Dawnguard.esm",
                "Dragonborn.esm",
                "Hearthfires.esm",
                "Update.esm",
                "MyMod.esp",
                "Another.esp"))

        // Then it should have rendered the index appropriately for that masterlist
        assertEquals("56 34 12 05", bytes.toReadableHexString())
    }

    @Test
    fun `should throw an error when reindexing if the master is not found in the masterlist`() {
        // Given a form ID with a given master
        val formId = FormId(4, "MyMod.esp")

        // When we try to turn it into bytes using a masterlist without the right master
        try {
            formId.toLittleEndianBytes(listOf("Dawnguard.esm"))
            fail("Should have thrown an error")
        } catch (e: IllegalArgumentException) {
            // expected
        }

        // Then it should have thrown an error


        // Then it should throw an error
    }
}