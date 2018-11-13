package com.lunivore.chimmer

import com.lunivore.chimmer.binary.ModBinary
import com.lunivore.chimmer.skyrim.Weapon

// Note that this is a top-level class; tests / documentation
// can be found in the scenarios module.
data class Mod(val name: String, private val modBinary: ModBinary) {

    val weapons: List<Weapon>
        get() = modBinary.find("WEAP")?.map { Weapon(it) } ?: listOf()

    val masters: Set<String>
        get() = modBinary.masters

    fun renderTo(loadOrderForMasters: List<ModFilename>, consistencyRecorder: ConsistencyRecorder, renderer: (ByteArray) -> Unit) {
        modBinary.render(loadOrderForMasters, consistencyRecorder, renderer)
    }

    fun withWeapons(weapons: List<Weapon>): Mod {
        return Mod(name, modBinary.createOrReplaceGrup("WEAP", weapons))
    }
}