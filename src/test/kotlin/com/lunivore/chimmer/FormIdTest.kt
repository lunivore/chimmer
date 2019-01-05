package com.lunivore.chimmer

import org.junit.Assert.*
import org.junit.Test

class FormIdTest {

    @Test
    fun `should provide its master file as a string`() {
        // Given a rawFormId with an index of 01 and a masterlist of Skyrim, Dawnguard, and another
        val formId = FormId(null, 0x01abababu, "Skyrim.esm, Dawnguard.esm, ANOther.esp".split(", "))

        // When we ask it for its master
        // Then it should correctly identify as Dawnguard
        assertEquals("Dawnguard.esm", formId.master)
    }

    @Test
    fun `should provide the loading mod as the master file if the master is the current mod`() {
        // Given a rawFormId with an index of 03 and a masterlist of Skyrim, Dawnguard, and another
        val formId = FormId("Current.esp", 0x03abababu, "Skyrim.esm, Dawnguard.esm, ANOther.esp".split(", "))

        // When we ask it for its master
        // Then it should correctly identify the current mod as the master and provide it
        assertEquals("Current.esp", formId.master)
    }

    @Test
    fun `should provide no master file if the record is new`() {
        // Given a new formId
        val formId = FormId.createNew("Skyrim.esm, Dawnguard.esm, ANOther.esp".split(", "))

        // When we ask it for its master then it should also provide nothing at all, no matter its masters
        assertNull(formId.master)
    }

    @Test
    fun `should throw an IndexOutOfBounds if the masterlist isn't big enough and there's no loading mod`() {
        // Given a rawFormId with an index of 04 and a masterlist of Skyrim, Dawnguard, and another
        val formId = FormId(null, 0x04abababu, "Skyrim.esm, Dawnguard.esm, ANOther.esp".split(", "))

        // When we ask it for its master
        // Then it should throw an exception
        try {
            formId.master
            fail()
        } catch (e : IllegalStateException) {
            // expected
        }
    }

    @Test
    fun `should throw an exception if the masterlist is too big even for the loading mod`() {
        // Given a rawFormId with an index of 04 and a masterlist of Skyrim, Dawnguard, and another
        val formId = FormId("Current.esp", 0x05abababu, "Skyrim.esm, Dawnguard.esm, ANOther.esp".split(", "))

        // When we ask it for its master
        // Then it should throw an exception
        try {
            formId.master
            fail()
        } catch (e : IllegalStateException) {
            // expected
        }
    }

    @Test
    fun `should provide the raw indexed form id as a unit or bytes`() {
        // Given a form Id
        val formId = FormId(null, 0x010a0b0cu, listOf("Skyrim.esm"))

        // Then it should be available as a uint or bytes
        assertEquals(0x010a0b0cu, formId.raw)
        assertEquals(byteArrayOf(0x0c, 0x0b, 0x0a, 0x01).toList(), formId.rawBytes.toList()) // No byteArray equality!?
    }

    @Test
    fun `should provide a readable version of the indexed formId`() {
        // Given a form Id
        val formId = FormId(null, 0x010a0b0cu, listOf("Skyrim.esm"))

        // Then it should be readable
        assertEquals("010A0B0C", formId.toBigEndianHexString())
    }

    @Test
    fun `should tell us if it's a new FormId`() {
        // Given an existing FormId, then it should not be new
        assertFalse(FormId(null, 0x12345678u, listOf()).isNew())

        // Given a FormId created for a new record, then it should be new
        assertTrue(FormId.createNew(listOf("Skyrim.esm")).isNew())
    }

    @Test
    fun `should give us the unindexed FormId`() {
        // Given a FormId with an index
        val formId = FormId(null, 0x01abcdefu, listOf("Skyrim.esm", "Dawnguard.esm"))

        // When we ask for the unindexed bit then it should lose the first byte
        assertEquals(0x00abcdefu, formId.unindexed)
    }

    @Test
    fun `should provide a reindexed list of bytes for a new master file for a new mod`() {
        // Given a FormId with an index that's found in the masterlist
        val formId = FormId(null, 0x01abcdefu, listOf("Skyrim.esm", "MiscellaneousKeyword.esp"))

        // When we ask for it to be reindexed for a new masterlist
        // (This happens when there are other records in the new mod that also have masters)
        val newMasters = listOf("Skyrim.esm", "Dawnguard.esm", "MiscellaneousKeyword.esp")
        val reindexedFormId = formId.reindex(newMasters)

        // Then it should have the new index in the right place
        val expectedFormId = FormId(null, 0x02abcdefu, newMasters)
        assertEquals(expectedFormId, reindexedFormId)
    }

    @Test
    fun `should be able to reindex for a new mod when the old mod was the master`() {
        // Given a FormId from an old mod (note 02 index denoting current mod as master)
        val formId = FormId("OldMod.esp", 0x02abcdefu, listOf("Skyrim.esm", "MiscellaneousKeyword.esp"))

        // When we ask for it to be reindexed for a new masterlist
        val newMasters = listOf("Skyrim.esm", "Dawnguard.esm", "MiscellaneousKeyword.esp", "OldMod.esp")
        val reindexedFormId = formId.reindex(newMasters)

        // Then it should find the old mod in the new masterlist and use that as the index
        val expectedFormId = FormId(null, 0x03abcdefu, newMasters)
        assertEquals(expectedFormId, reindexedFormId)
    }

    @Test
    fun `should be able to reindex for a new mod when the old mod wasn't in the masters`() {
        // Given a FormId from an old mod (note 02 index denoting current mod as master)
        val formId = FormId("OldMod.esp", 0x02abcdefu, listOf("Skyrim.esm", "MiscellaneousKeyword.esp"))

        // When we ask for it to be reindexed for a new masterlist
        val newMasters = listOf("Skyrim.esm", "Dawnguard.esm", "MiscellaneousKeyword.esp")
        val reindexedFormId = formId.reindex(newMasters)

        // Then it should have added the loadng mod to the masters and used that
        val expectedMasters = listOf("Skyrim.esm", "Dawnguard.esm", "MiscellaneousKeyword.esp", "OldMod.esp")
        val expectedFormId = FormId(null, 0x03abcdefu, expectedMasters)
        assertEquals(expectedFormId, reindexedFormId)
    }

    @Test
    fun `should throw an exception if asked to reindex for new masters when master not found and no loading mod`() {
        // Given a misformed FormId
        val formId = FormId(null, 0x02abcdefu, listOf("Skyrim.esm", "MiscellaneousKeyword.esp"))

        // When we ask for it to be reindexed for a new mod
        val newMasters = listOf("Skyrim.esm", "Dawnguard.esm", "MiscellaneousKeyword.esp")

        try {
            val reindexedFormId = formId.reindex(newMasters)
            fail("Should have thrown an exception")
        } catch(e: IllegalStateException) {
            // Then it should throw an exception as it has no way of knowing if the current mod is its master or not
            // expected
        }
    }

    @Test
    fun `should ignore requests to reindex new keywords`() {
        // Given a FormId from an newly created mod
        val formId = FormId.createNew(listOf("Skyrim.esm", "MiscellaneousKeyword.esp"))

        // When we ask for it to be reindexed for a new mod
        val newMasters = listOf("Skyrim.esm", "Dawnguard.esm", "MiscellaneousKeyword.esp")
        val reindexedFormId = formId.reindex(newMasters)

        // Then it should be ignored
        assertEquals(formId, reindexedFormId)
    }
}



