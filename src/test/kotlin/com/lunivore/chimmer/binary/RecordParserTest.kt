package com.lunivore.chimmer.binary

import com.lunivore.chimmer.ConsistencyRecorder
import com.lunivore.chimmer.FormId
import com.lunivore.chimmer.testheplers.Hex
import com.lunivore.chimmer.testheplers.fakeConsistencyRecorder
import com.lunivore.chimmer.testheplers.toReadableHexString
import org.junit.Assert
import org.junit.Test
import java.io.ByteArrayOutputStream

class RecordParserTest {


    val menu = object :SubrecordMenu {
        override fun findProvider(recordType: String, subrecordType: String): SubrecordProvider {
            return {bytes -> ByteSub.create(subrecordType, bytes) }
        }
    }

    @Test
    fun `should be able to parse Tes4 from bytes`() {
        // Given a mod which contains an iron sword
        val binary = (Hex.CHIMMER_MOD_HEADER + Hex.IRON_SWORD_WEAPON_GROUP).replace(" ", "")

        // When we parse the record header from it
        val result = RecordParser(menu).parseTes4("Wibble.esp", binary.fromHexStringToByteList())

        // Then we should get back one record  with all the relevant subrecords in
        val record = result.parsed
        Assert.assertEquals("HEDR, CNAM, MAST, DATA".split(", "), record.subrecords.map { it.type })

        // And the records should have the correct data
        Assert.assertEquals("Chimmer", record.find("CNAM")?.asString())

        // And the rest of the data should also be passed back in the result.
        val expectedHex = Hex.IRON_SWORD_WEAPON_GROUP
        val actualHex = result.rest.toByteArray().toReadableHexString()
        Assert.assertEquals(expectedHex, actualHex)
    }

    @Test
    fun `should be able to parse non-Tes4 records from bytes too`() {
        // Given an iron sword weapon as bytes
        val hex = Hex.IRON_SWORD_WEAPON

        // And some other random data
        val rest = "0E FF BB DD"

        // When we get the record from it
        val result = RecordParser(menu).parseAll("Wibble.esp", (hex + rest).fromHexStringToByteList(), listOf("Skyrim.esm"))

        // Then we should have back a record of type "WEAP"
        Assert.assertEquals("WEAP", result.parsed[0].type)

        // And the rest should be returned too for further parsing
        Assert.assertEquals(rest, result.rest.toReadableHexString())
    }

    @Test
    fun `should return empty grups (CLDC, HAIR, RGDL, SCPT, SCOL, PWAT) without any trouble`() {
        // Given an empty grup (so no bytes)
        // When we parse it

        val result = RecordParser(menu).parseAll("Wibble.esp", listOf(), listOf("Skyrim.esm"))

        // Then we should just get back an empty list.
        Assert.assertEquals(0, result.parsed.size)
    }

    @Test
    fun `should throw an exception with mod, type and form id if it's malformed`() {
        // Given an iron sword weapon as bytes but with a few missing
        val goodHex = Hex.IRON_SWORD_WEAPON
        val badHex = goodHex.substring(0, goodHex.length - 3)

        // When we parse it
        try {
            RecordParser(menu).parseAll("Wibble.esp", badHex.fromHexStringToByteList(), listOf("Skyrim.esm"))
            Assert.fail()
        } catch (e: IllegalStateException) {
            // Then it should have thrown an exception
            Assert.assertTrue(e.message!!.contains("Wibble.esp"))
            Assert.assertTrue(e.message!!.contains("WEAP"))
            Assert.assertTrue(e.message!!.contains("00012EB7")) // Iron sword form id
        }
    }

    @Test
    fun `should render itself and its subrecords as bytes on request`() {
        // Given a record containing an iron sword
        val hex = Hex.IRON_SWORD_WEAPON
        val masters = listOf("Skyrim.esm")
        val record = RecordParser(menu).parseAll("Wibble.esp", hex.fromHexStringToByteList(), masters).parsed[0]

        // When we turn it back into bytes

        val rendered = ByteArrayOutputStream()
        record.render(masters, ::fakeConsistencyRecorder){ rendered.write(it) }

        // Then we should have the bytes back again identically
        Assert.assertEquals(hex, rendered.toByteArray().toReadableHexString())
    }

    @Test
    fun `should look up any existing form id in the consistency accessor where appropriate`() {

        // Given a record containing an iron sword
        // And a new sword that was generated last time with a new EDID that was tracked for consistency
        val consistencyRecorder: ConsistencyRecorder = {
            if (it == "MY_MOD_Editor_Id_123456") "00ABCD".fromHexStringToByteList().toLittleEndianUInt()
            else throw IllegalArgumentException("This will not happen in this test code.")
        }

        val hex = Hex.IRON_SWORD_WEAPON
        val record = RecordParser(menu).parseAll("Wibble.esp", hex.fromHexStringToByteList(), listOf("Skyrim.esm")).parsed[0]

        // When we copy the record as new
        val newRecord = record.with(Subrecord.create("EDID", "MY_MOD_Editor_Id_123456\u0000".toByteList())).copyAsNew()

        // Then the raw should show that it's new
        Assert.assertTrue(newRecord.isNew())
        Assert.assertEquals("FFFFFFFF", newRecord.formId.toBigEndianHexString())

        // But it should still have the original masters
        Assert.assertEquals(listOf("Skyrim.esm"), newRecord.masters)

        // When we render the bytes out with a new masterlist
        val byteArray = ByteArrayOutputStream()
        newRecord.render(listOf("Skyrim.esm", "Dawnguard.esm"), consistencyRecorder) {byteArray.write(it)}

        // And load it back again
        val parsedRecord = RecordParser(menu).parseAll("Wibble.esp",
                byteArray.toByteArray().toList(),
                listOf("Skyrim.esm", "Dawnguard.esm")).parsed[0]

        // Then the form Id should be the newRecord but with an index of 02
        Assert.assertEquals("02CDAB00", parsedRecord.formId.toBigEndianHexString())
    }

    @Test
    fun `should be able to replace subrecords`() {
        // Given a record containing an iron sword
        val hex = Hex.IRON_SWORD_WEAPON
        val record = RecordParser(menu).parseAll("Wibble.esp", hex.fromHexStringToByteList(), listOf("Skyrim.esm")).parsed[0]

        // When we change the EDID record
        val changedRecord = record.with(Subrecord.create("EDID", "MY_MOD_Editor_Id_123456\u0000".toByteList()))

        // Then it should be a copy but with the new EDID field.
        Assert.assertEquals("MY_MOD_Editor_Id_123456", changedRecord.find("EDID")?.asString())
    }

    @Test
    fun `should be able to add subrecords if not found`() {
        // Given a record containing an iron sword
        val hex = Hex.IRON_SWORD_WEAPON
        val record = RecordParser(menu).parseAll("Wibble.esp", hex.fromHexStringToByteList(), listOf("Skyrim.esm")).parsed[0]

        // When we add another weapon as a template
        val template = 0x010b0c0du.toLittleEndianBytes().toList()
        val changedRecord = record.with(Subrecord.create("CNAM", template))

        // Then it should be a copy but with the template added.
        Assert.assertEquals(template, changedRecord.find("CNAM")?.bytes)
    }

    @Test
    fun `should convert its own FormId for a new masterlist`() {
        // Given a record containing an iron sword
        val hex = Hex.IRON_SWORD_WEAPON
        val record = RecordParser(menu).parseAll("Wibble.esp", hex.fromHexStringToByteList(), listOf("Skyrim.esm")).parsed[0]

        // When we ask it to render with a new list of masters (having converted it to subrecords)
        // NB: Can delete the explicit "convert to subrecords" once we're able to convert internal FormIds for
        // all records, because that will happen anyway
        record.subrecords
        val byteArray = ByteArrayOutputStream()
        record.render(listOf("Whatever.esm", "Skyrim.esm"), ::fakeConsistencyRecorder) {byteArray.write(it)}

        // Then it should render its form id with a new index to represent its place in that masterlist
        val renderedHex = byteArray.toByteArray()

        val newRecord = RecordParser(menu).parseAll("Wibble.esp", renderedHex.toList(), listOf("Skyrim.esm")).parsed[0]
        Assert.assertEquals(FormId.create("Wibble.esp", 0x01000000u or record.formId.unindexed, listOf("Skyrim.esm")),
                newRecord.formId)
    }

    @Test
    fun `should find its originating mod in a masterlist of merged mods and use that as a master`() {
        // Given a record containing an iron sword
        val hex = Hex.IRON_SWORD_WEAPON
        val record = RecordParser(menu).parseAll("IronSword.esp", hex.fromHexStringToByteList(), listOf("Skyrim.esm")).parsed[0]

        // That's been copied as a new object to the new mod (so it has no master yet)
        // (Note that saving and reloading through Chimmer gives it the name of the mod that it's being loaded with;
        // note also that the masterlist here refers to internals of the Iron Sword and not the origin any more)
        val newFormId = FormId.create("IronSword.esp", 0x01abcdefu, listOf("Skyrim.esm"))
        val newRecord = record.copy(formId = newFormId)

        // When we ask it to render with a new list of masters that contains its own origin
        val byteArray = ByteArrayOutputStream()
        newRecord.render(listOf("Skyrim.esm", "IronSword.esp"), ::fakeConsistencyRecorder) {byteArray.write(it)}

        // Then it should render its form id with a new index to represent its place in that masterlist
        val renderedHex = byteArray.toByteArray().toReadableHexString()

        // (LittleEndian byte, so the index appears at the end of the hex, and we changed the form id)
        Assert.assertEquals(hex.replace("B7 2E 01 00", "EF CD AB 01"), renderedHex)
    }

    @Test
    fun `should throw an exception if its master is not found in the list of mods`() {
        // Given a record containing an iron sword
        val hex = Hex.IRON_SWORD_WEAPON
        val record = RecordParser(menu).parseAll("IronSword.esp", hex.fromHexStringToByteList(), listOf("Skyrim.esm")).parsed[0]

        // Which we've worked on (so it's converted to subrecords)
        record.subrecords

        // When we ask it to render with a new list of masters that does not contain its origin
        // Then it should throw an IllegalArgumentException
        try {
            record.render(listOf("Whatever.esm", "Another.esp"), ::fakeConsistencyRecorder) {  }
            Assert.fail()
        } catch (e : IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun `should throw an exception if we tried to convert a record to new masters without de-subbing it first`() {
        // TODO: Finish reindexing ALL formIds for all records, then delete the associated check and this test
        // Given a record containing an NPC which we're not going to work with
        val hex = Hex.COMPRESSED_NPC_RECORD.fromHexStringToByteList()
        val npc = RecordParser(menu).parseAll("Dremora.esp", hex, listOf("Skyrim.esm")).parsed[0]

        // When we ask it to render with a new list of masters that does not contain its origin
        try {
            npc.render(listOf("Whatever.esm", "Another.esp"), ::fakeConsistencyRecorder) {  }
            Assert.fail()
        } catch(e: IllegalStateException) {
            // Then it should throw an IllegalStateException
        }

    }

    @Test
    fun `should include new master files in any TES4 header render`() {
        // Given a TES4 record with the usual subrecords (and no master / data pairs)
        val tes4 = RecordParser(menu).parseTes4("MyMod.esp", Hex.CHIMMER_MOD_HEADER.fromHexStringToByteList()).parsed

        // When we render it with a list of new masters
        val bytes = ByteArrayOutputStream()
        val newMasters = "Skyrim.esm, Dawnguard.esm, AnotherMod.esp".split(", ")
        tes4.render(newMasters, {throw Exception("Not used")}, {bytes.write(it)})

        // Then they should also be parsed in (which we can tell by reloading the header).
        val renderedTes4 = RecordParser(menu).parseTes4("MyMod.esp", bytes.toByteArray().toList()).parsed

        Assert.assertEquals(newMasters, renderedTes4.masters)
    }
}