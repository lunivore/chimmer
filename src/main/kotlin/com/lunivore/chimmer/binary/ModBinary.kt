package com.lunivore.chimmer.binary

import com.lunivore.chimmer.ConsistencyRecorder
import com.lunivore.chimmer.FormId
import com.lunivore.chimmer.FormIdKeyComparator
import com.lunivore.chimmer.binary.ModBinary.Companion.GRUP_ORDER
import com.lunivore.chimmer.binary.Record.Companion.consistencyRecorderForTes4


// TODO: Recalculate next available object id (highest + 1024), number of records and groups, and the masterlist.

@UseExperimental(ExperimentalUnsignedTypes::class)
data class ModBinary(val modName: String?, val header: Record, val grups: List<Grup>) : List<Grup> by grups {
    companion object {
        internal val GRUP_ORDER = fromCommaDelimitedToList("""
            GMST, KYWD, LCRT, AACT, TXST, GLOB, CLAS, FACT,
            HDPT, HAIR, EYES, RACE, SOUN, ASPC, MGEF, SCPT,
            LTEX, ENCH, SPEL, SCRL, ACTI, TACT, ARMO, BOOK,
            CONT, DOOR, INGR, LIGH, MISC, APPA, STAT, SCOL,
            MSTT, PWAT, GRAS, TREE, CLDC, FLOR, FURN, WEAP,
            AMMO, NPC_, LVLN, KEYM, ALCH, IDLM, COBJ, PROJ,
            HAZD, SLGM, LVLI, WTHR, CLMT, SPGD, RFCT, REGN,
            NAVI, CELL, WRLD, DIAL, QUST, IDLE, PACK, CSTY,
            LSCR, LVSP, ANIO, WATR, EFSH, EXPL, DEBR, IMGS,
            IMAD, FLST, PERK, BPTD, ADDN, AVIF, CAMS, CPTH,
            VTYP, MATT, IPCT, IPDS, ARMA, ECZN, LCTN, MESG,
            RGDL, DOBJ, LGTM, MUSC, FSTP, FSTS, SMBN, SMQN,
            SMEN, DLBR, MUST, DLVW, WOOP, SHOU, EQUP, RELA,
            SCEN, ASTP, OTFT, ARTO, MATO, MOVT, HAZD, SNDR,
            DUAL, SNCT, SOPM, COLL, CLFM, REVB
        """)


        private fun fromCommaDelimitedToList(grupList: String) =
                grupList.lines().joinToString("").split(",").map {it.trim()}

        fun parse(loadingMod: String, bytes: ByteArray): ModBinary {

            val result = Record.parseTes4(loadingMod, bytes.toList())

            val headerRecord = result.parsed
            val grups = Grup.parseAll(loadingMod, result.rest, headerRecord.masters)

            return ModBinary(loadingMod, headerRecord, grups)
        }

        fun create(): ModBinary {

            val tes4 = Record.createTes4()
            return ModBinary(null, tes4, listOf())
        }
    }

    val masters: Set<String>
        get() {
            return grups.flatMap { it.records.flatMap { it.masters.plus(it.formId.master) } }.filterNot { it.isNullOrEmpty() }.map { it!! }.toSet()
//
//            val mastersOfRecords = grups.flatMap { it.records.flatMap { it.masters.plus(it.formId.master) } }
//            val originsWhereNoMasters = grups.flatMap {
//                it.records.flatMap {
//                    if (it.formId.master == null) listOf(it.formId.loadingMod) else listOf<String>()
//                }
//            }
//            val mastersToUse = (mastersOfRecords + originsWhereNoMasters).filterNot { it.isNullOrEmpty() }.map { it!! }.toSet()
//            return mastersToUse
        }

    fun render(loadOrderForMasters: List<String>, consistencyRecorder: ConsistencyRecorder, renderer: (ByteArray) -> Unit) {

        val mastersToUse = masters
        val orderedMasters = loadOrderForMasters.filter { mastersToUse.contains(it) }.plus(
                mastersToUse.filter { !loadOrderForMasters.contains(it) }
        )

        header.render(orderedMasters, consistencyRecorderForTes4, renderer)
        grups.forEach { it.render(orderedMasters, consistencyRecorder, renderer) }
    }

    fun <T : RecordWrapper<T>> createOrReplaceGrup(type: String, recordWrappers: List<T>): ModBinary {
        if (grups.any { it.isType(type)})  {
            return copy(grups = grups.map {
                if (!it.isType(type)) it
                else {
                    Grup(type, it.headerBytes, recordWrappers.map { it.record })
                }
            })
        } else {
            val grupsBeforeType = GRUP_ORDER.subList(0, GRUP_ORDER.indexOf(type))
            val typeList = grups.map { it.type }
            val lastGrupWeContainBeforeType = typeList.lastOrNull { grupsBeforeType.contains(it) }
            val newGrups = grups.toMutableList()
            val newGrup = Grup(type, Grup.EMPTY_HEADER_BYTES, recordWrappers.map { it.record })
            if (lastGrupWeContainBeforeType == null) {
                newGrups.add(newGrup)
            } else {
                newGrups.add(typeList.indexOf(lastGrupWeContainBeforeType) + 1, newGrup)
            }
            return copy(grups = newGrups)
        }
    }

    fun find(type: String): Grup? = find { it.type == type }
}

fun List<ModBinary>.merge(loadOrder: List<String>): ModBinary {
    try {

        // Creates a list of all the grups for each grupname, eg:
        // ARMO1, ARMO2
        // (No KYWDS, empty list)
        // WEAP1, WEAP3, WEAP5
        val grupsByNameFromEachMod = GRUP_ORDER.map { grupName -> this.map { it.find(grupName) }.filterNotNull() }

        // For each record in each grup, finds the last version of it and adds it to a map by FormId.Key
        // ARMO: ArmoRec1, ArmoRec2... ArmoRec14
        // (No KYWDS, still an empty list)
        // WEAP: WeapRec1, WeapRec2... WeapRec8
        val grupOrderedMapsOfLastRecordsWithFormIdKey = grupsByNameFromEachMod.map {
            it.foldRight(mutableMapOf<FormId.Key, Record>()) { grup, mapOfRecordsByFormIdKey ->
                grup.forEach {
                    try {
                        mapOfRecordsByFormIdKey.putIfAbsent(it.formId.key, it)
                    } catch (e: Exception) {
                        throw Exception("Error merging record=${it.formId.toBigEndianHexString()} with EDID=${it.find("EDID")?.asString()}", e)
                    }
                }
                mapOfRecordsByFormIdKey
            }
        }

        // For each grup name, order the records by FormId then make a new grup with all the records in it,
        // and a mod with the new grups.
        val comparator = FormIdKeyComparator(loadOrder)
        val newGrups = grupOrderedMapsOfLastRecordsWithFormIdKey.mapIndexed { i, mappy ->
            if (grupsByNameFromEachMod[i].isEmpty()) null else
                grupsByNameFromEachMod[i].first().copy(records = mappy.keys
                        .sortedWith(comparator).map { mappy[it]!! })
        }.filterNotNull()

        return ModBinary.create().copy(grups = newGrups)
    } catch (e: Exception) {
        throw Exception("Error merging mods, loadOrder=${loadOrder}", e)
    }
}
