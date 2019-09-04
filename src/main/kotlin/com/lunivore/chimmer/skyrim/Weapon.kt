package com.lunivore.chimmer.skyrim

import com.lunivore.chimmer.FormId
import com.lunivore.chimmer.binary.*
import com.lunivore.chimmer.helpers.Masters
import com.lunivore.chimmer.helpers.MastersWithOrigin

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
        CROSSBOW(9)
    }

    override fun create(newRecord: Record): Weapon = Weapon(newRecord)

    val flags: UShort
            get() = record.find("DNAM")?.asBytes()?.subList(0x0C, 0x0E)?.toLittleEndianUShort() ?: throw IllegalStateException("No DNAM found in weapon ${formId.asDebug()}")
    val weaponType: WeaponType
            get() = WeaponType.values()[record.find("DNAM")?.asBytes()?.subList(0, 1)?.toLittleEndianInt() ?: throw IllegalStateException("No DNAM found in weapon ${formId.asDebug()}")]
    val template: FormId?
            get() = record.find("CNAM")?.asFormId(MastersWithOrigin(record.formId.originMod, record.masters))

    val damage : UShort
            get() = record.find("DATA")?.asBytes()?.subList(0x08, 0x0A)?.toLittleEndianUShort() ?: throw IllegalStateException("No DATA found in weapon ${formId.asDebug()}")

    val weight : Float
            get() = record.find("DATA")?.asBytes()?.subList(0x04, 0x08)?.toLittleEndianFloat() ?: throw IllegalStateException("No DATA found in weapon ${formId.asDebug()}")

    val value : UInt
            get() = record.find("DATA")?.asBytes()?.subList(0x0, 0x04)?.toLittleEndianUInt() ?: throw IllegalStateException("No DATA found in weapon ${formId.asDebug()}")

    val reach: Float
            get() = record.find("DNAM")?.asBytes()?.subList(0x08, 0x0C)?.toLittleEndianFloat() ?: throw IllegalStateException("No DNAM found in weapon ${formId.asDebug()}")
    val speed: Float
            get() = record.find("DNAM")?.asBytes()?.subList(0x04, 0x08)?.toLittleEndianFloat() ?: throw IllegalStateException("No DNAM found in weapon ${formId.asDebug()}")

    val keywords: List<FormId>
            get() = (record.find("KWDA") as KsizKwdaSub?)?.keywords ?: listOf()

    val criticalDamage: Int
            get() = (record.find("CRDT") as CrdtSub?)?.critDamage?.toInt() ?: 0
    val criticalMultiplier: Float
            get() = (record.find("CRDT") as CrdtSub?)?.critMultiplier ?: 0.0f
    val criticalSpellEffect: FormId
            get() = (record.find("CRDT") as CrdtSub?)?.critSpellEffect ?: FormId.NULL_REFERENCE

    val enchantment: FormId?
        get() = (record.find("EITM") as FormIdSub)?.asFormId(MastersWithOrigin(record.formId.originMod, record.masters))

    val editorId: String
        get() {
            return record.find("EDID")?.asString() ?: ""
        }

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
        val data = record.find("DATA")?.asBytes() ?: throw IllegalStateException("No DATA found in weapon ${formId.asDebug()}")
        val newData = gold.toLittleEndianByteList().plus(data.subList(0x04, 0x0A))
        return create(record.with(ByteSub.create("DATA", newData)))
    }

    fun withWeight(weight: Float): Weapon {
        val data = record.find("DATA")?.asBytes() ?: throw IllegalStateException("No DATA found in weapon ${formId.asDebug()}")
        val newData = data.subList(0, 0x04).plus(weight.toLittleEndianByteList()).plus(data.subList(0x08, 0x0A))
        return create(record.with(ByteSub.create("DATA", newData)))
    }

    fun withDamage(damage: UShort): Weapon {
        val data = record.find("DATA")?.asBytes() ?: throw IllegalStateException("No DATA found in weapon ${formId.asDebug()}")
        val newData = data.subList(0, 0x08).plus(damage.toLittleEndianByteList())
        return create(record.with(ByteSub.create("DATA", newData)))
    }

    fun withReach(reach: Float): Weapon {
        val dnam = record.find("DNAM")?.asBytes() ?: throw IllegalStateException("No DNAM found in weapon ${formId.asDebug()}")
        val newDnam = dnam.subList(0, 0x08).plus(reach.toLittleEndianByteList()).plus(dnam.subList(0x0C, 0x64))
        return create(record.with(ByteSub.create("DNAM", newDnam)))
    }

    fun withSpeed(speed: Float): Weapon {
        val dnam = record.find("DNAM")?.asBytes() ?: throw IllegalStateException("No DNAM found in weapon ${formId.asDebug()}")
        val newDnam = dnam.subList(0, 0x04).plus(speed.toLittleEndianByteList()).plus(dnam.subList(0x08, 0x64))
        return create(record.with(ByteSub.create("DNAM", newDnam)))
    }

    fun withFlags(flags: UShort): Weapon {
        val dnam = record.find("DNAM")?.asBytes() ?: throw IllegalStateException("No DNAM found in weapon ${formId.asDebug()}")
        val newDnam = dnam.subList(0, 0x0C).plus(flags.toLittleEndianByteList()).plus(dnam.subList(0x0E, 0x64))
        return create(record.with(ByteSub.create("DNAM", newDnam)))
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
