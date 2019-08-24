package com.lunivore.chimmer.helpers

import com.lunivore.chimmer.Logging
import com.lunivore.chimmer.binary.toLittleEndianBytes
import com.lunivore.chimmer.binary.toReadableHexString


/**
 * Tiny Types are small wrappers around values which would otherwise have the same types. It makes it much,
 * much easier to see where we're using the wrong type somewhere.
 *
 * This helps prevent us mixing up things like Unindexed and Indexed form ids,
 * masterlists vs. masterlists with the origin at the end, master mods and origin mods, etc.
 *
 * https://darrenhobbs.com/2007/04/11/tiny-types/
 */

data class OriginMod(val value: String)
data class Master(val value: String)
data class EditorId(val value: String)

data class UnindexedFormId(val value: UInt) {
    fun toReadableHexString(): String = value.toLittleEndianBytes().toReadableHexString()
}

data class IndexedFormId(val value: UInt)

data class Masters(val value: List<String>) {
    companion object { val NONE = Masters(listOf() )}
}
data class LoadOrder(val value: List<String>)

data class MastersWithOrigin(val origin: String, val masters: List<String>) {

    companion object { val logger by Logging() }

    init {
        if (masters.contains(origin)) logger.warn("Masterlist $masters contains origin $origin")
    }

    fun containsAll(candidates: List<String>): Boolean {
        return masters.plus(origin).containsAll(candidates)
    }

    val fullList: List<String> = masters.plus(origin)
}