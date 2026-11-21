package com.factoria.ui.checklist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.factoria.core.designsystem.*

@Composable
fun ChecklistScreen(
    viewModel: ChecklistViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val tasks by viewModel.tasks.collectAsState()

    val planets = listOf("All", "Nauvis", "Vulcanus", "Gleba", "Fulgora", "Aquilo", "Space Platform")
    val planetOptions = listOf("Nauvis", "Vulcanus", "Gleba", "Fulgora", "Aquilo", "Space Platform")

    val filteredTasks = tasks.filter { task ->
        uiState.selectedPlanet == "All" || task.planet.equals(uiState.selectedPlanet, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Add Task Card
        FactoriaCard(borderColor = CircuitGreen) {
            Text(
                text = "ADD FACTORY TASK / OUTPOST TODO",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = CircuitGreen,
                    letterSpacing = 1.2.sp
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = uiState.newTaskTitle,
                onValueChange = { viewModel.setNewTaskTitle(it) },
                label = { Text("Task Description (e.g., 'Build 50/s Red Circuits')", color = TextSecondary) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CircuitGreen,
                    unfocusedBorderColor = SlateBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(planetOptions) { planet ->
                        FilterChip(
                            selected = uiState.newTaskPlanet == planet,
                            onClick = { viewModel.setNewTaskPlanet(planet) },
                            label = { Text(planet, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FactorioOrange,
                                selectedLabelColor = Color.Black,
                                containerColor = SlateSurfaceVariant,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { viewModel.addTask() },
                    colors = ButtonDefaults.buttonColors(containerColor = CircuitGreen)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ADD", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Filter Pills
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(planets) { planet ->
                FilterChip(
                    selected = uiState.selectedPlanet == planet,
                    onClick = { viewModel.selectPlanetFilter(planet) },
                    label = { Text(planet, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = IndustrialYellow,
                        selectedLabelColor = Color.Black,
                        containerColor = SlateSurface,
                        labelColor = TextSecondary
                    )
                )
            }
        }

        // Tasks List
        if (filteredTasks.isEmpty()) {
            FactoriaCard {
                Text(
                    text = "No checklist tasks found. Add a task above to plan your factory goals offline!",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredTasks, key = { it.id }) { task ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClick { viewModel.toggleTask(task.id, task.is_completed) },
                        shape = RoundedCornerShape(6.dp),
                        color = SlateSurface,
                        border = BorderStroke(1.dp, if (task.is_completed) CircuitGreen.copy(alpha = 0.4f) else SlateBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Checkbox(
                                    checked = task.is_completed,
                                    onCheckedChange = { viewModel.toggleTask(task.id, task.is_completed) },
                                    colors = CheckboxDefaults.colors(checkedColor = CircuitGreen)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = task.title,
                                        color = if (task.is_completed) TextSecondary else TextPrimary,
                                        textDecoration = if (task.is_completed) TextDecoration.LineThrough else TextDecoration.None,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Surface(
                                        color = IndustrialYellow.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = task.planet.uppercase(),
                                            color = IndustrialYellow,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            IconButton(onClick = { viewModel.deleteTask(task.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = CircuitRed)
                            }
                        }
                    }
                }
            }
        }
    }
}
