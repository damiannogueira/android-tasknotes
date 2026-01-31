package com.damian.tasknotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.damian.tasknotes.ui.theme.TaskNotesTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            // Tema Material 3 de la app
            TaskNotesTheme {
                TaskNotesApp()
            }
        }
    }
}