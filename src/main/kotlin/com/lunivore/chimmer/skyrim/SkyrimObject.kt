package com.lunivore.chimmer.skyrim

import com.lunivore.chimmer.ExistingFormId
import com.lunivore.chimmer.FormId
import com.lunivore.chimmer.NewFormId
import com.lunivore.chimmer.binary.*
import com.lunivore.chimmer.helpers.EditorId
import com.lunivore.chimmer.helpers.OriginMod


data class StructLookup(val sub : String, val start: Int, val end: Int)

@UseExperimental(ExperimentalUnsignedTypes::class)
abstract class SkyrimObject<T : RecordWrapper<T>>(override val record: Record) : RecordWrapper<T> {

    override fun copyAsNew(newMaster : String, editorId : String): T {
        return create(record.copyAsNew(OriginMod(newMaster), EditorId(editorId)))
    }

    protected abstract fun create(record: Record): T
    protected fun structSubToUShort(lookup: StructLookup) = (record.find(lookup.sub) as StructSub).toUShort(lookup.start, lookup.end)
    protected fun structSubToUInt(lookup: StructLookup) = (record.find(lookup.sub) as StructSub).toUInt(lookup.start, lookup.end)
    protected fun structSubToInt(lookup: StructLookup) = (record.find(lookup.sub) as StructSub).toInt(lookup.start, lookup.end)
    protected fun structSubToFloat(lookup: StructLookup) = (record.find(lookup.sub) as StructSub).toFloat(lookup.start, lookup.end)
    protected fun formIdSubToFormId(subrecordType: String) = (record.find(subrecordType) as FormIdSub?)?.asFormId()
    protected fun byteSubToString(subrecordType: String) = (record.find(subrecordType) as ByteSub?)?.asString() ?: ""


    protected fun withStructPart(lookup: StructLookup, value: Any): T {
        val data = (record.find(lookup.sub) as StructSub).with(lookup.start, lookup.end, value)
        return create(record.with(StructSub.create(lookup.sub, data)))
    }

    protected fun withFormId(subrecordType: String, formId: FormId) = create(record.with(FormIdSub.create(subrecordType, formId)))
    fun delete(): T {
        return create(record.copy(flags = record.flags.or(Record.Companion.HeaderFlags.DELETED.flag), lazySubrecords = listOf(), recordBytes = listOf()))
    }

    override val formId
        get() = record.formId

    override val originMod: String
        get() = if (record.isNew()) (record.formId as NewFormId).originMod
                else (record.formId as ExistingFormId).originMod
}
