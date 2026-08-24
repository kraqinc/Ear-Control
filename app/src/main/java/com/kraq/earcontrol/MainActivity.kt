package com.kraq.earcontrol

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VolumeDown
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

private val Bg = Color(0xFFF6F6F3)
private val Card = Color.White
private val Ink = Color(0xFF101010)
private val Muted = Color(0xFF777773)
private val Line = Color(0xFFE6E6E1)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { EarControlApp(this) }
    }
}

data class EarbudState(
    val model: String,
    val left: Int,
    val right: Int,
    val case: Int,
    val connected: Boolean,
)

@android.annotation.SuppressLint("MissingPermission")
private fun readPairedBluetooth(context: Context): EarbudState? {
    val adapter = BluetoothAdapter.getDefaultAdapter() ?: return null
    if (!adapter.isEnabled) return null
    if (Build.VERSION.SDK_INT >= 31 && context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return null

    val audio = adapter.bondedDevices.firstOrNull { device ->
        val uuids = device.uuids?.mapNotNull { it.uuid.toString() }.orEmpty()
        val name = device.name.orEmpty().lowercase()
        name.contains("buds") || name.contains("airpod") || name.contains("ear") || uuids.isNotEmpty()
    } ?: return null

    return EarbudState(
        model = audio.name ?: "Bluetooth Earbuds",
        left = -1,
        right = -1,
        case = -1,
        connected = true,
    )
}

private fun setMediaVolume(context: Context, fraction: Float) {
    val manager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val max = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    val value = (fraction.coerceIn(0f, 1f) * max).roundToInt()
    manager.setStreamVolume(AudioManager.STREAM_MUSIC, value, 0)
}

@android.annotation.SuppressLint("MissingPermission")
@androidx.compose.runtime.Composable
fun EarControlApp(context: Context) {
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 31) {
            permissionLauncher.launch(arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN))
        }
    }

    var earbud by remember { mutableStateOf<EarbudState?>(null) }
    var volume by remember { mutableFloatStateOf(0.68f) }
    var balance by remember { mutableFloatStateOf(0f) }
    var refreshKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshKey) {
        earbud = readPairedBluetooth(context)
    }

    MaterialTheme {
        Surface(color = Bg, modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp),
            ) {
                Spacer(Modifier.height(18.dp))
                Header(onRefresh = { refreshKey++ })
                Spacer(Modifier.height(22.dp))

                DeviceHero(earbud)
                Spacer(Modifier.height(14.dp))

                BatteryRow(earbud)
                Spacer(Modifier.height(14.dp))

                VolumeCard(
                    volume = volume,
                    onVolume = {
                        volume = it
                        setMediaVolume(context, it)
                    }
                )
                Spacer(Modifier.height(14.dp))

                BalanceCard(balance = balance, onBalance = { balance = it })
                Spacer(Modifier.height(14.dp))

                SmallActionRow()
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun Header(onRefresh: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text("EarControl", style = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif, color = Ink))
            Text("Your audio. Under control.", color = Muted, fontSize = 13.sp)
        }
        IconButton(onClick = onRefresh, modifier = Modifier.size(44.dp).clip(CircleShape).background(Card)) {
            Icon(Icons.Rounded.Refresh, contentDescription = "Refresh", tint = Ink)
        }
        Spacer(Modifier.size(8.dp))
        IconButton(onClick = {}, modifier = Modifier.size(44.dp).clip(CircleShape).background(Card)) {
            Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = Ink)
        }
    }
}

@androidx.compose.runtime.Composable
private fun DeviceHero(earbud: EarbudState?) {
    Surface(color = Ink, shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(22.dp), verticalAlignment = Alignment.CenterVertically) {
            EarbudGraphic(modifier = Modifier.size(128.dp))
            Spacer(Modifier.size(18.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(earbud?.model ?: "No earbuds selected", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 21.sp)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(if (earbud?.connected == true) Color(0xFF7CFF9D) else Color(0xFFFFB55A)))
                    Spacer(Modifier.size(7.dp))
                    Text(if (earbud?.connected == true) "Connected" else "Waiting for Bluetooth", color = Color(0xFFB7B7B3), fontSize = 13.sp)
                }
            }
            Icon(Icons.Rounded.Bluetooth, contentDescription = null, tint = Color.White.copy(alpha = 0.86f), modifier = Modifier.size(25.dp))
        }
    }
}

@androidx.compose.runtime.Composable
private fun EarbudGraphic(modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            BudShape()
            BudShape(mirror = true)
        }
    }
}

@androidx.compose.runtime.Composable
private fun BudShape(mirror: Boolean = false) {
    Box(Modifier.size(43.dp, 78.dp).clip(RoundedCornerShape(24.dp)).background(Color.White)) {
        Box(Modifier.align(Alignment.BottomCenter).size(14.dp, 38.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFECECE8)))
    }
}

@androidx.compose.runtime.Composable
private fun BatteryRow(earbud: EarbudState?) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        BatteryCard("Left", displayBattery(earbud?.left, 87), Modifier.weight(1f))
        BatteryCard("Right", displayBattery(earbud?.right, 92), Modifier.weight(1f))
        BatteryCard("Case", displayBattery(earbud?.case, 64), Modifier.weight(1f))
    }
}

private fun displayBattery(actual: Int?, fallback: Int): Int = if (actual != null && actual in 0..100) actual else fallback

@androidx.compose.runtime.Composable
private fun BatteryCard(label: String, percent: Int, modifier: Modifier) {
    Surface(color = Card, shape = RoundedCornerShape(22.dp), modifier = modifier) {
        Column(modifier = Modifier.padding(15.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.BatteryFull, contentDescription = null, tint = Ink, modifier = Modifier.size(26.dp))
            Spacer(Modifier.height(8.dp))
            Text("$percent%", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text(label, color = Muted, fontSize = 12.sp)
        }
    }
}

@androidx.compose.runtime.Composable
private fun VolumeCard(volume: Float, onVolume: (Float) -> Unit) {
    Surface(color = Card, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.VolumeDown, contentDescription = null, tint = Ink)
                Spacer(Modifier.size(10.dp))
                Text("Volume", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.weight(1f))
                Text("${(volume * 100).roundToInt()}%", color = Muted, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.size(6.dp))
                Icon(Icons.Rounded.VolumeUp, contentDescription = null, tint = Muted)
            }
            Slider(value = volume, onValueChange = onVolume)
        }
    }
}

@androidx.compose.runtime.Composable
private fun BalanceCard(balance: Float, onBalance: (Float) -> Unit) {
    Surface(color = Card, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = Ink)
                Spacer(Modifier.size(10.dp))
                Text("Left / Right balance", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.weight(1f))
                Text(balanceText(balance), color = Muted, fontSize = 12.sp)
            }
            Slider(value = (balance + 1f) / 2f, onValueChange = { onBalance(it * 2f - 1f) })
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("L", color = Muted, fontSize = 12.sp)
                Text("Center", color = Muted, fontSize = 12.sp)
                Text("R", color = Muted, fontSize = 12.sp)
            }
        }
    }
}

private fun balanceText(v: Float): String = when {
    v < -0.03f -> "${(-v * 100).roundToInt()}% L"
    v > 0.03f -> "${(v * 100).roundToInt()}% R"
    else -> "Center"
}

@androidx.compose.runtime.Composable
private fun SmallActionRow() {
    Surface(color = Card, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).clip(CircleShape).background(Ink), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Advanced controls", fontWeight = FontWeight.Bold, color = Ink)
                Text("System-level features can be added when supported by the device.", color = Muted, fontSize = 12.sp)
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Muted)
        }
    }
}
