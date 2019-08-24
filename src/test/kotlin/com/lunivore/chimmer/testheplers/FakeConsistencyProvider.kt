package com.lunivore.chimmer.testheplers

import com.lunivore.chimmer.helpers.EditorId
import com.lunivore.chimmer.helpers.UnindexedFormId

fun fakeConsistencyRecorder(edid: EditorId): UnindexedFormId {
    return UnindexedFormId(0x00ffffffu)
}