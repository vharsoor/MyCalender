package dev.sudhanshu.calender.presentation.view

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
// noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import dagger.hilt.android.AndroidEntryPoint
import dev.sudhanshu.calender.presentation.ui.theme.CalenderTheme
import kotlinx.coroutines.delay
import dev.sudhanshu.calender.R
import dev.sudhanshu.calender.util.SettingsPreferences
import kotlin.random.Random
import kotlin.random.nextInt
import android.util.Log

@AndroidEntryPoint
class Splash : ComponentActivity() {

    private var hasNavigatedToMainActivity = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        if (!SettingsPreferences.getInstance(this).isUserAvailable()) {
            val randomSixDigitNumber = Random.nextInt(100000, 1000000)
            SettingsPreferences.getInstance(this).setUserId(randomSixDigitNumber)
        }

        setContent {
            CalenderTheme {
                Scaffold(modifier = Modifier.background(color = MaterialTheme.colors.background)) { padding ->
                    padding.calculateTopPadding()
                    SplashScreen(onNavigation = {
                        val intent = Intent(this, MainActivity::class.java)
                        Log.d("CalendarIntegration", "Navigating to MainActivity")
                        if (!hasNavigatedToMainActivity) {
                            hasNavigatedToMainActivity = true
                            Log.d("CalendarIntegration", "Navigating to MainActivity II")
                            startActivity(intent)}
                    }){

                    }
                }
            }
        }
    }
}

@Composable
fun SplashScreen(
    onNavigation: () -> Unit,
    function: () -> Unit
) {

    val alphaAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        alphaAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000)
        )
        delay(1000)
    }

    onNavigation()


    Splash(alpha = alphaAnim.value)
}

@Composable
fun Splash(alpha: Float) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colors.background)
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = null,
            modifier = Modifier.alpha(alpha)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    Splash(alpha = 1f)
}
