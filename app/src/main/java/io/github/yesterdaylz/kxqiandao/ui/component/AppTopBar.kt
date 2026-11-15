package io.github.yesterdaylz.kxqiandao.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.yesterdaylz.kxqiandao.util.Constants

/**
 * 顶栏：LOGO + 标题 + 定时签退
 */
@Composable
fun AppTopBar(
    onLogoDoubleTap: () -> Unit,
    onScheduleSignOut: () -> Unit,
) {
    Column(modifier = Modifier.statusBarsPadding()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppLogo(onDoubleTap = onLogoDoubleTap)
                Spacer(modifier = Modifier.width(12.dp))
                Text("三院科协", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            if (Constants.ENABLE_SCHEDULED_SIGN_OUT) {
                IconButton(onClick = onScheduleSignOut) {
                    Icon(Icons.Default.Alarm, contentDescription = "定时签退")
                }
            }
        }
    }
}
