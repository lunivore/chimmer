package com.lunivore.chimmer.skyrim

import com.lunivore.chimmer.ExistingFormId
import com.lunivore.chimmer.FormId
import com.lunivore.chimmer.binary.*

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

    override fun create(newRecord: Record): Weapon {
        return Weapon(newRecord)
    }

    val weaponType: WeaponType = WeaponType.values()[record.find("DNAM")?.bytes?.subList(0, 1)?.toLittleEndianInt() ?: throw IllegalStateException("No DNAM found in weapon ${formId.asDebug()}")]
    val template: FormId? = record.find("CNAM")?.asFormId(record.formId.master, record.masters)
    val damage : UShort = record.find("DATA")?.bytes?.toLittleEndianUShort() ?: throw IllegalStateException("No DATA found in weapon ${formId.asDebug()}")
    val reach: Float = record.find("DNAM")?.bytes?.subList(0x08, 0x0C)?.toLittleEndianFloat() ?: throw IllegalStateException("No DNAM found in weapon ${formId.asDebug()}")
    val speed: Float = record.find("DNAM")?.bytes?.subList(0x04, 0x08)?.toLittleEndianFloat() ?: throw IllegalStateException("No DNAM found in weapon ${formId.asDebug()}")

    val keywords: List<FormId>
            get(){
                val kwda = record.find("KWDA") ?: return listOf()
                return kwda.bytes.chunked(4).map { FormId.create(loadingMod, it.toLittleEndianUInt(), record.masters) }
            }

    val editorId: String
        get() {
            return record.find("EDID")?.asString() ?: ""
        }

    fun plusKeyword(keywordToAdd: FormId): Weapon { return withKeywords(keywords.plus(keywordToAdd)) }

    fun withKeywords(keywords: List<FormId>): Weapon {
        val orderedMasters = includeNewMasters(keywords)

        return create(reindex(orderedMasters, "KWDA")
                .with(ByteSub.create("KSIZ", keywords.size.toLittleEndianBytes().toList()))
                .with(ByteSub.create("KWDA", keywords.fold(listOf()) { r, t -> r + t.reindexedFor(orderedMasters)})))
    }

    fun withTemplate(newTemplate: FormId): Weapon {
        val orderedMasters = includeNewMasters(listOf(newTemplate))
        return create(reindex(orderedMasters,"CNAM").with(ByteSub.create("CNAM", (newTemplate as ExistingFormId).reindexedFor(orderedMasters))))
    }

    fun withDamage(damage: UShort): Weapon {
        val data = record.find("DATA")?.bytes ?: throw IllegalStateException("No DATA found in weapon ${formId.asDebug()}")
        val newData = data.subList(0, 0x08).plus(damage.toLittleEndianBytes().toList())
        return create(record.with(ByteSub.create("DATA", newData)))
    }

    fun withReach(reach: Float): Weapon {
        val dnam = record.find("DNAM")?.bytes ?: throw IllegalStateException("No DNAM found in weapon ${formId.asDebug()}")
        val newDnam = dnam.subList(0, 0x08).plus(reach.toLittleEndianBytes().toList()).plus(dnam.subList(0x0C, dnam.size))
        return create(record.with(ByteSub.create("DNAM", newDnam)))
    }

    fun withSpeed(speed: Float): Weapon {
        val dnam = record.find("DNAM")?.bytes ?: throw IllegalStateException("No DNAM found in weapon ${formId.asDebug()}")
        val newDnam = dnam.subList(0, 0x04).plus(speed.toLittleEndianBytes().toList()).plus(dnam.subList(0x08, dnam.size))
        return create(record.with(ByteSub.create("DNAM", newDnam)))
    }

    fun withFlags(flags: UShort): Weapon {
        val dnam = record.find("DNAM")?.bytes ?: throw IllegalStateException("No DNAM found in weapon ${formId.asDebug()}")
        val newDnam = dnam.subList(0, 0x0E).plus(flags.toLittleEndianBytes().toList()).plus(dnam.subList(0x10, dnam.size))
        return create(record.with(ByteSub.create("DNAM", newDnam)))
    }

    private fun includeNewMasters(newFormIds: List<FormId>): List<String> {
        // TODO: Duplication with ModBinary; move this somewhere useful
        return includeNewMasters(newFormIds.map { it.master }.toSet())
    }

    private fun includeNewMasters(newMasters: Set<String>): List<String> {
        val orderedMasters = record.masters.plus(
                newMasters.filter { !record.masters.contains(it) }
        )
        return orderedMasters
    }


    private fun reindex(newMasters: List<String>, skip : String): Record {
        // NB: Keywords missed out here as it's the only one we're replacing anyway right now;
        // need to update this to include keywords if any other form ids are replaced

        val subrecordsToReindex = "BAMT, BIDS, CNAM, EITM, ETYP, INAM, KWDA, NAM7, NAM8, NAM9, SNAM, TNAM, UNAM, WNAM, XNAM, YNAM, ZNAM"
                .splitToSequence(", ").filterNot { it == skip }

        val reindexedSubs = subrecordsToReindex.map { record.find(it) }.filterNotNull()
                .map { Pair(it, FormId.create(loadingMod, it.bytes.toLittleEndianUInt(), record.masters).reindexedFor(newMasters)) }
                .map { ByteSub.create(it.first.type, it.second) }
                .plus(reindexCRDT(newMasters))

        return reindexedSubs.fold(record) {r, s -> r.with(s) }.copy(masters = newMasters)
    }

    private fun reindexCRDT(newMasters: List<String>): List<Subrecord> {
        val crdt = record.find("CRDT") ?: return listOf()
        val spellEffect = FormId.create(loadingMod, crdt.bytes.subList(12, 16).toLittleEndianUInt(), record.masters)
        val reindexedSpellEffect = spellEffect.reindexedFor(newMasters)
        return listOf(ByteSub.create("CRDT", crdt.bytes.subList(0, 12).plus(reindexedSpellEffect)))
    }




}
