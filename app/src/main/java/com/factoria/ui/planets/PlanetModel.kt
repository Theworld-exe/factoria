package com.factoria.ui.planets

data class PlanetInfo(
    val id: String,
    val name: String,
    val subtitle: String,
    val solarMultiplier: Double,
    val dayNightCycleSec: Int,
    val primaryResources: List<String>,
    val exclusiveTechs: List<String>,
    val environmentalHazards: String,
    val strategyTips: String,
    val description: String
)

object PlanetDataset {
    val allPlanets = listOf(
        PlanetInfo(
            id = "nauvis",
            name = "Nauvis",
            subtitle = "Starting Terrestrial Planet",
            solarMultiplier = 1.0,
            dayNightCycleSec = 416,
            primaryResources = listOf("Iron Ore", "Copper Ore", "Coal", "Stone", "Crude Oil", "Uranium Ore"),
            exclusiveTechs = listOf("Basic Automation", "Nuclear Power", "Rocket Silo", "Oil Processing"),
            environmentalHazards = "Standard Biter & Spitter alien nest evolution driven by industrial pollution.",
            strategyTips = "Build a solid main bus, establish military choke walls with flamethrowers, and launch your first rocket.",
            description = "The primary temperate home world. Rich in traditional ores and oil fields."
        ),
        PlanetInfo(
            id = "vulcanus",
            name = "Vulcanus",
            subtitle = "Volcanic Ash & Molten Lava Planet",
            solarMultiplier = 2.5,
            dayNightCycleSec = 250,
            primaryResources = listOf("Calcite", "Tungsten Ore", "Lava", "Sulfur", "Coal"),
            exclusiveTechs = listOf("Foundry", "Metallurgic Science", "Tungsten Carbide", "Big Mining Drill"),
            environmentalHazards = "Patrolling territorial Demolishers. Intense heat; traditional furnaces are ineffective.",
            strategyTips = "Pump molten lava into Foundries to cast plates directly. Use Railguns to defeat Demolishers.",
            description = "A scorching volcanic landscape covered in lava rivers, sulfur pits, and massive Calcite deposits."
        ),
        PlanetInfo(
            id = "gleba",
            name = "Gleba",
            subtitle = "Fungal Swamp & Organic Ecosystem",
            solarMultiplier = 0.5,
            dayNightCycleSec = 600,
            primaryResources = listOf("Yumako Fruit", "Jellynut", "Bioflux", "Agricultural Science"),
            exclusiveTechs = listOf("Biochamber", "Agricultural Science", "Nutrient Processing", "Heating Tower"),
            environmentalHazards = "Perishable organic materials spoil over time. Hostile Pentapods attack when eggs mature.",
            strategyTips = "Process raw Yumako and Jellynut immediately before spoilage. Use Biochambers for organic loops.",
            description = "A dense, humid swamp world where all organic materials spoil over time into Spoilage."
        ),
        PlanetInfo(
            id = "fulgora",
            name = "Fulgora",
            subtitle = "Desolate Lightning & Scrap Ruins",
            solarMultiplier = 0.2,
            dayNightCycleSec = 180,
            primaryResources = listOf("Scrap", "Holmium Ore", "Heavy Oil", "Lightning Energy"),
            exclusiveTechs = listOf("Electromagnetic Plant", "Electromagnetic Science", "Superconductor", "Tesla Gun"),
            environmentalHazards = "Constant lightning strikes across mesa plateaus; no natural water bodies.",
            strategyTips = "Build Lightning Collectors to convert strikes directly into high-voltage energy. Recycle Scrap.",
            description = "A ruined ocean planet of oil and mesa islands swept by perpetual lightning storms."
        ),
        PlanetInfo(
            id = "aquilo",
            name = "Aquilo",
            subtitle = "Frozen Methane Ocean World",
            solarMultiplier = 0.01,
            dayNightCycleSec = 1200,
            primaryResources = listOf("Fluorine", "Ammoniacal Solution", "Lithium", "Quantum Processors"),
            exclusiveTechs = listOf("Cryogenic Science", "Fusion Reactor", "Promethium Science", "Quantum Computer"),
            environmentalHazards = "Extreme sub-zero freezing temperatures require heat pipes for all structures.",
            strategyTips = "Insulate all factory buildings with heat pipes. Solar panels are nearly useless; rely on Fusion.",
            description = "An ice-bound liquid methane ocean planet at the outer edge of the star system."
        ),
        PlanetInfo(
            id = "space-platform",
            name = "Space Platform",
            subtitle = "Interplanetary Orbital Transport",
            solarMultiplier = 1.0,
            dayNightCycleSec = 0,
            primaryResources = listOf("Metallic Asteroids", "Carbonic Asteroids", "Oxidizer Asteroids"),
            exclusiveTechs = listOf("Space Platform Hub", "Asteroid Collector", "Crusher", "Promethium Science"),
            environmentalHazards = "Asteroid field collisions during interplanetary transit.",
            strategyTips = "Use Asteroid Collectors and Crushers to harvest fuel and ammo directly from space debris.",
            description = "Mobile orbital factory platforms used for automated interplanetary transport and science."
        )
    )
}
