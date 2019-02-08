package com.lunivore.chimmer.binary

import com.lunivore.chimmer.ConsistencyRecorder
import com.lunivore.chimmer.Logging

/**
 * See: https://en.uesp.net/wiki/Tes5Mod:Mod_File_Format#Groups
 */
@UseExperimental(ExperimentalUnsignedTypes::class)
data class Grup(val type: String, val headerBytes: List<Byte>, val records: List<Record>) : List<Record> by records {

    companion object {
        val EMPTY_HEADER_BYTES = ByteArray(12).toList()
        val logger by Logging()

        fun parseAll(loadingMod: String, bytes: List<Byte>, masters: List<String>): List<Grup> {

            val grups = mutableListOf<Grup>()
            var rest = bytes

            while (rest.size > 3 && String(rest.subList(0, 4).toByteArray()) == "GRUP") {
                val grupLength = rest.subList(4, 8).toLittleEndianInt() // Note that this INCLUDES the group header (24 bytes)
                val type = String(rest.subList(8, 12).toByteArray())
                val grupHeaderBytes = rest.subList(12, 24)
                val recordBytes = rest.subList(24, grupLength)

                rest = if (rest.size >= grupLength) rest.subList(grupLength, rest.size) else listOf()

                logger.info("Found Grup $type with length $grupLength")

                val grup = Grup(loadingMod, type, grupHeaderBytes, recordBytes, masters)

                grups.add(grup)
            }
            return grups
        }

    }

    constructor(loadingMod: String, type: String, headerBytes: List<Byte>, records: List<Byte>, masters: List<String>) :
            this(type, headerBytes, Record.parseAll(loadingMod, records, masters).parsed)

    fun render(masters: List<String>, consistencyRecorder : ConsistencyRecorder, renderer: (ByteArray) -> Unit) {

        var lengthOfRecords = 0
        forEach { it.render(masters, consistencyRecorder) { lengthOfRecords += it.size } }

        val lengthIncludingGroupHeader = 24 + lengthOfRecords

        renderer("GRUP".toByteArray())
        renderer(lengthIncludingGroupHeader.toLittleEndianBytes())
        renderer(type.toByteArray())
        renderer(headerBytes.toByteArray())
        forEach { it.render(masters, consistencyRecorder, renderer) }
    }

    fun isType(type: String): Boolean {
        return this.type == type
    }

}
