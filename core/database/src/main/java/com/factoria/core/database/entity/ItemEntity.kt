package com.factoria.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.factoria.core.model.Item

@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val localized_name: String,
    val item_group: String,
    val subgroup: String,
    val stack_size: Int,
    val fuel_value: Double? = 0.0,
    val fuel_category: String? = null,
    val fuel_emissions: Double? = 1.0,
    val spoilage_time: Double? = 0.0,
    val icon_path: String? = null
) {
    fun toDomainModel(): Item = Item(
        id = id,
        name = name,
        localizedName = localized_name,
        itemGroup = item_group,
        subgroup = subgroup,
        stackSize = stack_size,
        fuelValueJoules = fuel_value ?: 0.0,
        fuelCategory = fuel_category,
        fuelEmissionsMultiplier = fuel_emissions ?: 1.0,
        spoilageTimeSeconds = spoilage_time ?: 0.0,
        iconPath = icon_path
    )
}
