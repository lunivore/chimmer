package com.lunivore.chimmer.testheplers

import com.lunivore.chimmer.EditorId
import com.lunivore.chimmer.UnindexedFormId
import com.lunivore.chimmer.binary.fromHexStringToByteList
import com.lunivore.chimmer.binary.toLittleEndianUInt

fun fakeConsistencyRecorder(edid: EditorId): UnindexedFormId {
    return "FFFFFF".fromHexStringToByteList().toLittleEndianUInt()
}