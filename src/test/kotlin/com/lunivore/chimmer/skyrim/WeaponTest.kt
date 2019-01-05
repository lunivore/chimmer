package com.lunivore.chimmer.skyrim

import com.lunivore.chimmer.FormId
import com.lunivore.chimmer.binary.Record
import com.lunivore.chimmer.binary.Subrecord
import com.lunivore.chimmer.binary.fromHexStringToByteList
import com.lunivore.chimmer.testheplers.Hex
import com.lunivore.chimmer.testheplers.toReadableHexString
import org.junit.Assert.assertEquals
import org.junit.Test

@UseExperimental(ExperimentalUnsignedTypes::class)
class WeaponTest {

    @Test
    fun `should be able to read keywords`() {
        // Given an iron sword loaded as a weapon
        val masters = listOf("Skyrim.esm", "Miscellaneous.esp")
        val weapon = parseIronSword(masters)

        // Then it should have the relevant keywords
        val expectedKeywords = listOf(0x0001e711u, 0x0001e718u, 0x0008f958u).map { FormId("MyMod.esp", it, masters) }
        assertEquals(expectedKeywords, weapon.keywords)

    }

    @Test
    fun `should be able to add (set) keywords`() {
        // Given an iron sword loaded as a weapon
        val masters = listOf("Skyrim.esm", "Miscellaneous.esp")
        val weapon = parseIronSword(masters)

        // When we add a new keyword from another mod (note it has a loadingMod set, but its index is beyond
        // the masters so NewKeywords.esp is its master)
        val newKeyword = FormId("NewKeywords.esp", 0x010a0b0cu, listOf("Skyrim.esm"))
        val newWeapon = weapon.withKeywords(weapon.keywords.plus(newKeyword))

        // Then our new weapon should have all the keywords
        val newMasters = listOf("Skyrim.esm", "Miscellaneous.esp", "NewKeywords.esp")
        val expectedKeywords = listOf(0x0001e711u, 0x0001e718u, 0x0008f958u).map { FormId("MyMod.esp", it, newMasters) }
                .plus(FormId("MyMod.esp", 0x020a0b0cu, newMasters))
        assertEquals(expectedKeywords, newWeapon.keywords)

        // And the underlying record should have both the KWDA and KSIZ records set correctly
        // (note we expect the index of the new keyword to change)
        val actualKSIZ = newWeapon.record.find("KSIZ")
        val actualKWDA = newWeapon.record.find("KWDA")

        val expectedKSIZ = Subrecord("KSIZ", "04 00 00 00".fromHexStringToByteList())
        val expectedKWDA = Subrecord("KWDA", "11 e7 01 00 18 e7 01 00 58 f9 08 00 0c 0b 0a 02".fromHexStringToByteList())

        assertEquals(expectedKSIZ, actualKSIZ)
        assertEquals(expectedKWDA, actualKWDA)

        // And the new master should have been added to the record's masters
        assertEquals(newMasters, newWeapon.record.masters)
    }

    @Test
    fun `should reindex all FormId fields when adding new masters`() {
        // Given an iron sword loaded as a weapon
        // With an ETYP from this mod
        val masters = listOf("Skyrim.esm", "Miscellaneous.esp")
        val weapon = Weapon(Record.parseAll("MyMod.esp",
                Hex.IRON_SWORD_WEAPON.fromHexStringToByteList(),
                masters).parsed[0]
                .with(Subrecord("ETYP", "0C 0B 0A 02".fromHexStringToByteList())))

        // When we add a new keyword (which requires a new master)
        val newKeyword = FormId("NewKeywords.esp", 0x010a0b0cu, listOf("Skyrim.esm"))
        val newWeapon = weapon.withKeywords(weapon.keywords.plus(newKeyword))

        // Then the ETYP record should have had its index changed too
        assertEquals("0C 0B 0A 03", newWeapon.record.find("ETYP")!!.toReadableHexString())
    }

    @Test
    fun `should reindex CRDT formId if we have one`() {
        // Given an iron sword loaded as a weapon
        // With a CRDT record we made up with a new crit effect
        val masters = listOf("Skyrim.esm", "Miscellaneous.esp")
        val weapon = Weapon(Record.parseAll("MyMod.esp",
                Hex.IRON_SWORD_WEAPON.fromHexStringToByteList(),
                masters).parsed[0]
                .with(Subrecord("CRDT", "09 00 00 00 00 00 80 3F 01 FF FF FF 0A 0B 0C 02".fromHexStringToByteList())))


        // When we add a keyword to force remastering
        val newKeyword = FormId("NewKeywords.esp", 0x010a0b0cu, listOf("Skyrim.esm"))
        val newWeapon = weapon.withKeywords(weapon.keywords.plus(newKeyword))

        // Then the CRDT record should have been updated too
        val expectedCrdt = "09 00 00 00 00 00 80 3F 01 FF FF FF 0A 0B 0C 03"
        assertEquals(expectedCrdt, newWeapon.record.find("CRDT")!!.bytes.toReadableHexString())

    }

    private fun parseIronSword(masters: List<String>): Weapon {
        return Weapon(Record.parseAll("MyMod.esp",
                Hex.IRON_SWORD_WEAPON.fromHexStringToByteList(),
                masters).parsed[0])
    }
}