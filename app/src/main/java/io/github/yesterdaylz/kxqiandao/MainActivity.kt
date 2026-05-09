package io.github.yesterdaylz.kxqiandao

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import io.github.yesterdaylz.kxqiandao.ui.screen.MainScreen
import io.github.yesterdaylz.kxqiandao.ui.theme.BackgroundGray
import io.github.yesterdaylz.kxqiandao.ui.theme.KexieTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KexieTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BackgroundGray
                ) {
                    MainScreen()
                }
            }
        }
    }
}