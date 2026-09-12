package com.neurophone.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.neurophone.app.ui.navigation.NeuroNavHost
import com.neurophone.app.ui.theme.NeuroBackground
import com.neurophone.app.ui.theme.NeuroPhoneTheme

class MainActivity : ComponentActivity() {

    private val viewModel: NeuroViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = application as NeuroPhoneApp
                return NeuroViewModel(app) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NeuroPhoneTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = NeuroBackground
                ) {
                    NeuroNavHost(viewModel = viewModel)
                }
            }
        }
    }
}