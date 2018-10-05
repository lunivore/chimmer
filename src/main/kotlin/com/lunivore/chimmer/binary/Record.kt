package com.lunivore.chimmer.binary

import com.lunivore.chimmer.ConsistencyRecorder
import com.lunivore.chimmer.FormId
import com.lunivore.chimmer.Logging
import javax.xml.bind.DatatypeConverter

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
data class Record(val type: String, val flags: Int, val formId: FormId, val formVersion: Int, val subrecords: List<Subrecord>,
                  val masters: List<String>, val consistencyRecorder: ConsistencyRecorder)
    : List<Subrecord> by subrecords {

    companion object {
        val RECORD_HEADER_SIZE = 24
        val logger by Logging()

        val consistencyRecorderForTes4: ConsistencyRecorder = {
            throw IllegalStateException("TES4 records should always have form id of 0")
        }

        fun parseTes4(bytes: List<Byte>): ParseResult<Record> {
            val (headerBytes, subrecordResult, rest) = parseDataForType("TES4", bytes)
            val masters = findMastersForTes4HeaderRecordOnly(subrecordResult.parsed)

            return ParseResult(create(headerBytes, subrecordResult.parsed, masters,
                    consistencyRecorderForTes4), rest)
        }

        fun parseAll(bytes: List<Byte>, masters: List<String>, consistencyRecorder: ConsistencyRecorder): ParseResult<List<Record>> {
            val records = mutableListOf<Record>()
            var rest = bytes
            // TODO: Throw an exception if the bytes aren't long enough to be a record

            val type = String(bytes.subList(0, 4).toByteArray())

            while (startsWithRecordType(rest, type)) {

                val (headerBytes, subrecordResult, newRest) = parseDataForType(type, rest)
                rest = newRest

                records.add(create(headerBytes, subrecordResult.parsed, masters, consistencyRecorder))
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

            logger.info("Found type $type with form Id ${DatatypeConverter.printHexBinary(bytes.subList(12, 16).toByteArray())}, length $recordLength")

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

        /**
         * We discard everything else in the Record header as it's either never used or we'll derive it (e.g.: data size).
         */
        fun create(headerBytes: List<Byte>, subrecords: List<Subrecord>, masters: List<String>,
                    consistencyRecorder: ConsistencyRecorder) : Record {


            val type = String(headerBytes.subList(0, 4).toByteArray())
            val flags = headerBytes.subList(8, 12).toLittleEndianInt()
            val formId = if (type == "TES4") FormId.TES4 else
                FormId(headerBytes.subList(12, 16), masters)
            val formVersion = headerBytes.subList(20, 22).toLittleEndianInt() // Oldrim = 43, SSE = 44
            return Record(
                    type,
                    flags,
                    formId,
                    formVersion,
                    subrecords,
                    masters,
                    consistencyRecorder)
        }

    }


    fun renderTo(renderer: (ByteArray) -> Unit, masterList: List<String>) {
        if (type == "TES4") {
            renderTes4ReplacingMasterlistTo(renderer, masterList)
        } else {
            var size = 0
            subrecords.forEach { it.renderTo { size += it.size } }

            renderHeader(renderer, size, this.formId.toLittleEndianBytes(masterList))
            subrecords.forEach { it.renderTo(renderer) }
        }




    }

    private fun renderHeader(renderer: (ByteArray) -> Unit, size: Int, formId: ByteArray) {
        renderer(type.toByteArray())
        renderer(size.toLittleEndianBytes())
        renderer(flags.toLittleEndianBytes())
        renderer(formId)
        renderer(0.toLittleEndianBytes())
        renderer(formVersion.toShort().toLittleEndianBytes())
        renderer(0.toShort().toLittleEndianBytes())
    }

    private fun renderTes4ReplacingMasterlistTo(renderer: (ByteArray) -> Unit, masterList: List<String>) {
        val tes4MasterSubrecords : List<Subrecord> =
            masterList.map {
                listOf(
                        Subrecord("MAST", "$it\u0000".toByteList()),
                        Subrecord("DATA", listOf()))
            }.flatten()

        val masterListTypes = listOf("MAST", "DATA")
        val subrecordsWithoutMasters = subrecords.filter {
            !masterListTypes.contains(it.type)
        }

        var size = 0
        subrecordsWithoutMasters.forEach { it.renderTo { size += it.size } }
        tes4MasterSubrecords.forEach { it.renderTo { size += it.size } }

        renderHeader(renderer, size, ByteArray(4))
        subrecordsWithoutMasters.forEach { it.renderTo(renderer)}
        tes4MasterSubrecords.forEach { it.renderTo(renderer) }
    }

    /**
     * A convenience method for looking up subrecords by type, since this is something we'll be doing a lot.
     */
    fun find(type: String): Subrecord? {
        return find { it.type == type }
    }

    fun copyAsNew(master : String): Record {
        val editorId: String = find("EDID")?.asString()
                ?: throw IllegalStateException("This record has no EDID subrecord and cannot be copied as new")
        val unindexedFormId = consistencyRecorder(editorId)

        return copy(formId = FormId(unindexedFormId, master), masters = masters + listOf(master))
    }

    fun with(newSubrecord: Subrecord): Record {
        val subrecords = map { if (it.type == newSubrecord.type) newSubrecord else it }
        return copy(subrecords = subrecords)
    }


}