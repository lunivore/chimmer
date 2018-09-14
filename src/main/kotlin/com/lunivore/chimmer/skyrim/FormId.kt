package com.lunivore.chimmer.skyrim

import com.lunivore.chimmer.binary.toLittleEndianBytes

data class FormId(val formId: Int, val masters: List<String>) {
    fun asReadableString(): String {
        return String(formId.toLittleEndianBytes())
    }
}
