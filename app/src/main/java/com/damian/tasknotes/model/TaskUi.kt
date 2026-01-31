package com.damian.tasknotes.model

// Modelo simple para UI (lo persistimos como JSON en DataStore)
data class TaskUi(
    val id: Int,
    val title: String,
    val description: String,
    val done: Boolean
)

// Filtro opcional para la lista (si lo usás en la UI)
enum class TaskFilter {
    ALL,
    PENDING,
    DONE
}