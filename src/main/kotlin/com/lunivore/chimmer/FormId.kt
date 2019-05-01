package com.lunivore.chimmer

import com.lunivore.chimmer.binary.toLittleEndianBytes

typealias UnindexedFormId = UInt

@ExperimentalUnsignedTypes
data class FormId(val loadingMod: String?, val raw: UInt, val masters: List<String>) {

    companion object {

        fun createNew(masters: List<String>): FormId {
            return FormId(null, UNINDEXED_FORM_ID_FOR_NEW_RECORD, masters)
        }

        // Note that this is never used in mods; FF is reserved for things created in-game so is safe to use here
        // as a temporary measure.
        private val UNINDEXED_FORM_ID_FOR_NEW_RECORD = 0xffffffffu
        private val INDEX_OF_NEW_RECORD = 0xff
        private val TES4_FORMID = 0u
        private val I_DAYS_TO_RESPAWN_VENDOR_GMST = 0x0123C00Eu
    }

    init {
        val index = findIndex()
        if (index != INDEX_OF_NEW_RECORD && index > masters.size && !isTheHorribleIDaysToRespawnVendorGMST()
                || (index == masters.size && loadingMod == null && raw != TES4_FORMID)) {
            throw IllegalArgumentException("FormId ${toBigEndianHexString()} has an index too large for the masterlist: ${masters}")
        }
    }

    private fun isTheHorribleIDaysToRespawnVendorGMST() =
            raw == I_DAYS_TO_RESPAWN_VENDOR_GMST && loadingMod == "Skyrim.esm"

    /**
     * Returns the master for this object, or null if the object belongs to the current mod
     * (for which the FormId index will be 1 more than the last master).
     * For example, if masters are "Skyrim.esm" and "Dawnguard.esm" then an index of 00 will be Skyrim,
     * an index of 01 will be Dawnguard and an index of 02 returns no master, indicating that this mod is the
     * master itself.
     *
     * Any other indexes will result in an exception.
     */
    val master
        get() = findMaster()

    private fun findIndex() : Int = raw.and(0xff000000u).shr(24).toInt()

    data class  Key(val master: String?, val unindexed: UnindexedFormId)
    val key
        get() = Key(findMaster(), unindexed)

    val unindexed: UnindexedFormId
        get() = raw and 0x00ffffffu

    val rawBytes: ByteArray
        get() = raw.toLittleEndianBytes()
    val rawByteList: List<Byte>
        get() = rawBytes.toList()

    private fun findMaster(): String? {
        val index = findIndex()
        return if (index == 0xff) null
            else if (index < masters.size) masters[index]
            else if (index == masters.size || isTheHorribleIDaysToRespawnVendorGMST()) loadingMod else
            throw IllegalStateException("This should never happen as non-new forms are checked for master on construction: FormId=${toBigEndianHexString()}")
    }

    fun isNew() = raw == UNINDEXED_FORM_ID_FOR_NEW_RECORD

    fun reindex(newMasters: List<String>): FormId {
        if (this.isNew()) return this

        if (master == null) throw IllegalStateException("This should never happen as non-new forms are checked for master on construction: FormId=${toBigEndianHexString()}")

        val mastersToUse = if (newMasters.contains(master!!)) newMasters else newMasters.plus(master!!)
        val newIndex = mastersToUse.indexOf(master!!)

        if (newIndex == -1) throw IllegalStateException("""
            Could not find master when reindexing FormId=${toBigEndianHexString()},
            masterlist=$masters,
            newMasterlist=$newMasters,
            loadingMod=$loadingMod,
            master=$master""".trimIndent())

        return FormId(null, newIndex.toUInt().shl(24).or(unindexed), mastersToUse)
    }

    /**
     * Returns the original hex string with which the form id was created, independent of its master's load order.
     */
    fun toBigEndianHexString(): String = raw.toString(16).padStart(8, '0').toUpperCase()
}

@UseExperimental(ExperimentalUnsignedTypes::class)
class FormIdKeyComparator(private val loadOrder: List<String>) : Comparator<FormId.Key>{
    override fun compare(o1: FormId.Key?, o2: FormId.Key?): Int {
        if(o1 == null || o2 == null) throw IllegalArgumentException("This should never happen as FormIds are never null")

        val o1Index = loadOrder.indexOf(o1.master)
        val o2Index = loadOrder.indexOf(o2.master)

        if (o1Index == -1  || o2Index == -1)
            throw IllegalArgumentException("Error comparing FormIds; o1.master=${o1.master}, o2.master=${o2.master}, loadOrder=$loadOrder")

        return if (o1Index == o2Index) o1.unindexed.compareTo(o2.unindexed) else o1Index.compareTo(o2Index)
    }
}