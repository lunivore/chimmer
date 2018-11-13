package com.lunivore.chimmer.binary

import com.lunivore.chimmer.ConsistencyRecorder
import com.lunivore.chimmer.binary.Record.Companion.consistencyRecorderForTes4
import com.lunivore.chimmer.skyrim.FormId


// TODO: Recalculate next available object id (highest + 1024), number of records and groups, and the masterlist.

@UseExperimental(ExperimentalUnsignedTypes::class)
data class ModBinary(val header: Record, val grups: List<Grup>) : List<Grup> by grups {
    companion object {
        private val OLDRIM_VERSION = 43.toUShort()
        private val GRUP_ORDER = fromCommaDelimitedToList("""
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

            return ModBinary(headerRecord, grups)
        }

        fun create(): ModBinary {
            val tes4subrecords = listOf(
                    Subrecord("HEDR", listOf(
                            1.7f.toLittleEndianBytes().toList(),
                            0.toLittleEndianBytes().toList(),
                            1024.toLittleEndianBytes().toList()).flatten()),
                    Subrecord("CNAM", "Chimmer\u0000".toByteArray().toList()),
                    Subrecord("SNAM", "\u0000".toByteArray().toList()))

            val tes4 = Record(null, "TES4", 0u, FormId(0u, listOf()), OLDRIM_VERSION, tes4subrecords, listOf())
            return ModBinary(tes4, listOf())
        }
    }

    val masters: Set<String>
        get() {
            val mastersOfRecords = grups.flatMap { it.records.flatMap { it.masters } }
            val originsWhereNoMasters = grups.flatMap {
                it.records.flatMap {
                    if (it.formId.master == null) listOf(it.loadingMod) else listOf<String>()
                }
            }
            val mastersToUse = (mastersOfRecords + originsWhereNoMasters).filterNot { it.isNullOrEmpty() }.map { it!! }.toSet()
            return mastersToUse
        }

    fun render(loadOrderForMasters: List<String>, consistencyRecorder: ConsistencyRecorder, renderer: (ByteArray) -> Unit) {

        val mastersToUse = masters
        val orderedMasters = loadOrderForMasters.filter { mastersToUse.contains(it) }.plus(
                mastersToUse.filter { !loadOrderForMasters.contains(it) }
        )

        header.render(orderedMasters, consistencyRecorderForTes4, renderer)
        grups.forEach { it.render(loadOrderForMasters, consistencyRecorder, renderer) }
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
