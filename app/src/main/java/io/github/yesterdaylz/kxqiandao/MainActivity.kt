package io.github.yesterdaylz.kxqiandao

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.github.yesterdaylz.kxqiandao.ui.screen.MainScreen
import io.github.yesterdaylz.kxqiandao.ui.theme.BackgroundGray
import io.github.yesterdaylz.kxqiandao.ui.theme.KXqiandaoTheme
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