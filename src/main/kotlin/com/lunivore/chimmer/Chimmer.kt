package com.lunivore.chimmer

import com.lunivore.chimmer.binary.ModBinary
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException

class Chimmer(val outputFolder: File = File("."), private val consistencyFileHandler: ConsistencyFileHandler = ConsistencyFileHandler(outputFolder)) {

    fun load(modFolder: File, loadOrderFile: File): List<Mod> {
        return load(modFolder, loadOrderFile.readLines())
    }

    fun load(modFolder: File, loadOrder: List<ModFilename>): List<Mod> {
        if (!modFolder.exists()) {
            throw FileNotFoundException("Could not find mod folder '${modFolder.absolutePath}'")
        }
        return loadOrder.map {
            val matchingFiles = modFolder.listFiles { _, name -> name == it }
            if (matchingFiles.isEmpty()) {
                throw FileNotFoundException("Could not find '$it' in folder '${modFolder.absolutePath}'")
            }

            Mod(it, ModBinary.parse(matchingFiles[0].readBytes(),
                    consistencyFileHandler.recorderFor(it)))
        }
    }

    fun save(mod: Mod) {

        val newFile = File(outputFolder, mod.name)
        newFile.createNewFile()

        val bytes = ByteArrayOutputStream()
        mod.renderTo { bytes.write(it) }
        newFile.writeBytes(bytes.toByteArray())

        consistencyFileHandler.saveConsistency(mod.name)
    }

    fun createMod(modName: String): Mod {
        val accessor = consistencyFileHandler.recorderFor(modName)
        return Mod(modName, ModBinary.create(accessor))

    }
}
