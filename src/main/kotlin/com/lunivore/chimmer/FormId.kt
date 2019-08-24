package com.lunivore.chimmer

import com.lunivore.chimmer.binary.toBigEndianHexString
import com.lunivore.chimmer.binary.toLittleEndianByteList
import com.lunivore.chimmer.binary.toLittleEndianBytes
import com.lunivore.chimmer.binary.toReadableHexString
import com.lunivore.chimmer.helpers.*

interface FormId {

    data class Key(val master: String, val unindexed: UInt)

    val originMod: String
    val key: Key
    val master: String

    companion object {

        val NULL_REFERENCE: FormId = ExistingFormId(OriginMod("Skyrim.esm"), Master("Skyrim.esm"), UnindexedFormId(0u))

        val TES4: FormId = Tes4HeaderFormId("Skyrim.esm")

        fun createNew(originModWrapper: OriginMod, editorIdWrapper: EditorId): FormId {
            return NewFormId(originModWrapper, editorIdWrapper)
        }
    }

    fun isNew(): Boolean
    fun asDebug(): String
    fun toBytes(mastersWithOrigin: MastersWithOrigin, ignoredCr: ConsistencyRecorder): List<Byte>
}

@ExperimentalUnsignedTypes
data class ExistingFormId(private val originModWrapper: OriginMod, private val masterWrapper: Master, private val unindexedWrapper: UnindexedFormId) : FormId {

    override val originMod: String
        get() = originModWrapper.value
    override val key: FormId.Key
        get() = FormId.Key(master, unindexed)

    override fun toBytes(mastersWithOrigin: MastersWithOrigin, ignoredCr: ConsistencyRecorder): List<Byte> {

        var index = mastersWithOrigin.masters.indexOf(master)

        if (index == -1) {
            if (master == mastersWithOrigin.origin) index = mastersWithOrigin.masters.size else
                throw IllegalArgumentException("""
                    Could not find master when reindexing FormId=${asDebug()},
                    masterlist=${mastersWithOrigin.masters}""".trimIndent())
        }

        return index.toUInt().shl(24).or(unindexed).toLittleEndianByteList()
    }

    override fun asDebug(): String = "${this::class.simpleName} : ${toBigEndianHexString()}"

    companion object {
        private val I_DAYS_TO_RESPAWN_VENDOR_GMST = IndexedFormId(0x0123C00Eu)

        fun create(mastersWithOrigin: MastersWithOrigin, formId: IndexedFormId) : FormId {
            val index = formId.value.and(0xff000000u).shr(24).toInt()
            val master =
                    if (index == mastersWithOrigin.masters.size) mastersWithOrigin.origin
                    else if (index < mastersWithOrigin.masters.size) mastersWithOrigin.masters[index]
                    else if (isTheHorribleIDaysToRespawnVendorGMST(mastersWithOrigin, formId))  "Skyrim.esm"
                    else throw IllegalArgumentException("""
                        Could not find master in FormId ${formId.value.toLittleEndianBytes().toReadableHexString()};
                         mastersWithOrigin was $mastersWithOrigin
                        """.trimIndent())
            return ExistingFormId(OriginMod(mastersWithOrigin.origin), Master(master), UnindexedFormId(formId.value.and(0x00ffffffu)))
        }

        private fun isTheHorribleIDaysToRespawnVendorGMST(mastersWithOrigin: MastersWithOrigin, indexedFormId: IndexedFormId) =
                (mastersWithOrigin.origin == "Skyrim.esm" && indexedFormId == I_DAYS_TO_RESPAWN_VENDOR_GMST)
    }

    override val master: String
        get() = masterWrapper.value

    val unindexed: UInt
        get() = unindexedWrapper.value

    override fun isNew() = false

    private fun toBigEndianHexString(): String = unindexed.toBigEndianHexString()
}

data class NewFormId(val originModWrapper: OriginMod, val editorIdWrapper : EditorId) : FormId {

    companion object {
        val logger by Logging()
    }

    val editorId = editorIdWrapper.value

    override val originMod: String
        get() = originModWrapper.value


    override val key: FormId.Key
            get() = throw IllegalStateException("Attempt to get key for comparison or sorting of new FormId - don't merge new mods before they're saved!")

    override fun toBytes(orderedMastersIncludingOrigin: MastersWithOrigin, consistencyRecorder: ConsistencyRecorder): List<Byte> {

        if (orderedMastersIncludingOrigin.origin != originMod) { logger.warn("Origin mod does not match ${orderedMastersIncludingOrigin.origin} for ${asDebug()}") }

        val indexOfOrigin = if (orderedMastersIncludingOrigin.origin == originMod) orderedMastersIncludingOrigin.masters.size else orderedMastersIncludingOrigin.fullList.indexOf(originMod)
        if (indexOfOrigin == -1) { throw IllegalArgumentException("Could not find origin $originMod in value $orderedMastersIncludingOrigin; ${asDebug()}") }
        return consistencyRecorder(editorIdWrapper).value.or(indexOfOrigin.toUInt().shl(24)).toLittleEndianByteList()
    }

    override fun asDebug(): String = "${this::class.simpleName}{originMod = $originMod, editorId = $editorId}"

    override fun isNew(): Boolean = true
    override val master: String = originMod
}

class Tes4HeaderFormId(override val originMod : String) : FormId {

    override val key: FormId.Key
            get() = throw IllegalStateException("Should never need the FormId of a TES4 Header record as it's always empty")

    override fun toBytes(ignoredMastersWithOrigin: MastersWithOrigin, ignoredCr: ConsistencyRecorder): List<Byte> {
        return 0x00000000u.toLittleEndianByteList()
    }

    override fun asDebug(): String = "${this::class.simpleName}"

    override fun isNew(): Boolean = throw IllegalArgumentException("Should never need to check the FormId on a TES4Header!")

    override val master: String
        get() = throw IllegalArgumentException("Should never need to check the FormId on a TES4Header!")
}
