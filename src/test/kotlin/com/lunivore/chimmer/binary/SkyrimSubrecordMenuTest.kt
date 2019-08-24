package com.lunivore.chimmer.binary

import com.lunivore.chimmer.helpers.MastersWithOrigin
import org.junit.Assert.assertTrue
import org.junit.Test


class SkyrimSubrecordMenuTest {

    @Test
    fun `should provide ByteSubs for ordinary subrecords`() {
        // Given the bytes of a subrecord which isn't a form id
        val bytes = "00 00 12 34".fromHexStringToByteList()

        // When we use the menu to get a provider, then use that provider to make a subrecord
        val subrecord = SkyrimSubrecordMenu().findProvider("WEAP", "VNAM")(MastersWithOrigin("Skyrim.esm", listOf()), bytes)

        // Then it should be a ByteSub
        assertTrue(subrecord is ByteSub)
    }

    @Test
    fun `should provide FormIdSubs for listed subrecords`() {
        // Given the bytes of a formId subrecord like Weapon Template
        val bytes = "00 12 34 00".fromHexStringToByteList()

        // When we use the menu to get a provider, then use that provider to make a subrecord
        val subrecord = SkyrimSubrecordMenu().findProvider("WEAP", "CNAM")(MastersWithOrigin("Skyrim.esm", listOf()), bytes)

        // Then it should be a FormIdSub
        assertTrue(subrecord is FormIdSub)
    }

}