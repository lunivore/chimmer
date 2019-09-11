package com.lunivore.chimmer.skyrim

import com.lunivore.chimmer.FormId
import com.lunivore.chimmer.binary.CrdtSub
import com.lunivore.chimmer.binary.FormIdSub
import com.lunivore.chimmer.binary.KsizKwdaSub
import com.lunivore.chimmer.binary.Record
import com.lunivore.chimmer.helpers.Masters

@UseExperimental(ExperimentalUnsignedTypes::class)
data class Weapon(override val record: Record) : SkyrimObject<Weapon>(record) {

    class Flags {

        companion object {
            val IGNORES_NORMAL_WEAPON_RESISTANCE: UShort = 0x01u
            val AUTOMATIC: UShort = 0x02u
            val HAS_SCOPE: UShort = 0x04u
            val CANT_DROP: UShort = 0x08u
            val HIDE_BACKPACK: UShort = 0x10u
            val EMBEDDED_WEAPON: UShort = 0x20u
            val DONT_USE_FIRST_PERSON_IS_ANIM: UShort = 0x40u
            val UNPLAYABLE: UShort = 0x80u
        }
    }

    companion object {

        private val VALUE = StructLookup("DATA", 0x0, 0x4)
        private val WEIGHT = StructLookup("DATA", 0x04, 0x08)
        private val DAMAGE = StructLookup("DATA", 0x08, 0x0A)

        private val WEAPON_TYPE = StructLookup("DNAM", 0x0, 0x1)
        private val SPEED = StructLookup("DNAM", 0x04, 0x08)
        private val REACH = StructLookup("DNAM", 0x08, 0x0C)
        private val FLAGS = StructLookup("DNAM", 0x0C, 0x0E)
    }

    enum class WeaponType(val type: Int) {
        OTHER(0), // Used by unarmed, weapon versions of knife and fork, and unpatched Woodcutter's Axe
        ONE_HAND_SWORD(1),
        ONE_HAND_DAGGER(2),
        ONE_HAND_AXE(3),
        ONE_HAND_MACE(4),
        TWO_HAND_SWORD(5),
        TWO_HAND_AXE(6),
        BOW(7),
        STAFF(8),
        CROSSBOW(9);

        companion object {
            fun lookup(index: Int): WeaponType = values()[index]
        }
    }

    override fun create(newRecord: Record): Weapon = Weapon(newRecord)

    val damage : UShort get() = structSubToUShort(DAMAGE)
    val weight : Float get() = structSubToFloat(WEIGHT)
    val value: UInt get() = structSubToUInt(VALUE)


    val weaponType: WeaponType get() = WeaponType.lookup(structSubToInt(WEAPON_TYPE))
    val flags: UShort get() = structSubToUShort(FLAGS)
    val reach: Float get() = structSubToFloat(REACH)
    val speed: Float get() = structSubToFloat(SPEED)

    val template: FormId? get() = formIdSubToFormId("CNAM")

    val keywords: List<FormId> get() = (record.find("KWDA") as KsizKwdaSub?)?.keywords ?: listOf()

    val criticalDamage: Int get() = (record.find("CRDT") as CrdtSub?)?.critDamage?.toInt() ?: 0
    val criticalMultiplier: Float get() = (record.find("CRDT") as CrdtSub?)?.critMultiplier ?: 0.0f
    val criticalSpellEffect: FormId get() = (record.find("CRDT") as CrdtSub?)?.critSpellEffect ?: FormId.NULL_REFERENCE

    val enchantment: FormId? get() = formIdSubToFormId("EITM")

    val editorId: String get() = byteSubToString("EDID")

    fun plusKeyword(keywordToAdd: FormId): Weapon { return withKeywords(keywords.plus(keywordToAdd)) }

    fun withKeywords(keywords: List<FormId>): Weapon {
        val newMasters = includeNewMasters(keywords)
        return create(record.with(KsizKwdaSub(keywords)).copy(mastersForParsing = newMasters))
    }

    fun withTemplate(newTemplate: FormId): Weapon {
        val newMasters = includeNewMasters(listOf(newTemplate))
        return create(record.with(FormIdSub.create("CNAM", newTemplate)).copy(mastersForParsing = newMasters))
    }

    fun withValue(value: UInt): Weapon = withStructPart(VALUE, value)
    fun withWeight(newWeight: Float): Weapon = withStructPart(WEIGHT, newWeight)
    fun withDamage(newDamage: UShort): Weapon = withStructPart(DAMAGE, newDamage)
    fun withReach(newReach: Float): Weapon = withStructPart(REACH, newReach)
    fun withSpeed(newSpeed: Float): Weapon = withStructPart(SPEED, newSpeed)
    fun withFlags(newFlags: UShort): Weapon = withStructPart(FLAGS, newFlags)

    fun withCriticalDamage(criticalDamage: Int): Weapon {
        return create(record.with(findOrCreateCrdt().copy(critDamage = criticalDamage.toUShort())))
    }

    fun withCriticalMultiplier(criticalMultiplier: Float): Weapon {
        return create(record.with(findOrCreateCrdt().copy(critMultiplier = criticalMultiplier)))
    }

    fun withCriticalSpellEffect(criticalSpellEffect: FormId): Weapon {
        val newMasters = includeNewMasters(listOf(criticalSpellEffect))
        return create(record.with(findOrCreateCrdt().copy(critSpellEffect = criticalSpellEffect)).copy(mastersForParsing = newMasters))
    }

    private fun findOrCreateCrdt() = (record.find("CRDT") as CrdtSub?) ?: CrdtSub(0u.toUShort(), 0.0f, 0u, FormId.NULL_REFERENCE)

    private fun includeNewMasters(newFormIds: List<FormId>): Masters {
        return includeNewMasters(newFormIds.map { it.master }.toSet())
    }

    private fun includeNewMasters(newMasters: Set<String>): Masters {
        val orderedMasters = record.masters.plus(
                newMasters.filter { !record.masters.contains(it) }
        ).filter { it != originMod }
        return Masters(orderedMasters)
    }

    fun withEnchantment(enchantment: FormId): Weapon = withFormId("EITM", enchantment)

}
