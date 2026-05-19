package io.github.yesterdaylz.kxqiandao

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import io.github.yesterdaylz.kxqiandao.ui.screen.MainScreen
import io.github.yesterdaylz.kxqiandao.ui.theme.BackgroundGray
import io.github.yesterdaylz.kxqiandao.ui.theme.KexieTheme
import io.github.yesterdaylz.kxqiandao.util.PermissionUtil

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermission()
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

    //
    private fun requestNotificationPermission() {
        if (!PermissionUtil.hasNotificationPermission(this)) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}