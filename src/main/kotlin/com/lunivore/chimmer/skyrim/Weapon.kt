package com.lunivore.chimmer.skyrim

import com.lunivore.chimmer.binary.Record

data class Weapon(override val record: Record) : SkyrimObject<Weapon>(record) {

    override fun create(record: Record): Weapon {
        return Weapon(record)
    }
}
