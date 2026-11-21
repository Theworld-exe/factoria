package com.factoria.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.factoria.core.model.Fluid

@Entity(tableName = "fluids")
data class FluidEntity(
    @PrimaryKey val id: String,
    val name: String,
    val localized_name: String,
    val default_temp: Double,
    val max_temp: Double?,
    val heat_capacity: Double,
    val fuel_value: Double = 0.0,
    val emissions_multiplier: Double = 1.0,
    val icon_path: String? = null
) {
    fun toDomainModel(): Fluid = Fluid(
        id = id,
        name = name,
        localizedName = localized_name,
        defaultTemp = default_temp,
        maxTemp = max_temp,
        heatCapacityJoules = heat_capacity,
        fuelValueJoules = fuel_value,
        emissionsMultiplier = emissions_multiplier,
        iconPath = icon_path
    )
}
