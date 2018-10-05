package com.lunivore.chimmer.scenariohelpers

import com.lunivore.chimmer.LoadingAndSavingMods
import java.io.File

fun asResourceFile(filename: String): File {
    val resourceEsp = LoadingAndSavingMods::class.java.getResource("/$filename").file
    return File(resourceEsp)
}