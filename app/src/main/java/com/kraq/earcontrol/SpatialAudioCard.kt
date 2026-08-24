package com.kraq.earcontrol

import android.content.Context
import android.media.AudioManager
import android.media.Spatializer
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headset
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Estado del audio espacial del dispositivo.
 * `available` = el hardware/SO soporta Spatializer (API 32+, requiere audífonos compatibles).
 * `enabled` = está activo ahora mismo.
 * `headTrackerAvailable` = si además hay head-tracking real reportado por el sistema.
 */
data class SpatialAudioState(
    val available: Boolean,
    val enabled: Boolean,
    val headTrackerAvailable: Boolean,
)

fun readSpatialAudioState(context: Context): SpatialAudioState {
    if (Build.VERSION.SDK_INT < 32) {
        return SpatialAudioState(available = false, enabled = false, headTrackerAvailable = false)
    }
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val spatializer: Spatializer = audioManager.spatializer
    val available = spatializer.immersiveAudioLevel != Spatializer.SPATIALIZER_IMMERSIVE_LEVEL_NONE
    val enabled = spatializer.isEnabled
    val headTracker = spatializer.isHeadTrackerAvailable
    return SpatialAudioState(available = available, enabled = enabled, headTrackerAvailable = headTracker)
}

private val Ink = Color(0xFF101010)
private val Muted = Color(0xFF777773)
private val Card = Color.White

@Composable
fun SpatialAudioCard(
    spatialState: SpatialAudioState,
    balance: Float, // -1f (izquierda) .. 1f (derecha), el mismo valor que ya usas en BalanceCard
) {
    // Convertimos el balance en un ángulo simulado alrededor de la "cabeza" (0 = centro/frente).
    // Esto no es head-tracking real: es una representación visual de dónde "vive" el audio ahora mismo.
    val targetAngle = balance * 70f // grados, limitado para que no dé toda la vuelta
    val animatedAngle by animateFloatAsState(
        targetValue = targetAngle,
        animationSpec = tween(durationMillis = 350),
        label = "spatial-angle",
    )

    Surface(color = Card, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = Ink)
                Spacer(Modifier.size(10.dp))
                Text("Spatial audio", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.weight(1f))
                StatusPill(spatialState)
            }

            Spacer(Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                contentAlignment = Alignment.Center,
            ) {
                SpatialPositionIndicator(angleDegrees = animatedAngle)
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = spatialDescription(spatialState),
                color = Muted,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun StatusPill(state: SpatialAudioState) {
    val (label, bg, fg) = when {
        !state.available -> Triple("Not supported", Color(0xFFF1F1EC), Muted)
        state.enabled -> Triple("Active", Color(0xFFE3F7E9), Color(0xFF1E8A4C))
        else -> Triple("Available", Color(0xFFF1F1EC), Muted)
    }
    Surface(color = bg, shape = RoundedCornerShape(50)) {
        Text(
            text = label,
            color = fg,
            fontSize = 11.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

private fun spatialDescription(state: SpatialAudioState): String = when {
    !state.available -> "This device or your earbuds don't report spatial audio support."
    state.headTrackerAvailable && state.enabled -> "Head tracking is active. The dot below reflects your current L/R balance."
    state.enabled -> "Spatial audio is on, without head tracking. Position reflects L/R balance only."
    else -> "Spatial audio is supported but currently off."
}

/**
 * Indicador visual: una "cabeza" central fija y un punto que orbita según el ángulo,
 * más un arco sutil que muestra el rango de movimiento. Puramente representativo.
 */
@Composable
private fun SpatialPositionIndicator(angleDegrees: Float) {
    val headColor = Ink
    val trackColor = Color(0xFFE6E6E1)
    val dotColor = Color(0xFF3B82F6)

    Canvas(modifier = Modifier.size(140.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2.4f

        // Arco de rango de movimiento
        drawCircle(
            color = trackColor,
            radius = radius,
            center = center,
            style = Stroke(width = 3.dp.toPx()),
        )

        // Punto de sonido, orbitando (angleDegrees: -70=izq, 0=centro/arriba, 70=der)
        val radians = Math.toRadians((angleDegrees - 90).toDouble())
        val dotX = center.x + radius * cos(radians).toFloat()
        val dotY = center.y + radius * sin(radians).toFloat()
        drawCircle(color = dotColor, radius = 9.dp.toPx(), center = Offset(dotX, dotY))

        // "Cabeza" central
        drawCircle(color = headColor, radius = 20.dp.toPx(), center = center)
    }

    Icon(
        imageVector = Icons.Rounded.Headset,
        contentDescription = null,
        tint = Color.White,
        modifier = Modifier.size(20.dp),
    )
}
