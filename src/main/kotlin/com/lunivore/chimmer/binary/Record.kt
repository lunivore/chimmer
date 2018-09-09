package com.lunivore.chimmer.binary

import com.lunivore.chimmer.Logging

/**
 * Records hold data about some in-game object or concept, like an iron sword or a fireball spell. They have a
 * type (which must be 4 letters long), like "WEAP" for weapon, and subrecords which contain data about the object,
 * like the name of the weapon and the damage it does.
 *
 * There's also a header record for the entire mod, which is always of type "TES4" and which appears at the start of
 * every mod. Note that the TES4 header is where the masters originate, so any TES4 record will ignore the (usually
 * empty) list of masters passed in.
 *
 * See http://en.uesp.net/wiki/Tes5Mod:Mod_File_Format#Records
 */
data class Record(val header: List<Byte>, val subrecords: List<Subrecord>,
                  val masters: List<String> =
                          findMastersForTes4HeaderRecordOnly(subrecords))
    : List<Subrecord> by subrecords {

    companion object {
        val RECORD_HEADER_SIZE = 24
        val logger by Logging()

        fun parseTes4(bytes: List<Byte>): ParseResult<Record> {
            val (headerBytes, subrecordResult, rest) = parseDataForType("TES4", bytes)
            val masters = findMastersForTes4HeaderRecordOnly(subrecordResult.parsed)
            return ParseResult(Record(headerBytes, subrecordResult.parsed, masters), rest)
        }

        fun parseAll(bytes: List<Byte>, masters: List<String>): ParseResult<List<Record>> {
            val records = mutableListOf<Record>()
            var rest = bytes
            // TODO: Throw an exception if the bytes aren't long enough to be a record

            val type = String(bytes.subList(0, 4).toByteArray())

            while (startsWithRecordType(rest, type)) {

                val (headerBytes, subrecordResult, newRest) = parseDataForType(type, rest)
                rest = newRest

                records.add(Record(headerBytes, subrecordResult.parsed, masters))
            }
            return ParseResult(records, rest)
        }

        private fun parseDataForType(type: String, bytes: List<Byte>):
                Triple<List<Byte>, ParseResult<List<Subrecord>>, List<Byte>> {

            // As well as the mod having a header, each record has a header too.
            // The header is always of a fixed length, starting with the record type as a 4-letter string.
            val headerBytes = bytes.subList(0, RECORD_HEADER_SIZE)

            // The first field after the record type is always the length of the data for the record
            // (we'll recreate this for any records we need it for; here we're just using it to work out what
            // to read in).
            val recordLength = bytes.subList(4, 8).toLittleEndianInt()

            // TODO: Throw an exception if we don't have as many bytes as the recordLength says we should

            val postHeaderData = bytes.subList(24, bytes.size)
            val recordData = postHeaderData.subList(0, recordLength)

            logger.info("Found type $type with form Id ${bytes.subList(12, 16).toList()}, length $recordLength")

            val rest = if (postHeaderData.size > recordLength)
                postHeaderData.subList(recordLength, postHeaderData.size) else listOf()

            val subrecordsResult = Subrecord.parse(recordData)

            val result = Triple(headerBytes, subrecordsResult, rest)
            return result
        }

        private fun findMastersForTes4HeaderRecordOnly(subrecords: List<Subrecord>): List<String> {
            return subrecords.filter { it.type == "MAST" }.map { it.asString() }
        }

        /**
         * Checks to see if we've got the type of record we're looking for at the head of the byte list.
         */
        private fun startsWithRecordType(bytes: List<Byte>, type: String) =
                bytes.size > 3 && parseType(bytes) == type

        /**
         * Records and subrecords are assumed to always have a 4-letter type code.
         */
        private fun parseType(bytes: List<Byte>) = String(bytes.subList(0, 4).toByteArray())

    }

    fun renderTo(renderer: (ByteArray) -> Unit) {
        renderer(header.toByteArray())
        subrecords.forEach { it.renderTo(renderer) }
    }

    /**
     * A convenience method for looking up subrecords by type, since this is something we'll be doing a lot.
     */
    fun find(type: String): Subrecord? {
        return find { it.type == type }
    }


}