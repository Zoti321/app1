package com.example.mynativeapp1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mynativeapp1.ui.theme.MyNativeApp1Theme
import com.example.mynativeapp1.ui.weather.WeatherScreen
import com.example.mynativeapp1.ui.weather.WeatherViewModel
import com.example.mynativeapp1.ui.weather.WeatherViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyNativeApp1Theme {
                val viewModel: WeatherViewModel = viewModel(
                    factory = WeatherViewModelFactory(applicationContext),
                )
                WeatherScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
