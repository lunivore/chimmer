package com.lunivore.chimmer.testheplers

import java.io.File

fun asResourceFile(filename: String): File {
    val resourceEsp = Hex::class.java.getResource("/$filename").file
    return File(resourceEsp)
}