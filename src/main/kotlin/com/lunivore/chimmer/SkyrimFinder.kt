package com.lunivore.chimmer

import com.sun.deploy.util.WinRegistry
import java.io.File

class SkyrimFinder(
        private val steamFolder: File = File(WinRegistry.getString(
        WinRegistry.HKEY_LOCAL_MACHINE,
        "SOFTWARE\\Wow6432Node\\Valve\\Steam",
        "InstallPath")),
        private val pluginsFolder: File = File(System.getenv("LOCALAPPDATA"), "Skyrim")) {

    companion object {
        val LIBRARY_FOLDER_FILENAME = "libraryfolders.vdf"
        val APP_MANIFEST_FILENAME = "appmanifest_72850.acf"
    }

    fun findSkyrimFolder(): File {

        var candidateFolder :File? = steamFolder

        if (!File(candidateFolder, "steamapps/$APP_MANIFEST_FILENAME").exists()) {
            val libraryFolderFile = File(candidateFolder, "steamapps/$LIBRARY_FOLDER_FILENAME")

            if (!libraryFolderFile.exists()) throw IllegalStateException("Could not find Skyrim manifest $APP_MANIFEST_FILENAME or $LIBRARY_FOLDER_FILENAME in steamapps at $steamFolder")

            val libraryFolderText = libraryFolderFile.readText()
            val pattern = Regex("^\\s*\"\\d+\"\\s*\"(.*?)\"$", RegexOption.MULTILINE)

            val matches = pattern.findAll(libraryFolderText)
            if (matches.count() > 0) {
                val libraryFolders = matches.map { File(it.groupValues[1]) }
                candidateFolder = libraryFolders.firstOrNull { File(it, "steamapps/$APP_MANIFEST_FILENAME").exists() }
            } else throw IllegalStateException("No libraries present in libraryfolders.vdf in steamapps at ${steamFolder}")
        }

        if (candidateFolder == null) throw IllegalStateException("Could not find Skyrim manifest $APP_MANIFEST_FILENAME in any Steam games folder")

        val skyrimFolder = File(candidateFolder, "steamapps/common/Skyrim")
        if (!skyrimFolder.exists()) {
            throw IllegalStateException("Skyrim folder should have been located at ${candidateFolder.absolutePath} but was not found.")
        }
        else  return skyrimFolder
    }

    fun findLoadOrderFile(): File {
        return File(pluginsFolder, "plugins.txt")
    }

}
