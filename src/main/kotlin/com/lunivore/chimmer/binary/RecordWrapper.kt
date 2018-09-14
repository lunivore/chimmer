package com.lunivore.chimmer.binary

import com.lunivore.chimmer.skyrim.FormId

interface RecordWrapper<T> {
    fun copyAsNew(): T = copy(record.copyAsNew())

    fun copy(newRecord: Record): T

    fun formId(): FormId = FormId(record.formId, record.masters)

    val record: Record
}
