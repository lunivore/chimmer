package com.lunivore.chimmer

import com.lunivore.chimmer.binary.toLittleEndianBytes
import com.lunivore.chimmer.testheplers.toReadableHexString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ConsistencyFileHandlerTest {

    @get:Rule val outputFolder = TemporaryFolder()

    @Test
    fun `should look up any EditorId which exists in the file already and return the associated unindexed FormId`() {
        // Given a consistency file which already exists
        val modName = "myMod_${System.currentTimeMillis()}"
        val consistencyFile = outputFolder.newFile("$modName.consistency")

        consistencyFile.writeBytes("""
            MY_MOD_EditorId_123456:9876AB
            MY_MOD_OtherId_abcdef:FF1234
        """.trimIndent().toByteArray())

        // When we ask the consistency file handler for a form id for an existing editor id
        val consistencyRecorder = ConsistencyFileHandler(outputFolder.root, {File(outputFolder.root, consistencyFile.name)}).recorderFor(modName)
        val unindexedFormId = consistencyRecorder("MY_MOD_EditorId_123456")

        // Then it should give it back to us
        // (remembering that all ints are 4 bytes, of which the unindexed form Id takes 3,
        // and all little Endian ints are reversed so the extra 00 will be at the end)
        assertEquals("98 76 AB 00", unindexedFormId.toLittleEndianBytes().toReadableHexString())
    }

    @Test
    fun `should find the last added id, increment it, and add it to the consistency file when a new form id is requested`() {

        // Given a consistency file which already exists
        val modName = "myMod_${System.currentTimeMillis()}"
        val consistencyFile = outputFolder.newFile("$modName.consistency")
        consistencyFile.createNewFile()

        consistencyFile.writeBytes("""
            MY_MOD_EditorId_123456:9876AB
            MY_MOD_OtherId_abcdef:9876AC
        """.trimIndent().toByteArray())

        // When we ask the consistency file handler for a form id for a new editor id
        val consistencyRecorder = ConsistencyFileHandler(outputFolder.root, {File(outputFolder.root, consistencyFile.name)}).recorderFor(modName)
        val unindexedFormId = consistencyRecorder("MY_MOD_NewId_7890ab")

        // Then it should give the next form id back to us (remembering that for little-Endian bytes, the smallest
        // byte that gets incremented is on the left and we get a spare zero byte back too)
        assertEquals("99 76 AC 00", unindexedFormId.toLittleEndianBytes().toReadableHexString())

        // Tidying up
        consistencyFile.delete()
    }

    @Test
    fun `should give us back a form id of 2048 when the file is new or empty`() {

        // Given a consistency file which does not exist
        val modName = "myMod_${System.currentTimeMillis()}"

        // When we ask the consistency file handler for a form id for a new editor id
        val consistencyRecorder = ConsistencyFileHandler(outputFolder.root).recorderFor(modName)
        val unindexedFormId = consistencyRecorder("MY_MOD_NewId_7890ab")

        // Then it should give us back an id of 2048
        assertEquals(2048u, unindexedFormId)

        // When we ask for the next one
        val nextId = consistencyRecorder("MY_MOD_AnotherNewId")

        // Then it should give us back an id of 2049
        assertEquals(2049u, nextId)
    }

    @Test
    fun `should create a new output file for new consistency along with any folders required`() {
        // Given a consistency file which does not exist and a folder structure which doesn't exist either
        val modName = "new_folder_${System.currentTimeMillis()}/also_new/myMod"

        // When we ask for a new id
        val handler = ConsistencyFileHandler(outputFolder.root) {File(outputFolder.root, it)}
        val consistencyRecorder = handler.recorderFor(modName)
        val unindexedFormId = consistencyRecorder("MY_MOD_NewId_7890ab")

        // and then save the file for that mod
        handler.saveConsistency(modName)

        // then the file and all folders should be created
        val newFile = File(outputFolder.root, modName)
        assertTrue(newFile.exists())

        // And it should have recorded the rawFormId (which will be 2048 as above)
        val expected = "MY_MOD_NewId_7890ab:" + String(2048.toLittleEndianBytes()).subSequence(1, 3)
        val actual = newFile.readLines()[0]
        assertEquals(expected, actual)
    }
}