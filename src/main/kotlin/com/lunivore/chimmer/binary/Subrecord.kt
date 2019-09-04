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

                if (rest.size < 6 + length) return ParseResult(listOf(), listOf(), "Failed to parseAll subrecord of type $type") // TODO Ditto

                subrecords.add(menu.findProvider(recordType, type)(mastersWithOrigin, rest.subList(6, 6 + length)))
                rest = if (rest.size <= 6 + length) listOf() else rest.subList(6 + length, rest.size)
            }

            return ParseResult(subrecords, rest)
        }
    }

    fun renderTo(mastersIncludingOrigin: MastersWithOrigin, consistencyRecorder: ConsistencyRecorder, renderer: (ByteArray) -> Unit)

    fun asBytes(): List<Byte>
    fun asString(): String
    fun asFloat(): Float
    fun asFormId(mastersWithOrigin : MastersWithOrigin): FormId?
}

data class ByteSub private constructor(override val type: String, val bytes: List<Byte>) : Subrecord{
    override val formIds: List<FormId> = listOf()
    override val containsFormIds : Boolean = false

    companion object{
        fun create(type: String, bytes: List<Byte>): ByteSub {
            return ByteSub(type, bytes)
        }
    }

    override fun asBytes() = bytes

    override fun renderTo(ignoredMasters: MastersWithOrigin, ignoredCr: ConsistencyRecorder, renderer: (ByteArray) -> Unit)
            = renderTo(renderer)

    fun renderTo(renderer: (ByteArray) -> Unit) {
        renderer(type.toByteArray())
        renderer(bytes.size.toShort().toLittleEndianBytes())
        renderer(bytes.toByteArray())
    }

    override fun asString(): String {
        return String(bytes.toByteArray()).trimEnd('\u0000')
    }

    override fun asFloat(): Float {
        return bytes.subList(0, 4).toLittleEndianFloat()
    }

    override fun asFormId(mastersWithOrigin : MastersWithOrigin): FormId {
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

    override fun asFormId(mastersWithOrigin : MastersWithOrigin): FormId = formId

    override fun asFloat(): Float = throw IllegalArgumentException("FormId ${asDebug()} cannot be read as a float")
    override fun asString(): String = throw IllegalArgumentException("FormId ${asDebug()} cannot be rendered as a string. Use 'asDebug()' if you want info.")
    override fun asBytes() : List<Byte> = throw IllegalArgumentException("FormId ${asDebug()} cannot be rendered as raw bytes without a master list")

    private fun asDebug(): String = "FormIdSub{ type = $type, formId = ${formId.asDebug()}"
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
    override fun asFormId(mastersWithOrigin : MastersWithOrigin): FormId = throw IllegalArgumentException("CrdtSub cannot be read as a FormId. Cast it explicitly to a CrdtSub then get the bits you need.")
    override fun asFloat(): Float = throw IllegalArgumentException("CrdtSub cannot be read as a float")
    override fun asString(): String = throw IllegalArgumentException("CrdtSub cannot be rendered as a string. Use 'asDebug()' if you want more info.")
    override fun asBytes() : List<Byte> = throw IllegalArgumentException("CrdtSub cannot be rendered as raw bytes without a master list")

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

    private fun asDebug(): String = "CrdtSub"
}

@UseExperimental(ExperimentalUnsignedTypes::class)
data class KsizKwdaSub(val keywords : List<FormId>) : Subrecord {
    override val containsFormIds: Boolean = true
    override val formIds: List<FormId> = keywords

    companion object{
        fun create(mastersWithOrigin : MastersWithOrigin, bytes: List<Byte>): KsizKwdaSub {
           return KsizKwdaSub(bytes.chunked(4).map { ExistingFormId.create(mastersWithOrigin, IndexedFormId(it.toLittleEndianUInt())) })
        }
    }
    override fun asFormId(mastersWithOrigin : MastersWithOrigin): FormId = throw IllegalArgumentException("CrdtSub cannot be read as a FormId. Cast it explicitly to a CrdtSub then get the bits you need.")
    override fun asFloat(): Float = throw IllegalArgumentException("CrdtSub cannot be read as a float")
    override fun asString(): String = throw IllegalArgumentException("CrdtSub cannot be rendered as a string. Use 'asDebug()' if you want more info.")
    override fun asBytes() : List<Byte> = throw IllegalArgumentException("CrdtSub cannot be rendered as raw bytes without a master list")

    override val type: String
        get() = "KWDA"

    override fun renderTo(masters: MastersWithOrigin, consistencyRecorder: ConsistencyRecorder, renderer: (ByteArray) -> Unit) {

        val kwdasAsBytes = keywords.map {it.toBytes(masters, consistencyRecorder)}.flatten()
        ByteSub.create("KSIZ", keywords.count().toUInt().toLittleEndianByteList()).renderTo(renderer)
        ByteSub.create("KWDA", kwdasAsBytes).renderTo(renderer)
    }
}


/** Used for subs which are covered by other subs, like KSIZ / KWDA **/
class NonRenderingSub(override val type : String) : Subrecord {
    override val containsFormIds = false
    override val formIds: List<FormId> = listOf()

    override fun asFormId(mastersWithOrigin : MastersWithOrigin): FormId = throw IllegalArgumentException("This $type is an empty sub; which usually means it's being dealt with somewhere else.")
    override fun asFloat(): Float = throw IllegalArgumentException("This $type is an empty sub; which usually means it's being dealt with somewhere else.")
    override fun asString(): String = throw IllegalArgumentException("This $type is an empty sub; which usually means it's being dealt with somewhere else.")
    override fun asBytes() : List<Byte> = throw IllegalArgumentException("This $type is an empty sub; which usually means it's being dealt with somewhere else.")

    override fun renderTo(ignoredMasters: MastersWithOrigin, ignoredCr: ConsistencyRecorder, ignoredRenderer: (ByteArray) -> Unit) {
        // Deliberately does nothing.
    }
}