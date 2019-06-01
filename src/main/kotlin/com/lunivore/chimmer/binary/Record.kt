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
 * To avoid having to parse every record, a record will either be constructed with subrecords, or with recordBytes;
 * they may be turned into subrecords later.
 *
 * See http://en.uesp.net/wiki/Tes5Mod:Mod_File_Format#Records
 */
@UseExperimental(ExperimentalUnsignedTypes::class)
data class Record private constructor(val type: String, val flags: UInt, val formId: FormId, val formVersion: UShort, val recordBytes: List<Byte>?, var lazySubrecords: List<Subrecord>?,
                  val masters: List<String>, val menu : SubrecordMenu) {

    companion object {

        private val SUBRECORDS_BY_RECORD_TYPE = mapOf(
                "WEAP" to "EDID, VMAD, OBND, FULL, MODL, MODS, EITM, EAMT, ETYP, BIDS, BAMT, YNAM, ZNAM, KSIZ, KWDA, DESC, INAM, WNAM, SNAM, XNAM, NAM7, TNAM, UNAM, NAM9, NAM8, DATA, DNAM, CRDT, VNAM, CNAM".split(", ")
        )

        private val REVISION_FIELD_IN_RECORD_HEADER = 0u
        private val UNKNOWN_FIELDS_IN_RECORD_HEADER = 0u.toUShort()

        fun create(type: String, flags: UInt, formId: FormId, formVersion: UShort, recordBytes: List<Byte>?, lazySubrecords: List<Subrecord>?, masters: List<String>, menu: SubrecordMenu): Record {
            return Record(type, flags, formId, formVersion, recordBytes, lazySubrecords, masters, menu)
        }
    }

    constructor(modName : String, headerBytes: List<Byte>, recordBytes: List<Byte>?, subrecords: List<Subrecord>?, masters: List<String>, menu: SubrecordMenu) :
            // We discard everything else in the Record header as it's either never used
            // or we'll derive it (e.g.: data size).
            this(String(headerBytes.subList(0, 4).toByteArray()), // Type
                    headerBytes.subList(8, 12).toLittleEndianUInt(),  // Flags
                    FormId.create(modName, headerBytes.subList(12, 16).toLittleEndianUInt(), masters),  // FormId (indexed to given masters)
                    headerBytes.subList(20, 22).toLittleEndianUShort(), // Version; Oldrim = 43, SSE = 44
                    recordBytes, subrecords, masters, menu)

    val subrecords : List<Subrecord>
        get() {
            if (lazySubrecords == null) {
                val result = Subrecord.parse(menu, recordBytes!!)
                if (result.failed) throw IllegalStateException(createErrorMessage(type, recordBytes!!, formId.loadingMod))

                lazySubrecords = result.parsed
            }
            return lazySubrecords!!
        }

    private fun createErrorMessage(type: String, bytes: List<Byte>, modName: String?): String {
        val modToUse = modName ?: "new mod"
        return "Record $type with formId ${formIdAsString(bytes)} malformed in mod ${modToUse}"
    }

    private fun formIdAsString(bytes: List<Byte>) =
            DatatypeConverter.printHexBinary(bytes.subList(12, 16).reversed().toByteArray())

    fun render(newMasters: List<String>, consistencyRecorder: ConsistencyRecorder, renderer: (ByteArray) -> Unit) {

        val formIdToRender : UInt = if (type == "TES4") 0x00000000u
            else { getFormIdToRender(consistencyRecorder, newMasters) }

        if (type == "TES4" || recordBytes == null) {

            val subrecordsToRender = if (type == "TES4") {
                tes4SubrecordsWithMastDataPairs(newMasters)
            } else {
                subrecords!!
            }

            var size = 0
            subrecordsToRender.forEach { it.renderTo { size += it.size } }

            renderRecordHeader(renderer, size, formIdToRender)

            subrecordsToRender.forEach { it.renderTo(renderer) }
        } else {
            renderRecordHeader(renderer, recordBytes.size, formIdToRender)
            renderer(recordBytes!!.toByteArray())
        }
    }

    private fun renderRecordHeader(renderer: (ByteArray) -> Unit, size: Int, formIdToRender: UInt) {
        renderer(type.toByteArray())
        renderer(size.toLittleEndianBytes())
        renderer(flags.toLittleEndianBytes())
        renderer(formIdToRender.toLittleEndianBytes())
        renderer(REVISION_FIELD_IN_RECORD_HEADER.toLittleEndianBytes())
        renderer(formVersion.toLittleEndianBytes())
        renderer(UNKNOWN_FIELDS_IN_RECORD_HEADER.toLittleEndianBytes())
    }

    private fun tes4SubrecordsWithMastDataPairs(newMasters: List<String>): List<Subrecord> {
        val masterRecords = newMasters.flatMap {
            listOf( Subrecord.create("MAST", "$it\u0000".toByteList()), Subrecord.create("DATA", listOf())) }
        val subrecordsToRender : List<Subrecord> = (listOf("HEDR", "CNAM", "SNAM").map { find(it)} +
                masterRecords + listOf("ONAM", "INTV", "INCC").map { find(it)}).filterNotNull()
        return subrecordsToRender
    }

    private fun getFormIdToRender(consistencyRecorder: ConsistencyRecorder, newMasters: List<String>): UInt {
        return if (formId.isNew()) { getConsistentFormIdOrCreateOne(consistencyRecorder, newMasters) }
        else reindexForNewMasters(newMasters)
    }

    private fun getConsistentFormIdOrCreateOne(consistencyRecorder: ConsistencyRecorder, newMasters: List<String>): UInt {
        val editorId: String = find("EDID")?.asString()?.trim('\u0000')
                ?: throw IllegalStateException("This record has no EDID subrecord and cannot be copied as new")
        val unindexedFormId = consistencyRecorder(editorId)

        val nextMasterIndex = newMasters.size
        val indexedFormId = (nextMasterIndex.toUInt() shl 24) + unindexedFormId
        return indexedFormId
    }

    private fun reindexForNewMasters(newMasters: List<String>): UInt {

        // Note that we can't currently reindex all internal formIds, so if this hasn't been converted to
        // subrecords (because we know how to convert it), and the masters have changed, that's a VERY BAD THING.
        // It's fine if the original masters are still at the start of the new master list though.
        if (lazySubrecords == null && (
                        newMasters.size < masters.size || newMasters.subList(0, masters.size) != masters)) {
            val modToUse = formId.loadingMod ?: "new mod"
            throw IllegalStateException("Attempted to reindex unworked record $type with formId ${formId.toBigEndianHexString()} from mod $modToUse; old masters was $masters, new masters are $newMasters")
        }

        val master = formId.master
        val newMasterIndex = if (master == null) newMasters.size else newMasters.indexOf(master)
        if (newMasterIndex == -1) { throw IllegalArgumentException("The master $master was not found in the masterlist $newMasters.") }
        return (newMasterIndex.toUInt() shl 24) + formId.unindexed
    }

    fun copyAsNew(): Record {
        return copy(formId = FormId.createNew(masters))

    }

    fun with(newSubrecord: Subrecord): Record {
        if (subrecords.find { it.type == newSubrecord.type } != null) {
            val newSubrecords = subrecords.map { if (it.type == newSubrecord.type) newSubrecord else it }
            return copy(recordBytes = null, lazySubrecords = newSubrecords)
        } else {
            if (!SUBRECORDS_BY_RECORD_TYPE.containsKey(type) ||
                    !SUBRECORDS_BY_RECORD_TYPE[type]!!.contains(newSubrecord.type)) {
                throw IllegalArgumentException("Cannot find order for new subrecord $type/${newSubrecord.type}")
            }
            val newSubrecords = subrecords.plus(newSubrecord).sortedBy { SUBRECORDS_BY_RECORD_TYPE[type]!!.indexOf(it.type) }
            return copy(recordBytes = null, lazySubrecords = newSubrecords)
        }
    }

    fun isNew(): Boolean {
        return formId.isNew()
    }

    /**
     * A convenience method for looking up subrecords by type, since this is something we'll be doing a lot.
     */
    fun find(type: String): Subrecord? {
        return subrecords.find { it.type == type }
    }

}
