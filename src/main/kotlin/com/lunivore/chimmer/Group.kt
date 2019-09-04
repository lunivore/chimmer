package com.lunivore.chimmer

enum class Group(val type : String) {
    GameSetting("GMST"),
    Keyword("KYWD"),
    LocationReferenceType("LCRT"),
    Action("AACT"),
    TextureSet("TXST"),
    GlobalVariable("GLOB"),
    Class("CLAS"),
    Faction("FACT"),
    HeadPart("HDPT"),
    Eyes("EYES"),
    Race("RACE"),
    Sound("SOUN"),
    AcousticSpace("ASPC"),
    MagicEffect("MGEF"),
    LandTexture("LTEX"),
    Enchantment("ENCH"),
    Spell("SPEL"),
    Scroll("SCRL"),
    Activator("ACTI"),
    TalkingActivator("TACT"),
    Armor("ARMO"),
    Book("BOOK"),
    Container("CONT"),
    Door("DOOR"),
    Ingredient("INGR"),
    Light("LIGH"),
    MiscObject("MISC"),
    Apparatus("APPA"),
    MovableStatic("MSTT"),
    Grass("GRAS"),
    Tree("TREE"),
    Flora("FLOR"),
    Furniture("FURN"),
    Weapon("WEAP"),
    Ammo("AMMO"),
    Npc("NPC_"),
    LeveledActor("LVLN"),
    Key("KEYM"),
    Potion("ALCH"),
    IdleMarker("IDLM"),
    ConstructableObject("COBJ"),
    Projectile("PROJ"),
    Hazard("HAZD"),
    SoulGem("SLGM"),
    LeveledItem("LVLI"),
    Weather("WTHR"),
    Climate("CLMT"),
    ShaderParticleGeometry("SPGD"),
    VisualEffect("RFCT"),
    Region("REGN"),
    Navigation("NAVI"),
    Cell("CELL"),
    WorldSpace("WRLD"),
    DialogTopic("DIAL"),
    Quest("QUST"),
    IdleAnimation("IDLE"),
    AiPackage("PACK"),
    CombatStyle("CSTY"),
    LoadScreen("LSCR"),
    LeveledSpell("LVSP"),
    AnimatedObject("ANIO"),
    WaterType("WATR"),
    EffectShader("EFSH"),
    Explosion("EXPL"),
    Debris("DEBR"),
    ImageSpace("IMGS"),
    ImageSpaceModifier("IMAD"),
    FormList("FLST"),
    Perk("PERK"),
    BodyPartData("BPTD"),
    AddonNode("ADDN"),
    ActorValues("AVIF"),
    CameraShot("CAMS"),
    CameraPath("CPTH"),
    VoiceType("VTYP"),
    MaterialType("MATT"),
    ImpactData("IPCT"),
    ImpactDataSet("IPDS"),
    Armature("ARMA"),
    EncounterZone("ECZN"),
    Location("LCTN"),
    Message("MESG"),
    DefaultObjectManager("DOBJ"),
    LightingTemplate("LGTM"),
    Music("MUSC"),
    Footstep("FSTP"),
    FootstepSet("FSTS"),
    StoryManagerBranchNode("SMBN"),
    StoryManagerQuestNode("SMQN"),
    StoryManaagerEventNode("SMEN"),
    DialogBranch("DLBR"),
    MusicTrack("MUST"),
    DialogView("DLVW"),
    WordOfPower("WOOP"),
    Shout("SHOU"),
    EquipSlot("EQUP"),
    Relationship("RELA"),
    Scene("SCEN"),
    AssociationType("ASTP"),
    Outfit("OTFT"),
    ArtObject("ARTO"),
    MaterialObject("MATO"),
    MovementType("MOVT"),
    SoundReference("SNDR"),
    DualCastData("DUAL"),
    SoundCategory("SNCT"),
    SoundOutputModel("SOPM"),
    CollisionLayer("COLL"),
    Color("CLFM"),
    ReverbParameters("REVB");

    companion object {
        val All : List<Group> = values().toList()
    }
}

fun List<Group>.matches(type: String) : Boolean = this.map { it.type }.contains(type)
