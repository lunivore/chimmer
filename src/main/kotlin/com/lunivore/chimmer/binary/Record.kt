package com.lunivore.chimmer.binary

import com.lunivore.chimmer.ConsistencyRecorder
import com.lunivore.chimmer.ExistingFormId
import com.lunivore.chimmer.FormId
import com.lunivore.chimmer.Logging
import com.lunivore.chimmer.helpers.*
import javax.xml.bind.DatatypeConverter

/**
 * Records hold data about some in-game object or concept, like an iron sword or a fireball spell. They have a
 * type (which must be 4 letters long), like "WEAP" for weapon, and subrecords which contain data about the object,
 * like the name of the weapon and the damage it does.
 *
 * There's also a header record for the entire mod, which is always of type "TES4" and which appears at the start of
 * every mod. Note that the TES4 header is where the value originate, so any TES4 record will ignore the (usually
 * empty) list of value passed in.
 *
 * To avoid having to parse every record, a record will either be constructed with subrecords, or with recordBytes;
 * they may be turned into subrecords later. Don't use both!
 *
 * See http://en.uesp.net/wiki/Tes5Mod:Mod_File_Format#Records
 */
@UseExperimental(ExperimentalUnsignedTypes::class)
data class Record private constructor(val type: String, val flags: UInt, val formId: FormId, val formVersion: UShort, val recordBytes: List<Byte>?, var lazySubrecords: List<Subrecord>?,
                                      private val mastersForParsing: Masters, val menu: SubrecordMenu) {

    // TODO: Masters shouldn't be stored here really; we can derive it from all the FormIdSubs etc. if we denote them as containing FormIds.
    // We already throw a wobbly if anyone tries to convert records with a new masterlist without rendering them to Subs first.

    companion object {

        val SUPPORTED_RECORD_TYPES_TO_SUB_TYPES = mapOf(
                "WEAP" to "EDID, VMAD, OBND, FULL, MODL, MODS, EITM, EAMT, ETYP, BIDS, BAMT, YNAM, ZNAM, KSIZ, KWDA, DESC, INAM, WNAM, SNAM, XNAM, NAM7, TNAM, UNAM, NAM9, NAM8, DATA, DNAM, CRDT, VNAM, CNAM".split(", "),
                "KYWD" to "EDID, CNAM".split(", ")
        )

        private val REVISION_FIELD_IN_RECORD_HEADER = 0u
        private val UNKNOWN_FIELDS_IN_RECORD_HEADER = 0u.toUShort()
        private val logger by Logging()

        fun createNew(type: String, flags: UInt, formId: FormId, formVersion: UShort, lazySubrecords: List<Subrecord>?, masters: Masters): Record {
            return Record(type, flags, formId, formVersion, null, lazySubrecords, masters, SubrecordMenu.UNUSED)
        }

        fun createWithBytes(mastersWithOrigin : MastersWithOrigin, headerBytes: List<Byte>, recordBytes: List<Byte>?, menu: SubrecordMenu) : Record {
            // We discard everything else in the Record header as it's either never used
            // or we'll derive it (e.g.: data size).
            return Record(String(headerBytes.subList(0, 4).toByteArray()), // Type
                    headerBytes.subList(8, 12).toLittleEndianUInt(),  // Flags
                    ExistingFormId.create(mastersWithOrigin, IndexedFormId(headerBytes.subList(12, 16).toLittleEndianUInt())),  // FormId (indexed to given value)
                    headerBytes.subList(20, 22).toLittleEndianUShort(), // Version; Oldrim = 43, SSE = 44
                    recordBytes, null, Masters(mastersWithOrigin.masters), menu)
        }

        fun createWithSubrecords(mastersWithOrigin: MastersWithOrigin, headerBytes: List<Byte>, subrecords: List<Subrecord>?, menu: SubrecordMenu) : Record {
            // We discard everything else in the Record header as it's either never used
            // or we'll derive it (e.g.: data size).
            return Record(String(headerBytes.subList(0, 4).toByteArray()), // Type
                    headerBytes.subList(8, 12).toLittleEndianUInt(),  // Flags
                    ExistingFormId.create(mastersWithOrigin, IndexedFormId(headerBytes.subList(12, 16).toLittleEndianUInt())),  // FormId (indexed to given value)
                    headerBytes.subList(20, 22).toLittleEndianUShort(), // Version; Oldrim = 43, SSE = 44
                    null, subrecords, Masters(mastersWithOrigin.masters), menu)
        }
    }

    val masters: List<String>
            get() {
                if (type == "TES4") return mastersForParsing.value else {
                    val setOfAll = subrecords.filter { it.containsFormIds }.flatMap { it.formIds.map { it.master } }.plus(formId.master).toSet()
                    val containedByMastersProvidedForParsing = mastersForParsing.value.filter { setOfAll.contains(it) }
                    val notContainedByMastersProvidedForParsing = setOfAll.filter { !mastersForParsing.value.contains(it) }
                    return containedByMastersProvidedForParsing.plus(notContainedByMastersProvidedForParsing)
                }
            }

    val subrecords : List<Subrecord>
        get() {
            if (lazySubrecords == null) {
                val result = Subrecord.parse(menu, type, MastersWithOrigin(formId.originMod, mastersForParsing.value), recordBytes!!)
                if (result.failed) throw IllegalStateException(createMalformedErrorMessage(type, recordBytes!!, formId.master))

                lazySubrecords = result.parsed
            }
            return lazySubrecords!!
        }

    private fun createMalformedErrorMessage(type: String, bytes: List<Byte>, modName: String): String {
        return "Record $type with formId ${formIdAsString(bytes)} malformed in mod $modName"
    }

    private fun formIdAsString(bytes: List<Byte>) =
            DatatypeConverter.printHexBinary(bytes.subList(12, 16).reversed().toByteArray())

    fun render(mastersWithOrigin: MastersWithOrigin, consistencyRecorder: ConsistencyRecorder, renderer: (ByteArray) -> Unit) {

        val formIdToRender : List<Byte> = if (type == "TES4") 0u.toLittleEndianByteList()
            else { getFormIdToRender(consistencyRecorder, mastersWithOrigin) }

        if (type == "TES4" || SUPPORTED_RECORD_TYPES_TO_SUB_TYPES.keys.contains(type)) {
            val subrecordsToRender = if (type == "TES4") {
                tes4SubrecordsWithMastDataPairs(mastersWithOrigin.masters)
            } else {
                if (!mastersWithOrigin.containsAll(masters)) {
                    throw IllegalArgumentException("Record ${formId.asDebug()} value ${masters} were not contained in the mastersWithOrigin ${mastersWithOrigin}") }
                subrecords
            }

            var size = 0
            subrecordsToRender.forEach { it.renderTo(mastersWithOrigin, consistencyRecorder) { size += it.size } }
            renderRecordHeader(renderer, size, formIdToRender)
            subrecordsToRender.forEach { it.renderTo(mastersWithOrigin, consistencyRecorder, renderer) }
        } else if (recordBytes != null) {
            val newMasterlistIncompatible = mastersWithOrigin.masters.size < mastersForParsing.value.size || mastersWithOrigin.masters.subList(0, mastersForParsing.value.size) != mastersForParsing.value
            if (newMasterlistIncompatible) throw IllegalStateException("Attempted to render record of type $type, formId{ ${formId.asDebug()} with a new masterlist and without converting form ids.")
            renderRecordHeader(renderer, recordBytes.size, formIdToRender)
            renderer(recordBytes.toByteArray())
        } else {
            logger.info("Skipping empty record ${asDebug()}")
        }
    }

    private fun renderRecordHeader(renderer: (ByteArray) -> Unit, size: Int, formIdToRender: List<Byte>) {
        renderer(type.toByteArray())
        renderer(size.toLittleEndianBytes())
        renderer(flags.toLittleEndianBytes())
        renderer(formIdToRender.toByteArray())
        renderer(REVISION_FIELD_IN_RECORD_HEADER.toLittleEndianBytes())
        renderer(formVersion.toLittleEndianBytes())
        renderer(UNKNOWN_FIELDS_IN_RECORD_HEADER.toLittleEndianBytes())
    }

    private fun tes4SubrecordsWithMastDataPairs(newMasters: List<String>): List<Subrecord> {
        val masterRecords = newMasters.flatMap {
            listOf(ByteSub.create("MAST", "$it\u0000".toByteList()), ByteSub.create("DATA", listOf())) }
        val subrecordsToRender : List<Subrecord> = (listOf("HEDR", "CNAM", "SNAM").map { find(it)} +
                masterRecords + listOf("ONAM", "INTV", "INCC").map { find(it)}).filterNotNull()
        return subrecordsToRender
    }

    private fun getFormIdToRender(consistencyRecorder: ConsistencyRecorder, newMasters: MastersWithOrigin): List<Byte> {
        return formId.toBytes(newMasters, consistencyRecorder)
    }

    private fun getConsistentFormIdOrCreateOne(consistencyRecorder: ConsistencyRecorder, newMasters: Masters): IndexedFormId {
        val editorId: String = find("EDID")?.asString()?.trim('\u0000')
                ?: throw IllegalStateException("This record has no EDID subrecord and cannot be copied as new")
        val unindexedFormId = consistencyRecorder(EditorId(editorId))

        val nextMasterIndex = newMasters.value.size
        val indexedFormId = (nextMasterIndex.toUInt() shl 24) + unindexedFormId.value
        return IndexedFormId(indexedFormId)
    }

    private fun reindexExistingFormIdForNewMasters(newMasters: List<String>): UInt {
        // TODO: Remove this once the FormId is inside a FormIdSub
        val existingFormId = formId as ExistingFormId

        // Note that we can't currently reindex all internal formIds, so if this hasn't been converted to
        // subrecords (because we know how to convert it), and the value have changed, that's a VERY BAD THING.
        // It's fine if the original value are still at the start of the new master list though.
        val newMasterlistIncompatible = newMasters.size < mastersForParsing.value.size || newMasters.subList(0, mastersForParsing.value.size) != mastersForParsing.value
        if (lazySubrecords == null && newMasterlistIncompatible) {
            throw IllegalStateException("""
                Attempted to reindex unworked record $type with ${formId.asDebug()}; 
                old value was $masters, new value are $newMasters. Please explicitly convert to subrecords before trying to reindex."""
                    .trimIndent())
        }

        val newMasterIndex = newMasters.indexOf(formId.master)
        if (newMasterIndex == -1) { throw IllegalArgumentException("The master ${formId.master} was not found in the masterlist $newMasters.") }
        return (newMasterIndex.toUInt() shl 24) + existingFormId.unindexed
    }

    fun copyAsNew(modItGoesInto: OriginMod, editorId: EditorId): Record {
        return copy(formId = FormId.createNew(modItGoesInto, editorId))
    }

    fun with(newSubrecord: Subrecord): Record {
        if (subrecords.find { it.type == newSubrecord.type } != null) {
            val newSubrecords = subrecords.map { if (it.type == newSubrecord.type) newSubrecord else it }
            return copy(recordBytes = null, lazySubrecords = newSubrecords)
        } else {
            if (!SUPPORTED_RECORD_TYPES_TO_SUB_TYPES.containsKey(type) ||
                    !SUPPORTED_RECORD_TYPES_TO_SUB_TYPES[type]!!.contains(newSubrecord.type)) {
                throw IllegalArgumentException("Cannot find order for new subrecord $type/${newSubrecord.type}")
            }
            val newSubrecords = subrecords.plus(newSubrecord).sortedBy { SUPPORTED_RECORD_TYPES_TO_SUB_TYPES[type]!!.indexOf(it.type) }
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

    fun asDebug() : String = "${this.javaClass.simpleName}{ ${formId.asDebug()} "
}
