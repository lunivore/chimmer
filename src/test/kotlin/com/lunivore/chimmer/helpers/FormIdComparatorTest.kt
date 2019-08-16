package com.lunivore.chimmer.helpers

import com.lunivore.chimmer.FormId
import org.junit.Assert.assertEquals
import org.junit.Test


class FormIdComparatorTest {
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
        val comparator = FormIdComparator(loadOrder)
        val sortedList = formIds.map{it.key}.sortedWith(comparator)

        // Then they should be sorted by load order first, then by FormId.
        val expectedList = listOf(0, 2, 1, 4, 3, 5).map {formIds[it].key}
        assertEquals(expectedList, sortedList)
    }

    // TODO: Also do this for new FormIds
}
