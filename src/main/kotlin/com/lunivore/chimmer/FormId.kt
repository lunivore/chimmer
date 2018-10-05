package com.lunivore.chimmer

import com.lunivore.chimmer.binary.toLittleEndianBytes
import com.lunivore.chimmer.binary.toLittleEndianInt

data class FormId(val unindexFormId : UnindexedFormId, val master : String)
{
    companion object {
        val TES4: FormId = FormId(0, "")

    }

    constructor(indexedFormId: List<Byte>, masters: List<String>) :
            this(indexedFormId.subList(0, 3).toLittleEndianInt(),
                    masters[indexedFormId[3].toInt()])

    fun toLittleEndianBytes(masterList: List<String>): ByteArray {
        val index = masterList.indexOf(master)
        if (index == -1) throw IllegalArgumentException(
                "Could not find master $master  in the mod list")
        val indexedFormId = unindexFormId + (index shl 24)
        return indexedFormId.toLittleEndianBytes()

    }
}
