package com.lunivore.chimmer.helpers

import com.lunivore.chimmer.ExistingFormId
import org.junit.Assert.assertEquals
import org.junit.Test


class FormIdComparatorTest {
    @UseExperimental(ExperimentalUnsignedTypes::class)
    @Test
    fun `should provide a comparator for FormIds against a known load order`() {
        // Given a load order with a number of value
        val loadOrder : List<String> =
                listOf("Skyrim.esm", "Dawnguard.esm", "Hearthfires.esm", "MyMod1.esp", "MyMod2.esp")

        // And formIds originating from different mods
        val formIds = listOf(
                ExistingFormId.create(MastersWithOrigin("Skyrim.esm", listOf()), IndexedFormId(0x00aaaaaau)),
                ExistingFormId.create(MastersWithOrigin("Dawnguard.esm", listOf("Skyrim.esm")), IndexedFormId(0x01ccccccu)),
                ExistingFormId.create(MastersWithOrigin("MyMod1.esp", listOf("Skyrim.esm","Dawnguard.esm")), IndexedFormId(0x01bbbbbbu)),
                ExistingFormId.create(MastersWithOrigin("MyMod2.esp", listOf("Skyrim.esm", "Dawnguard.esm", "MyMod1.esp")), IndexedFormId(0x02eeeeeeu)),
                ExistingFormId.create(MastersWithOrigin("MyMod2.esp", listOf("Skyrim.esm", "Dawnguard.esm", "MyMod1.esp")), IndexedFormId(0x01ddddddu)),
                ExistingFormId.create(MastersWithOrigin("MyMod2.esp", listOf("Skyrim.esm", "Dawnguard.esm", "MyMod1.esp")), IndexedFormId(0x03ffffffu))
        )

        // When we sort those FormIds
        val comparator = FormIdComparator(loadOrder)
        val sortedList = formIds.map{it.key}.sortedWith(comparator)

        // Then they should be sorted by load order first, then by FormId.
        val expectedList = listOf(0, 2, 1, 4, 3, 5).map {formIds[it].key}
        assertEquals(expectedList, sortedList)
    }

    // TODO: Also do this for new FormIds
}
