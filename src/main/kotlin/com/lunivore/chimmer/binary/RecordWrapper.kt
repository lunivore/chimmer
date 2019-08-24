package com.lunivore.chimmer.binary

import com.lunivore.chimmer.FormId

@UseExperimental(ExperimentalUnsignedTypes::class)
interface RecordWrapper<T : RecordWrapper<T>> {

    fun copyAsNew(newMaster : String, editorId : String): T
    val formId: FormId
    val record: Record
    val originMod: String?
}
