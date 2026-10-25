package io.github.yesterdaylz.kxqiandao

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import io.github.yesterdaylz.kxqiandao.ui.screen.MainScreen
import io.github.yesterdaylz.kxqiandao.ui.theme.KexieTheme
import io.github.yesterdaylz.kxqiandao.ui.theme.KxTheme
import io.github.yesterdaylz.kxqiandao.ui.theme.ThemeMode
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
            val themeKey by KexieApplication.instance.preferencesManager.themeMode
                .collectAsState(initial = ThemeMode.System.key)
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (ThemeMode.fromKey(themeKey)) {
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
                ThemeMode.System -> systemDark
            }

            KexieTheme(darkTheme = darkTheme) {
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