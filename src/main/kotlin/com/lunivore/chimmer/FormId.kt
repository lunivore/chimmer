package com.lunivore.chimmer

import com.lunivore.chimmer.binary.toLittleEndianBytes

typealias UnindexedFormId = UInt



interface FormId {

    data class  Key(val master: String?, val unindexed: UnindexedFormId)

    val key: Key
    val master: String

    companion object {

        val TES4: FormId = Tes4HeaderFormId()
        // Note that this is never used in mods; FF is reserved for things created in-game so is safe to use here
        // as a temporary measure.
        val UNINDEXED_FORM_ID_FOR_NEW_RECORD = 0xffffffffu

        fun create(loadingMod: String, raw: UInt, masters: List<String>) : FormId {
            return ExistingFormId(loadingMod, raw, masters)
        }

        fun createNew(originMod: String, editorId: String): FormId {
            return NewFormId(originMod, editorId)
        }
    }

    fun isNew(): Boolean
    fun asDebug(): String
    fun reindexedFor(orderedMasters: List<String>): List<Byte>
}

@ExperimentalUnsignedTypes
data class ExistingFormId(val originMod: String, private val raw: UInt, val masters: List<String>) : FormId {
    override val key: FormId.Key = FormId.Key(master, unindexed)

    override fun reindexedFor(newMasters: List<String>): List<Byte> {
        // TODO: Have tried to change this as little as possible because it's well past time to check this in,
        // but this can be massively simplified once we create the FormIdSub.

        val mastersToUse = if (newMasters.contains(master)) newMasters else newMasters.plus(master)
        val newIndex = mastersToUse.indexOf(master)

        if (newIndex == -1) throw IllegalStateException("""
            Could not find master when reindexing FormId=${toBigEndianHexString()},
            masterlist=$masters,
            newMasterlist=$newMasters,
            originMod=$originMod,
            master=$master""".trimIndent())

        // TODO: Replace this throughout with one method
        return newIndex.toUInt().shl(24).or(unindexed).toLittleEndianBytes().toList()
    }

    override fun asDebug(): String = "${this::class.simpleName} : ${toBigEndianHexString()}"

    companion object {
        private val I_DAYS_TO_RESPAWN_VENDOR_GMST = 0x0123C00Eu
        val TES4_FORMID = 0u
    }

    init {
        val index = findIndex()
        if (index > masters.size && !isTheHorribleIDaysToRespawnVendorGMST()) {
            throw IllegalArgumentException("FormId ${toBigEndianHexString()} from mod ${originMod} has an index too large for the masterlist: ${masters}")
        }
    }

    private fun isTheHorribleIDaysToRespawnVendorGMST() =
            raw == I_DAYS_TO_RESPAWN_VENDOR_GMST && originMod == "Skyrim.esm"

    override val master: String
        get() {
            val index = findIndex()
            return if (index < masters.size) masters[index]
                    else if (index == masters.size || !isTheHorribleIDaysToRespawnVendorGMST()) originMod
                    else if (isTheHorribleIDaysToRespawnVendorGMST()) "Skyrim.esm" else
                    throw IllegalStateException("This should never happen as non-new forms are checked for master on construction: FormId=${toBigEndianHexString()}")
        }

    val unindexed: UnindexedFormId
        get() = raw and 0x00ffffffu

    private fun findIndex() : Int = raw.and(0xff000000u).shr(24).toInt()


    override fun isNew() = false

    fun toBigEndianHexString(): String = raw.toString(16).padStart(8, '0').toUpperCase()
}

data class NewFormId(val originatingMod: String, val editorId : String) : FormId {
    override val key: FormId.Key
            get() = throw IllegalStateException("Attempt to get key for comparison or sorting of new FormId - don't merge new mods before they're saved!")

    override fun reindexedFor(orderedMasters: List<String>): List<Byte> {
        TODO("Not implemented for new form ids - $originatingMod - $editorId")
    }

    override fun asDebug(): String = "${this::class.simpleName} : $originatingMod : $editorId"

    override fun isNew(): Boolean = true
    override val master: String = originatingMod
}

class Tes4HeaderFormId() : FormId {

    override val key: FormId.Key
            get() = throw IllegalStateException("Should never need the FormId of a TES4 Header record as it's always empty")

    override fun reindexedFor(orderedMasters: List<String>): List<Byte> {
        return 0x00000000u.toLittleEndianBytes().toList()
    }

    override fun asDebug(): String = "${this::class.simpleName}"

    override fun isNew(): Boolean = throw IllegalArgumentException("Should never need to check the FormId on a TES4Header!")

    override val master: String
        get() = throw IllegalArgumentException("Should never need to check the FormId on a TES4Header!")
}
