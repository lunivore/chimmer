package com.lunivore.chimmer

import org.junit.Assert.*
import org.junit.Test

class FormIdTest {

    @Test
    fun `should provide its master file as a string`() {
        // Given a rawFormId with an index of 01 and a masterlist of Skyrim, Dawnguard, and another
        val formId = FormId(0x01abababu, "Skyrim.esm, Dawnguard.esm, ANOther.esp".split(", "))

        // When we ask it for its master
        // Then it should correctly identify as Dawnguard
        assertEquals("Dawnguard.esm", formId.master)
    }

    @Test
    fun `should provide no master file if the master is the current mod`() {
        // Given a rawFormId with an index of 03 and a masterlist of Skyrim, Dawnguard, and another
        val formId = FormId(0x03abababu, "Skyrim.esm, Dawnguard.esm, ANOther.esp".split(", "))

        // When we ask it for its master
        // Then it should correctly identify the current mod as the master and provide nothing at all
        assertNull(formId.master)
    }

    @Test
    fun `should provide no master file if the record is new`() {
        // Given a new formId
        val formId = FormId.createNew("Skyrim.esm, Dawnguard.esm, ANOther.esp".split(", "))

        // When we ask it for its master then it should also provide nothing at all, no matter its masters
        assertNull(formId.master)
    }

    @Test
    fun `should throw an IndexOutOfBounds with an explanation if the master list provided isn't big enough`() {
        // Given a rawFormId with an index of 04 and a masterlist of Skyrim, Dawnguard, and another
        val formId = FormId(0x04abababu, "Skyrim.esm, Dawnguard.esm, ANOther.esp".split(", "))

        // When we ask it for its master
        // Then it should throw an exception
        try {
            formId.master
            fail()
        } catch (e : IndexOutOfBoundsException) {
            // expected
        }
    }

    @Test
    fun `should provide a readable version of the indexed formId`() {
        // Given a form Id
        val formId = FormId(0x010a0b0cu, listOf("Skyrim.esm"))

        // Then it should be readable
        assertEquals("010A0B0C", formId.toBigEndianHexString())
    }

    @Test
    fun `should tell us if it's a new FormId`() {
        // Given an existing FormId, then it should not be new
        assertFalse(FormId(0x12345678u, listOf()).isNew())

        // Given a FormId created for a new record, then it should be new
        assertTrue(FormId.createNew(listOf("Skyrim.esm")).isNew())
    }

    @Test
    fun `should give us the unindexed FormId`() {
        // Given a FormId with an index
        val formId = FormId(0x01abcdefu, listOf("Skyrim.esm", "Dawnguard.esm"))

        // When we ask for the unindexed bit then it should lose the first byte
        assertEquals(0x00abcdefu, formId.unindexed)
    }
}



