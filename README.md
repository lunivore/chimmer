# Chimmer

Chimmer lets you change your mods easily! It will:

- load a list of mods
- in the order you specify (via a list, a multi-line string, or a file like plugins.txt)
- using your Steam Skyrim directory and plugins.txt as a default
- **TODO** - filtered if you want to
- **TODO** - including compressed records like NPCs
- allowing you to merge mods into one
- and programmatically manipulate a mod
- and create new records
- **TODO** - and use those new records in other records
- **TODO** - or mark records as deleted
- retaining consistency with previous patch runs via a consistency file a la SkyProc
- allowing new records to reference other new records with an editorId
- while keeping it all in sync with any changes to the master list
- including any FormIds used internally to records
- and save it to a new mod.

It is technically:

- written in Kotlin
- using data classes based on Bethesda's Group / Record / Subrecord format
- with nice wrappers round them that are more readable
- while keeping the binary format behind the covers
- so that the information doesn't diverge
- allowing you to save it all
- with sensible default settings.

To load and save mods:

    val chimmer = Chimmer(File("MyOutputFolder"), listOf("Skyrim.esm", "Dawnguard.esm"))
    val mods = chimmer.load(modFolder, pluginsList, filters)

To create new mods, ensuring consistency with any previous patch run:

    val chimmer = Chimmer(File("MyOutputFolder"), File("plugins.txt"))
    val mod1 = chimmer.createMod("Chimmer1.esp")
    val mod2 = chimmer.createMod("Chimmer2.esp")
    chimmer.save(mod1)
    chimmer.save(mod2)

To manipulate the mod records:

    val mySword = mod.weapons.filter { it.name = "Iron Sword" }.withDamage ({ it.damage * 6 })
    val otherWeapons = mod.weapons.map { it.withDamage({ it.damage * 3 }) }
    val newMod = mod.withWeapons( { otherWeapons + listOf(mySword) } )

To handle something that Chimmer doesn't:

- Write a wrapper for a new grup (like the SLGM Soul Gem grup), or
- Write an extension method for an existing wrapper (like the SNAM / equipping sound that WEAP / Weapon makes), or
- Extend RecordWrapper yourself, or
- Add an issue to this repository asking me nicely to do it (I may say no), or
- Make a fork of this project and do it yourself.



