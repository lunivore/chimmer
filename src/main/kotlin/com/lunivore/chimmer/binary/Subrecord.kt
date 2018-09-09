package com.lunivore.chimmer.binary

import org.apache.logging.log4j.LogManager

class Subrecord(val type: String, val bytes: List<Byte>) {
    fun renderTo(renderer: (ByteArray) -> Unit) {
        renderer(type.toByteArray())
        renderer(bytes.size.toShort().toLittleEndianBytes())
        renderer(bytes.toByteArray())
    }

    fun asString(): String {
        return String(bytes.toByteArray()).trimEnd('\u0000')
    }

    companion object {
        @JvmStatic
        val logger = LogManager.getLogger(Subrecord::class.java)

        fun parse(bytes: List<Byte>): ParseResult<List<Subrecord>> {
            var rest = bytes
            val subrecords = mutableListOf<Subrecord>()

            // All subrecords have a 4-letter code, followed by a short (2 bytes) showing their length.
            // If we don't have at least 6 bytes, we're done.
            while (rest.size > 5) {
                val type = String(rest.subList(0, 4).toByteArray())
                val length = rest.subList(4, 6).toLittleEndianInt()

                logger.debug("Subrecord $type, length $length")

                // TODO: Throw an error if the rest of the bytes aren't long enough

                subrecords.add(Subrecord(type, rest.subList(6, 6 + length)))
                rest = if (rest.size <= 6 + length) listOf() else rest.subList(6 + length, rest.size)
            }

            // TODO: Throw an error if there are any bytes left over. Since we're greedy when it comes to creating
            // our subrecords, we need to be given the exact bytes; anything else means something went wrong.
            return ParseResult(subrecords, rest)
        }

    }

}
