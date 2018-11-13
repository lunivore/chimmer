package com.lunivore.chimmer.binary

import javax.xml.bind.DatatypeConverter


/** Use for anything which isn't counting or sizing things, like FormIds and flags. **/
@ExperimentalUnsignedTypes
fun List<Byte>.toLittleEndianUInt(): UInt {
    return this.foldIndexed(0u) { i, acc, b -> acc or (b.toUInt() and 0xFFu shl (8 * i)) }
}

/** Use for small ids like versions. */
@ExperimentalUnsignedTypes
fun List<Byte>.toLittleEndianUShort(): UShort {
    return this.toLittleEndianUInt().toUShort()
}

/** Use for sizes and counts. */
fun List<Byte>.toLittleEndianInt(): Int {
    return this.foldIndexed(0) { i, acc, b -> acc or (b.toInt() and 0xFF shl (8 * i)) }
}

/** Used for small sizes and counts, like the length of Subrecords */
@ExperimentalUnsignedTypes
fun UShort.toLittleEndianBytes(): ByteArray {
    val bytes = ByteArray(2)

    bytes[0] = this.toByte()
    bytes[1] = this.toInt().shr(8).toByte()

    return bytes
}

/** Used for things like the Skyrim version, which is 1.7 */
@UseExperimental(ExperimentalUnsignedTypes::class)
fun List<Byte>.toLittleEndianFloat(): Float {
    return Float.fromBits(this.toLittleEndianUInt().toInt())
}

fun Short.toLittleEndianBytes(): ByteArray {
    val bytes = ByteArray(2)

    bytes[0] = this.toByte()
    bytes[1] = this.toInt().ushr(8).toByte()

    return bytes
}

@ExperimentalUnsignedTypes
fun UInt.toLittleEndianBytes(): ByteArray {
    val bytes = ByteArray(4)
    for (i in 0..3) {
        bytes[i] = this.shr(i * 8).toByte()
    }
    return bytes
}

@ExperimentalUnsignedTypes
fun Int.toLittleEndianBytes(): ByteArray {
    val bytes = ByteArray(4)
    for (i in 0..3) {
        bytes[i] = this.ushr(i * 8).toByte()
    }
    return bytes
}

@UseExperimental(ExperimentalUnsignedTypes::class)
fun Float.toLittleEndianBytes(): ByteArray {
    return this.toBits().toUInt().toLittleEndianBytes()
}

fun String.toByteList(): List<Byte> {
    return this.toByteArray().toList()
}

fun String.fromHexStringToByteList(): List<Byte> {
    return DatatypeConverter.parseHexBinary(this.replace(" ", "")).toList()
}