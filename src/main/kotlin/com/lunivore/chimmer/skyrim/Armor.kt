package com.lunivore.chimmer.skyrim

import com.lunivore.chimmer.binary.Record

@UseExperimental(ExperimentalUnsignedTypes::class)
data class Armor(override val record: Record) : SkyrimObject<Armor>(record) {
    override fun create(newRecord: Record): Armor {
        return Armor(newRecord)
    }
}
