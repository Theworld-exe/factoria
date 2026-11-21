package com.factoria.ui.enemies

data class ResistanceInfo(
    val type: String,
    val decrease: Double = 0.0,
    val percent: Double = 0.0
)

data class EnemyUnit(
    val id: String,
    val name: String,
    val category: String, // "Biters", "Spitters", "Worms", "Nests", "Space Age"
    val planet: String, // "Nauvis", "Vulcanus", "Gleba", "Fulgora", "Aquilo"
    val health: Double,
    val evolutionThreshold: Double,
    val attackType: String,
    val attackDamage: String,
    val attackRange: String,
    val moveSpeed: Double,
    val resistances: List<ResistanceInfo>,
    val recommendedCounter: String,
    val description: String
)

object EnemyDataset {
    val allEnemies = listOf(
        EnemyUnit(
            id = "small-biter",
            name = "Small Biter",
            category = "Biters",
            planet = "Nauvis",
            health = 15.0,
            evolutionThreshold = 0.00,
            attackType = "Melee (Physical)",
            attackDamage = "6",
            attackRange = "1.0",
            moveSpeed = 4.3,
            resistances = emptyList(),
            recommendedCounter = "Gun Turret, Submachine Gun with Firearm Magazine.",
            description = "Basic hostile alien melee unit. Swarms early outposts before armor piercing ammo is available."
        ),
        EnemyUnit(
            id = "medium-biter",
            name = "Medium Biter",
            category = "Biters",
            planet = "Nauvis",
            health = 75.0,
            evolutionThreshold = 0.20,
            attackType = "Melee (Physical)",
            attackDamage = "15",
            attackRange = "1.0",
            moveSpeed = 5.1,
            resistances = listOf(
                ResistanceInfo("Physical", 4.0, 10.0),
                ResistanceInfo("Fire", 0.0, 10.0)
            ),
            recommendedCounter = "Piercing Rounds Magazine, Defender Capsules, Wall Chokepoints.",
            description = "Faster and tougher than small biters. Resists basic physical damage."
        ),
        EnemyUnit(
            id = "big-biter",
            name = "Big Biter",
            category = "Biters",
            planet = "Nauvis",
            health = 375.0,
            evolutionThreshold = 0.50,
            attackType = "Melee (Physical)",
            attackDamage = "30",
            attackRange = "1.0",
            moveSpeed = 4.6,
            resistances = listOf(
                ResistanceInfo("Physical", 8.0, 20.0),
                ResistanceInfo("Fire", 0.0, 10.0),
                ResistanceInfo("Explosion", 10.0, 20.0)
            ),
            recommendedCounter = "Flamethrower Turrets, Laser Turrets, Tank Machine Gun.",
            description = "Heavy assault alien unit capable of tearing through unreinforced walls quickly."
        ),
        EnemyUnit(
            id = "behemoth-biter",
            name = "Behemoth Biter",
            category = "Biters",
            planet = "Nauvis",
            health = 3000.0,
            evolutionThreshold = 0.90,
            attackType = "Melee (Physical)",
            attackDamage = "90",
            attackRange = "1.0",
            moveSpeed = 5.0,
            resistances = listOf(
                ResistanceInfo("Physical", 12.0, 10.0),
                ResistanceInfo("Fire", 0.0, 10.0),
                ResistanceInfo("Explosion", 12.0, 10.0),
                ResistanceInfo("Laser", 0.0, 20.0)
            ),
            recommendedCounter = "Uranium Ammo, Flamethrower Array + Overlapping Laser Defense, Landmines.",
            description = "Colossal endgame Nauvis terrestrial threat requiring concentrated heavy firepower."
        ),
        EnemyUnit(
            id = "small-spitter",
            name = "Small Spitter",
            category = "Spitters",
            planet = "Nauvis",
            health = 10.0,
            evolutionThreshold = 0.25,
            attackType = "Ranged (Acid)",
            attackDamage = "12",
            attackRange = "13.0",
            moveSpeed = 4.0,
            resistances = emptyList(),
            recommendedCounter = "Gun Turrets, Heavy Armor.",
            description = "Light ranged alien that spits acid projectiles at turrets and players from range."
        ),
        EnemyUnit(
            id = "medium-spitter",
            name = "Medium Spitter",
            category = "Spitters",
            planet = "Nauvis",
            health = 50.0,
            evolutionThreshold = 0.40,
            attackType = "Ranged (Acid Splash)",
            attackDamage = "24 + Puddle",
            attackRange = "14.0",
            moveSpeed = 3.6,
            resistances = listOf(ResistanceInfo("Acid", 0.0, 10.0)),
            recommendedCounter = "Laser Turrets, Personal Laser Defense.",
            description = "Ranged unit that leaves corrosive acid puddles on impact, damaging structures over time."
        ),
        EnemyUnit(
            id = "big-spitter",
            name = "Big Spitter",
            category = "Spitters",
            planet = "Nauvis",
            health = 200.0,
            evolutionThreshold = 0.70,
            attackType = "Ranged (Acid Splash)",
            attackDamage = "45 + Puddle",
            attackRange = "15.0",
            moveSpeed = 3.2,
            resistances = listOf(ResistanceInfo("Acid", 0.0, 20.0)),
            recommendedCounter = "Flamethrower Turrets, Tank Cannon Explosive Shells.",
            description = "Dangerous ranged unit capable of outranging basic unupgraded turrets."
        ),
        EnemyUnit(
            id = "behemoth-spitter",
            name = "Behemoth Spitter",
            category = "Spitters",
            planet = "Nauvis",
            health = 1500.0,
            evolutionThreshold = 0.90,
            attackType = "Ranged (Acid Stream)",
            attackDamage = "90 + Large Puddle",
            attackRange = "16.0",
            moveSpeed = 2.8,
            resistances = listOf(ResistanceInfo("Acid", 0.0, 30.0)),
            recommendedCounter = "Artillery Outposts, Spidertron with Explosive Rockets.",
            description = "Endgame ranged horror with devastating acid splash area damage."
        ),
        EnemyUnit(
            id = "small-worm",
            name = "Small Worm",
            category = "Worms",
            planet = "Nauvis",
            health = 200.0,
            evolutionThreshold = 0.00,
            attackType = "Stationary Ranged Acid",
            attackDamage = "30",
            attackRange = "25.0",
            moveSpeed = 0.0,
            resistances = emptyList(),
            recommendedCounter = "Grenades, Car Drive-by with Ammo.",
            description = "Stationary defensive turret-like organism protecting alien nests."
        ),
        EnemyUnit(
            id = "medium-worm",
            name = "Medium Worm",
            category = "Worms",
            planet = "Nauvis",
            health = 500.0,
            evolutionThreshold = 0.30,
            attackType = "Stationary Ranged Acid",
            attackDamage = "60",
            attackRange = "30.0",
            moveSpeed = 0.0,
            resistances = listOf(ResistanceInfo("Acid", 0.0, 10.0)),
            recommendedCounter = "Tank, Poison Capsules, Rocket Launcher.",
            description = "Medium stationary defensive organism with high acid range."
        ),
        EnemyUnit(
            id = "big-worm",
            name = "Big Worm",
            category = "Worms",
            planet = "Nauvis",
            health = 750.0,
            evolutionThreshold = 0.50,
            attackType = "Stationary Ranged Acid",
            attackDamage = "90",
            attackRange = "38.0",
            moveSpeed = 0.0,
            resistances = listOf(ResistanceInfo("Acid", 0.0, 20.0)),
            recommendedCounter = "Tank Explosive Shells, Personal Laser Defense.",
            description = "Heavy stationary defensive worm protecting expansion nests."
        ),
        EnemyUnit(
            id = "behemoth-worm",
            name = "Behemoth Worm",
            category = "Worms",
            planet = "Nauvis",
            health = 2000.0,
            evolutionThreshold = 0.90,
            attackType = "Stationary Ranged Acid",
            attackDamage = "120",
            attackRange = "48.0",
            moveSpeed = 0.0,
            resistances = listOf(ResistanceInfo("Acid", 0.0, 30.0)),
            recommendedCounter = "Artillery Shells, Spidertron Rocket Volleys.",
            description = "Massive endgame stationary turret organism with extreme attack range."
        ),
        EnemyUnit(
            id = "biter-nest",
            name = "Biter Nest",
            category = "Nests",
            planet = "Nauvis",
            health = 500.0,
            evolutionThreshold = 0.00,
            attackType = "Spawns Biters",
            attackDamage = "N/A",
            attackRange = "N/A",
            moveSpeed = 0.0,
            resistances = listOf(
                ResistanceInfo("Physical", 0.0, 10.0),
                ResistanceInfo("Fire", 0.0, 50.0)
            ),
            recommendedCounter = "Artillery, Cluster Grenades, Rocket Launcher.",
            description = "Spawns biters when absorbing pollution. Target with high explosive damage."
        ),
        EnemyUnit(
            id = "spitter-nest",
            name = "Spitter Nest",
            category = "Nests",
            planet = "Nauvis",
            health = 500.0,
            evolutionThreshold = 0.25,
            attackType = "Spawns Spitters",
            attackDamage = "N/A",
            attackRange = "N/A",
            moveSpeed = 0.0,
            resistances = listOf(
                ResistanceInfo("Physical", 0.0, 10.0),
                ResistanceInfo("Fire", 0.0, 50.0)
            ),
            recommendedCounter = "Artillery, Tank Explosive Shells, Spidertron.",
            description = "Spawns spitters upon absorbing ambient pollution."
        ),
        EnemyUnit(
            id = "demolisher-small",
            name = "Small Demolisher",
            category = "Space Age",
            planet = "Vulcanus",
            health = 20000.0,
            evolutionThreshold = 0.00,
            attackType = "Territorial Tremor / Impact",
            attackDamage = "500 Physical",
            attackRange = "Body Sweep",
            moveSpeed = 6.0,
            resistances = listOf(
                ResistanceInfo("Fire", 0.0, 100.0),
                ResistanceInfo("Impact", 50.0, 80.0),
                ResistanceInfo("Physical", 15.0, 50.0)
            ),
            recommendedCounter = "Railgun, Explosive Rocket Turrets, High Quality Ammo.",
            description = "Colossal subterranean worm patrolling Vulcanus territory. Completely immune to fire."
        ),
        EnemyUnit(
            id = "demolisher-medium",
            name = "Medium Demolisher",
            category = "Space Age",
            planet = "Vulcanus",
            health = 50000.0,
            evolutionThreshold = 0.40,
            attackType = "Territorial Tremor / Impact",
            attackDamage = "1200 Physical",
            attackRange = "Body Sweep",
            moveSpeed = 6.5,
            resistances = listOf(
                ResistanceInfo("Fire", 0.0, 100.0),
                ResistanceInfo("Impact", 100.0, 85.0),
                ResistanceInfo("Physical", 25.0, 60.0)
            ),
            recommendedCounter = "Overlapping Railgun Array, Artillery, Legendary Ammo.",
            description = "Giant Vulcanus territory warden requiring heavy anti-armor ordnance."
        ),
        EnemyUnit(
            id = "demolisher-big",
            name = "Big Demolisher",
            category = "Space Age",
            planet = "Vulcanus",
            health = 150000.0,
            evolutionThreshold = 0.80,
            attackType = "Territorial Tremor / Impact",
            attackDamage = "3000 Physical",
            attackRange = "Body Sweep",
            moveSpeed = 7.0,
            resistances = listOf(
                ResistanceInfo("Fire", 0.0, 100.0),
                ResistanceInfo("Impact", 200.0, 90.0),
                ResistanceInfo("Physical", 50.0, 75.0)
            ),
            recommendedCounter = "Legendary Quality Railguns & Rocket Turrets with Speed Modules.",
            description = "Apex Vulcanus titan requiring concentrated high-tier Space Age military tech."
        ),
        EnemyUnit(
            id = "pentapod-strafer",
            name = "Pentapod Strafer",
            category = "Space Age",
            planet = "Gleba",
            health = 300.0,
            evolutionThreshold = 0.00,
            attackType = "Agile Spore Spit",
            attackDamage = "35 Bio",
            attackRange = "12.0",
            moveSpeed = 5.2,
            resistances = listOf(ResistanceInfo("Poison", 0.0, 50.0)),
            recommendedCounter = "Shotgun, Flamethrower Turrets.",
            description = "Fast, agile Gleba pentapod that leaps around defenses while spitting bio-spores."
        ),
        EnemyUnit(
            id = "pentapod-stomper",
            name = "Pentapod Stomper",
            category = "Space Age",
            planet = "Gleba",
            health = 2500.0,
            evolutionThreshold = 0.50,
            attackType = "Trample / Crushing Strike",
            attackDamage = "150 Physical",
            attackRange = "2.0",
            moveSpeed = 3.8,
            resistances = listOf(
                ResistanceInfo("Physical", 10.0, 30.0),
                ResistanceInfo("Explosion", 20.0, 40.0)
            ),
            recommendedCounter = "Laser Array, High Damage Armor Piercing Shells.",
            description = "Heavy five-legged Gleba stomper that crushes factories and walls."
        ),
        EnemyUnit(
            id = "pentapod-wrangler",
            name = "Pentapod Wrangler",
            category = "Space Age",
            planet = "Gleba",
            health = 800.0,
            evolutionThreshold = 0.30,
            attackType = "Spawns Minion Swarms",
            attackDamage = "20 Bio",
            attackRange = "10.0",
            moveSpeed = 4.0,
            resistances = listOf(ResistanceInfo("Poison", 0.0, 30.0)),
            recommendedCounter = "Tesla Gun, Chain Lightning, Flamethrower.",
            description = "Gleba commander organism that directs and spawns bio-minion swarms."
        ),
        EnemyUnit(
            id = "pentapod-egg-nest",
            name = "Pentapod Egg Nest",
            category = "Space Age",
            planet = "Gleba",
            health = 1200.0,
            evolutionThreshold = 0.00,
            attackType = "Spawns Hatchling Pentapods",
            attackDamage = "N/A",
            attackRange = "N/A",
            moveSpeed = 0.0,
            resistances = listOf(ResistanceInfo("Fire", 0.0, 30.0)),
            recommendedCounter = "Incineration, Flamethrower Array.",
            description = "Organic Gleba nest that hatches pentapod units when disturbed."
        )
    )
}
