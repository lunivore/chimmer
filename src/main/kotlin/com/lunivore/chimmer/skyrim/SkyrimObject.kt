package com.lunivore.chimmer.skyrim

import com.lunivore.chimmer.binary.Record
import com.lunivore.chimmer.binary.RecordWrapper

@UseExperimental(ExperimentalUnsignedTypes::class)
abstract class SkyrimObject<T : RecordWrapper<T>>(override val record: Record) : RecordWrapper<T> {

    override fun copyAsNew(): T {
        return create(record.copyAsNew())
    }

    protected abstract fun create(record: Record): T

    override val formId
        get() = record.formId

    override val loadingMod: String?
        get() = record.loadingMod
}
