package com.lunivore.chimmer

import com.lunivore.chimmer.binary.ModBinary
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException

class Chimmer {
    fun load(modFolder: File, loadOrderFile: File, consistencyFile: File? = null): List<Mod> {
        return load(modFolder, loadOrderFile.readLines(), consistencyFile)
    }

    fun load(modFolder: File, loadOrder: List<String>, consistencyFile: File? = null): List<Mod> {
        if (!modFolder.exists()) {
            throw FileNotFoundException("Could not find mod folder '${modFolder.absolutePath}'")
        }
        return loadOrder.map {
            val matchingFiles = modFolder.listFiles { _, name -> name == it }
            if (matchingFiles.isEmpty()) {
                throw FileNotFoundException("Could not find '$it' in folder '${modFolder.absolutePath}'")
            }

            Mod(it, ModBinary.parse(matchingFiles[0].readBytes()))
        }
    }

    fun save(mod: Mod, outputFolder: File, filename: String, consistencyFileName: String = "chimmer/${filename}_consistency") {
        val newFile = File(outputFolder, filename)
        newFile.createNewFile()

        val bytes = ByteArrayOutputStream()
        mod.renderTo { bytes.write(it) }
        newFile.writeBytes(bytes.toByteArray())
    }

    fun createMod(modName: String): Mod {
        TODO("not implemented") //To change body of parsed functions use File | Settings | File Templates.
    }


}
