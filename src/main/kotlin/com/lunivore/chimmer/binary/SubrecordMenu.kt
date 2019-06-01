package com.lunivore.chimmer.binary

typealias SubrecordProvider = (List<Byte>) -> Subrecord

interface SubrecordMenu {
    fun findProvider(recordType: String, subrecordType: String): SubrecordProvider

}

class SkyrimSubrecordMenu : SubrecordMenu {
    override fun findProvider(recordType: String, subrecordType: String): SubrecordProvider  = { bytes ->
        ByteSub.create(subrecordType, bytes)
    }
}

