package com.damian.tasknotes.ui.screens

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.damian.tasknotes.model.TaskFilter
import com.damian.tasknotes.model.TaskUi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    tasks: List<TaskUi>,
    onAddClick: () -> Unit,
    onToggleDone: (TaskUi) -> Unit,
    onDelete: (TaskUi) -> Unit,
    onEdit: (Int) -> Unit
) {
    // Tarea seleccionada para confirmar borrado en un AlertDialog
    var taskToDelete by remember { mutableStateOf<TaskUi?>(null) }

    // Filtro local para mostrar todas / pendientes / hechas
    var filter by remember { mutableStateOf(TaskFilter.ALL) }

    val pendingCount = tasks.count { !it.done }
    val pendingText = if (pendingCount == 1) "1 pendiente" else "$pendingCount pendientes"

    val filteredTasks = when (filter) {
        TaskFilter.ALL -> tasks
        TaskFilter.PENDING -> tasks.filter { !it.done }
        TaskFilter.DONE -> tasks.filter { it.done }
    }

    val pendingTasks = filteredTasks.filter { !it.done }.sortedBy { it.id }
    val doneTasks = filteredTasks.filter { it.done }.sortedBy { it.id }

    Scaffold(
        topBar = { TopAppBar(title = { Text("TaskNotes — $pendingText") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = "Agregar")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            item {
                FilterBar(
                    selected = filter,
                    onSelect = { filter = it }
                )
            }

            if (filter == TaskFilter.ALL) {
                if (pendingTasks.isEmpty() && doneTasks.isEmpty()) {
                    item { EmptyState(text = "No hay tareas todavía") }
                } else {
                    if (pendingTasks.isNotEmpty()) {
                        item { SectionHeader(title = "Pendientes") }
                        items(pendingTasks, key = { it.id }) { task ->
                            TaskRow(
                                task = task,
                                onToggleDone = { onToggleDone(task) },
                                onDeleteClick = { taskToDelete = task },
                                onEditClick = { onEdit(task.id) }
                            )
                        }
                    }

                    if (doneTasks.isNotEmpty()) {
                        item { SectionHeader(title = "Hechas") }
                        items(doneTasks, key = { it.id }) { task ->
                            TaskRow(
                                task = task,
                                onToggleDone = { onToggleDone(task) },
                                onDeleteClick = { taskToDelete = task },
                                onEditClick = { onEdit(task.id) }
                            )
                        }
                    }
                }
            } else {
                val listForFilter = pendingTasks + doneTasks
                if (listForFilter.isEmpty()) {
                    item {
                        val msg =
                            if (filter == TaskFilter.PENDING) "No hay tareas pendientes"
                            else "No hay tareas hechas"
                        EmptyState(text = msg)
                    }
                } else {
                    items(listForFilter, key = { it.id }) { task ->
                        TaskRow(
                            task = task,
                            onToggleDone = { onToggleDone(task) },
                            onDeleteClick = { taskToDelete = task },
                            onEditClick = { onEdit(task.id) }
                        )
                    }
                }
            }
        }
    }

    // Confirmación de borrado
    if (taskToDelete != null) {
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = { Text("Eliminar tarea") },
            text = { Text("¿Eliminar \"${taskToDelete!!.title}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete(taskToDelete!!)
                        taskToDelete = null
                    }
                ) { Text("Eliminar") }
            },
            dismissButton = {
                OutlinedButton(onClick = { taskToDelete = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp)
    )
    Divider()
}

@Composable
private fun EmptyState(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBar(
    selected: TaskFilter,
    onSelect: (TaskFilter) -> Unit
) {
    val options = listOf(
        TaskFilter.ALL to "Todas",
        TaskFilter.PENDING to "Pendientes",
        TaskFilter.DONE to "Hechas"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text("Filtro", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, (value, label) ->
                SegmentedButton(
                    selected = selected == value,
                    onClick = { onSelect(value) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
                ) {
                    Text(label)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Divider()
    }
}

@Composable
private fun TaskRow(
    task: TaskUi,
    onToggleDone: () -> Unit,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val rowAlpha = if (task.done) 0.6f else 1f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ✅ Checkbox NO abre editar
        Checkbox(
            checked = task.done,
            onCheckedChange = { onToggleDone() }
        )

        Spacer(modifier = Modifier.width(8.dp))

        // ✅ Solo esta zona abre editar (y usando overload compatible con tu Indication)
        Column(
            modifier = Modifier
                .weight(1f)
                .alpha(rowAlpha)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = LocalIndication.current
                ) { onEditClick() }
        ) {
            Text(
                text = task.title,
                textDecoration = if (task.done) TextDecoration.LineThrough else TextDecoration.None
            )

            if (task.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = task.description,
                    fontStyle = FontStyle.Italic,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            SuggestionChip(
                onClick = { /* solo visual */ },
                label = { Text(if (task.done) "HECHA" else "PENDIENTE") },
                enabled = false
            )
        }

        // ✅ Delete NO abre editar
        IconButton(onClick = onDeleteClick) {
            Icon(Icons.Default.Delete, contentDescription = "Eliminar")
        }
    }

    Divider()
}