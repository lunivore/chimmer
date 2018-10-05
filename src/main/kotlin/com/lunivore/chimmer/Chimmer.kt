package com.lunivore.chimmer

import com.lunivore.chimmer.binary.ModBinary
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException

class Chimmer(val outputFolder: File = File("."),
              private val consistencyFileHandler: ConsistencyFileHandler = ConsistencyFileHandler(outputFolder),
              private val loadRealSkyrimFiles : Boolean = true) {

    companion object {
        val REAL_SKYRIM_FILES = "Skyrim.esm, Update.esm, Dawnguard.esm, Dragonborn.esm, Hearthfires.esm"
                .split(",").map { it.trim() }
    }

    private var lastSeenLoadOrder: MutableList<ModFilename> = mutableListOf()

    fun load(modFolder: File, loadOrderFile: File): List<Mod> {
        return load(modFolder, loadOrderFile.readLines())
    }

    fun load(modFolder: File, loadOrder: List<ModFilename>): List<Mod> {
        if (!modFolder.exists()) {
            throw FileNotFoundException("Could not find mod folder '${modFolder.absolutePath}'")
        }
        lastSeenLoadOrder = loadOrder.toMutableList()

        // This enables us to work without loading the extremely big files
        // which ship with Skyrim; also other mod authors who might need to
        // reference them as masters but don't need any of the details in the records.
        val reallyLoadThisLoadOrder = if (loadRealSkyrimFiles) loadOrder
            else loadOrder.filterNot { REAL_SKYRIM_FILES .contains(it)  }

        return reallyLoadThisLoadOrder.map {
            val matchingFiles = modFolder.listFiles { _, name -> name == it }
            if (matchingFiles.isEmpty()) {
                throw FileNotFoundException("Could not find '$it' in folder '${modFolder.absolutePath}'")
            }

            Mod(it, ModBinary.parse(matchingFiles[0].readBytes(),
                    consistencyFileHandler.recorderFor(it)))
        }
    }

    fun save(mod: Mod, loadOrder: List<String> = lastSeenLoadOrder) {

        // TODO We're currently using the whole load order as the masterlist.
        // It should be possible to get each RecordWrapper to identify their formIds
        // - they need to do that anyway to keep themselves consistent. That would easily
        // enable us to create a list of only those masters which are actually being used.

        val newFile = File(outputFolder, mod.name)
        newFile.createNewFile()

        val bytes = ByteArrayOutputStream()
        mod.renderTo({ bytes.write(it) }, loadOrder)
        newFile.writeBytes(bytes.toByteArray())

        consistencyFileHandler.saveConsistency(mod.name)

    }

    fun createMod(modName: String): Mod {
        val accessor = consistencyFileHandler.recorderFor(modName)
        if (!lastSeenLoadOrder.contains(modName)) lastSeenLoadOrder.add(modName)
        return Mod(modName, ModBinary.create(modName, accessor))
    }
}
