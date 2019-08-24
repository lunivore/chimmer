package com.lunivore.chimmer

import com.lunivore.chimmer.helpers.OriginMod
import java.io.File

interface FileHandler {
    val outputFolder: File
    fun findSkyrimFolder(): File
    fun findLoadOrderFile(): File
    fun recorderFor(modName: String): ConsistencyRecorder
    fun saveConsistency(modName: String)
}

data class SensibleDefaultFileHandler(override val outputFolder: File = File("."),
                                      private val consistencyFileHandler: ConsistencyFileHandler = ConsistencyFileHandler(outputFolder),
                                      private val skyrimFinder: SkyrimFinder = SkyrimFinder()) : FileHandler {

    override fun findSkyrimFolder()= skyrimFinder.findSkyrimFolder()
    override fun findLoadOrderFile() = skyrimFinder.findLoadOrderFile()
    override fun recorderFor(modName: String) = consistencyFileHandler.recorderFor(OriginMod(modName))
    override fun saveConsistency(modName: String) = consistencyFileHandler.saveConsistency(OriginMod(modName))
}
