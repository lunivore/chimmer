package com.lunivore.chimmer.binary

import com.lunivore.chimmer.ConsistencyRecorder
import com.lunivore.chimmer.FormId
import com.lunivore.chimmer.binary.Record.Companion.consistencyRecorderForTes4


data class ModBinary(val header: Record, val grups: List<Grup>) : List<Grup> by grups {
    companion object {
        private val OLDRIM_VERSION = 43
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

        fun parse(bytes: ByteArray, consistencyRecorder: ConsistencyRecorder): ModBinary {

            val result = Record.parseTes4(bytes.toList())

            val headerRecord = result.parsed
            val grups = Grup.parseAll(result.rest, headerRecord.masters, consistencyRecorder)

            return ModBinary(headerRecord, grups)
        }

        fun create(modName: String, accessor: ConsistencyRecorder): ModBinary {
            val tes4subrecords = listOf(
                    Subrecord("HEDR", listOf(
                            1.7f.toLittleEndianBytes().toList(),
                            0.toLittleEndianBytes().toList(),
                            1024.toLittleEndianBytes().toList()).flatten()),
                    Subrecord("CNAM", "Chimmer\u0000".toByteList()),
                    Subrecord("SNAM", "\u0000".toByteList()))

            // Note that we're not adding the masterlist here; it needs to be derived when we're ready to save.

            val tes4 = Record("TES4", 0, FormId.TES4, OLDRIM_VERSION, tes4subrecords, listOf(modName),
                    consistencyRecorderForTes4)
            return ModBinary(tes4, listOf())
        }
    }

    fun renderTo(renderer: (ByteArray) -> Unit, masterList: List<String>) {
        header.renderTo(renderer, masterList)
        grups.forEach { it.renderTo(renderer, masterList) }
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
