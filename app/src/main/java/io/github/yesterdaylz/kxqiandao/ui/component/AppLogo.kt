package io.github.yesterdaylz.kxqiandao.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import io.github.yesterdaylz.kxqiandao.R
import kotlinx.coroutines.launch

/**
 * 顶栏 logo
 */
@Composable
fun AppLogo(onDoubleTap: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val rotation = remember { Animatable(0f) }

    AsyncImage(
        model = R.drawable.logo,
        contentDescription = "Logo",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(50.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDoubleTap()
                        scope.launch {
                            rotation.animateTo(rotation.value + 360f, tween(600))
                        }
                    }
                )
            }
            .rotate(rotation.value)
            .clip(RoundedCornerShape(10.dp))
    )
}
