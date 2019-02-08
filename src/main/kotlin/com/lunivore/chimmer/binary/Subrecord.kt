package com.lunivore.chimmer.binary

import org.apache.logging.log4j.LogManager

@UseExperimental(ExperimentalUnsignedTypes::class)
data class Subrecord(val type: String, val bytes: List<Byte>) {
    fun renderTo(renderer: (ByteArray) -> Unit) {
        renderer(type.toByteArray())
        renderer(bytes.size.toShort().toLittleEndianBytes())
        renderer(bytes.toByteArray())
    }

    fun asString(): String {
        return String(bytes.toByteArray()).trimEnd('\u0000')
    }

    fun asFloat(): Float {
        return bytes.subList(0, 4).toLittleEndianFloat()
    }

    companion object {
        @JvmStatic
        val logger = LogManager.getLogger(Subrecord::class.java)

        fun parse(bytes: List<Byte>): ParseResult<List<Subrecord>> {
            var rest = bytes
            val subrecords = mutableListOf<Subrecord>()

            // All subrecords have a 4-letter code, followed by a short (2 bytes) showing their length.
            // If we don't have at least 6 bytes, we're done.
            if (rest.size < 6) return ParseResult(listOf(), listOf(), "Failed to parse subrecord")

            while (rest.size > 5) {
                val type = String(rest.subList(0, 4).toByteArray())
                val length = rest.subList(4, 6).toLittleEndianInt()

                logger.debug("Subrecord $type, length $length")

                if (rest.size < 6 + length) return ParseResult(listOf(), listOf(), "Failed to parse subrecord of type $type")

                subrecords.add(Subrecord(type, rest.subList(6, 6 + length)))
                rest = if (rest.size <= 6 + length) listOf() else rest.subList(6 + length, rest.size)
            }

            return ParseResult(subrecords, rest)
        }

    }
}
