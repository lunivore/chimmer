package com.lunivore.chimmer.binary

import com.lunivore.chimmer.ConsistencyRecorder

data class Grup(val type: String, val headerBytes: List<Byte>, val records: List<Record>) : List<Record> by records {

    companion object {
        val EMPTY_HEADER_BYTES = ByteArray(12).toList()

        fun parseAll(bytes: List<Byte>, masters: List<String>, consistencyRecorder: ConsistencyRecorder): List<Grup> {

            val grups = mutableListOf<Grup>()
            var rest = bytes

            while (rest.size > 3 && String(rest.subList(0, 4).toByteArray()) == "GRUP") {
                val grupLength = rest.subList(4, 8).toLittleEndianInt() // Note that this INCLUDES the group header (24 bytes)
                val type = String(rest.subList(8, 12).toByteArray())
                val grupHeaderBytes = rest.subList(12, 24)
                val recordBytes = rest.subList(24, grupLength)

                rest = if (rest.size >= grupLength) rest.subList(grupLength, rest.size) else listOf()

                Record.logger.debug("Found Grup $type with length $grupLength")

                val grup = Grup(type, grupHeaderBytes, recordBytes, masters, consistencyRecorder)

                grups.add(grup)
            }
            return grups
        }

    }

    constructor(type: String, headerBytes: List<Byte>, records: List<Byte>, masters: List<String>,
                consistencyRecorder: ConsistencyRecorder) :
            this(type, headerBytes, Record.parseAll(records, masters, consistencyRecorder).parsed)

    fun renderTo(renderer: (ByteArray) -> Unit, masterList: List<String>) {

        var lengthOfRecords = 0
        forEach { it.renderTo({ lengthOfRecords += it.size }, masterList) }

        val lengthIncludingGroupHeader = 24 + lengthOfRecords

        renderer("GRUP".toByteArray())
        renderer(lengthIncludingGroupHeader.toLittleEndianBytes())
        renderer(type.toByteArray())
        renderer(headerBytes.toByteArray())
        forEach { it.renderTo(renderer, masterList) }
    }

    fun isType(type: String): Boolean {
        return this.type == type
    }

}
