package com.lunivore.chimmer.binary

import com.lunivore.chimmer.helpers.MastersWithOrigin

typealias SubrecordProvider = (MastersWithOrigin, List<Byte>) -> Subrecord

interface SubrecordMenu {
    companion object {
        val UNUSED : SubrecordMenu = UnusedSubrecordMenu()
    }

    fun findProvider(recordType: String, subrecordType: String): SubrecordProvider

}

class UnusedSubrecordMenu : SubrecordMenu {
    override fun findProvider(recordType: String, subrecordType: String): SubrecordProvider {
        throw IllegalArgumentException("This ${this.javaClass.simpleName} is a null object normally used when subrecords have already been provided. This exception should never be thrown.")
    }

}

class SkyrimSubrecordMenu : SubrecordMenu {

    data class MenuKey(val recordType: String, val subrecordType: String)

    companion object {
        val WEAP_SUBS = "BAMT, BIDS, CNAM, EITM, ETYP, INAM, NAM7, NAM8, NAM9, SNAM, TNAM, UNAM, WNAM, XNAM, YNAM, ZNAM"
                .split(", ").map {MenuKey("WEAP", it) to createFormIdSubProvider(it) }
                .plus(MenuKey("WEAP", "CRDT") to createCrdtSubProvider())
                .plus(MenuKey("WEAP", "KSIZ") to createNonRenderingSubProvider("KSIZ"))
                .plus(MenuKey("WEAP", "KWDA") to createKwdaSubProvider())
                .plus("DATA, DNAM".split(", ").map { MenuKey("WEAP", it) to createStructSubProvider(it) })

        val ARMO_SUBS = "EITM, YNAM, ZNAM, ETYP, BIDS, BAMT, RNAM, TNAM"
            .split(", ").map {MenuKey("WEAP", it) to createFormIdSubProvider(it) }

        val PROVIDER_BY_SUBRECORD : Map<MenuKey, SubrecordProvider> = (WEAP_SUBS + ARMO_SUBS).toMap()

        private fun createFormIdSubProvider(subrecordType: String) : SubrecordProvider = { mastersWithOrigin, bytes -> FormIdSub.create(mastersWithOrigin, subrecordType, bytes) }
        private fun createCrdtSubProvider(): SubrecordProvider = { mastersWithOrigin, bytes -> CrdtSub.create(mastersWithOrigin, bytes) }
        private fun createKwdaSubProvider(): SubrecordProvider = { mastersWithOrigin, bytes -> KsizKwdaSub.create(mastersWithOrigin, bytes) }
        private fun createNonRenderingSubProvider(subrecordType: String): SubrecordProvider = { _, _ -> NonRenderingSub(subrecordType)}
        private fun createStructSubProvider(subrecordType: String): SubrecordProvider = {_, bytes -> StructSub(subrecordType, bytes) }
    }


    override fun findProvider(recordType: String, subrecordType: String): SubrecordProvider =
            PROVIDER_BY_SUBRECORD[MenuKey(recordType, subrecordType)]
                    ?: { _, bytes -> ByteSub.create(subrecordType, bytes)}
}

