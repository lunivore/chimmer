package com.lunivore.chimmer.skyrim

import com.lunivore.chimmer.FormId
import com.lunivore.chimmer.binary.*
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
        data class StructLookup(val sub : String, val start: Int, val end: Int)

        private val VALUE = StructLookup("DATA", 0x0, 0x4)
        private val WEIGHT = StructLookup("DATA", 0x04, 0x08)
        private val DAMAGE = StructLookup("DATA", 0x08, 0x0A)

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

    val damage : UShort get() = (record.find(DAMAGE.sub) as StructSub).toUShort(DAMAGE.start, DAMAGE.end)
    val weight : Float get() = (record.find(WEIGHT.sub) as StructSub).toFloat(WEIGHT.start, WEIGHT.end)
    val value: UInt get() = (record.find(VALUE.sub) as StructSub).toUInt(VALUE.start, VALUE.end)


    val weaponType: WeaponType = WeaponType.lookup((record.find("DNAM") as StructSub).toInt(0, 1))
    val flags: UShort get() = (record.find(FLAGS.sub) as StructSub).toUShort(FLAGS.start, FLAGS.end)
    val reach: Float get() = (record.find(REACH.sub) as StructSub).toFloat(REACH.start, REACH.end)
    val speed: Float get() = (record.find(SPEED.sub) as StructSub).toFloat(SPEED.start, SPEED.end)

    val template: FormId? get() = (record.find("CNAM") as FormIdSub?)?.asFormId()

    val keywords: List<FormId> get() = (record.find("KWDA") as KsizKwdaSub?)?.keywords ?: listOf()

    val criticalDamage: Int get() = (record.find("CRDT") as CrdtSub?)?.critDamage?.toInt() ?: 0
    val criticalMultiplier: Float get() = (record.find("CRDT") as CrdtSub?)?.critMultiplier ?: 0.0f
    val criticalSpellEffect: FormId get() = (record.find("CRDT") as CrdtSub?)?.critSpellEffect ?: FormId.NULL_REFERENCE

    val enchantment: FormId? get() = (record.find("EITM") as FormIdSub?)?.asFormId()

    val editorId: String get() = (record.find("EDID") as ByteSub?)?.asString() ?: ""

    fun plusKeyword(keywordToAdd: FormId): Weapon { return withKeywords(keywords.plus(keywordToAdd)) }

    fun withKeywords(keywords: List<FormId>): Weapon {
        val newMasters = includeNewMasters(keywords)
        return create(record.with(KsizKwdaSub(keywords)).copy(mastersForParsing = newMasters))
    }

    fun withTemplate(newTemplate: FormId): Weapon {
        val newMasters = includeNewMasters(listOf(newTemplate))
        return create(record.with(FormIdSub.create("CNAM", newTemplate)).copy(mastersForParsing = newMasters))
    }

    fun withValue(gold: UInt): Weapon {
        val data = (record.find(VALUE.sub) as StructSub).with(VALUE.start, VALUE.end, gold)
        return create(record.with(StructSub.create(VALUE.sub, data)))
    }

    fun withWeight(newWeight: Float): Weapon {
        val data = (record.find(WEIGHT.sub) as StructSub).with(WEIGHT.start, WEIGHT.end, newWeight)
        return create(record.with(StructSub.create(WEIGHT.sub, data)))
    }

    fun withDamage(newDamage: UShort): Weapon {
        val data = (record.find(DAMAGE.sub) as StructSub).with(DAMAGE.start, DAMAGE.end, newDamage)
        return create(record.with(StructSub.create(DAMAGE.sub, data)))
    }

    fun withReach(newReach: Float): Weapon {
        val data = (record.find(REACH.sub) as StructSub).with(REACH.start, REACH.end, newReach)
        return create(record.with(StructSub.create(REACH.sub, data)))
    }

    fun withSpeed(newSpeed: Float): Weapon {
        val data = (record.find(SPEED.sub) as StructSub).with(SPEED.start, SPEED.end, newSpeed)
        return create(record.with(StructSub.create(SPEED.sub, data)))
    }

    fun withFlags(newFlags: UShort): Weapon {
        val data = (record.find(FLAGS.sub) as StructSub).with(FLAGS.start, FLAGS.end, newFlags)
        return create(record.with(StructSub.create(FLAGS.sub, data)))
    }

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

    fun withEnchantment(enchantment: FormId): Weapon {
        return create(record.with(FormIdSub.create("EITM", enchantment)))
    }

}
