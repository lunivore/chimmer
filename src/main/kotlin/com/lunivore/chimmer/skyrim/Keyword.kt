package com.lunivore.chimmer.skyrim

import com.lunivore.chimmer.binary.Record

class Keyword(record : Record) : SkyrimObject<Keyword>(record) {

    override fun create(record: Record): Keyword {
        return Keyword(record)
    }


}
