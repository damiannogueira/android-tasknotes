package com.damian.tasknotes

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.damian.tasknotes.model.TaskUi
import com.damian.tasknotes.ui.screens.CreateTaskScreen
import com.damian.tasknotes.ui.screens.TaskListScreen
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first

/* ---------- DataStore (persistencia simple en JSON) ----------
Guardamos la lista completa como JSON para mantener la app simple.
*/
private const val PREFS_NAME = "tasknotes_prefs"
private const val TASKS_JSON = "tasks_json"

private val Context.dataStore by preferencesDataStore(name = PREFS_NAME)
private val TASKS_JSON_KEY = stringPreferencesKey(TASKS_JSON)

private fun tasksToJson(tasks: List<TaskUi>): String = Gson().toJson(tasks)

private fun jsonToTasks(json: String): List<TaskUi> {
    val type = object : TypeToken<List<TaskUi>>() {}.type
    return Gson().fromJson<List<TaskUi>>(json, type) ?: emptyList()
}

private suspend fun saveTasks(context: Context, tasks: List<TaskUi>) {
    context.dataStore.edit { prefs ->
        prefs[TASKS_JSON_KEY] = tasksToJson(tasks)
    }
}

private suspend fun loadTasks(context: Context): List<TaskUi> {
    val prefs = context.dataStore.data.first()
    val json = prefs[TASKS_JSON_KEY].orEmpty()
    if (json.isBlank()) return emptyList()

    return try {
        jsonToTasks(json)
    } catch (_: Exception) {
        // Si el JSON está corrupto, limpiamos y arrancamos vacío
        clearTasksStorage(context)
        emptyList()
    }
}

private suspend fun clearTasksStorage(context: Context) {
    context.dataStore.edit { prefs ->
        prefs.remove(TASKS_JSON_KEY)
    }
}

/* ---------- App + Navegación ---------- */

private const val ROUTE_LIST = "list"
private const val ROUTE_CREATE = "create"
private const val ROUTE_EDIT = "edit/{taskId}"

@Composable
fun TaskNotesApp() {
    val context = LocalContext.current
    val navController = rememberNavController()

    // Estado en memoria (se guarda automáticamente en DataStore)
    val tasks = remember { mutableStateListOf<TaskUi>() }
    var nextId by remember { mutableIntStateOf(1) }

    // Carga inicial: trae tareas guardadas y calcula el próximo id
    LaunchedEffect(Unit) {
        val loaded = loadTasks(context)
        tasks.clear()
        tasks.addAll(loaded)
        nextId = (tasks.maxOfOrNull { it.id } ?: 0) + 1
    }

    // Guardado automático: cada vez que cambia la lista, persistimos en DataStore
    LaunchedEffect(tasks.size, tasks.toList()) {
        saveTasks(context, tasks.toList())
    }

    NavHost(
        navController = navController,
        startDestination = ROUTE_LIST
    ) {
        composable(ROUTE_LIST) {
            TaskListScreen(
                tasks = tasks,
                onAddClick = { navController.navigate(ROUTE_CREATE) },
                onToggleDone = { task ->
                    val index = tasks.indexOfFirst { it.id == task.id }
                    if (index != -1) {
                        tasks[index] = task.copy(done = !task.done)
                    }
                },
                onDelete = { task ->
                    tasks.remove(task)
                },
                onEdit = { taskId ->
                    navController.navigate("edit/$taskId")
                }
            )
        }

        composable(ROUTE_CREATE) {
            CreateTaskScreen(
                onBack = { navController.popBackStack() },
                onSave = { title, description ->
                    tasks.add(
                        TaskUi(
                            id = nextId++,
                            title = title.trim(),
                            description = description.trim(),
                            done = false
                        )
                    )
                    navController.popBackStack()
                }
            )
        }

        composable(ROUTE_EDIT) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId")?.toIntOrNull()
            val task = tasks.firstOrNull { it.id == taskId }

            if (task == null) {
                // Si el id no existe, volvemos atrás
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                CreateTaskScreen(
                    onBack = { navController.popBackStack() },
                    initialTitle = task.title,
                    initialDescription = task.description,
                    topBarTitle = "Editar tarea",
                    onSave = { title, description ->
                        val index = tasks.indexOfFirst { it.id == task.id }
                        if (index != -1) {
                            tasks[index] = task.copy(
                                title = title.trim(),
                                description = description.trim()
                            )
                        }
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}