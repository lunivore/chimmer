package com.lunivore.chimmer.binary

import com.lunivore.chimmer.FormId

@UseExperimental(ExperimentalUnsignedTypes::class)
interface RecordWrapper<T : RecordWrapper<T>> {

    fun copyAsNew(): T

    val formId: FormId
    val record: Record
    val loadingMod: String?
}
