package com.lunivore.chimmer.skyrim

import com.lunivore.chimmer.FormId
import com.lunivore.chimmer.binary.ByteSub
import com.lunivore.chimmer.binary.Record
import com.lunivore.chimmer.binary.toLittleEndianByteList
import com.lunivore.chimmer.helpers.EditorId
import com.lunivore.chimmer.helpers.Masters
import com.lunivore.chimmer.helpers.OriginMod

class Keyword(record : Record) : SkyrimObject<Keyword>(record) {

    companion object {

        fun create(originMod: String, editorId: String, color: UInt) : Keyword {
            return Keyword(Record.createNew("KYWD", 0u,
                    FormId.createNew(OriginMod(originMod), EditorId(editorId)),
                    43u.toUShort(),
                    listOf(ByteSub.create("EDID", (editorId + "\\0").toByteArray().toList()),
                            ByteSub.create("CNAM", 0u.toUShort().toLittleEndianByteList())),
                    Masters(listOf())))
        }

    }

    override fun create(record: Record): Keyword {
        return Keyword(record)
    }



}
