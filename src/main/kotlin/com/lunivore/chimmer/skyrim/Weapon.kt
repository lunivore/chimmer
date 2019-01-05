package com.lunivore.chimmer.skyrim

import com.lunivore.chimmer.FormId
import com.lunivore.chimmer.binary.Record
import com.lunivore.chimmer.binary.Subrecord
import com.lunivore.chimmer.binary.toLittleEndianBytes
import com.lunivore.chimmer.binary.toLittleEndianUInt

@UseExperimental(ExperimentalUnsignedTypes::class)
data class Weapon(override val record: Record) : SkyrimObject<Weapon>(record) {

    override fun create(newRecord: Record): Weapon {
        return Weapon(newRecord)
    }

    val keywords: List<FormId>
            get(){
                val kwda = record.find("KWDA") ?: return listOf()
                return kwda.bytes.chunked(4).map { FormId(loadingMod, it.toLittleEndianUInt(), record.masters) }
            }

    fun plusKeyword(keywordToAdd: FormId): Weapon { return withKeywords(keywords.plus(keywordToAdd)) }

    fun withKeywords(keywords: List<FormId>): Weapon {
        // TODO: Duplication with ModBinary; move this somewhere useful
        val newMasters = keywords.flatMap { it.masters.plus(it.master) }.filterNot { it.isNullOrEmpty() }.map { it!! }.toSet()
        val orderedMasters = record.masters.filter { newMasters.contains(it) }.plus(
                newMasters.filter { !record.masters.contains(it) }
        )

        val reindexedKeywords = keywords.map { it.reindex(orderedMasters) }

        return create(reindex(orderedMasters)
                .with(Subrecord("KSIZ", reindexedKeywords.size.toLittleEndianBytes().toList()))
                .with(Subrecord("KWDA", reindexedKeywords.fold(listOf()) { r, t -> r + t.rawByteList})))
    }

    private fun reindex(newMasters: List<String>): Record {
        // NB: Keywords missed out here as it's the only one we're replacing anyway right now;
        // need to update this to include keywords if any other form ids are replaced

        val subrecordsToReindex = "BAMT, BIDS, CNAM, EITM, ETYP, INAM, NAM7, NAM8, NAM9, SNAM, TNAM, UNAM, WNAM, XNAM, YNAM, ZNAM"
                .splitToSequence(", ")

        val reindexedSubs = subrecordsToReindex.map { record.find(it) }.filterNotNull()
                .map { Pair(it, FormId(loadingMod, it.bytes.toLittleEndianUInt(), record.masters).reindex(newMasters)) }
                .map { Subrecord(it.first.type, it.second.rawByteList)}
                .plus(reindexCRDT(newMasters))

        return reindexedSubs.fold(record) {r, s -> r.with(s) }.copy(masters = newMasters)
    }

    private fun reindexCRDT(newMasters: List<String>): List<Subrecord> {
        val crdt = record.find("CRDT") ?: return listOf()
        val spellEffect = FormId(loadingMod, crdt.bytes.subList(12, 16).toLittleEndianUInt(), record.masters)
        val reindexedSpellEffect = spellEffect.reindex(newMasters)
        return listOf(Subrecord("CRDT", crdt.bytes.subList(0, 12).plus(reindexedSpellEffect.rawByteList)))
    }

}
