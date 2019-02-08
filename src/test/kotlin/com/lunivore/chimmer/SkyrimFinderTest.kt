package com.lunivore.chimmer

import com.lunivore.chimmer.testheplers.asResourceFile
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SkyrimFinderTest {

    @get:Rule
    val outputFolder = TemporaryFolder()

    @Test
    fun `should be able to find Skyrim from direct Steam installation`() {

        // Given a fake Steam folder
        val steamFolder = asResourceFile("FakeSteamFolder")

        // And a SkyrimFinder which uses that folder
        val finder = SkyrimFinder(steamFolder)

        // When we ask it for the location of Skyrim
        val skyrimLocation = finder.findSkyrimFolder()

        // Then it should tell us it's in that folder
        assertEquals(File(steamFolder, "steamapps/common/Skyrim"), skyrimLocation)
    }

    @Test
    fun `should throw an error if the Skyrim folder is not found`() {
        // Given a Skyrim folder which doesn't exist
        val wrongSteamFolder = asResourceFile("FakeSteamFolder").parentFile

        // And a SkyrimFinder which uses that folder
        val finder = SkyrimFinder(wrongSteamFolder)

        // When we ask it for the location of Skyrim
        try {
            finder.findSkyrimFolder()
            fail("Should have thrown an exception")
        } catch (e : IllegalStateException) {
            // Then it should have thrown an exception
        }
    }

    @Test
    fun `should search library folders for Skyrim if the acf file is not present`() {
        // Given a Steam apps folder without the right .acf file but a libraryfolders.vdf file instead
        val libraryFolder = asResourceFile("FakeSteamFolder")
        val wrongFolder = libraryFolder.parentFile
        val libraryText = """
            "LibraryFolders"
            {
                "TimeNextStatsReport"		"something"
                "ContentStatsID"		"somethingsomething"
                "1"		"${wrongFolder.absolutePath }"
                "2"		"${libraryFolder.absolutePath}"
            }""".trimIndent()

        val steamAppsFolder = outputFolder.newFolder("steamapps")
        val librariesFile = File(steamAppsFolder, "libraryfolders.vdf")
        librariesFile.writeBytes(libraryText.toByteArray())

        // And a SkyrimFinder which uses that folder
        val finder = SkyrimFinder(outputFolder.root)

        // When we ask it for the location of Skyrim
        val skyrimLocation = finder.findSkyrimFolder()

        // Then it should track it down to the right place
        assertEquals(File(libraryFolder, "steamapps/common/Skyrim"), skyrimLocation)
    }

    @Test
    fun `should throw an exception if the library folders file has no libraries in it`() {
        // Given a Steam apps libraryfolders.vdf file with no libraries
        val libraryText = """
            "LibraryFolders"
            {
                "TimeNextStatsReport"		"something"
                "ContentStatsID"		"somethingsomething"
            }""".trimIndent()

        val steamAppsFolder = outputFolder.newFolder("steamapps")
        val librariesFile = File(steamAppsFolder, "libraryfolders.vdf")
        librariesFile.writeBytes(libraryText.toByteArray())

        // And a SkyrimFinder which uses that folder
        val finder = SkyrimFinder(outputFolder.root)

        // When we ask it for the location of Skyrim
        try {
            finder.findSkyrimFolder()
            fail("Should have thrown an exception")
        } catch (e: java.lang.IllegalStateException) {
            // Then it should throw an exception
        }
    }

    @Test
    fun `should throw an exception if no Skyrim manifest is found in any library folder`() {
        // Given a Steam apps libraryfolders.vdf file with only folders not having Skyrim in them
        val libraryFolder = asResourceFile("FakeSteamFolder")
        val wrongFolder = libraryFolder.parentFile
        val libraryText = """
            "LibraryFolders"
            {
                "TimeNextStatsReport"		"something"
                "ContentStatsID"		"somethingsomething"
                "1"		"${wrongFolder.absolutePath }"
            }""".trimIndent()

        val steamAppsFolder = outputFolder.newFolder("steamapps")
        val librariesFile = File(steamAppsFolder, "libraryfolders.vdf")
        librariesFile.writeBytes(libraryText.toByteArray())

        // And a SkyrimFinder which uses that folder
        val finder = SkyrimFinder(outputFolder.root)

        // When we ask it for the location of Skyrim
        try {
            finder.findSkyrimFolder()
            fail("Should have thrown an exception")
        } catch (e: java.lang.IllegalStateException) {
            // Then it should throw an exception
        }
    }

    @Test
    fun `should find the plugins file in the given directory`() {
        // Given a plugins file
        val pluginsFile = asResourceFile("plugins.txt")

        // And a Skyrim finder which uses its folder for plugins
        val finder = SkyrimFinder(pluginsFolder = pluginsFile.parentFile)

        // When we ask it for the plugins.txt
        val result = finder.findLoadOrderFile()

        // Then it should be the right file
        assertEquals(pluginsFile, result)

    }
}