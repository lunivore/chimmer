package com.lunivore.chimmer

import com.lunivore.chimmer.binary.ModBinary
import com.lunivore.chimmer.skyrim.Weapon

// Note that this is a top-level class; tests / documentation
// can be found in the scenarios module.
data class Mod(val name: String, private val modBinary: ModBinary) {

    val weapons: List<Weapon>
        get() = modBinary.find("WEAP")?.map { Weapon(it) } ?: listOf()

    fun renderTo(renderer: (ByteArray) -> Unit, masterList: List<String>) {
        modBinary.renderTo(renderer, masterList)
    }

    fun withWeapons(weapons: List<Weapon>): Mod {
        return Mod(name, modBinary.createOrReplaceGrup("WEAP", weapons))
    }
}

fun List<Mod>.weapons(predicate: (Weapon) -> Boolean): List<Weapon> {
    return this.fold(mutableMapOf<FormId, Weapon>()) {acc, mod ->
        mod.weapons.filter(predicate).forEach {
            acc.put(it.formId(), it)
        }
        acc
    }.values.toList()
}