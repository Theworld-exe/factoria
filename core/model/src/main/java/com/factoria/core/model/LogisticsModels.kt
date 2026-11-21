package com.factoria.core.model

import kotlinx.serialization.Serializable

@Serializable
data class BeltTier(
    val id: String,
    val name: String,
    val itemsPerSecond: Double,
    val itemsPerMinute: Double = itemsPerSecond * 60.0,
    val laneItemsPerSecond: Double = itemsPerSecond / 2.0,
    val colorHex: String
)

object StandardBeltTiers {
    val TransportBelt = BeltTier("transport-belt", "Transport Belt (Yellow)", 15.0, colorHex = "#EFD03B")
    val FastTransportBelt = BeltTier("fast-transport-belt", "Fast Transport Belt (Red)", 30.0, colorHex = "#D84315")
    val ExpressTransportBelt = BeltTier("express-transport-belt", "Express Transport Belt (Blue)", 45.0, colorHex = "#1E88E5")
    val TurboTransportBelt = BeltTier("turbo-transport-belt", "Turbo Transport Belt (Green)", 60.0, colorHex = "#43A047")

    val all = listOf(TransportBelt, FastTransportBelt, ExpressTransportBelt, TurboTransportBelt)
}

@Serializable
data class InserterThroughput(
    val inserterName: String,
    val source: String,
    val destination: String,
    val stackSize: Int,
    val itemsPerSecond: Double,
    val notes: String
)

@Serializable
data class SteamPowerCalculation(
    val targetPowerMW: Double,
    val offshorePumpsNeeded: Double,
    val boilersNeeded: Double,
    val steamEnginesNeeded: Double,
    val waterRequiredPerSec: Double,
    val coalFuelRequiredPerSec: Double
)

@Serializable
data class NuclearPowerCalculation(
    val reactorCount: Int,
    val layout: String, // e.g., "2x2", "2xN"
    val totalPowerOutputMW: Double,
    val heatExchangersNeeded: Int,
    val steamTurbinesNeeded: Int,
    val waterRequiredPerSec: Double,
    val fuelCellsPerHour: Double
)

@Serializable
data class SolarPowerCalculation(
    val desiredContinuousPowerMW: Double,
    val solarPanelsNeeded: Long,
    val accumulatorsNeeded: Long,
    val totalFootprintTiles: Long,
    val planetMultiplier: Double = 1.0
)

@Serializable
data class TrainCapacity(
    val cargoWagons: Int,
    val fluidWagons: Int,
    val itemStackSize: Int = 50,
    val totalItemCapacity: Int = cargoWagons * 40 * itemStackSize,
    val totalFluidCapacity: Double = fluidWagons * 25000.0
)
