package com.lunivore.chimmer.binary

import com.lunivore.chimmer.FormId
import com.lunivore.chimmer.Logging
import com.lunivore.chimmer.helpers.Masters
import com.lunivore.chimmer.helpers.MastersWithOrigin
import com.lunivore.chimmer.helpers.OriginMod
import javax.xml.bind.DatatypeConverter

class RecordParser(val menu : SubrecordMenu = SkyrimSubrecordMenu()) {
    private val OLDRIM_VERSION = 43.toUShort()


    val RECORD_HEADER_SIZE = 24
    val logger by Logging()


    fun parseTes4(originMod: OriginMod, bytes: List<Byte>): ParseResult<Record> {
        val temporaryOriginWithEmptyMasters = MastersWithOrigin(originMod.value, listOf())
        val (headerBytes, recordBytes, rest) = parseDataForType(temporaryOriginWithEmptyMasters, "TES4", bytes)

        val subrecordsResult = Subrecord.parseAll(menu, "TES4", temporaryOriginWithEmptyMasters, recordBytes)
        if (subrecordsResult.failed) throw IllegalStateException(createErrorMessage("TES4", bytes, originMod))

        val masters = Masters(findMastersForTes4HeaderRecordOnly(subrecordsResult.parsed))

        return ParseResult(Record.createWithSubrecords(MastersWithOrigin(originMod.value, masters.value), headerBytes, subrecordsResult.parsed, menu), rest)
    }

    fun parseAll(mastersWithOrigin: MastersWithOrigin, bytes: List<Byte>): ParseResult<List<Record>> {
        if (bytes.isEmpty()) return ParseResult(listOf(), bytes)

        val records = mutableListOf<Record>()
        var rest = bytes

        val type = String(bytes.subList(0, 4).toByteArray())

        while (startsWithRecordType(rest, type)) {

            val (headerBytes, recordBytes, newRest) = parseDataForType(mastersWithOrigin, type, rest)
            rest = newRest
            val record = Record.createWithBytes(mastersWithOrigin, headerBytes, recordBytes, menu)
            records.add(record)
        }
        return ParseResult(records, rest)
    }

    private fun parseDataForType(mastersWithOrigin: MastersWithOrigin, type: String, bytes: List<Byte>):
            Triple<List<Byte>, List<Byte>, List<Byte>> {

        // As well as the mod having a header, each record has a header too.
        // The header is always of a fixed length, starting with the record type as a 4-letter string.
        val headerBytes = bytes.subList(0, RECORD_HEADER_SIZE)

        // The first field after the record type is always the length of the data for the record
        // (we'll recreate this for any records we need it for; here we're just using it to work out what
        // to read in).
        val recordLength = bytes.subList(4, 8).toLittleEndianInt()

        if (bytes.size < 24 + recordLength) throw IllegalStateException(
                createErrorMessage(type, bytes, OriginMod(mastersWithOrigin.origin)))

        val postHeaderData = bytes.subList(24, bytes.size)
        val recordData = postHeaderData.subList(0, recordLength)

        logger.debug("Found type $type with form Id ${formIdAsString(bytes)}, length $recordLength")

        val rest = if (postHeaderData.size > recordLength)
            postHeaderData.subList(recordLength, postHeaderData.size) else listOf()

        val result = Triple(headerBytes, recordData, rest)
        return result
    }

    private fun createErrorMessage(type: String, bytes: List<Byte>, modName: OriginMod): String {
        return "Record $type with formId ${formIdAsString(bytes)} malformed in mod ${modName.value}"
    }

    private fun formIdAsString(bytes: List<Byte>) =
            DatatypeConverter.printHexBinary(bytes.subList(12, 16).reversed().toByteArray())

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

    fun createTes4(): Record {

        val tes4subrecords = listOf(
                ByteSub.create("HEDR", listOf(
                                1.7f.toLittleEndianByteList(),
                                0.toLittleEndianByteList(),
                                1024.toLittleEndianByteList()).flatten()),
                ByteSub.create("CNAM", "Chimmer\u0000".toByteArray().toList()),
                ByteSub.create("SNAM", "\u0000".toByteArray().toList()))
        return Record.createNew("TES4", 0u, FormId.TES4, OLDRIM_VERSION, tes4subrecords, Masters.NONE)
    }
}