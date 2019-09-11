package com.lunivore.chimmer

import com.lunivore.chimmer.binary.ModBinary
import com.lunivore.chimmer.binary.merge
import com.lunivore.chimmer.helpers.LoadOrder
import com.lunivore.chimmer.skyrim.Armor
import com.lunivore.chimmer.skyrim.Keyword
import com.lunivore.chimmer.skyrim.Npc
import com.lunivore.chimmer.skyrim.Weapon

// Note that this is a top-level class; tests / documentation
// can be found in the scenarios module.
data class Mod(val name: String, internal val modBinary: ModBinary) {

    val weapons: List<Weapon>
        get() = modBinary.find("WEAP")?.map { Weapon(it) } ?: listOf()

    val armors: List<Armor>
        get() = modBinary.find("ARMO")?.map { Armor(it) } ?: listOf()

    val keywords: List<Keyword>
        get() = modBinary.find("KYWD")?.map { Keyword(it) } ?: listOf()

    val npcs: List<Npc>
        get() = modBinary.find("NPC_")?.map { Npc(it) } ?: listOf()


    val masters: Set<String>
        get() = modBinary.masters


    fun renderTo(loadOrderForMasters: List<String>, consistencyRecorder: ConsistencyRecorder, renderer: (ByteArray) -> Unit) {
        modBinary.render(LoadOrder(loadOrderForMasters), consistencyRecorder, renderer)
    }

    fun withWeapons(weapons: List<Weapon>): Mod {
        return Mod(name, modBinary.createOrReplaceGrup("WEAP", weapons))
    }

    fun withKeywords(keywords: List<Keyword>): Mod {
        return Mod(name, modBinary.createOrReplaceGrup("KYWD", keywords))
    }

    fun withNpcs(npcs: List<Npc>): Mod {
        return Mod(name, modBinary.createOrReplaceGrup("NPC_", npcs))
    }
}

fun List<Mod>.merge(modName: String, loadOrder: List<String>) : Mod {
    val modBinaries : List<ModBinary> = this.map { it.modBinary }
    return Mod(modName, modBinaries.merge(modName, loadOrder))

}

