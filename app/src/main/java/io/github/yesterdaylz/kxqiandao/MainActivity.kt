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
import io.github.yesterdaylz.kxqiandao.ui.theme.KexieTheme
import io.github.yesterdaylz.kxqiandao.ui.theme.KxTheme
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
                val colors = KxTheme.colors
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = colors.background,
                    contentColor = colors.onSurface
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