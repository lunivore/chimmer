package com.lunivore.chimmer.binary

import com.lunivore.chimmer.FormId

interface RecordWrapper<T> {
    fun copyAsNew(master : String): T = copy(record.copyAsNew(master))

    fun copy(newRecord: Record): T

    fun formId(): FormId = record.formId

    val record: Record
}
