package com.lunivore.chimmer.skyrim

import com.lunivore.chimmer.ExistingFormId
import com.lunivore.chimmer.FormId
import com.lunivore.chimmer.binary.RecordParser
import com.lunivore.chimmer.binary.fromHexStringToByteList
import com.lunivore.chimmer.binary.toBigEndianHexString
import com.lunivore.chimmer.helpers.EditorId
import com.lunivore.chimmer.helpers.IndexedFormId
import com.lunivore.chimmer.helpers.MastersWithOrigin
import com.lunivore.chimmer.helpers.OriginMod
import com.lunivore.chimmer.testheplers.Hex
import org.junit.Assert.assertEquals
import org.junit.Test

@UseExperimental(ExperimentalUnsignedTypes::class)
class WeaponTest {

    @Test
    fun `should be able to read and add (set) keywords`() {
        // Given an iron sword loaded as a weapon
        val masters = listOf("Skyrim.esm", "Miscellaneous.esp")
        val weapon = parseIronSword("MyMod.esp", masters)


        val newKeyword = FormId.createNew(OriginMod("NewKeywords.esp"), EditorId("NewKeywords_KWd1"))
        val newKeywordList = weapon.keywords.plus(newKeyword)
        val newWeapon = weapon.withKeywords(newKeywordList)

        // Then our new weapon should have all the keywords
        assertEquals(newKeywordList, newWeapon.keywords)
    }

    @Test
    fun `should be able to read and set Crdt elements`() {
        // Given an iron sword loaded as a weapon
        // With a CRDT record we made up with a new crit effect
        // (The 02 index should make the value be our
        val masters = listOf("Skyrim.esm", "Miscellaneous.esp")

        // When we parseAll it
        val weapon = Weapon(RecordParser().parseAll(MastersWithOrigin("MyMod.esp",masters),
                Hex.IRON_SWORD_WEAPON.fromHexStringToByteList()).parsed[0])

        // Then we should be able to read all the crdt elements
        assertEquals(3, weapon.criticalDamage)
        assertEquals(0.0f, weapon.criticalMultiplier)
        val expectedCritSpellEffect = ExistingFormId.create(MastersWithOrigin("MyMod.esp", listOf("Skyrim.esm", "Miscellaneous.esp")), IndexedFormId(0x0u))
        val actualCritSpellEffect = weapon.criticalSpellEffect
        assertEquals(expectedCritSpellEffect, actualCritSpellEffect)

        // And we should be able to set and re-read them
        assertEquals(6, weapon.withCriticalDamage(6).criticalDamage)

        assertEquals(1.2f, weapon.withCriticalMultiplier(1.2f).criticalMultiplier)

        val critSpellEffect = FormId.createNew(OriginMod("AnotherMod.esp"), EditorId("AnotherMod_NewCritEffect"))

        // Honestly, this looks simpler than it really is; there's a whole subrecord under those covers.
        assertEquals(critSpellEffect, weapon.withCriticalSpellEffect(critSpellEffect).criticalSpellEffect)

    }

    @Test
    fun `should allow us to view and change other aspects of a Weapon`() {
        // Given an iron sword loaded as a weapon
        val masters = listOf("Skyrim.esm", "Dawnguard.esm")
        val sword = Weapon(RecordParser().parseAll(MastersWithOrigin("MyMod.esp",
                masters), Hex.IRON_SWORD_WEAPON.fromHexStringToByteList()).parsed[0])

        // And a crossbow that we're going to steal bits from
        val crossbow = Weapon(RecordParser().parseAll(MastersWithOrigin("MyMod.esp",
                masters), Hex.CROSSBOW_WEAPON.fromHexStringToByteList()).parsed[0])

        // When we change certain values of the iron sword
        val enchantment = FormId.createNew(OriginMod("AnotherMod.esp"), EditorId("MyEnchantment"))
        val newSword = sword
                .withTemplate(crossbow.formId)
                .withDamage((sword.damage * 2u).toUShort())
                .withReach(sword.reach * 1.5f)
                .withSpeed(sword.speed * 3.0f)
                .withFlags(Weapon.Flags.CANT_DROP)
                .withEnchantment(enchantment)
                .withValue(50u)
                .withWeight(3.8f)

        // Then the changes should be consistent
        assertEquals((crossbow.formId as ExistingFormId).unindexed.toBigEndianHexString(), (newSword.template as ExistingFormId).unindexed.toBigEndianHexString())

        // And we should be able to view things too
        assertEquals(Weapon.WeaponType.ONE_HAND_SWORD, newSword.weaponType)
        assertEquals(crossbow.formId, newSword.template)
        assertEquals((sword.damage * 2u).toUShort(), newSword.damage)
        assertEquals(sword.reach * 1.5f, newSword.reach)
        assertEquals(sword.speed * 3.0f, newSword.speed)
        assertEquals(Weapon.Flags.CANT_DROP, newSword.flags)
        assertEquals(enchantment, newSword.enchantment)
        assertEquals(50u, newSword.value)
        assertEquals(3.8f, newSword.weight)
    }


    private fun parseIronSword(loadingMod: String, masters: List<String>): Weapon {
        return Weapon(RecordParser().parseAll(MastersWithOrigin(loadingMod,
                masters), Hex.IRON_SWORD_WEAPON.fromHexStringToByteList()).parsed[0])
    }
}