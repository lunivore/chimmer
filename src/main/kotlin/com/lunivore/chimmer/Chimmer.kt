package com.lunivore.chimmer

import com.lunivore.chimmer.binary.ModBinary
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException

enum class ModsToLoad(val loadBethesdaMods : Boolean) {
    LOAD_ALL(true),
    SKIP_BETHESDA_MODS(false)
}

class Chimmer(val configuration : FileHandler = SensibleDefaultFileHandler()) {

    companion object {
        val BETHESDA_FILES : List<String> =
                "Skyrim.esm, Dawnguard.esm, Dragonborn.esm, Update.esm".split(", ")

        val logger by Logging()
    }

    private var lastSeenLoadOrder: List<ModFilename> = listOf()



    fun load(modsToLoad : ModsToLoad): List<Mod> {
        val skyrimFolder = configuration.findSkyrimFolder()
        val modsFolder = File(skyrimFolder, "Data")
        val pluginsTxtFile = configuration.findLoadOrderFile()

        return load(modsFolder, pluginsTxtFile, modsToLoad)
    }

    fun load(modFolder: File, loadOrder: File, modsToLoad: ModsToLoad): List<Mod> {
        return load(modFolder, loadOrder.readLines(), modsToLoad)
    }

    fun load(modFolder: File, loadOrderFile: File): List<Mod> {
        return load(modFolder, loadOrderFile.readLines())
    }

    fun load(modFolder: File, loadOrder: List<ModFilename>, modsToLoad: ModsToLoad = ModsToLoad.LOAD_ALL): List<Mod> {
        lastSeenLoadOrder = loadOrder
        if (!modFolder.exists()) {
            throw FileNotFoundException("Could not find mod folder '${modFolder.absolutePath}'")
        }

        return loadOrder.filterNot { it.startsWith("#") }
                .filter { modsToLoad.loadBethesdaMods || !BETHESDA_FILES.contains(it) }
                .map {
            val matchingFiles = modFolder.listFiles { _, name -> name == it }
            if (matchingFiles.isEmpty()) {
                throw FileNotFoundException("Could not find '$it' in folder '${modFolder.absolutePath}'")
            }
            logger.info("Loading mod $it")
            Mod(it, ModBinary.parse(it, matchingFiles[0].readBytes()))
        }
    }

    /**
     * If no load order is provided for the master files, Chimmer will use whatever it last saw. All masters will be
     * saved regardless, with any new master files appended to the load order provided.
     */
    fun save(mod: Mod, loadOrderForMasters: List<ModFilename> = lastSeenLoadOrder) {

        val accessor = configuration.recorderFor(mod.name)

        val newFile = File(configuration.outputFolder, mod.name)
        newFile.createNewFile()

        val bytes = ByteArrayOutputStream()
        mod.renderTo(loadOrderForMasters, accessor) { bytes.write(it) }
        newFile.writeBytes(bytes.toByteArray())

        configuration.saveConsistency(mod.name)
    }

    fun createMod(modName: String): Mod {
        return Mod(modName, ModBinary.create())

    }

    fun merge(newModName: String, mods: List<Mod>): Mod {
        return mods.merge(newModName, lastSeenLoadOrder)
    }
}
