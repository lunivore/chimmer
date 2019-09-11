package com.lunivore.chimmer.skyrim

import com.lunivore.chimmer.binary.Record
import com.lunivore.chimmer.binary.RecordParser
import com.lunivore.chimmer.binary.fromHexStringToByteList
import com.lunivore.chimmer.helpers.MastersWithOrigin
import com.lunivore.chimmer.testheplers.Hex
import org.junit.Assert.assertEquals
import org.junit.Test

class SkyrimObjectTest {
    @Test
    fun `should be able to mark things as deleted`() {
        // Given a Skyrim Object
        val sword = Weapon(RecordParser().parseAll(MastersWithOrigin("IronSword.esp",
                listOf("Skyrim.esm")), Hex.IRON_SWORD_WEAPON.fromHexStringToByteList()).parsed[0])

        // When we mark it for deletion
        val deletedSword = sword.delete()

        // Then the appropriate flag should be set
        val record = deletedSword.record
        assertEquals(Record.Companion.HeaderFlags.DELETED.flag, record.flags)

        // And all the subrecords should be missing.
        assertEquals(0, record.subrecords.size)
    }
}