package com.lunivore.chimmer

import org.junit.Assert.*
import org.junit.Test

class ExistingFormIdTest {

    @Test
    fun `should provide its master file as a string`() {
        // Given a rawFormId with an index of 01 and a masterlist of Skyrim, Dawnguard, and another
        val formId = FormId.create("ANOther.esp", 0x01abababu, "Skyrim.esm, Dawnguard.esm, ANOther.esp".split(", "))

        // When we ask it for its master
        // Then it should correctly identify as Dawnguard
        assertEquals("Dawnguard.esm", formId.master)
    }

    @Test
    fun `should provide the loading mod as the master file if the master is the current mod`() {
        // Given a rawFormId with an index of 03 and a masterlist of Skyrim, Dawnguard, and another
        val formId = FormId.create("Current.esp", 0x03abababu, "Skyrim.esm, Dawnguard.esm, ANOther.esp".split(", "))

        // When we ask it for its master
        // Then it should correctly identify the current mod as the master and provide it
        assertEquals("Current.esp", formId.master)
    }

    @Test
    fun `should throw an IllegalArgumentException if it's created with an index bigger than the masterlist + loading mod`() {
        // When we try to create a rawFormId with an index of 04 and a masterlist of Skyrim, Dawnguard, and another
        // (note that 03 would denote the loading mod as master)
        try {
            FormId.create("Current.esp", 0x04abababu, "Skyrim.esm, Dawnguard.esm, ANOther.esp".split(", "))
            fail("Should have thrown an exception")
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun `should provide a readable version of the indexed formId`() {
        // Given a form Id
        val formId = FormId.create("Current.esp", 0x010a0b0cu, listOf("Skyrim.esm"))

        // Then it should be readable
        assertEquals("010A0B0C", (formId as ExistingFormId).toBigEndianHexString())
    }


    @Test
    fun `should tell us it's not a new FormId`() {
        // Given an existing FormId, then it should not be new
        assertFalse(FormId.create("Current.esp", 0x00345678u, listOf()).isNew())
    }


    @Test
    fun `should give us the unindexed FormId`() {
        // Given a FormId with an index
        val formId = FormId.create("MyMod.esp", 0x01abcdefu, listOf("Skyrim.esm", "Dawnguard.esm"))

        // When we ask for the unindexed bit then it should lose the first byte
        assertEquals(0x00abcdefu, (formId as ExistingFormId).unindexed)
    }

    @Test
    fun `should handle iDaysToRespawnVendor because WTF Bethesda why is there an index of 01 in Skyrim esm`() {
        // Given iDaysToRespawnVendor's formId
        val iDaysToRespawnVendor = FormId.create("Skyrim.esm", 0x0123C00Eu, listOf())

        // Then it should show Skyrim as its master
        assertEquals("Skyrim.esm", iDaysToRespawnVendor.master)

        // When we reindex it with Skyrim in first place for a new mod (Skyrim will always be in first place)
        val newMasters = listOf("Skyrim.esm", "MyMod.esp")

        // Then it should have its formId reindexed to 0
        assertEquals("Skyrim.esm", FormId.create("Skyrim.esm", 0x0023C00Eu, newMasters).master)
    }
}

class NewFormIdTest {

    @Test
    fun `should provide the new mod name as a master`() {
        // Given a new formId
        val formId = FormId.createNew("Blip.esp", "MyEditorId")

        // Then it should provide the creating mod as a master
        assertEquals("Blip.esp", formId.master)
    }


    @Test
    fun `should tell us it's a new FormId`() {
        // Given an existing FormId, then it should not be new
        assertTrue(FormId.createNew("Blip.esp", "MyEditorId").isNew())
    }

}



