package com.lunivore.chimmer

import java.io.File
import java.io.FileNotFoundException

class Chimmer {
    fun load(modFolder: File, loadOrderFile: File, consistencyFile: File?): List<Mod> {
        return load(modFolder, loadOrderFile.readLines(), consistencyFile)
    }

    fun load(modFolder: File, loadOrder: List<String>, consistencyFile: File?): List<Mod> {
        if (!modFolder.exists()) { throw FileNotFoundException("Could not find mod folder '${modFolder.absolutePath}'") }
        return loadOrder.map {
            val matchingFiles = modFolder.listFiles { _, name -> name == it }
            if (matchingFiles.isEmpty()) { throw FileNotFoundException("Could not find '$it' in folder '${modFolder.absolutePath}'") }
            Mod(it)
        }
    }

}
