package com.factoria.ui.logistics

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.factoria.core.designsystem.*
import com.factoria.core.model.*
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max

@Composable
fun LogisticsScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Belts & Inserters", "Power & Nuclear", "Beacons & Modules", "Belt Balancers", "Thermal Steam Buffer", "Fluids", "Trains & Signals", "Quality Matrix", "Space Age Cargo", "Gleba Spoilage")
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateBackground)
    ) {
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = SlateSurface,
            contentColor = FactorioOrange,
            edgePadding = 8.dp,
            divider = { HorizontalDivider(color = SlateBorder) }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, maxLines = 1, fontSize = 12.sp) }
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (selectedTab) {
                0 -> BeltsAndInsertersCalculator()
                1 -> PowerCalculators()
                2 -> BeaconsAndModulesCalculator()
                3 -> BeltBalancersGuide()
                4 -> ThermalSteamStorageCalculator()
                5 -> FluidCalculator()
                6 -> TrainCalculator()
                7 -> QualityMatrixCalculator()
                8 -> SpaceAgeCargoCalculator()
                9 -> GlebaSpoilageCalculator()
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun BeltsAndInsertersCalculator() {
    var selectedBeltIndex by remember { mutableIntStateOf(0) }
    val beltTiers = StandardBeltTiers.all

    FactoriaCard(borderColor = FactorioOrange) {
        Text("BELT THROUGHPUT", fontWeight = FontWeight.Bold, color = FactorioOrange)
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            beltTiers.forEachIndexed { idx, tier ->
                FilterChip(
                    selected = selectedBeltIndex == idx,
                    onClick = { selectedBeltIndex = idx },
                    label = { Text(tier.name.split(" ")[0]) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(android.graphics.Color.parseColor(tier.colorHex)),
                        selectedLabelColor = Color.Black,
                        containerColor = SlateSurfaceVariant,
                        labelColor = TextPrimary
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        val currentBelt = beltTiers[selectedBeltIndex]
        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard("Total Speed", "${currentBelt.itemsPerSecond.toInt()}", "items/s", FactorioOrange, Modifier.weight(1f))
            MetricCard("Per Lane", "${currentBelt.laneItemsPerSecond}", "items/s", IndustrialYellow, Modifier.weight(1f))
            MetricCard("Per Minute", "${currentBelt.itemsPerMinute.toInt()}", "items/m", CircuitBlue, Modifier.weight(1f))
        }
    }

    var inserterStackSize by remember { mutableIntStateOf(12) }
    FactoriaCard {
        Text("INSERTER THROUGHPUT ESTIMATES", fontWeight = FontWeight.Bold, color = IndustrialYellow)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Based on Factorio 2.0 rotation speeds and stack capacity bonus",
            color = TextSecondary,
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(8.dp))

        Text("Hand Stack Size Bonus: $inserterStackSize", color = TextPrimary)
        Slider(
            value = inserterStackSize.toFloat(),
            onValueChange = { inserterStackSize = it.toInt() },
            valueRange = 1f..16f,
            steps = 14,
            colors = SliderDefaults.colors(thumbColor = FactorioOrange, activeTrackColor = FactorioOrange)
        )

        Spacer(modifier = Modifier.height(8.dp))
        val inserters = listOf(
            Triple("Fast Inserter (Chest-to-Chest)", 2.31 * minOf(inserterStackSize, 3), "180° swing"),
            Triple("Fast Inserter (Chest-to-Belt)", 1.7 * minOf(inserterStackSize, 3), "Drop penalty"),
            Triple("Bulk Inserter (2.0) (Chest-to-Chest)", 2.31 * inserterStackSize, "Full stack"),
            Triple("Bulk Inserter (2.0) (Chest-to-Belt)", 1.9 * inserterStackSize, "Full stack to belt"),
            Triple("Stack Inserter (Space Age)", 2.5 * minOf(inserterStackSize + 4, 16), "High tier")
        )

        inserters.forEach { (name, rate, note) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(name, color = TextPrimary, fontSize = 13.sp)
                    Text(note, color = TextSecondary, fontSize = 11.sp)
                }
                Text(
                    String.format(Locale.US, "%.1f /s", rate),
                    color = CircuitGreen,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
            HorizontalDivider(color = SlateBorder.copy(alpha = 0.5f))
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Info, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                "Approximation: Real throughput varies based on belt fullness and turn angle.",
                color = TextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun BeaconsAndModulesCalculator() {
    var selectedMachineIndex by remember { mutableIntStateOf(0) }
    val machines = listOf(
        Triple("Assembler 3", 1.25, 375.0),
        Triple("Foundry", 4.0, 2500.0),
        Triple("Electromagnetic Plant", 2.0, 2000.0),
        Triple("Biochamber", 2.0, 500.0),
        Triple("Electric Furnace", 2.0, 180.0)
    )

    var moduleCount by remember { mutableIntStateOf(4) }
    var beaconCount by remember { mutableIntStateOf(8) }

    FactoriaCard(borderColor = QualityEpicColor) {
        Text("BEACON & MODULE IMPACT CALCULATOR", fontWeight = FontWeight.Bold, color = QualityEpicColor)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Calculate net crafting speed, productivity %, and energy demand with surround beacons", color = TextSecondary, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(12.dp))

        Text("Select Machine", color = IndustrialYellow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            machines.forEachIndexed { idx, (mName, _, _) ->
                FilterChip(
                    selected = selectedMachineIndex == idx,
                    onClick = { selectedMachineIndex = idx },
                    label = { Text(mName, fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = QualityEpicColor,
                        selectedLabelColor = Color.Black,
                        containerColor = SlateSurfaceVariant,
                        labelColor = TextPrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("Internal Modules (Slot Count: $moduleCount)", color = IndustrialYellow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Slider(
            value = moduleCount.toFloat(),
            onValueChange = { moduleCount = it.toInt() },
            valueRange = 0f..8f,
            steps = 7,
            colors = SliderDefaults.colors(thumbColor = QualityEpicColor, activeTrackColor = QualityEpicColor)
        )

        Text("Surrounding Beacons (Count: $beaconCount)", color = IndustrialYellow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Slider(
            value = beaconCount.toFloat(),
            onValueChange = { beaconCount = it.toInt() },
            valueRange = 0f..16f,
            steps = 15,
            colors = SliderDefaults.colors(thumbColor = FactorioOrange, activeTrackColor = FactorioOrange)
        )

        val (mName, baseSpeed, baseKW) = machines[selectedMachineIndex]

        val beaconSpeedEffect = beaconCount * 0.25
        val internalSpeedEffect = moduleCount * 0.50
        val internalProdEffect = moduleCount * 0.10
        val internalEnergyEffect = moduleCount * 0.70

        val netSpeedMult = max(0.2, 1.0 + internalSpeedEffect + beaconSpeedEffect)
        val finalSpeed = baseSpeed * netSpeedMult
        val netEnergyMult = max(0.2, 1.0 + internalEnergyEffect + (beaconCount * 0.15))
        val finalkW = baseKW * netEnergyMult

        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard("Crafting Speed", String.format(Locale.US, "%.2fx", finalSpeed), "", FactorioOrange, Modifier.weight(1f))
            MetricCard("Productivity", "+${(internalProdEffect * 100).toInt()}%", "", CircuitGreen, Modifier.weight(1f))
            MetricCard("Energy Draw", if (finalkW >= 1000) String.format(Locale.US, "%.2f MW", finalkW / 1000.0) else "${finalkW.toInt()} kW", "", IndustrialYellow, Modifier.weight(1f))
        }
    }
}

@Composable
fun BeltBalancersGuide() {
    FactoriaCard(borderColor = IndustrialYellow) {
        Text("BELT BALANCERS & JUNCTIONS GUIDE", fontWeight = FontWeight.Bold, color = IndustrialYellow)
        Spacer(modifier = Modifier.height(6.dp))
        Text("Standard throughput-unbound belt balancers evenly distribute item flow across lanes.", color = TextSecondary, fontSize = 12.sp)

        Spacer(modifier = Modifier.height(10.dp))
        Text("• 2-to-2 Balancer: Single splitter connecting 2 parallel belts.", color = TextPrimary, fontSize = 12.sp)
        Text("• 4-to-4 Balancer: 4 Splitters arranged in 2 lines with 2 crisscross underground belts.", color = TextPrimary, fontSize = 12.sp)
        Text("• 8-to-8 Balancer: 12 Splitters arranged in 3 stages for full lane throughput independence.", color = TextPrimary, fontSize = 12.sp)
        Text("• Side-Loading Rule: Side-loading onto a belt loads items exclusively onto that specific lane.", color = CircuitBlue, fontSize = 12.sp)
    }
}

@Composable
fun ThermalSteamStorageCalculator() {
    var targetStoredGJ by remember { mutableStateOf("10") }

    FactoriaCard(borderColor = CircuitBlue) {
        Text("THERMAL & STEAM POWER STORAGE CALCULATOR", fontWeight = FontWeight.Bold, color = CircuitBlue)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Calculate storage tanks required to buffer 500°C nuclear vs 165°C boiler steam", color = TextSecondary, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = targetStoredGJ,
            onValueChange = { targetStoredGJ = it },
            label = { Text("Target Stored Energy (Gigajoules - GJ)", color = TextSecondary) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CircuitBlue,
                unfocusedBorderColor = SlateBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        val desiredGJ = max(0.1, targetStoredGJ.toDoubleOrNull() ?: 10.0)

        val tanksNuclear = ceil(desiredGJ / 2.425).toInt()
        val tanksBoiler = ceil(desiredGJ / 0.750).toInt()

        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard("500°C Nuclear Tanks", "$tanksNuclear", "tanks", CircuitGreen, Modifier.weight(1f))
            MetricCard("165°C Boiler Tanks", "$tanksBoiler", "tanks", IndustrialYellow, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("• 1 Storage Tank holds 25,000 steam units.", color = TextSecondary, fontSize = 12.sp)
        Text("• 500°C Nuclear Steam Tank Capacity = 2.425 Gigajoules (GJ) of potential power.", color = CircuitGreen, fontSize = 12.sp)
    }
}

@Composable
fun PowerCalculators() {
    var steamTargetMW by remember { mutableStateOf("36") }

    FactoriaCard(borderColor = FactorioOrange) {
        Text("STEAM POWER (1:20:40 RATIO)", fontWeight = FontWeight.Bold, color = FactorioOrange)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = steamTargetMW,
            onValueChange = { steamTargetMW = it },
            label = { Text("Desired Output (MW)", color = TextSecondary) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = FactorioOrange,
                unfocusedBorderColor = SlateBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        val mw = steamTargetMW.toDoubleOrNull() ?: 36.0
        val engines = ceil(mw / 0.9).toInt()
        val boilers = ceil(engines / 2.0).toInt()
        val pumps = ceil(boilers / 20.0).toInt()
        val coalPerSec = boilers * 1.8

        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard("Boilers", "$boilers", "", FactorioOrange, Modifier.weight(1f))
            MetricCard("Engines", "$engines", "", IndustrialYellow, Modifier.weight(1f))
            MetricCard("Offshore", "$pumps", "", CircuitBlue, Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("Fuel Demand (Coal): ${String.format(Locale.US, "%.1f", coalPerSec)} coal/s (${String.format(Locale.US, "%.1f", coalPerSec * 60)}/min)", color = TextSecondary)
    }

    var reactorRows by remember { mutableStateOf("2") }
    var reactorCols by remember { mutableStateOf("2") }

    FactoriaCard(borderColor = CircuitGreen) {
        Text("DYNAMIC NxM NUCLEAR REACTOR MATRIX", fontWeight = FontWeight.Bold, color = CircuitGreen)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Calculates neighbor adjacency bonuses (+100% / +200%), exchangers & fuel cell burn rates", color = TextSecondary, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = reactorRows,
                onValueChange = { reactorRows = it },
                label = { Text("Reactor Rows (N)", color = TextSecondary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CircuitGreen,
                    unfocusedBorderColor = SlateBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
            OutlinedTextField(
                value = reactorCols,
                onValueChange = { reactorCols = it },
                label = { Text("Reactor Columns (M)", color = TextSecondary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CircuitGreen,
                    unfocusedBorderColor = SlateBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
        }

        val rows = max(1, reactorRows.toIntOrNull() ?: 2)
        val cols = max(1, reactorCols.toIntOrNull() ?: 2)
        val totalReactors = rows * cols

        val totalMW = if (rows == 1 && cols == 1) {
            40.0
        } else if (rows == 1 || cols == 1) {
            val length = max(rows, cols)
            2 * 80.0 + max(0, length - 2) * 120.0
        } else {
            val corners = 4
            val edges = 2 * (rows - 2) + 2 * (cols - 2)
            val interior = (rows - 2) * (cols - 2)
            corners * 120.0 + edges * 160.0 + interior * 200.0
        }

        val exchangers = ceil(totalMW / 10.0).toInt()
        val turbines = ceil(totalMW / 5.82).toInt()
        val waterReq = exchangers * 103.0
        val offshorePumps = ceil(waterReq / 1200.0).toInt()
        val fuelCellsPerHour = (totalReactors * 3600) / 200

        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard("Output", "${totalMW.toInt()}", "MW", CircuitGreen, Modifier.weight(1f))
            MetricCard("Exchangers", "$exchangers", "", FactorioOrange, Modifier.weight(1f))
            MetricCard("Turbines", "$turbines", "", IndustrialYellow, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("• Water Demand: ${waterReq.toInt()} units/s ($offshorePumps Offshore Pumps)", color = CircuitBlue, fontSize = 12.sp)
        Text("• Fuel Consumption: $fuelCellsPerHour Uranium Fuel Cells / hour ($totalReactors reactors)", color = TextSecondary, fontSize = 12.sp)
    }

    var targetSolarMW by remember { mutableStateOf("100") }
    var selectedPlanet by remember { mutableStateOf("Nauvis") }
    val planetMultipliers = mapOf(
        "Nauvis" to 1.0,
        "Vulcanus" to 2.5,
        "Fulgora" to 0.2,
        "Gleba" to 0.5,
        "Aquilo" to 0.01
    )

    FactoriaCard(borderColor = IndustrialYellow) {
        Text("SOLAR & ACCUMULATOR RATIO", fontWeight = FontWeight.Bold, color = IndustrialYellow)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            planetMultipliers.keys.forEach { planet ->
                FilterChip(
                    selected = selectedPlanet == planet,
                    onClick = { selectedPlanet = planet },
                    label = { Text(planet, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = IndustrialYellow,
                        selectedLabelColor = Color.Black,
                        containerColor = SlateSurfaceVariant,
                        labelColor = TextPrimary
                    )
                )
            }
        }

        OutlinedTextField(
            value = targetSolarMW,
            onValueChange = { targetSolarMW = it },
            label = { Text("Desired Sustained Power (MW)", color = TextSecondary) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = IndustrialYellow,
                unfocusedBorderColor = SlateBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        val desiredMW = targetSolarMW.toDoubleOrNull() ?: 100.0
        val solarMult = planetMultipliers[selectedPlanet] ?: 1.0
        val avgPanelKW = 42.0 * solarMult
        val panels = if (avgPanelKW > 0) ceil((desiredMW * 1000.0) / avgPanelKW).toLong() else 0L
        val accumulators = (panels * 0.84).toLong()

        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard("Solar Panels", "$panels", "", IndustrialYellow, Modifier.weight(1f))
            MetricCard("Accumulators", "$accumulators", "", CircuitBlue, Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text("Accumulator / Solar Panel ratio: 0.84 (Nauvis Standard)", color = TextSecondary, fontSize = 11.sp)
    }
}

@Composable
fun FluidCalculator() {
    FactoriaCard(borderColor = CircuitBlue) {
        Text("FACTORIO 2.0 FLUID MECHANICS", fontWeight = FontWeight.Bold, color = CircuitBlue)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Factorio 2.0 completely redesigned fluid physics into a network-based simulation. " +
            "Fluid flows instantaneously through contiguous pipe segments up to high capacities.",
            color = TextPrimary,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text("KEY CONSTANTS & RULES:", fontWeight = FontWeight.Bold, color = IndustrialYellow)
        Text("• Pump Throughput: 1200 units/second max", color = TextPrimary)
        Text("• Offshore Pump Output: 1200 water/second", color = TextPrimary)
        Text("• Storage Tank Capacity: 25,000 units", color = TextPrimary)
        Text("• Pipe Segment Rule: Keep segments within continuous lengths or use pumps to reset pressure bottlenecks.", color = TextSecondary, fontSize = 12.sp)
    }
}

@Composable
fun TrainCalculator() {
    var locomotives by remember { mutableStateOf("1") }
    var wagons by remember { mutableStateOf("4") }
    var stackSize by remember { mutableStateOf("50") }

    FactoriaCard(borderColor = FactorioOrange) {
        Text("TRAIN CARGO & THROUGHPUT", fontWeight = FontWeight.Bold, color = FactorioOrange)
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = locomotives,
                onValueChange = { locomotives = it },
                label = { Text("Locos", color = TextSecondary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FactorioOrange,
                    unfocusedBorderColor = SlateBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
            OutlinedTextField(
                value = wagons,
                onValueChange = { wagons = it },
                label = { Text("Cargo Wagons", color = TextSecondary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FactorioOrange,
                    unfocusedBorderColor = SlateBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
            OutlinedTextField(
                value = stackSize,
                onValueChange = { stackSize = it },
                label = { Text("Stack Size", color = TextSecondary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FactorioOrange,
                    unfocusedBorderColor = SlateBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
        }

        val wCount = max(0, wagons.toIntOrNull() ?: 4)
        val sSize = max(1, stackSize.toIntOrNull() ?: 50)
        val totalItems = wCount * 40 * sSize
        val totalFluids = wCount * 25000

        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard("Item Capacity", "$totalItems", "items", FactorioOrange, Modifier.weight(1f))
            MetricCard("Fluid Capacity", "${totalFluids / 1000}k", "units", CircuitBlue, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("Unloading Time (12 Inserters/Wagon):", fontWeight = FontWeight.Bold, color = IndustrialYellow)
        val stackUnloadTime = if (wCount > 0 && totalItems > 0) totalItems / (wCount * 12 * 27.7) else 0.0
        Text("• Bulk/Stack Inserters: ~${String.format(Locale.US, "%.1f", stackUnloadTime)}s to active chest buffer", color = TextSecondary, fontSize = 12.sp)
    }

    FactoriaCard(borderColor = CircuitGreen) {
        Text("RAILWAY SIGNALS & INTERSECTION RULES", fontWeight = FontWeight.Bold, color = CircuitGreen)
        Spacer(modifier = Modifier.height(6.dp))
        Text("• Rail Signal (Block Protection): Place at the EXIT of intersections to protect clear blocks ahead.", color = TextPrimary, fontSize = 12.sp)
        Text("• Chain Signal (Deadlock Prevention): Place BEFORE entering intersections so trains stop before blocking crossing tracks.", color = TextPrimary, fontSize = 12.sp)
        Text("• Golden Rule: 'Chain in, Rail out'. Never place a Rail Signal inside an intersection block.", color = IndustrialYellow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun QualityMatrixCalculator() {
    var baseCraftingSpeed by remember { mutableStateOf("1.25") }
    var baseEnergykW by remember { mutableStateOf("375") }

    FactoriaCard(borderColor = QualityEpicColor) {
        Text("FACTORIO 2.0 QUALITY TIER MATRIX", fontWeight = FontWeight.Bold, color = QualityEpicColor)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Side-by-side scaling multipliers for crafting speed, energy, and equipment stats", color = TextSecondary, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = baseCraftingSpeed,
                onValueChange = { baseCraftingSpeed = it },
                label = { Text("Base Crafting Speed", color = TextSecondary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = QualityEpicColor,
                    unfocusedBorderColor = SlateBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
            OutlinedTextField(
                value = baseEnergykW,
                onValueChange = { baseEnergykW = it },
                label = { Text("Base Energy (kW)", color = TextSecondary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = QualityEpicColor,
                    unfocusedBorderColor = SlateBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
        }

        val baseSpeed = baseCraftingSpeed.toDoubleOrNull() ?: 1.25
        val basekW = baseEnergykW.toDoubleOrNull() ?: 375.0

        val tiers = listOf(
            Triple(QualityTier.NORMAL, 1.0, "Base"),
            Triple(QualityTier.UNCOMMON, 1.3, "+30% Boost"),
            Triple(QualityTier.RARE, 1.6, "+60% Boost"),
            Triple(QualityTier.EPIC, 1.9, "+90% Boost"),
            Triple(QualityTier.LEGENDARY, 2.5, "+150% Boost")
        )

        Spacer(modifier = Modifier.height(12.dp))
        tiers.forEach { (tier, multiplier, label) ->
            val scaledSpeed = baseSpeed * multiplier
            val scaledEnergy = basekW * multiplier

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    QualityBadge(quality = tier)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(label, color = TextSecondary, fontSize = 11.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        String.format(Locale.US, "Speed: %.2fx", scaledSpeed),
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    )
                    Text(
                        String.format(Locale.US, "Power: %.0f kW", scaledEnergy),
                        color = CircuitGreen,
                        fontSize = 11.sp
                    )
                }
            }
            HorizontalDivider(color = SlateBorder.copy(alpha = 0.4f))
        }
    }
}

@Composable
fun SpaceAgeCargoCalculator() {
    var originPlanet by remember { mutableStateOf("Nauvis") }
    var destPlanet by remember { mutableStateOf("Vulcanus") }
    var itemQuantity by remember { mutableStateOf("5000") }
    var stackSize by remember { mutableStateOf("50") }

    val planets = listOf("Nauvis", "Vulcanus", "Gleba", "Fulgora", "Aquilo")

    FactoriaCard(borderColor = CircuitBlue) {
        Text("SPACE AGE CARGO ROCKET CALCULATOR", fontWeight = FontWeight.Bold, color = CircuitBlue)
        Spacer(modifier = Modifier.height(8.dp))

        Text("Origin Planet", color = IndustrialYellow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            planets.forEach { p ->
                FilterChip(
                    selected = originPlanet == p,
                    onClick = { originPlanet = p },
                    label = { Text(p, fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CircuitBlue,
                        selectedLabelColor = Color.Black,
                        containerColor = SlateSurfaceVariant,
                        labelColor = TextPrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text("Destination Planet", color = IndustrialYellow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            planets.forEach { p ->
                FilterChip(
                    selected = destPlanet == p,
                    onClick = { destPlanet = p },
                    label = { Text(p, fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FactorioOrange,
                        selectedLabelColor = Color.Black,
                        containerColor = SlateSurfaceVariant,
                        labelColor = TextPrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = itemQuantity,
                onValueChange = { itemQuantity = it },
                label = { Text("Total Items to Ship", color = TextSecondary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CircuitBlue,
                    unfocusedBorderColor = SlateBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
            OutlinedTextField(
                value = stackSize,
                onValueChange = { stackSize = it },
                label = { Text("Item Stack Size", color = TextSecondary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CircuitBlue,
                    unfocusedBorderColor = SlateBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
        }

        val totalItems = itemQuantity.toDoubleOrNull() ?: 5000.0
        val sSize = max(1.0, stackSize.toDoubleOrNull() ?: 50.0)

        val totalStacks = ceil(totalItems / sSize).toInt()
        val podsRequired = ceil(totalStacks / 20.0).toInt()
        val rocketLaunches = ceil(podsRequired / 5.0).toInt()
        val rocketFuelNeeded = rocketLaunches * 1000

        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard("Cargo Stacks", "$totalStacks", "stacks", FactorioOrange, Modifier.weight(1f))
            MetricCard("Rocket Pods", "$podsRequired", "pods", CircuitBlue, Modifier.weight(1f))
            MetricCard("Launches", "$rocketLaunches", "rockets", CircuitGreen, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("Estimated Rocket Fuel Required: $rocketFuelNeeded units", color = TextSecondary, fontSize = 12.sp)
    }
}

@Composable
fun GlebaSpoilageCalculator() {
    var transitTimeSeconds by remember { mutableStateOf("120") }
    var baseSpoilageMinutes by remember { mutableStateOf("5.0") }
    var initialQuantity by remember { mutableStateOf("1000") }

    FactoriaCard(borderColor = CircuitGreen) {
        Text("GLEBA SPOILAGE & FRESHNESS CALCULATOR", fontWeight = FontWeight.Bold, color = CircuitGreen)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Calculate freshness decay and net yield during planetary transport", color = TextSecondary, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = initialQuantity,
                onValueChange = { initialQuantity = it },
                label = { Text("Initial Quantity", color = TextSecondary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CircuitGreen,
                    unfocusedBorderColor = SlateBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
            OutlinedTextField(
                value = baseSpoilageMinutes,
                onValueChange = { baseSpoilageMinutes = it },
                label = { Text("Spoilage Time (Min)", color = TextSecondary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CircuitGreen,
                    unfocusedBorderColor = SlateBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
        }

        OutlinedTextField(
            value = transitTimeSeconds,
            onValueChange = { transitTimeSeconds = it },
            label = { Text("Transit / Elapsed Time (Seconds)", color = TextSecondary) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CircuitGreen,
                unfocusedBorderColor = SlateBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        val initQty = initialQuantity.toDoubleOrNull() ?: 1000.0
        val baseSec = max(1.0, (baseSpoilageMinutes.toDoubleOrNull() ?: 5.0) * 60.0)
        val elapsedSec = max(0.0, transitTimeSeconds.toDoubleOrNull() ?: 120.0)

        val freshnessFraction = max(0.0, 1.0 - (elapsedSec / baseSec))
        val freshnessPercent = freshnessFraction * 100.0
        val survivingYield = initQty * freshnessFraction
        val spoiledYield = initQty - survivingYield

        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard("Freshness", String.format(Locale.US, "%.1f%%", freshnessPercent), "", CircuitGreen, Modifier.weight(1f))
            MetricCard("Surviving", "${survivingYield.toInt()}", "items", FactorioOrange, Modifier.weight(1f))
            MetricCard("Spoiled", "${spoiledYield.toInt()}", "spoilage", IndustrialYellow, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(8.dp))
        val statusText = if (freshnessPercent > 50.0) {
            "Fresh: High potency yield for crafting."
        } else if (freshnessPercent > 0.0) {
            "Degrading: Transport quickly to prevent total spoilage."
        } else {
            "Spoiled: Entire batch converted to Spoilage."
        }
        Text("Status: $statusText", color = TextSecondary, fontSize = 12.sp)
    }
}
