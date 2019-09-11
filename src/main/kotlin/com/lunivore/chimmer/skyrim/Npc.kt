package com.lunivore.chimmer.skyrim

import com.lunivore.chimmer.binary.ByteSub
import com.lunivore.chimmer.binary.Record

data class Npc(override val record: Record) : SkyrimObject<Npc>(record) {
    val editorId: String get() = (record.find("EDID") as ByteSub?)?.asString() ?: ""

    override fun create(record: Record): Npc {
        return Npc(record)
    }

}
