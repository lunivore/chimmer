package com.lunivore.chimmer

import org.junit.Assert.*
import org.junit.Test

class FormIdTest {

    @Test
    fun `should provide its master file as a string`() {
        // Given a rawFormId with an index of 01 and a masterlist of Skyrim, Dawnguard, and another
        val formId = FormId.create(null, 0x01abababu, "Skyrim.esm, Dawnguard.esm, ANOther.esp".split(", "))

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
    fun `should provide no master file if the record is new`() {
        // Given a new formId
        val formId = FormId.createNew("Skyrim.esm, Dawnguard.esm, ANOther.esp".split(", "))

        // When we ask it for its master then it should also provide nothing at all, no matter its masters
        assertNull(formId.master)
    }

    @Test
    fun `should throw an IllegalArgumentException if it's created without an appropriate master`() {
        // When we try to create a rawFormId with an index of 03 and a masterlist of Skyrim, Dawnguard, and another
        try {
            FormId.create(null, 0x03abababu, "Skyrim.esm, Dawnguard.esm, ANOther.esp".split(", "))
            fail("Should have thrown an exception")
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun `should throw an IllegalArgumentException if it's created without an appropriate master even if loading mod exists`() {
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
        assertEquals("010A0B0C", formId.toBigEndianHexString())
    }

    @Test
    fun `should tell us if it's a new FormId`() {
        // Given an existing FormId, then it should not be new
        assertFalse(FormId.create("Current.esp", 0x00345678u, listOf()).isNew())

        // Given a FormId created for a new record, then it should be new
        assertTrue(FormId.createNew(listOf("Skyrim.esm")).isNew())
    }

    @Test
    fun `should give us the unindexed FormId`() {
        // Given a FormId with an index
        val formId = FormId.create(null, 0x01abcdefu, listOf("Skyrim.esm", "Dawnguard.esm"))

        // When we ask for the unindexed bit then it should lose the first byte
        assertEquals(0x00abcdefu, formId.unindexed)
    }

    @Test
    fun `should provide a reindexed list of bytes for a new master file for a new mod`() {
        // Given a FormId with an index that's found in the masterlist
        val formId = FormId.create(null, 0x01abcdefu, listOf("Skyrim.esm", "MiscellaneousKeyword.esp"))

        // When we ask for it to be reindexed for a new masterlist
        // (This happens when there are other records in the new mod that also have masters)
        val newMasters = listOf("Skyrim.esm", "Dawnguard.esm", "MiscellaneousKeyword.esp")
        val reindexedFormId = formId.reindex(newMasters)

        // Then it should have the new index in the right place
        val expectedFormId = FormId.create(null, 0x02abcdefu, newMasters)
        assertEquals(expectedFormId, reindexedFormId)
    }

    @Test
    fun `should be able to reindex for a new mod when the old mod was the master`() {
        // Given a FormId from an old mod (note 02 index denoting current mod as master)
        val formId = FormId.create("OldMod.esp", 0x02abcdefu, listOf("Skyrim.esm", "MiscellaneousKeyword.esp"))

        // When we ask for it to be reindexed for a new masterlist
        val newMasters = listOf("Skyrim.esm", "Dawnguard.esm", "MiscellaneousKeyword.esp", "OldMod.esp")
        val reindexedFormId = formId.reindex(newMasters)

        // Then it should find the old mod in the new masterlist and use that as the index
        val expectedFormId = FormId.create(null, 0x03abcdefu, newMasters)
        assertEquals(expectedFormId, reindexedFormId)
    }

    @Test
    fun `should be able to reindex for a new mod when the old mod wasn't in the masters`() {
        // Given a FormId from an old mod (note 02 index denoting current mod as master)
        val formId = FormId.create("OldMod.esp", 0x02abcdefu, listOf("Skyrim.esm", "MiscellaneousKeyword.esp"))

        // When we ask for it to be reindexed for a new masterlist
        val newMasters = listOf("Skyrim.esm", "Dawnguard.esm", "MiscellaneousKeyword.esp")
        val reindexedFormId = formId.reindex(newMasters)

        // Then it should have added the loadng mod to the masters and used that
        val expectedMasters = listOf("Skyrim.esm", "Dawnguard.esm", "MiscellaneousKeyword.esp", "OldMod.esp")
        val expectedFormId = FormId.create(null, 0x03abcdefu, expectedMasters)
        assertEquals(expectedFormId, reindexedFormId)
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

    @Test
    fun `should provide a comparator for FormIds against a known load order`() {
        // Given a load order with a number of masters
        val loadOrder : List<String> =
                listOf("Skyrim.esm", "Dawnguard.esm", "Hearthfires.esm", "MyMod1.esp", "MyMod2.esp")

        // And formIds originating from different mods
        val formIds = listOf(
                FormId.create("Skyrim.esm", 0x00aaaaaau, listOf("Skyrim.esm")),
                FormId.create("Dawnguard.esm", 0x01ccccccu, listOf("Skyrim.esm","Dawnguard.esm")),
                FormId.create("MyMod1.esp", 0x01bbbbbbu, listOf("Skyrim.esm", "Dawnguard.esm")),
                FormId.create("MyMod2.esp", 0x02eeeeeeu, listOf("Skyrim.esm", "Dawnguard.esm", "MyMod1.esp")),
                FormId.create("MyMod2.esp", 0x01ddddddu, listOf("Skyrim.esm", "Dawnguard.esm", "MyMod1.esp")),
                FormId.create("MyMod2.esp", 0x03ffffffu, listOf("Skyrim.esm", "Dawnguard.esm", "MyMod1.esp"))
        )

        // When we sort those FormIds
        val comparator = FormIdKeyComparator(loadOrder)
        val sortedList = formIds.map {it.key}.sortedWith(comparator)

        // Then they should be sorted by load order first, then by FormId.
        val expectedList = listOf(0, 2, 1, 4, 3, 5).map {formIds[it].key}
        assertEquals(expectedList, sortedList)
    }

    @Test
    fun `should handle iDaysToRespawnVendor because WTF Bethesda why is there an index of 01 in Skyrim esm`() {
        // Given iDaysToRespawnVendor's formId
        val iDaysToRespawnVendor = FormId.create("Skyrim.esm", 0x0123C00Eu, listOf())

        // Then it should show Skyrim as its master
        assertEquals("Skyrim.esm", iDaysToRespawnVendor.master)

        // When we reindex it with Skyrim in first place for a new mod (Skyrim will always be in first place)
        val newMasters = listOf("Skyrim.esm", "MyMod.esp")
        val reindexed = iDaysToRespawnVendor.reindex(newMasters)

        // Then it should have its formId reindexed to 0
        assertEquals(FormId.create(null, 0x0023C00Eu, newMasters), reindexed)
    }
}



