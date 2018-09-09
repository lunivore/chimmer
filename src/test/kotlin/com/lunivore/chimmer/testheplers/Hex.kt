package com.lunivore.chimmer.testheplers

import com.lunivore.chimmer.binary.Subrecord
import javax.xml.bind.DatatypeConverter

fun String.fromHexStringToByteList(): List<Byte> {
    return DatatypeConverter.parseHexBinary(this.replace(" ", "")).toList()
}

fun ByteArray.toReadableHexString(): String {
    return DatatypeConverter.printHexBinary(this).foldIndexed("") { i, string, char ->
        string + char + (if (i % 2 > 0) " " else "")
    }.trimEnd()
}

fun List<Byte>.toReadableHexString() = this.toByteArray().toReadableHexString()

fun Subrecord.toReadableHexString() = this.bytes.toReadableHexString()

class Hex {

    companion object {
        /**
         * Contains the TES4 header file for a mod:
         * - Is master
         * - Author "Chimmer"
         * - no description
         * - Skyrim.esm as master file
         * - Version 43
         */
        val CHIMMER_MOD_HEADER =
                "54 45 53 34 3F 00 00 00 01 00 00 00 00 00 00 00 " +
                        "00 00 00 00 2B 00 00 00 48 45 44 52 0C 00 9A 99 " +
                        "D9 3F 05 00 00 00 00 08 00 00 43 4E 41 4D 08 00 " +
                        "43 68 69 6D 6D 65 72 00 4D 41 53 54 0B 00 53 6B " +
                        "79 72 69 6D 2E 65 73 6D 00 44 41 54 41 08 00 00 " +
                        "00 00 00 00 00 00 00"

        val IRON_SWORD_GROUP_HEADER = "47 52 55 50 1B 02 00 00 57 45 41 50 00 00 00 00 " +
                "00 00 00 00 00 00 00 00"

        val IRON_SWORD_WEAPON_RECORD_HEADER = "57 45 41 50 EB 01 00 00 " +
                "00 00 00 00 B7 2E 01 00 00 00 00 00 28 00 00 00"

        val IRON_SWORD_WEAPON_DATA =
                "45 44 49 44 0A 00 49 72 6F 6E 53 77 6F 72 64 00 " +
                        "4F 42 4E 44 0C 00 FA FF F5 FF FF FF 06 00 3F 00 " +
                        "01 00 46 55 4C 4C 0B 00 49 72 6F 6E 20 53 77 6F " +
                        "72 64 00 4D 4F 44 4C 1B 00 57 65 61 70 6F 6E 73 " +
                        "5C 49 72 6F 6E 5C 4C 6F 6E 67 53 77 6F 72 64 2E " +
                        "6E 69 66 00 4D 4F 44 54 84 00 02 00 00 00 0A 00 " +
                        "00 00 00 00 00 00 F8 AE 22 4D 64 64 73 00 68 EF " +
                        "BA 75 BC A5 22 7A 64 64 73 00 68 EF BA 75 33 16 " +
                        "8C 07 64 64 73 00 A0 B8 63 05 03 D8 C2 40 64 64 " +
                        "73 00 A0 B8 63 05 DA 8E 92 91 64 64 73 00 26 2C " +
                        "33 3B 4F E0 75 64 64 64 73 00 A0 B8 63 05 0F 61 " +
                        "36 6C 64 64 73 00 68 EF BA 75 F3 2C 04 2A 64 64 " +
                        "73 00 68 EF BA 75 B8 EC 30 7B 64 64 73 00 26 2C " +
                        "33 3B A4 63 69 F4 64 64 73 00 68 EF BA 75 45 54 " +
                        "59 50 04 00 42 3F 01 00 42 49 44 53 04 00 FF 83 " +
                        "01 00 42 41 4D 54 04 00 C2 74 07 00 4B 53 49 5A " +
                        "04 00 03 00 00 00 4B 57 44 41 0C 00 11 E7 01 00 " +
                        "18 E7 01 00 58 F9 08 00 44 45 53 43 01 00 00 49 " +
                        "4E 41 4D 04 00 AC 3C 01 00 57 4E 41 4D 04 00 B0 " +
                        "6B 03 00 54 4E 41 4D 04 00 30 C7 03 00 4E 41 4D " +
                        "39 04 00 2E C7 03 00 4E 41 4D 38 04 00 2F C7 03 " +
                        "00 44 41 54 41 0A 00 19 00 00 00 00 00 10 41 07 " +
                        "00 44 4E 41 4D 64 00 01 00 00 00 00 00 80 3F 00 " +
                        "00 80 3F 00 00 91 00 00 00 00 00 00 00 00 00 00 " +
                        "FF 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 " +
                        "00 00 00 00 00 80 3F CD CC 4C 3F 00 00 00 00 00 " +
                        "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 " +
                        "00 00 00 06 00 00 00 00 00 00 00 00 00 00 00 FF " +
                        "FF FF FF 00 00 00 00 00 00 40 3F 43 52 44 54 10 " +
                        "00 03 00 00 00 00 00 00 00 01 FF FF FF 00 00 00 " +
                        "00 56 4E 41 4D 04 00 01 00 00 00"

        val IRON_SWORD_WEAPON = "$IRON_SWORD_WEAPON_RECORD_HEADER $IRON_SWORD_WEAPON_DATA"

        val IRON_SWORD_WEAPON_GROUP = "$IRON_SWORD_GROUP_HEADER $IRON_SWORD_WEAPON"

        val CROSSBOW_WEAPON =
                "57 45 41 50 91 01 " +
                        "00 00 00 00 00 00 01 08 00 01 00 00 00 00 2B 00 " +
                        "00 00 45 44 49 44 0D 00 44 4C 43 31 43 72 6F 73 " +
                        "73 42 6F 77 00 4F 42 4E 44 0C 00 E5 FF E5 FF FD " +
                        "FF 1B 00 1B 00 06 00 46 55 4C 4C 09 00 43 72 6F " +
                        "73 73 62 6F 77 00 4D 4F 44 4C 24 00 44 4C 43 30 " +
                        "31 5C 57 65 61 70 6F 6E 73 5C 43 72 6F 73 73 62 " +
                        "6F 77 5C 43 72 6F 73 73 62 6F 77 2E 6E 69 66 00 " +
                        "4D 4F 44 54 0C 00 02 00 00 00 00 00 00 00 00 00 " +
                        "00 00 45 54 59 50 04 00 45 3F 01 00 42 49 44 53 " +
                        "04 00 C6 93 01 00 42 41 4D 54 04 00 B6 74 07 00 " +
                        "4B 53 49 5A 04 00 03 00 00 00 4B 57 44 41 0C 00 " +
                        "15 E7 01 00 19 E7 01 00 58 F9 08 00 44 45 53 43 " +
                        "01 00 00 49 4E 41 4D 04 00 11 A3 00 01 57 4E 41 " +
                        "4D 04 00 02 08 00 01 53 4E 41 4D 04 00 34 8E 00 " +
                        "01 58 4E 41 4D 04 00 35 8E 00 01 54 4E 41 4D 04 " +
                        "00 30 C7 03 00 4E 41 4D 39 04 00 C9 7E 01 01 4E " +
                        "41 4D 38 04 00 CA 7E 01 01 44 41 54 41 0A 00 78 " +
                        "00 00 00 00 00 60 41 13 00 44 4E 41 4D 64 00 09 " +
                        "00 00 00 00 00 80 3F 00 00 80 3F 00 00 93 00 00 " +
                        "00 00 00 00 00 00 00 05 FF 01 00 00 00 FA 43 00 " +
                        "00 FA 44 00 00 00 00 00 00 00 00 00 00 80 3F 00 " +
                        "00 80 3F 00 00 00 00 00 00 00 00 00 00 00 00 00 " +
                        "00 00 00 00 00 00 00 00 00 00 00 08 00 00 00 00 " +
                        "00 00 00 00 00 00 00 FF FF FF FF 00 00 00 00 00 " +
                        "00 40 3F 43 52 44 54 10 00 09 00 00 00 00 00 80 " +
                        "3F 01 FF FF FF 00 00 00 00 56 4E 41 4D 04 00 02 " +
                        "00 00 00"

        val SKYRIM_AND_DAWNGUARD_MASTERS =
                "4D 41 53 54 0B 00 53 6B " +
                        "79 72 69 6D 2E 65 73 6D 00 44 41 54 41 08 00 00 " +
                        "00 00 00 00 00 00 00 4D 41 53 54 0E 00 44 61 77 " +
                        "6E 67 75 61 72 64 2E 65 73 6D 00 44 41 54 41 08 " +
                        "00 00 00 00 00 00 00 00 00"

        val UNINTERESTING_COLOUR_GROUP =
                "47 52 55 50 5C 00 00 00 43 4C 46 4D 00 " +
                        "00 00 00 00 00 00 00 00 00 00 00 43 4C 46 4D 2C " +
                        "00 00 00 00 00 00 00 86 9D 09 00 00 00 00 00 28 " +
                        "00 00 00 45 44 49 44 06 00 57 68 69 74 65 00 46 " +
                        "55 4C 4C 06 00 57 68 69 74 65 00 43 4E 41 4D 04 " +
                        "00 FD FD FD 00 46 4E 41 4D 04 00 01 00 00 00"
    }
}