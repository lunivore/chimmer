package com.lunivore.chimmer

import com.lunivore.chimmer.binary.fromHexStringToByteList
import com.lunivore.chimmer.binary.toLittleEndianUInt
import com.lunivore.chimmer.helpers.EditorId
import com.lunivore.chimmer.helpers.OriginMod
import com.lunivore.chimmer.helpers.UnindexedFormId
import java.io.File

typealias NamingConvention = (OriginMod) -> File
typealias ConsistencyRecorder = (EditorId) -> UnindexedFormId
typealias MapForMod = MutableMap<EditorId, UnindexedFormId>

@UseExperimental(ExperimentalUnsignedTypes::class)
class ConsistencyFileHandler(private val workingDirectory : File,
                             private val namingConvention: NamingConvention
                                = { File(workingDirectory, "chimmer/${it}_consistency.txt") }) {

    private val mapsForMods : MutableMap<OriginMod, MapForMod> = mutableMapOf()

    // TODO: This needs to be a constant, and then the dynamic one needs to exist per mod.
    // But we need to do a bunch of work around the last used record anyway.
    private var lastUsedId: UInt = 2047u

    private fun findOrCreate(originMod : OriginMod, editorId: EditorId): UnindexedFormId {
        val mapForMod = mapsForMods[originMod] ?: mutableMapOf()
        mapsForMods[originMod] = mapForMod

        val unindexedFormId = mapForMod[editorId]
        return if (unindexedFormId == null) {
            val nextId = UnindexedFormId(lastUsedId + 1u)
            mapForMod[editorId] = nextId
            lastUsedId = nextId.value
            nextId }
        else {
            unindexedFormId
        }
    }

    fun recorderFor(modFilename : OriginMod): ConsistencyRecorder {
        val mapForMod = mapsForMods[modFilename] ?: mutableMapOf()
        mapsForMods[modFilename] = mapForMod

        val file = namingConvention(modFilename)
        if (file.exists()) {
            val pairLines = file.readLines()
            val splitLines: List<Pair<EditorId, UnindexedFormId>> = pairLines.map {
                it.split(":")
            }.map { Pair(EditorId(it[0]), UnindexedFormId(it[1].fromHexStringToByteList().toLittleEndianUInt())) } // TODO: Kotlin's added radix stuff now; may be a better way
            mapForMod.putAll(splitLines)
            lastUsedId = splitLines.lastOrNull()?.second?.value ?: 2047u
        }
        return { findOrCreate(modFilename, it) }
    }

    fun saveConsistency(originMod: OriginMod) {
        val file = namingConvention(originMod)
        if (!file.parentFile.exists()) { file.parentFile.mkdirs() }
        file.createNewFile()

        val mapForMod = mapsForMods[originMod] ?: mutableMapOf()
        mapForMod.forEach {
            val unindexedFormIdAsString = it.value.value.toString(16).padStart(6, '0')
            file.appendText("${it.key.value}:${unindexedFormIdAsString}")
            file.appendText(System.lineSeparator())
        }
    }

}
