package com.kraq.earcontrol

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.location.Location
import android.location.LocationManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private val Background = Color(0xFFF6F6F3)
private val Card = Color.White
private val Ink = Color(0xFF101010)
private val Muted = Color(0xFF777773)
private val Soft = Color(0xFFE9E9E4)
private val Accent = Color(0xFF111111)

data class EarbudDevice(
    val address: String,
    val name: String,
    val connected: Boolean,
    val bonded: Boolean,
    val rssi: Int? = null,
)

enum class Screen {
    FIND_MY_EARBUDS,
    DYNAMIC_ISLAND,
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            EarControlApp()
        }
    }
}

@Composable
private fun EarControlApp() {
    val context = LocalContext.current

    var screen by remember {
        mutableStateOf(Screen.FIND_MY_EARBUDS)
    }

    var menuOpen by remember {
        mutableStateOf(false)
    }

    var islandEnabled by remember {
        mutableStateOf(
            Settings.canDrawOverlays(context)
        )
    }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Background,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {

                when (screen) {
                    Screen.FIND_MY_EARBUDS -> {
                        FindMyEarbudsScreen()
                    }

                    Screen.DYNAMIC_ISLAND -> {
                        DynamicIslandScreen(
                            enabled = islandEnabled,
                            onEnable = {
                                openOverlaySettings(context)
                                islandEnabled =
                                    Settings.canDrawOverlays(context)
                            }
                        )
                    }
                }

                AnimatedVisibility(
                    visible = menuOpen,
                    modifier = Modifier.align(Alignment.TopStart),
                ) {
                    SideMenu(
                        currentScreen = screen,
                        onSelect = {
                            screen = it
                            menuOpen = false
                        },
                        onClose = {
                            menuOpen = false
                        },
                    )
                }

                if (!menuOpen) {
                    IconButton(
                        onClick = {
                            menuOpen = true
                        },
                        modifier = Modifier
                            .padding(
                                top = 16.dp,
                                start = 16.dp,
                            )
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Card),
                    ) {
                        Icon(
                            Icons.Rounded.Menu,
                            contentDescription = "Menu",
                            tint = Ink,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SideMenu(
    currentScreen: Screen,
    onSelect: (Screen) -> Unit,
    onClose: () -> Unit,
) {
    Surface(
        color = Card,
        shape = RoundedCornerShape(
            topEnd = 28.dp,
            bottomEnd = 28.dp,
        ),
        shadowElevation = 12.dp,
        modifier = Modifier
            .fillMaxWidth(0.82f)
            .fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(20.dp),
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "EarControl",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                    modifier = Modifier.weight(1f),
                )

                IconButton(
                    onClick = onClose
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = Ink,
                    )
                }
            }

            Spacer(
                Modifier.height(24.dp)
            )

            MenuItem(
                title = "Find My Earbuds",
                icon = Icons.Rounded.Search,
                selected =
                    currentScreen ==
                        Screen.FIND_MY_EARBUDS,
                onClick = {
                    onSelect(
                        Screen.FIND_MY_EARBUDS
                    )
                },
            )

            Spacer(
                Modifier.height(10.dp)
            )

            MenuItem(
                title = "Dynamic Island",
                icon = Icons.Rounded.Headphones,
                selected =
                    currentScreen ==
                        Screen.DYNAMIC_ISLAND,
                onClick = {
                    onSelect(
                        Screen.DYNAMIC_ISLAND
                    )
                },
            )
        }
    }
}

@Composable
private fun MenuItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        color =
            if (selected) Soft
            else Color.Transparent,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Ink,
            )

            Spacer(
                Modifier.width(14.dp)
            )

            Text(
                title,
                color = Ink,
                fontSize = 16.sp,
                fontWeight =
                    if (selected)
                        FontWeight.Bold
                    else
                        FontWeight.Medium,
            )
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun FindMyEarbudsScreen() {
    val context = LocalContext.current

    var devices by remember {
        mutableStateOf<List<EarbudDevice>>(emptyList())
    }

    var refreshing by remember {
        mutableStateOf(false)
    }

    var locationGranted by remember {
        mutableStateOf(
            hasLocationPermission(context)
        )
    }

    var bluetoothGranted by remember {
        mutableStateOf(
            hasBluetoothPermission(context)
        )
    }

    var lastLocation by remember {
        mutableStateOf<Location?>(null)
    }

    var volume by remember {
        mutableFloatStateOf(
            readSystemVolume(context)
        )
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            locationGranted =
                hasLocationPermission(context)

            bluetoothGranted =
                hasBluetoothPermission(context)
        }

    LaunchedEffect(Unit) {
        val permissions = buildList {
            if (
                Build.VERSION.SDK_INT >= 31
            ) {
                add(
                    Manifest.permission.BLUETOOTH_CONNECT
                )
                add(
                    Manifest.permission.BLUETOOTH_SCAN
                )
            }

            if (
                Build.VERSION.SDK_INT >= 33
            ) {
                add(
                    Manifest.permission.POST_NOTIFICATIONS
                )
            }

            add(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            add(
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }.toTypedArray()

        permissionLauncher.launch(
            permissions
        )
    }

    fun refresh() {
        if (!bluetoothGranted) {
            return
        }

        refreshing = true

        devices =
            findConnectedEarbuds(context)

        if (locationGranted) {
            lastLocation =
                getLastKnownLocation(context)
        }

        refreshing = false
    }

    LaunchedEffect(bluetoothGranted) {
        if (bluetoothGranted) {
            refresh()
        }
    }

    DisposableEffect(bluetoothGranted) {
        if (!bluetoothGranted) {
            onDispose { }
        } else {
            val receiver =
                object : BroadcastReceiver() {
                    override fun onReceive(
                        context: Context,
                        intent: Intent,
                    ) {
                        refresh()
                    }
                }

            val filter =
                IntentFilter().apply {
                    addAction(
                        BluetoothDevice.ACTION_ACL_CONNECTED
                    )
                    addAction(
                        BluetoothDevice.ACTION_ACL_DISCONNECTED
                    )
                    addAction(
                        BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED
                    )
                }

            ContextCompat.registerReceiver(
                context,
                receiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )

            onDispose {
                context.unregisterReceiver(
                    receiver
                )
            }
        }
    }

    val primaryDevice =
        devices.firstOrNull()

    val nearbyText =
        proximityText(primaryDevice?.rssi)

    val mapLat =
        lastLocation?.latitude
            ?: 0.0

    val mapLon =
        lastLocation?.longitude
            ?: 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(
                start = 18.dp,
                end = 18.dp,
                top = 76.dp,
            ),
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    "Find My Earbuds",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                )

                Text(
                    "Automatic detection",
                    color = Muted,
                    fontSize = 13.sp,
                )
            }

            IconButton(
                onClick = {
                    refresh()
                }
            ) {
                Icon(
                    Icons.Rounded.Refresh,
                    contentDescription = "Refresh",
                    tint = Ink,
                )
            }
        }

        Spacer(
            Modifier.height(14.dp)
        )

        if (!bluetoothGranted) {
            PermissionCard(
                title = "Bluetooth permission required",
                description =
                    "EarControl needs Bluetooth access to detect your earbuds automatically.",
                buttonText = "Allow Bluetooth",
                onClick = {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_SCAN,
                        )
                    )
                },
            )

            Spacer(
                Modifier.height(12.dp)
            )
        }

        Surface(
            color = Card,
            shape = RoundedCornerShape(26.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {

                if (
                    locationGranted &&
                    lastLocation != null
                ) {
                    OSMMap(
                        latitude = mapLat,
                        longitude = mapLon,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .weight(1f),
                    )
                } else {
                    MapPlaceholder(
                        onRequestLocation = {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                )
                            )
                        }
                    )
                }

                Column(
                    modifier = Modifier.padding(18.dp)
                ) {

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically,
                    ) {

                        Box(
                            Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    if (
                                        primaryDevice != null &&
                                        primaryDevice.connected
                                    )
                                        Color(0xFF28A745)
                                    else
                                        Color(0xFFE7A22A)
                                )
                        )

                        Spacer(
                            Modifier.size(8.dp)
                        )

                        Text(
                            when {
                                primaryDevice == null ->
                                    "Searching for earbuds..."

                                primaryDevice.rssi != null ->
                                    nearbyText

                                else ->
                                    "Earbuds connected"
                            },
                            color = Ink,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Spacer(
                        Modifier.height(6.dp)
                    )

                    Text(
                        primaryDevice?.name
                            ?: "No compatible earbuds detected",
                        color = Muted,
                        fontSize = 14.sp,
                    )

                    Spacer(
                        Modifier.height(12.dp)
                    )

                    if (
                        primaryDevice != null
                    ) {
                        Row(
                            verticalAlignment =
                                Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Rounded.Bluetooth,
                                contentDescription = null,
                                tint = Ink,
                            )

                            Spacer(
                                Modifier.size(8.dp)
                            )

                            Text(
                                "Connected automatically",
                                color = Muted,
                                fontSize = 12.sp,
                            )
                        }
                    } else {
                        Text(
                            "Make sure the earbuds are turned on and connected to this phone.",
                            color = Muted,
                            fontSize = 12.sp,
                        )
                    }

                    Spacer(
                        Modifier.height(14.dp)
                    )

                    Text(
                        "Phone media volume",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ink,
                    )

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.VolumeUp,
                            contentDescription = null,
                            tint = Muted,
                        )

                        Spacer(
                            Modifier.size(8.dp)
                        )

                        Slider(
                            value = volume,
                            onValueChange = {
                                volume = it
                                setSystemVolume(
                                    context,
                                    it,
                                )
                            },
                            modifier =
                                Modifier.weight(1f),
                        )
                    }

                    Text(
                        "This controls the Android media stream. Independent left/right earbud volume is only shown when the hardware exposes that control.",
                        color = Muted,
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    buttonText: String,
    onClick: () -> Unit,
) {
    Surface(
        color = Card,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                color = Ink,
            )

            Spacer(
                Modifier.height(6.dp)
            )

            Text(
                description,
                color = Muted,
                fontSize = 12.sp,
            )

            Spacer(
                Modifier.height(12.dp)
            )

            Button(
                onClick = onClick
            ) {
                Text(buttonText)
            }
        }
    }
}

@Composable
private fun MapPlaceholder(
    onRequestLocation: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .background(Color(0xFFEDEDE7)),
        horizontalAlignment =
            Alignment.CenterHorizontally,
        verticalArrangement =
            Arrangement.Center,
    ) {
        Icon(
            Icons.Rounded.LocationOn,
            contentDescription = null,
            tint = Ink,
            modifier = Modifier.size(42.dp),
        )

        Spacer(
            Modifier.height(10.dp)
        )

        Text(
            "Location needed",
            fontWeight = FontWeight.Bold,
            color = Ink,
        )

        Spacer(
            Modifier.height(6.dp)
        )

        Text(
            "Allow location to show your last known position on the map.",
            color = Muted,
            fontSize = 12.sp,
        )

        Spacer(
            Modifier.height(12.dp)
        )

        OutlinedButton(
            onClick = onRequestLocation
        ) {
            Text("Allow location")
        }
    }
}

@Composable
private fun OSMMap(
    latitude: Double,
    longitude: Double,
    modifier: Modifier = Modifier,
) {
    val html = remember(
        latitude,
        longitude,
    ) {
        """
        <!doctype html>
        <html>
        <head>
          <meta name="viewport"
                content="width=device-width,
                initial-scale=1.0,
                maximum-scale=1.0,
                user-scalable=no">
          <link
            rel="stylesheet"
            href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"
          />
          <script
            src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js">
          </script>
          <style>
            html, body, #map {
              margin: 0;
              padding: 0;
              width: 100%;
              height: 100%;
              overflow: hidden;
            }
          </style>
        </head>
        <body>
          <div id="map"></div>
          <script>
            const lat = $latitude;
            const lon = $longitude;

            const map = L.map('map', {
              zoomControl: false,
              attributionControl: true
            }).setView([lat, lon], 17);

            L.tileLayer(
              'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
              {
                maxZoom: 19,
                attribution: '&copy; OpenStreetMap contributors'
              }
            ).addTo(map);

            L.circleMarker(
              [lat, lon],
              {
                radius: 9,
                color: '#111111',
                fillColor: '#111111',
                fillOpacity: 1
              }
            )
            .addTo(map)
            .bindPopup('Last known phone location')
            .openPopup();
          </script>
        </body>
        </html>
        """.trimIndent()
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                webViewClient = WebViewClient()
                loadDataWithBaseURL(
                    "https://localhost/",
                    html,
                    "text/html",
                    "UTF-8",
                    null,
                )
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(
                "https://localhost/",
                html,
                "text/html",
                "UTF-8",
                null,
            )
        },
    )
}

@Composable
private fun DynamicIslandScreen(
    enabled: Boolean,
    onEnable: () -> Unit,
) {
    val context = LocalContext.current

    var testExpanded by remember {
        mutableStateOf(true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(
                start = 18.dp,
                end = 18.dp,
                top = 76.dp,
            ),
    ) {

        Text(
            "Dynamic Island",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Ink,
        )

        Spacer(
            Modifier.height(6.dp)
        )

        Text(
            "EarControl floating status",
            color = Muted,
            fontSize = 13.sp,
        )

        Spacer(
            Modifier.height(24.dp)
        )

        DynamicIslandPreview(
            expanded = testExpanded,
            deviceName = "EarControl",
        )

        Spacer(
            Modifier.height(20.dp)
        )

        Surface(
            color = Card,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(18.dp)
            ) {

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically,
                ) {

                    Icon(
                        Icons.Rounded.Notifications,
                        contentDescription = null,
                        tint = Ink,
                    )

                    Spacer(
                        Modifier.size(10.dp)
                    )

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "Dynamic Island",
                            fontWeight = FontWeight.Bold,
                            color = Ink,
                        )

                        Text(
                            if (enabled)
                                "Overlay permission granted"
                            else
                                "Permission required",
                            color = Muted,
                            fontSize = 12.sp,
                        )
                    }

                    Switch(
                        checked = testExpanded,
                        onCheckedChange = {
                            testExpanded = it
                        },
                    )
                }

                Spacer(
                    Modifier.height(14.dp)
                )

                Text(
                    "EarControl uses Android's overlay permission so the island can stay visible above other apps.",
                    color = Muted,
                    fontSize = 12.sp,
                )

                Spacer(
                    Modifier.height(14.dp)
                )

                if (!enabled) {
                    Button(
                        onClick = onEnable,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            "Allow Dynamic Island"
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            startIslandService(
                                context
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            "Show Dynamic Island"
                        )
                    }

                    Spacer(
                        Modifier.height(8.dp)
                    )

                    OutlinedButton(
                        onClick = {
                            stopIslandService(
                                context
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            "Hide Dynamic Island"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DynamicIslandPreview(
    expanded: Boolean,
    deviceName: String,
) {
    val width by animateFloatAsState(
        targetValue =
            if (expanded) 0.92f
            else 0.28f,
        animationSpec =
            tween(320),
        label = "island-width",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp),
        contentAlignment =
            Alignment.TopCenter,
    ) {

        Surface(
            color = Color.Black,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth(width)
                .height(
                    if (expanded)
                        74.dp
                    else
                        34.dp
                ),
        ) {

            Row(
                modifier = Modifier.padding(
                    horizontal = 16.dp
                ),
                verticalAlignment =
                    Alignment.CenterVertically,
            ) {

                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            Color(0xFF6AFF91)
                        )
                )

                if (expanded) {
                    Spacer(
                        Modifier.size(10.dp)
                    )

                    Column {
                        Text(
                            deviceName,
                            color = Color.White,
                            fontWeight =
                                FontWeight.Bold,
                        )

                        Text(
                            "Find My Earbuds active",
                            color =
                                Color.White.copy(
                                    alpha = 0.65f
                                ),
                            fontSize = 11.sp,
                        )
                    }
                }
            }
        }
    }
}

private fun hasBluetoothPermission(
    context: Context,
): Boolean {
    return Build.VERSION.SDK_INT < 31 ||
        (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT,
            ) ==
                PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN,
            ) ==
                PackageManager.PERMISSION_GRANTED
        )
}

private fun hasLocationPermission(
    context: Context,
): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) ==
        PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) ==
            PackageManager.PERMISSION_GRANTED
}

@SuppressLint("MissingPermission")
private fun findConnectedEarbuds(
    context: Context,
): List<EarbudDevice> {
    val bluetoothManager =
        context.getSystemService(
            Context.BLUETOOTH_SERVICE
        ) as BluetoothManager

    val adapter =
        bluetoothManager.adapter
            ?: return emptyList()

    if (!adapter.isEnabled) {
        return emptyList()
    }

    val found =
        mutableListOf<EarbudDevice>()

    val a2dpDevices =
        bluetoothManager.getConnectedDevices(
            BluetoothProfileId.A2DP
        )

    for (device in a2dpDevices) {
        found += EarbudDevice(
            address = device.address,
            name =
                device.name
                    ?: "Bluetooth earbuds",
            connected = true,
            bonded =
                device.bondState ==
                    BluetoothDevice.BOND_BONDED,
        )
    }

    if (found.isEmpty()) {
        val headsetDevices =
            bluetoothManager.getConnectedDevices(
                BluetoothProfileId.HEADSET
            )

        for (device in headsetDevices) {
            found += EarbudDevice(
                address = device.address,
                name =
                    device.name
                        ?: "Bluetooth headset",
                connected = true,
                bonded =
                    device.bondState ==
                        BluetoothDevice.BOND_BONDED,
            )
        }
    }

    return found.distinctBy {
        it.address
    }
}

private object BluetoothProfileId {
    const val A2DP = 2
    const val HEADSET = 1
}

@SuppressLint("MissingPermission")
private fun getLastKnownLocation(
    context: Context,
): Location? {
    val manager =
        context.getSystemService(
            Context.LOCATION_SERVICE
        ) as LocationManager

    val providers =
        manager.getProviders(true)

    var best: Location? = null

    for (provider in providers) {
        val location =
            try {
                manager.getLastKnownLocation(
                    provider
                )
            } catch (_: SecurityException) {
                null
            }

        if (location != null) {
            if (
                best == null ||
                location.time > best.time
            ) {
                best = location
            }
        }
    }

    return best
}

private fun proximityText(
    rssi: Int?,
): String {
    if (rssi == null) {
        return "Bluetooth connected"
    }

    return when {
        rssi >= -55 ->
            "Very close"

        rssi >= -67 ->
            "Close"

        rssi >= -78 ->
            "Getting closer"

        else ->
            "Far away"
    }
}

private fun readSystemVolume(
    context: Context,
): Float {
    val manager =
        context.getSystemService(
            Context.AUDIO_SERVICE
        ) as AudioManager

    val max =
        manager.getStreamMaxVolume(
            AudioManager.STREAM_MUSIC
        )

    if (max <= 0) {
        return 0f
    }

    val current =
        manager.getStreamVolume(
            AudioManager.STREAM_MUSIC
        )

    return current.toFloat() /
        max.toFloat()
}

private fun setSystemVolume(
    context: Context,
    value: Float,
) {
    val manager =
        context.getSystemService(
            Context.AUDIO_SERVICE
        ) as AudioManager

    val max =
        manager.getStreamMaxVolume(
            AudioManager.STREAM_MUSIC
        )

    val current =
        (value.coerceIn(0f, 1f) * max)
            .roundToInt()

    manager.setStreamVolume(
        AudioManager.STREAM_MUSIC,
        current,
        0,
    )
}

private fun openOverlaySettings(
    context: Context,
) {
    if (Build.VERSION.SDK_INT >= 23) {
        context.startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse(
                    "package:${context.packageName}"
                ),
            )
        )
    }
}

private fun startIslandService(
    context: Context,
) {
    if (!Settings.canDrawOverlays(context)) {
        openOverlaySettings(context)
        return
    }

    val intent =
        Intent(
            context,
            DynamicIslandService::class.java,
        )

    ContextCompat.startForegroundService(
        context,
        intent,
    )
}

private fun stopIslandService(
    context: Context,
) {
    context.stopService(
        Intent(
            context,
            DynamicIslandService::class.java,
        )
    )
}

private fun createIslandChannel(
    context: Context,
) {
    val manager =
        context.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager

    if (
        Build.VERSION.SDK_INT >=
        Build.VERSION_CODES.O
    ) {
        manager.createNotificationChannel(
            NotificationChannel(
                "earcontrol_island",
                "EarControl Dynamic Island",
                NotificationManager.IMPORTANCE_LOW,
            )
        )
    }
}
