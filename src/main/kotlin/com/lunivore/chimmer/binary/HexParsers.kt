package com.lunivore.chimmer.binary

fun List<Byte>.toLittleEndianInt(): Int {
    return this.foldIndexed(0) { i, acc, b -> acc or (b.toInt() and 0xFF shl (8 * i)) }
}

fun Short.toLittleEndianBytes(): ByteArray {
    val bytes = ByteArray(2)

    bytes[0] = this.toByte()
    bytes[1] = this.toInt().ushr(8).toByte()

    return bytes
}

fun Int.toLittleEndianBytes(): ByteArray {
    val bytes = ByteArray(4)
    for (i in 0..3) {
        bytes[i] = this.ushr(i * 8).toByte()
    }
    return bytes
}
