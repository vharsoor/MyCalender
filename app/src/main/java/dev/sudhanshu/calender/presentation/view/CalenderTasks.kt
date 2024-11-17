package dev.sudhanshu.calender.presentation.view

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.TopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import dagger.hilt.android.AndroidEntryPoint
import dev.sudhanshu.calender.presentation.view.ui.theme.CalenderTheme
import dev.sudhanshu.calender.presentation.view.ui.theme.Typography
import dev.sudhanshu.calender.util.SettingsPreferences

@AndroidEntryPoint
class CalenderTasks : ComponentActivity() {

    private lateinit var settingsPreferences: SettingsPreferences

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        settingsPreferences = SettingsPreferences.getInstance(this)


        setContent {
            CalenderTheme {
                SetStatusBarColor()

                Scaffold(topBar = {
                    TopAppBar(
                        title = { Text(text = "Your task") },
                        backgroundColor = MaterialTheme.colors.background,
                        modifier = Modifier.background(MaterialTheme.colors.background)
                    )
                },
                    modifier = Modifier.fillMaxSize().background(color = MaterialTheme.colors.background)) { innerPadding ->
                    innerPadding.calculateTopPadding()
                    TaskListScreen(userId = settingsPreferences.getUserId())
                }
            }
        }
    }

    @Composable
    private fun SetStatusBarColor(color: Color = MaterialTheme.colors.background) {
        window.statusBarColor = color.toArgb()
        val decorView = window.decorView
        if (color == Color.White || color == Color(0xFFE5FF7F)) {
            decorView.systemUiVisibility = decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        } else {
            decorView.systemUiVisibility = decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        }
    }
}

