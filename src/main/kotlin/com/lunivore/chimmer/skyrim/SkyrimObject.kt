package com.lunivore.chimmer.skyrim

import com.lunivore.chimmer.ExistingFormId
import com.lunivore.chimmer.NewFormId
import com.lunivore.chimmer.binary.Record
import com.lunivore.chimmer.binary.RecordWrapper

@UseExperimental(ExperimentalUnsignedTypes::class)
abstract class SkyrimObject<T : RecordWrapper<T>>(override val record: Record) : RecordWrapper<T> {

    override fun copyAsNew(newMaster : String, editorId : String): T {
        return create(record.copyAsNew(newMaster, editorId))
    }

    protected abstract fun create(record: Record): T

    override val formId
        get() = record.formId

    override val loadingMod: String
        get() = if (record.isNew()) (record.formId as NewFormId).originatingMod
                else (record.formId as ExistingFormId).originMod
}
