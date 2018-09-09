# Chimmer

Chimmer lets you change your mods easily! It will:

- **TODO** - load a list of mods
- **TODO** - in the order you specify (via a list, a multi-line string, or a file like plugins.txt)
- **TODO** - filtered if you want to
- **TODO** - allowing you to programmatically manipulate the mod
- **TODO** - and create new records
- **TODO** - or mark records as deleted
- **TODO** - retaining consistency with previous patch runs via a consistency file a la SkyProc
- **TODO** - while keeping it all in sync with any changes to the master list
- **TODO** - and save it to a new mod.

It is technically:

- **TODO** - written in Kotlin
- **TODO** - using data classes based on Bethesda's Group / Record / Subrecord format
- **TODO** - with nice wrappers round them that are more readable
- **TODO** - while keeping the binary format behind the covers
- **TODO** - so that the information doesn't diverge
- **TODO** - allowing you to save it all
- **TODO** - with sensible default settings.

To load and save mods:

    val mods = Chimmer().load(modFolder, pluginsList, filters)
    Chimmer().save(mod, modFolder, modFileName, consistencyFile)

To create a new mod, ensuring consistency with any previous patch run:

    Chimmer().createMod("Chimmer.esp", consistencyFile)

To manipulate the mod records:

    val mySword = mod.weapons.filter { it.name = "Iron Sword" }.withDamage ({ it.damage * 6 })
    val otherWeapons = mod.weapons.map { it.withDamage({ it.damage * 3 }) }
    val newMod = mod.withWeapons( { it.weapons + listOf(mySword) } )

To handle something that Chimmer doesn't:

- Write a wrapper for a new grup (like the SLGM Soul Gem grup), or
- Write an extension method for an existing wrapper (like the SNAM / equipping sound that WEAP / Weapon makes), or
- Add an issue to this repository asking me nicely to do it (I may say no), or
- Make a fork of this project and do it yourself.



