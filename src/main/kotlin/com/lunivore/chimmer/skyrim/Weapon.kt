package com.lunivore.chimmer.skyrim

import com.lunivore.chimmer.binary.Record
import com.lunivore.chimmer.binary.RecordWrapper

data class Weapon(override val record: Record) : RecordWrapper<Weapon> {

    val name: String = record.find("CNAM")?.asString() ?: ""

}
