package com.lunivore.chimmer

import com.lunivore.chimmer.binary.fromHexStringToByteList
import com.lunivore.chimmer.binary.toLittleEndianBytes
import com.lunivore.chimmer.binary.toLittleEndianInt
import java.io.File

typealias ModFilename = String
typealias NamingConvention = (ModFilename) -> File
typealias EditorId = String
typealias UnindexedFormId = Int
typealias ConsistencyRecorder = (EditorId) -> UnindexedFormId
typealias MapForMod = MutableMap<EditorId, UnindexedFormId>

class ConsistencyFileHandler(private val workingDirectory : File,
                             private val namingConvention: NamingConvention
                                = { File(workingDirectory, "chimmer/${it}_consistency.txt") }) {

    private val mapsForMods : MutableMap<ModFilename, MapForMod> = mutableMapOf()
    private var lastUsedId: Int = 2047

    private fun findOrCreate(modFilename : ModFilename, editorId: EditorId): UnindexedFormId {
        val mapForMod = mapsForMods[modFilename] ?: mutableMapOf()
        mapsForMods[modFilename] = mapForMod

        val formId = mapForMod[editorId]
        return if (formId == null) {
            val nextId = lastUsedId + 1
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
            val splitLines: List<Pair<String, Int>> = pairLines.map {
                it.split(":")
            }.map { Pair(it[0], it[1].fromHexStringToByteList().toLittleEndianInt() ) }
            mapForMod.putAll(splitLines)
            lastUsedId = splitLines.lastOrNull()?.second ?: 2047
        }
        return { findOrCreate(modFilename, it) }
    }

    fun saveConsistency(modFilename: ModFilename) {
        val file = namingConvention(modFilename)
        if (!file.parentFile.exists()) { file.parentFile.mkdir() }
        file.createNewFile()

        val mapForMod = mapsForMods[modFilename] ?: mutableMapOf()
        mapForMod.forEach {
            file.writeText("${it.key}:${String(it.value.toLittleEndianBytes())} ) ")
            file.writeText(System.lineSeparator())
        }
    }

}
