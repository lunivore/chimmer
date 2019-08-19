package com.lunivore.chimmer

import com.lunivore.chimmer.binary.fromHexStringToByteList
import com.lunivore.chimmer.binary.toLittleEndianUInt
import java.io.File

typealias ModFilename = String
typealias NamingConvention = (ModFilename) -> File
typealias EditorId = String
typealias ConsistencyRecorder = (EditorId) -> UnindexedFormId
typealias MapForMod = MutableMap<EditorId, UnindexedFormId>

@UseExperimental(ExperimentalUnsignedTypes::class)
class ConsistencyFileHandler(private val workingDirectory : File,
                             private val namingConvention: NamingConvention
                                = { File(workingDirectory, "chimmer/${it}_consistency.txt") }) {

    private val mapsForMods : MutableMap<ModFilename, MapForMod> = mutableMapOf()

    // TODO: This needs to be a constant, and then the dynamic one needs to exist per mod.
    // But we need to do a bunch of work around the last used record anyway.
    private var lastUsedId: UInt = 2047u

    private fun findOrCreate(modFilename : ModFilename, editorId: EditorId): UnindexedFormId {
        val mapForMod = mapsForMods[modFilename] ?: mutableMapOf()
        mapsForMods[modFilename] = mapForMod

        val formId = mapForMod[editorId]
        return if (formId == null) {
            val nextId = lastUsedId + 1u
            mapForMod[editorId] = nextId
            lastUsedId = nextId

            nextId }
        else {
            formId
        }
    }

    fun recorderFor(modFilename : ModFilename): ConsistencyRecorder {
        val mapForMod = mapsForMods[modFilename] ?: mutableMapOf()
        mapsForMods[modFilename] = mapForMod

        val file = namingConvention(modFilename)
        if (file.exists()) {
            val pairLines = file.readLines()
            val splitLines: List<Pair<String, UInt>> = pairLines.map {
                it.split(":")
            }.map { Pair(it[0], it[1].fromHexStringToByteList().toLittleEndianUInt() ) } // TODO: Kotlin's added radix stuff now; may be a better way
            mapForMod.putAll(splitLines)
            lastUsedId = splitLines.lastOrNull()?.second ?: 2047u
        }
        return { findOrCreate(modFilename, it) }
    }

    fun saveConsistency(modFilename: ModFilename) {
        val file = namingConvention(modFilename)
        if (!file.parentFile.exists()) { file.parentFile.mkdirs() }
        file.createNewFile()

        val mapForMod = mapsForMods[modFilename] ?: mutableMapOf()
        mapForMod.forEach {
            val unindexedFormIdAsString = it.value.toString(16).padStart(6, '0')
            file.appendText("${it.key}:${unindexedFormIdAsString}")
            file.appendText(System.lineSeparator())
        }
    }

}
