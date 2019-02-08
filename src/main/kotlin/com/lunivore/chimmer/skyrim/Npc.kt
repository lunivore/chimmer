package com.lunivore.chimmer.skyrim

import com.lunivore.chimmer.binary.Record

data class Npc(override val record: Record) : SkyrimObject<Npc>(record) {
    override fun create(record: Record): Npc {
        return Npc(record)
    }

}
