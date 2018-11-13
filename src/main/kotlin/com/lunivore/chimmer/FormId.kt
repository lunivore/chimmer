package com.lunivore.chimmer.skyrim

@ExperimentalUnsignedTypes
data class FormId(val raw: UInt, val masters: List<String>) {

    companion object {
        fun createNew(masters: List<String>): FormId {
            return FormId(UNINDEXED_FORM_ID_FOR_NEW_RECORD, masters)
        }

        // Note that this is never used in mods; FF is reserved for things created in-game so is safe to use here
        // as a temporary measure.
        private val UNINDEXED_FORM_ID_FOR_NEW_RECORD : UInt = 0xffffffffu
    }

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

    val unindexed: UInt
        get() = raw and 0x00ffffffu

    private fun findMaster(): String? {
        val index = raw.and(0xff000000u).shr(24).toInt()
        return if (index < masters.size) masters[index] else if (index == masters.size || index == 0xff) null else
            throw IndexOutOfBoundsException("The master index $index used in form Id ${toBigEndianHexString()} is too big for the master list: ${masters}")
    }

    fun isNew() = raw == UNINDEXED_FORM_ID_FOR_NEW_RECORD

    fun toBigEndianHexString(): String = raw.toString(16).padStart(8, '0').toUpperCase()
}
