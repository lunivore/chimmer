package com.lunivore.chimmer.binary

/**
 * The result of parsing binary structures from a list of bytes. Contains the binary structure(s)
 * successfully parsed, and the bytes left over from those provided.
 */
data class ParseResult<T>(val parsed: T, val rest: List<Byte>, val errorMessage: String? = null) {
    val succeeded = errorMessage.isNullOrEmpty()
    val failed = !succeeded
}