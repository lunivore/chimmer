package com.lunivore.chimmer.binary

import com.lunivore.chimmer.ExistingFormId
import com.lunivore.chimmer.helpers.IndexedFormId
import com.lunivore.chimmer.helpers.MastersWithOrigin
import com.lunivore.chimmer.testheplers.Hex
import org.junit.Assert
import org.junit.Test


/**
 * Note this class is pretty sparse because Records are at the heart of all the binary parsing;
 * most behaviour is covered by scenarios so that we can load real files.
 */
@UseExperimental(ExperimentalUnsignedTypes::class)
class RecordTest {

    val menu = object :SubrecordMenu {
        override fun findProvider(recordType: String, subrecordType: String): SubrecordProvider {
            return { _, bytes -> ByteSub.create(subrecordType, bytes) }
        }
    }

    @Test
    fun `should provide all FormIds which are used in its records`() {
        // Given a weapon with CRDT, KWDA and CNAM from other mods
        val ironSword = RecordParser(menu).parseAll(MastersWithOrigin("IronSword.esp", listOf("Skyrim.esm")), Hex.IRON_SWORD_WEAPON.fromHexStringToByteList()).parsed[0]
        val crdt = CrdtSub(0u.toUShort(), 0.4f, 0u, ExistingFormId.create(MastersWithOrigin("MyCrdtMod.esp", listOf("Skyrim.esm")), IndexedFormId(0x01abcdefu)))
        val kwda = KsizKwdaSub(listOf(
                ExistingFormId.create(MastersWithOrigin("MyKwdaMod1.esp", listOf("Skyrim.esm")), IndexedFormId(0x01abcdefu)),
                ExistingFormId.create(MastersWithOrigin("MyKwdaMod2.esp", listOf("Skyrim.esm")), IndexedFormId(0x01abcdefu)),
                ExistingFormId.create(MastersWithOrigin("MyKwdaMod3.esp", listOf("Skyrim.esm")), IndexedFormId(0x01abcdefu)),
                ExistingFormId.create(MastersWithOrigin("MyKwdaMod3.esp", listOf("Skyrim.esm")), IndexedFormId(0x01abcde0u))))
        val cnam = FormIdSub.create("CNAM", ExistingFormId.create(MastersWithOrigin("MyCNamMod.esp", listOf("Skyrim.esm")), IndexedFormId(0x01abcdefu)) )

        val ironSwordWithOtherMasters = ironSword.with(crdt).with(kwda).with(cnam)

        Assert.assertTrue(ironSwordWithOtherMasters.masters.containsAll(listOf("Skyrim.esm", "MyKwdaMod1.esp", "MyKwdaMod2.esp", "MyKwdaMod3.esp", "MyCNamMod.esp", "MyCrdtMod.esp")))
    }
}


