package com.lunivore.chimmer

import java.io.File
import java.io.FileNotFoundException

class Chimmer {
    fun load(modFolder: File, loadOrderFile: File, consistencyFile: File?): List<Mod> {
        val loadOrder = loadOrderFile.readLines()
        return loadOrder.map {
            val file = modFolder.listFiles { _, name -> name == it }
            if (file.size < 0) { throw FileNotFoundException("Could not find '$it' in folder '${modFolder.absolutePath}'") }
            Mod(it)
        }
    }

}
