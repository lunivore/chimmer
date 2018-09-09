package com.lunivore.chimmer.binary

data class Grup(val headerBytes : List<Byte>, val records: List<Record>) : List<Record> by records {

    companion object {
        fun parseAll(bytes: List<Byte>, masters: List<String>): List<Grup> {

            val grups = mutableListOf<Grup>()
            var rest = bytes

            while (rest.size > 3 && String(rest.subList(0, 4).toByteArray()) == "GRUP") {
                val grupLength = rest.subList(4, 8).toLittleEndianInt() // Note that this INCLUDES the group header (24 bytes)
                val grupHeaderBytes = rest.subList(8, 24)
                val recordBytes = rest.subList(24, grupLength)

                rest = if (rest.size >= grupLength) rest.subList(grupLength, rest.size) else listOf()

                Record.logger.debug("Found Grup ${String(recordBytes.subList(0, 4).toByteArray())} with length $grupLength")

                val grup = Grup(grupHeaderBytes, recordBytes, masters)

                grups.add(grup)
            }
            return grups
        }

    }

    constructor(headerBytes: List<Byte>, records: List<Byte>, masters: List<String>) :
            this(headerBytes, Record.parseAll(records, masters).parsed)

    fun renderTo(renderer: (ByteArray) -> Unit) {

        var lengthOfRecords = 0
        forEach { it.renderTo { lengthOfRecords += it.size } }

        val lengthIncludingGroupHeader = 24 + lengthOfRecords

        renderer("GRUP".toByteArray())
        renderer(lengthIncludingGroupHeader.toLittleEndianBytes())
        renderer(headerBytes.toByteArray())
        forEach { it.renderTo(renderer) }
    }

}
