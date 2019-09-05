package com.lunivore.chimmer.binary

import com.lunivore.chimmer.ConsistencyRecorder
import com.lunivore.chimmer.FormId
import com.lunivore.chimmer.Group
import com.lunivore.chimmer.helpers.FormIdComparator
import com.lunivore.chimmer.helpers.LoadOrder
import com.lunivore.chimmer.helpers.MastersWithOrigin
import com.lunivore.chimmer.helpers.OriginMod
import com.lunivore.chimmer.matches


// TODO: Recalculate next available object id (highest + 1024), number of records and groups, and the masterlist.

@UseExperimental(ExperimentalUnsignedTypes::class)
data class ModBinary(val originMod: OriginMod, val header: Record, val grups: List<Grup>) : List<Grup> by grups {
    companion object {

        private val consistencyRecorderForTes4: ConsistencyRecorder = {
            throw IllegalStateException("TES4 records should always have form id of 0")
        }

        private fun fromCommaDelimitedToList(grupList: String) =
                grupList.lines().joinToString("").split(",").map {it.trim()}

        fun parse(originMod: OriginMod, bytes: ByteArray, filter: List<Group>): ModBinary {
            val result = RecordParser().parseTes4(originMod, bytes.toList())

            val headerRecord = result.parsed
            val grups = Grup.parseAll(MastersWithOrigin(originMod.value, headerRecord.masters), result.rest, filter)
                    .filter { filter.matches(it.type) }

            return ModBinary(originMod, headerRecord, grups)
        }

        fun create(modName: String): ModBinary {

            val tes4 = RecordParser().createTes4()
            return ModBinary(OriginMod(modName), tes4, listOf())
        }
    }

    val masters: Set<String>
        get() {
            return grups.flatMap { it.records.flatMap { it.masters } }.filterNot { it.isNullOrEmpty() }.map { it!! }.toSet()
        }

    fun render(loadOrderForMasters: LoadOrder, consistencyRecorder: ConsistencyRecorder, renderer: (ByteArray) -> Unit) {

        val mastersInLoadOrder = loadOrderForMasters.value.filter { masters.contains(it) }
        val mastersNotInLoadOrder = masters.filter { !loadOrderForMasters.value.contains(it) }
        val orderedMasters = mastersInLoadOrder.plus(mastersNotInLoadOrder).filterNot { it == originMod.value }
        val mastersWithOrigin = MastersWithOrigin(originMod.value, orderedMasters)

        header.render(mastersWithOrigin, consistencyRecorderForTes4, renderer)
        grups.forEach { it.render(mastersWithOrigin, consistencyRecorder, renderer) }
    }

    fun <T : RecordWrapper<T>> createOrReplaceGrup(type: String, recordWrappers: List<T>): ModBinary {
        if (grups.any { it.isType(type)})  {
            return copy(grups = grups.map {
                if (!it.isType(type)) it
                else {
                    Grup(type, it.headerBytes, recordWrappers.map { it.record })
                }
            })
        } else {
            val grup_order = Group.All.map { it.type }
            val grupsBeforeType = grup_order.subList(0, grup_order.indexOf(type))
            val typeList = grups.map { it.type }
            val lastGrupWeContainBeforeType = typeList.lastOrNull { grupsBeforeType.contains(it) }
            val newGrups = grups.toMutableList()
            val newGrup = Grup(type, Grup.EMPTY_HEADER_BYTES, recordWrappers.map { it.record })
            if (lastGrupWeContainBeforeType == null) {
                newGrups.add(newGrup)
            } else {
                newGrups.add(typeList.indexOf(lastGrupWeContainBeforeType) + 1, newGrup)
            }
            return copy(grups = newGrups)
        }
    }

    fun find(type: String): Grup? = find { it.type == type }
}

fun List<ModBinary>.merge(newModName: String, loadOrder: List<String>): ModBinary {
    try {

        // Creates a list of all the grups for each grupname, eg:
        // ARMO1, ARMO2
        // (No KYWDS, empty list)
        // WEAP1, WEAP3, WEAP5
        val grupsByNameFromEachMod = Group.All.map { grup -> this.map { it.find(grup.type) }.filterNotNull() }

        // For each record in each grup, finds the last version of it and adds it to a map by FormId.Key
        // ARMO: ArmoRec1, ArmoRec2... ArmoRec14
        // (No KYWDS, still an empty list)
        // WEAP: WeapRec1, WeapRec2... WeapRec8
        val grupOrderedMapsOfLastRecordsWithFormIdKey = grupsByNameFromEachMod.map {
            it.foldRight(mutableMapOf<FormId.Key, Record>()) { grup, mapOfRecordsByFormIdKey ->
                grup.forEach {
                    try {
                        mapOfRecordsByFormIdKey.putIfAbsent(it.formId.key, it)
                    } catch (e: Exception) {
                        throw Exception("Error merging record=${it.formId.asDebug()} with EDID=${it.find("EDID")?.asDebug()}", e)
                    }
                }
                mapOfRecordsByFormIdKey
            }
        }

        // For each grup name, order the records by FormId then make a new grup with all the records in it,
        // and a mod with the new grups.
        val comparator = FormIdComparator(loadOrder)
        val newGrups = grupOrderedMapsOfLastRecordsWithFormIdKey.mapIndexed { i, mappy ->
            if (grupsByNameFromEachMod[i].isEmpty()) null else
                grupsByNameFromEachMod[i].first().copy(records = mappy.keys
                        .sortedWith(comparator).map { mappy[it]!! })
        }.filterNotNull()

        return ModBinary.create(newModName).copy(grups = newGrups)
    } catch (e: Exception) {
        throw Exception("Error merging mods, loadOrder=${loadOrder}", e)
    }
}
