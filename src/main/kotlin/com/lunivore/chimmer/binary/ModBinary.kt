package com.lunivore.chimmer.binary


data class ModBinary(val header: Record, val grups: List<Grup>) : List<Grup> by grups {

    companion object {
        fun parse(bytes: ByteArray): ModBinary {

            val result = Record.parseTes4(bytes.toList())
            val headerRecord = result.parsed
            val grups = Grup.parseAll(result.rest, headerRecord.masters)

            return ModBinary(headerRecord, grups)
        }

    }

    fun renderTo(renderer: (ByteArray) -> Unit) {
        header.renderTo(renderer)
        grups.forEach { it.renderTo(renderer) }
    }
}
