package com.lunivore.chimmer

import org.junit.Rule
import org.junit.rules.TemporaryFolder

open class ChimmerScenario {

    @get:Rule
    val outputFolder = TemporaryFolder()
    fun fileHandler() : FileHandler = SensibleDefaultFileHandler(outputFolder = outputFolder.root)
}
