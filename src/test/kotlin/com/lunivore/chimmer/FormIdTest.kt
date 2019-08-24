package com.lunivore.chimmer

import com.lunivore.chimmer.binary.toBigEndianHexString
import com.lunivore.chimmer.binary.toLittleEndianByteList
import com.lunivore.chimmer.binary.toLittleEndianUInt
import com.lunivore.chimmer.helpers.*
import com.lunivore.chimmer.testheplers.fakeConsistencyRecorder
import org.junit.Assert.*
import org.junit.Test

class ExistingFormIdTest {

    @Test
    fun `should identify master from a list based on the form id index`() {
        // Given a rawFormId with an index of 01 and a masterlist of Skyrim, Dawnguard, and another
        val formId = ExistingFormId.create(MastersWithOrigin("ANOther.esp", "Skyrim.esm, Dawnguard.esm, ANOther.esp".split(", ")), IndexedFormId(0x01abababu))

        // When we ask it for its master
        // Then it should correctly identify as Dawnguard
        assertEquals("Dawnguard.esm", formId.master)
    }

    @Test
    fun `should identify origin as master if the index is just past the end of the list`() {
        // Given a rawFormId with an index of 01 and a masterlist of Skyrim, Dawnguard, and another
        val formId = ExistingFormId.create(MastersWithOrigin("ANOther.esp", "Skyrim.esm, Dawnguard.esm, Hearthfires.esm".split(", ")), IndexedFormId(0x03abababu))

        // When we ask it for its master
        // Then it should correctly identify as its origin
        assertEquals("ANOther.esp", formId.master)
    }

    @Test
    fun `should provide an index for bytes by finding its master in the masterlist`() {
        // Given a FormId with a master the same as the origin
        val formId = ExistingFormId.create(MastersWithOrigin("MyMod.esp", listOf("Skyrim.esm", "Dawnguard.esm")), IndexedFormId(0x02abababu))

        // When we turn it into bytes (it's been copied entire to another mod here)
        val bytes = formId.toBytes(MastersWithOrigin("AnotherMod.esp", listOf("Skyrim.esm", "Dawnguard.esm", "Hearthfires.esm", "MyMod.esp")), ::fakeConsistencyRecorder)

        // Then it should have the right index
        assertEquals(0x03u, bytes.toLittleEndianUInt().and(0xff000000u).shr(24))
    }

    @Test
    fun `should provide an index for the origin mod if the origin has not changed and is the master`() {
        val formId = ExistingFormId.create(MastersWithOrigin("MyMod.esp", listOf("Skyrim.esm", "Dawnguard.esm")), IndexedFormId(0x02abababu))

        // When we turn it into bytes with the same origin but a different masterlist
        val bytes = formId.toBytes(MastersWithOrigin("MyMod.esp", listOf("Skyrim.esm", "Dawnguard.esm", "Hearthfires.esm", "ANOther.esp")), ::fakeConsistencyRecorder)

        // Then it should have the right index
        assertEquals(0x04u, bytes.toLittleEndianUInt().and(0xff000000u).shr(24))
    }

    @Test
    fun `should provide the loading mod as the master file if the master is the current mod`() {
        // Given a rawFormId with an index of 03 and a masterlist of Skyrim, Dawnguard, and another
        val formId = ExistingFormId.create(MastersWithOrigin("Current.esp", "Skyrim.esm, Dawnguard.esm, ANOther.esp".split(", ")), IndexedFormId(0x03abababu))

        // When we ask it for its master
        // Then it should correctly identify the current mod as the master and provide it
        assertEquals("Current.esp", formId.master)
    }

    @Test
    fun `should throw an IllegalArgumentException if it's created with an index bigger than the masterlist + loading mod`() {
        // When we try to create a rawFormId with an index of 04 and a masterlist of Skyrim, Dawnguard, and another
        // (note that 03 would denote the loading mod as master)
        try {
            ExistingFormId.create(MastersWithOrigin("Current.esp", "Skyrim.esm, Dawnguard.esm, ANOther.esp".split(", ")), IndexedFormId(0x04abababu))
            fail("Should have thrown an exception")
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun `should provide a readable version of the unindexed formId`() {
        // Given a form Id
        val formId = ExistingFormId.create(MastersWithOrigin("Current.esp", listOf("Skyrim.esm")), IndexedFormId(0x010a0b0cu))

        // Then it should be readable
        assertEquals("000A0B0C", (formId as ExistingFormId).unindexed.toBigEndianHexString())
    }


    @Test
    fun `should tell us it's not a new FormId`() {
        // Given an existing FormId, then it should not be new
        assertFalse(ExistingFormId.create(MastersWithOrigin("Current.esp", listOf()), IndexedFormId(0x00345678u)).isNew())
    }


    @Test
    fun `should give us the unindexed FormId`() {
        // Given a FormId with an index
        val formId = ExistingFormId.create(MastersWithOrigin("MyMod.esp", listOf("Skyrim.esm", "Dawnguard.esm")), IndexedFormId(0x01abcdefu))

        // When we ask for the unindexed bit then it should lose the first byte
        assertEquals(0x00abcdefu, (formId as ExistingFormId).unindexed)
    }

    @Test
    fun `should handle iDaysToRespawnVendor because WTF Bethesda why is there an index of 01 in Skyrim esm`() {
        // Given iDaysToRespawnVendor's formId
        val iDaysToRespawnVendor = ExistingFormId.create(MastersWithOrigin("Skyrim.esm", listOf()), IndexedFormId(0x0123C00Eu))

        // Then it should show Skyrim as its master
        assertEquals("Skyrim.esm", iDaysToRespawnVendor.master)

        // When we reindex it with Skyrim in first place for a new mod (Skyrim will always be in first place)
        val newMasters = listOf("Skyrim.esm", "MyMod.esp")

        // Then it should have its formId reindexed to 0
        assertEquals("Skyrim.esm", ExistingFormId.create(MastersWithOrigin("Skyrim.esm", newMasters), IndexedFormId(0x0023C00Eu)).master)
    }
}

class NewFormIdTest {

    @Test
    fun `should provide the new mod name as a master`() {
        // Given a new formId
        val formId = FormId.createNew(OriginMod("Blip.esp"), EditorId("MyEditorId"))

        // Then it should provide the creating mod as a master
        assertEquals("Blip.esp", formId.master)
    }


    @Test
    fun `should tell us it's a new FormId`() {
        // Given an existing FormId, then it should not be new
        val newFormId = FormId.createNew(OriginMod("Blip.esp"), EditorId("MyEditorId"))
        assertTrue(newFormId.isNew())
    }

    @Test
    fun `should turn itself into bytes using the masterlist and consistency recorder provided`() {
        // Given a consistency recorder which will give our new form id an unindexed value of 0x00123456
        val cr : ConsistencyRecorder = {editorId ->
            if (editorId.value == "MyEditorId") UnindexedFormId(0x00123456u) else throw Exception("Should never happen")}

        // And a new FormId with a master
        val formId = FormId.createNew(OriginMod("Blip.esp"), EditorId("MyEditorId"))

        // When we turn it into bytes
        val bytes = formId.toBytes(MastersWithOrigin("Blip.esp", listOf("Skyrim.esm", "Whatever.esp")), cr)

        // Then it have the formId and correct index of 02
        assertEquals(0x02123456u.toLittleEndianByteList(), bytes)
    }

}



