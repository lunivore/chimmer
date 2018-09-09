package com.lunivore.chimmer

import com.lunivore.chimmer.binary.ModBinary
import com.lunivore.chimmer.skyrim.Weapon

data class Mod(val name: String, private val modBinary: ModBinary) {

    val weapons: List<Weapon>
        get() {
            return listOf()
        }

    fun renderTo(renderer: (ByteArray) -> Unit) {
        modBinary.renderTo(renderer)
    }

    fun withWeapons(weapons: List<Weapon>): Mod {
        return Mod(name, modBinary.replaceGrup("WEAP", weapons ))
    }
}