package com.lunivore.chimmer.scenariohelpers

import com.lunivore.chimmer.LoadingAndSavingFiles
import java.io.File

fun asResourceFile(filename: String): File {
    val resourceEsp = LoadingAndSavingFiles::class.java.getResource("/$filename").file
    return File(resourceEsp)
}