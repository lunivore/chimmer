package com.lunivore.chimmer.binary

import com.lunivore.chimmer.ConsistencyRecorder
import com.lunivore.chimmer.ExistingFormId
import com.lunivore.chimmer.FormId
import com.lunivore.chimmer.helpers.IndexedFormId
import com.lunivore.chimmer.helpers.MastersWithOrigin
import org.apache.logging.log4j.LogManager

@UseExperimental(ExperimentalUnsignedTypes::class)
interface Subrecord {

    val type: String
    val containsFormIds : Boolean
    val formIds: List<FormId>

    companion object {
        @JvmStatic
        val logger = LogManager.getLogger(Subrecord::class.java)

        fun parseAll(menu: SubrecordMenu, recordType: String, mastersWithOrigin: MastersWithOrigin, bytes: List<Byte>): ParseResult<List<Subrecord>> {
            var rest = bytes
            val subrecords = mutableListOf<Subrecord>()

            // All subrecords have a 4-letter code, followed by a short (2 bytes) showing their length.
            // If we don't have at least 6 bytes, we're done.
            if (rest.size < 6) return ParseResult(listOf(), listOf(), "Failed to parseAll subrecord") // TODO Should this be throwing an exception?

            while (rest.size > 5) {
                val type = String(rest.subList(0, 4).toByteArray())
                val length = rest.subList(4, 6).toLittleEndianInt()

                logger.debug("Subrecord $type, length $length")

                if (rest.size < 6 + length) return ParseResult(listOf(), listOf(), "Failed to parseAll subrecord of sub $type") // TODO Ditto

                subrecords.add(menu.findProvider(recordType, type)(mastersWithOrigin, rest.subList(6, 6 + length)))
                rest = if (rest.size <= 6 + length) listOf() else rest.subList(6 + length, rest.size)
            }

            return ParseResult(subrecords, rest)
        }
    }

    fun renderTo(mastersIncludingOrigin: MastersWithOrigin, consistencyRecorder: ConsistencyRecorder, renderer: (ByteArray) -> Unit)
    fun asDebug() : String

}

data class ByteSub private constructor(override val type: String, val bytes: List<Byte>) : Subrecord{
    override fun asDebug(): String = "${javaClass.simpleName} : $type, bytes = ${bytes.toReadableHexString()}"

    override val formIds: List<FormId> = listOf()
    override val containsFormIds : Boolean = false

    companion object{
        fun create(type: String, bytes: List<Byte>): ByteSub {
            return ByteSub(type, bytes)
        }
    }

    fun asBytes() = bytes

    override fun renderTo(ignoredMasters: MastersWithOrigin, ignoredCr: ConsistencyRecorder, renderer: (ByteArray) -> Unit)
            = renderTo(renderer)

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

    fun asFormId(mastersWithOrigin : MastersWithOrigin): FormId {
        return ExistingFormId.create(mastersWithOrigin, IndexedFormId(bytes.toLittleEndianUInt()))
    }

}

data class FormIdSub private constructor(override val type: String, val formId : FormId) : Subrecord {
    override val formIds: List<FormId> = listOf(formId)
    override val containsFormIds: Boolean = true

    companion object{
        fun create(mastersWithOrigin : MastersWithOrigin, type: String, bytes: List<Byte>): Subrecord {
            return FormIdSub(type, ExistingFormId.create(mastersWithOrigin, IndexedFormId(bytes.toLittleEndianUInt())))
        }

        fun create(type: String, formId: FormId): Subrecord {
            return FormIdSub(type, formId)
        }
    }

    override fun renderTo(mastersIncludingOrigin: MastersWithOrigin, consistencyRecorder: ConsistencyRecorder, renderer: (ByteArray) -> Unit) {
        ByteSub.create(type, formId.toBytes(mastersIncludingOrigin, consistencyRecorder)).renderTo(renderer)
    }

    fun asFormId(): FormId = formId

    override fun asDebug(): String = "${javaClass.simpleName}{ sub = $type, formId = ${formId.asDebug()}"
}

@ExperimentalUnsignedTypes
data class CrdtSub(val critDamage : UShort, val critMultiplier: Float, val flags : UInt, val critSpellEffect : FormId) : Subrecord {

    override val formIds: List<FormId> = listOf(critSpellEffect)
    override val containsFormIds: Boolean = true
    companion object{
        fun create(mastersWithOrigin : MastersWithOrigin, bytes: List<Byte>): CrdtSub {
            return CrdtSub(
                    bytes.subList(0x00, 0x02).toLittleEndianUShort(),
                    bytes.subList(0x04, 0x08).toLittleEndianFloat(),
                    bytes.subList(0x08, 0x0C).toLittleEndianUInt(),
                    ExistingFormId.create(mastersWithOrigin, IndexedFormId(bytes.subList(0x0C, 0x0F).toLittleEndianUInt())))
        }
    }

    override val type: String
        get() = "CRDT"

    override fun renderTo(mastersIncludingOrigin: MastersWithOrigin, consistencyRecorder: ConsistencyRecorder, renderer: (ByteArray) -> Unit) {
        val bytes = critDamage.toLittleEndianByteList().plus(
                0u.toUShort().toLittleEndianByteList()).plus(
                critMultiplier.toLittleEndianByteList()).plus(
                flags.toLittleEndianByteList()).plus(
                critSpellEffect.toBytes(mastersIncludingOrigin, consistencyRecorder))
        ByteSub.create("CRDT",bytes).renderTo(mastersIncludingOrigin, consistencyRecorder, renderer)
    }

    override fun asDebug(): String = "CrdtSub"
}

@UseExperimental(ExperimentalUnsignedTypes::class)
data class KsizKwdaSub(val keywords : List<FormId>) : Subrecord {
    override fun asDebug(): String = "${javaClass.simpleName} : $keywords"

    override val containsFormIds: Boolean = true
    override val formIds: List<FormId> = keywords

    companion object{
        fun create(mastersWithOrigin : MastersWithOrigin, bytes: List<Byte>): KsizKwdaSub {
           return KsizKwdaSub(bytes.chunked(4).map { ExistingFormId.create(mastersWithOrigin, IndexedFormId(it.toLittleEndianUInt())) })
        }
    }

    override val type: String
        get() = "KWDA"

    override fun renderTo(masters: MastersWithOrigin, consistencyRecorder: ConsistencyRecorder, renderer: (ByteArray) -> Unit) {

        val kwdasAsBytes = keywords.map {it.toBytes(masters, consistencyRecorder)}.flatten()
        ByteSub.create("KSIZ", keywords.count().toUInt().toLittleEndianByteList()).renderTo(renderer)
        ByteSub.create("KWDA", kwdasAsBytes).renderTo(renderer)
    }
}

data class StructSub(override val type: String, val bytes: List<Byte>) : Subrecord {

    override val formIds: List<FormId> get() = listOf()

    override fun asDebug(): String = "${javaClass.simpleName} : sub = $type, bytes = ${bytes.toReadableHexString()}"

    companion object{
        fun create(type: String, bytes: List<Byte>): StructSub {
            return StructSub(type, bytes)
        }
    }

    override val containsFormIds: Boolean = false

    override fun renderTo(ignoredMWO: MastersWithOrigin, ignoredCr: ConsistencyRecorder, renderer: (ByteArray) -> Unit) {
        renderer(type.toByteArray())
        renderer(bytes.size.toShort().toLittleEndianBytes())
        renderer(bytes.toByteArray())
    }

    fun toUShort(start: Int, end: Int): UShort = bytes.subList(start, end).toLittleEndianUShort()
    fun toInt(start: Int, end: Int): Int = bytes.subList(start, end).toLittleEndianInt()
    fun toFloat(start: Int, end: Int): Float  = bytes.subList(start, end).toLittleEndianFloat()
    fun toUInt(start: Int, end: Int): UInt = bytes.subList(start, end).toLittleEndianUInt()

    private fun replaceSection(start: Int, end: Int, newBytes: List<Byte>): List<Byte> {
        return bytes.subList(0, start).plus(newBytes).plus(bytes.subList(end, bytes.size))
    }

    fun with(start: Int, end: Int, value: Any): List<Byte> {
        return replaceSection(start, end,
                when(value) {
                    is UShort -> value.toLittleEndianByteList()
                    is UInt -> value.toLittleEndianByteList()
                    is Float -> value.toLittleEndianByteList()
                    is Int -> value.toLittleEndianByteList()
                    else -> throw IllegalArgumentException("Unknown type ${value.javaClass.simpleName} set in ${javaClass.simpleName} of type $type")
                })
    }

}


/** Used for subs which are covered by other subs, like KSIZ / KWDA **/
class NonRenderingSub(override val type : String) : Subrecord {
    override fun asDebug(): String = "${javaClass.simpleName} : type = $type"

    override val containsFormIds = false
    override val formIds: List<FormId> = listOf()


    override fun renderTo(ignoredMasters: MastersWithOrigin, ignoredCr: ConsistencyRecorder, ignoredRenderer: (ByteArray) -> Unit) {
        // Deliberately does nothing.
    }
}