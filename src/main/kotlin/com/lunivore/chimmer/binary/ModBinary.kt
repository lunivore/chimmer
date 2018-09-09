package com.lunivore.chimmer.binary

// TODO: Recalculate size, next available object id (highest + 1024), number of records and groups, and the masterlist.
data class ModBinary(val header: Record, val grups: List<Grup>) : List<Grup> by grups {

    companion object {
        private val OLDRIM_VERSION = 43

        fun parse(bytes: ByteArray): ModBinary {

            val result = Record.parseTes4(bytes.toList())

            val headerRecord = result.parsed
            val grups = Grup.parseAll(result.rest, headerRecord.masters)

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

            val tes4 = Record("TES4", 0, "00000000".toByteArray().toList(), OLDRIM_VERSION, tes4subrecords, listOf())
            return ModBinary(tes4, listOf())
        }
    }

    fun renderTo(renderer: (ByteArray) -> Unit) {
        header.renderTo(renderer)
        grups.forEach { it.renderTo(renderer) }
    }

    fun replaceGrup(type: String, recordWrappers: List<RecordWrapper>) : ModBinary {
        return copy(grups = grups.map {
            if (!it.isType(type)) it
            else Grup(type, it.headerBytes, recordWrappers.map { it.record })})
    }
}
