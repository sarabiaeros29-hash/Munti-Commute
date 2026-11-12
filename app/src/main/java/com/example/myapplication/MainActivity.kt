package com.example.myapplication

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import kotlin.math.ceil

private data class Stop(val name: String, val t: Float, val lat: Double, val lon: Double)
private data class Route(val name: String, val color: Color, val stops: List<Stop>, val loopMin: Float, val fare: Int)
private data class Ej(val route: Int, val p: Float)

private val appRoutes = listOf(
    Route("Alabang - Filinvest", Color(0xFF10B981), listOf(
        Stop("Alabang Terminal", 0f, 14.4172, 121.0435),
        Stop("Corporate Ave", 0.2f, 14.4182, 121.0412),
        Stop("Filinvest City", 0.4f, 14.4165, 121.0405),
        Stop("Commerce Ave", 0.6f, 14.4190, 121.0360),
        Stop("Alabang Town Center", 0.8f, 14.4250, 121.0295),
        Stop("Madrigal", 1f, 14.4285, 121.0235)
    ), 20f, 13),
    Route("Alabang - Putatan", Color(0xFFF97316), listOf(
        Stop("Alabang Terminal", 0f, 14.4172, 121.0435),
        Stop("Junction", 0.2f, 14.4130, 121.0432),
        Stop("Bayanan", 0.5f, 14.4080, 121.0430),
        Stop("Putatan", 0.8f, 14.4000, 121.0435),
        Stop("Muntinlupa City Hall", 1f, 14.3950, 121.0440)
    ), 16f, 13),
    Route("Alabang - Poblacion", Color(0xFF3B82F6), listOf(
        Stop("Alabang Terminal", 0f, 14.4172, 121.0435),
        Stop("Bayanan", 0.35f, 14.4080, 121.0430),
        Stop("Poblacion", 0.7f, 14.3910, 121.0445),
        Stop("Tunasan", 1f, 14.3780, 121.0455)
    ), 18f, 15)
)

private fun estimateEta(routeIndex: Int, stopPosition: Float, units: List<Ej>): Int? {
    return units.filter { it.route == routeIndex }
        .minOfOrNull { ((stopPosition - it.p + 1f) % 1f) * appRoutes[routeIndex].loopMin }
        ?.let { ceil(it).toInt() }
}

private fun getGlassBorderBrush(isDark: Boolean) = Brush.linearGradient(
    if (isDark) listOf(Color.White.copy(0.45f), Color.White.copy(0.08f), Color.White.copy(0.25f))
    else listOf(Color(0xFF10B981).copy(0.40f), Color.White.copy(0.60f), Color(0xFF10B981).copy(0.20f))
)

private fun getLiquidBackgroundBrush(isDark: Boolean) = Brush.verticalGradient(
    if (isDark) listOf(Color(0xFF061412), Color(0xFF0F2B24), Color(0xFF081915))
    else listOf(Color(0xFFECFDF5), Color(0xFFD1FAE5), Color(0xFFE0F2FE))
)

private fun Modifier.glassCard(isDark: Boolean, shape: RoundedCornerShape = RoundedCornerShape(24.dp), backgroundColor: Color? = null): Modifier {
    val bg = backgroundColor ?: if (isDark) Color.White.copy(0.08f) else Color.White.copy(0.75f)
    return this.clip(shape).background(bg).border(1.dp, getGlassBorderBrush(isDark), shape)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = "MuntiCommuteApp/1.0 (Android; com.example.myapplication)"
        setContent {
            val systemInDark = isSystemInDarkTheme()
            var isDarkMode by remember { mutableStateOf(systemInDark) }
            val colorScheme = if (isDarkMode) {
                darkColorScheme(primary = Color(0xFF10B981), secondary = Color(0xFF3B82F6), surface = Color(0xFF0F2620), background = Color(0xFF061412), onSurface = Color.White, onBackground = Color.White)
            } else {
                lightColorScheme(primary = Color(0xFF059669), secondary = Color(0xFF2563EB), surface = Color.White, background = Color(0xFFECFDF5), onSurface = Color(0xFF064E3B), onBackground = Color(0xFF064E3B))
            }
            MaterialTheme(colorScheme = colorScheme) {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
                    Box(modifier = Modifier.fillMaxSize().background(getLiquidBackgroundBrush(isDarkMode))) {
                        MuntiCommuteApp(isDarkMode = isDarkMode, onToggleTheme = { isDarkMode = !isDarkMode })
                    }
                }
            }
        }
    }
}

@Composable
private fun MuntiCommuteApp(isDarkMode: Boolean, onToggleTheme: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedStop by remember { mutableStateOf("Alabang Terminal") }
    val units = remember {
        mutableStateListOf(Ej(0, 0.1f), Ej(0, 0.45f), Ej(0, 0.8f), Ej(1, 0.2f), Ej(1, 0.7f), Ej(2, 0.35f), Ej(2, 0.9f))
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(800.milliseconds)
            for (i in units.indices) units[i] = units[i].copy(p = (units[i].p + 0.004f) % 1f)
        }
    }

    val tabs = listOf(
        Triple("Map", Icons.Default.Map, "Live Map"),
        Triple("Routes", Icons.Default.Route, "Jeepney Routes"),
        Triple("Alerts", Icons.Default.Notifications, "Live Updates")
    )

    Scaffold(
        containerColor = Color.Transparent,
        topBar = { GlassHeader(isDarkMode = isDarkMode, onToggleTheme = onToggleTheme) },
        bottomBar = { GlassNavigationBar(tabs = tabs, selectedTab = selectedTab, onSelectTab = { selectedTab = it }, isDarkMode = isDarkMode) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (selectedTab) {
                0 -> MapScreen(units = units, selectedStop = selectedStop, onSelectStop = { selectedStop = it }, isDarkMode = isDarkMode)
                1 -> RoutesScreen(units = units, onOpenRoute = { index -> selectedStop = appRoutes[index].stops.first().name; selectedTab = 0 }, isDarkMode = isDarkMode)
                else -> AlertsScreen(isDarkMode = isDarkMode)
            }
        }
    }
}

@Composable
private fun GlassHeader(isDarkMode: Boolean, onToggleTheme: () -> Unit) {
    val cardBg = if (isDarkMode) Color.White.copy(0.07f) else Color.White.copy(0.75f)
    val titleColor = if (isDarkMode) Color.White else Color(0xFF064E3B)
    val subtitleColor = if (isDarkMode) Color.White.copy(0.7f) else Color(0xFF047857)

    Box(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp).glassCard(isDark = isDarkMode, shape = RoundedCornerShape(20.dp), backgroundColor = cardBg).padding(horizontal = 20.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Color(0xFF10B981).copy(0.9f), Color(0xFF047857).copy(0.5f)))), contentAlignment = Alignment.Center) {
                Icon(imageVector = Icons.Default.DirectionsBus, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "MuntiCommute", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = titleColor)
                Text(text = "Live Liquid Tracker • Muntinlupa", fontSize = 12.sp, color = subtitleColor)
            }
            Box(modifier = Modifier.size(38.dp).clip(CircleShape).background(if (isDarkMode) Color.White.copy(0.12f) else Color(0xFF10B981).copy(0.15f)).clickable { onToggleTheme() }, contentAlignment = Alignment.Center) {
                Icon(imageVector = if (isDarkMode) Icons.Default.WbSunny else Icons.Default.DarkMode, contentDescription = "Toggle Dark/Light Mode", tint = if (isDarkMode) Color(0xFFFBBF24) else Color(0xFF047857), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun GlassNavigationBar(tabs: List<Triple<String, ImageVector, String>>, selectedTab: Int, onSelectTab: (Int) -> Unit, isDarkMode: Boolean) {
    val barBg = if (isDarkMode) Color(0xFF0B1F1A).copy(0.85f) else Color.White.copy(0.88f)
    val activeLabelColor = if (isDarkMode) Color.White else Color(0xFF064E3B)
    val inactiveIconColor = if (isDarkMode) Color.White.copy(0.55f) else Color(0xFF047857).copy(0.55f)

    Box(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth().glassCard(isDark = isDarkMode, shape = RoundedCornerShape(32.dp), backgroundColor = barBg).padding(vertical = 8.dp, horizontal = 12.dp), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
            tabs.forEachIndexed { index, (label, icon, _) ->
                val isSelected = selectedTab == index
                val itemBg = if (isSelected) Brush.horizontalGradient(listOf(Color(0xFF10B981).copy(0.35f), Color(0xFF059669).copy(0.20f))) else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(itemBg).then(if (isSelected) Modifier.border(1.dp, Color(0xFF10B981).copy(0.5f), RoundedCornerShape(20.dp)) else Modifier).clickable { onSelectTab(index) }.padding(horizontal = 16.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = icon, contentDescription = label, tint = if (isSelected) Color(0xFF10B981) else inactiveIconColor, modifier = Modifier.size(20.dp))
                        if (isSelected) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = activeLabelColor)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MapScreen(units: List<Ej>, selectedStop: String, onSelectStop: (String) -> Unit, isDarkMode: Boolean) {
    val allStops = remember { appRoutes.flatMap { it.stops }.distinctBy { it.name } }
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapViewRef?.onResume()
                Lifecycle.Event.ON_PAUSE -> mapViewRef?.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapViewRef?.onDetach()
        }
    }

    Column(Modifier.fillMaxSize()) {
        LazyRow(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(allStops, key = { it.name }) { stop ->
                val isSelected = selectedStop == stop.name
                val chipBg = if (isSelected) Color(0xFF10B981).copy(if (isDarkMode) 0.35f else 0.25f) else if (isDarkMode) Color.White.copy(0.07f) else Color.White.copy(0.65f)
                val chipBorder = if (isSelected) Color(0xFF10B981).copy(0.8f) else if (isDarkMode) Color.White.copy(0.15f) else Color(0xFF10B981).copy(0.25f)
                val textColor = if (isSelected) (if (isDarkMode) Color.White else Color(0xFF064E3B)) else (if (isDarkMode) Color.White.copy(0.75f) else Color(0xFF047857))

                Box(modifier = Modifier.clip(RoundedCornerShape(18.dp)).background(chipBg).border(1.dp, chipBorder, RoundedCornerShape(18.dp)).clickable { onSelectStop(stop.name); mapViewRef?.controller?.animateTo(GeoPoint(stop.lat, stop.lon)) }.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = if (isSelected) Color(0xFF10B981) else textColor.copy(0.6f), modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = stop.name, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = textColor)
                    }
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp, vertical = 4.dp).glassCard(isDark = isDarkMode, shape = RoundedCornerShape(28.dp))) {
            AndroidView(
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(28.dp)),
                factory = { context ->
                    Configuration.getInstance().userAgentValue = "MuntiCommuteApp/1.0 (Android; com.example.myapplication)"
                    MapView(context).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                        controller.setZoom(14.2)
                        controller.setCenter(GeoPoint(14.4060, 121.0380))

                        appRoutes.forEach { route ->
                            val line = Polyline(this).apply {
                                setPoints(route.stops.map { GeoPoint(it.lat, it.lon) })
                                outlinePaint.color = when (route.name) {
                                    "Alabang - Filinvest" -> AndroidColor.rgb(16, 185, 129)
                                    "Alabang - Putatan" -> AndroidColor.rgb(249, 115, 22)
                                    else -> AndroidColor.rgb(59, 130, 246)
                                }
                                outlinePaint.strokeWidth = 12f
                            }
                            overlays.add(line)
                        }

                        val stopIcon = ContextCompat.getDrawable(context, R.drawable.ic_stop_marker)
                        val greenJeepIcon = ContextCompat.getDrawable(context, R.drawable.ic_jeep_marker_green)
                        val orangeJeepIcon = ContextCompat.getDrawable(context, R.drawable.ic_jeep_marker_orange)
                        val blueJeepIcon = ContextCompat.getDrawable(context, R.drawable.ic_jeep_marker_blue)

                        allStops.forEach { stop ->
                            val marker = Marker(this).apply {
                                position = GeoPoint(stop.lat, stop.lon)
                                title = stop.name
                                if (stopIcon != null) icon = stopIcon
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                setOnMarkerClickListener { _, _ -> onSelectStop(stop.name); controller.animateTo(position); true }
                            }
                            overlays.add(marker)
                        }

                        units.forEachIndexed { index, unit ->
                            val route = appRoutes[unit.route]
                            val jeepIcon = when (unit.route) {
                                0 -> greenJeepIcon
                                1 -> orangeJeepIcon
                                else -> blueJeepIcon
                            }
                            val marker = Marker(this).apply {
                                title = "E-jeepney ${index + 1}"
                                snippet = route.name
                                position = GeoPoint(route.stops.first().lat, route.stops.first().lon)
                                if (jeepIcon != null) icon = jeepIcon
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                relatedObject = "VEHICLE_$index"
                            }
                            overlays.add(marker)
                        }

                        mapViewRef = this
                        invalidate()
                    }
                },
                update = { mapView ->
                    units.forEachIndexed { index, unit ->
                        val route = appRoutes[unit.route]
                        val points = route.stops
                        val scaled = unit.p.coerceIn(0f, 1f) * (points.size - 1)
                        val startIndex = scaled.toInt().coerceAtMost(points.size - 2)
                        val fraction = scaled - startIndex
                        val start = points[startIndex]
                        val end = points[startIndex + 1]

                        val currentGeoPoint = GeoPoint(
                            start.lat + (end.lat - start.lat) * fraction,
                            start.lon + (end.lon - start.lon) * fraction
                        )

                        val vehicleMarker = mapView.overlays.filterIsInstance<Marker>().firstOrNull { it.relatedObject == "VEHICLE_$index" }
                        vehicleMarker?.position = currentGeoPoint
                    }
                    mapView.invalidate()
                }
            )
        }

        ArrivalSheet(stop = selectedStop, units = units, isDarkMode = isDarkMode)
    }
}

@Composable
private fun ArrivalSheet(stop: String, units: List<Ej>, isDarkMode: Boolean) {
    val sheetBg = if (isDarkMode) Color(0xFF0C241E).copy(0.90f) else Color.White.copy(0.88f)
    val titleColor = if (isDarkMode) Color.White else Color(0xFF064E3B)
    val subtitleColor = if (isDarkMode) Color.White.copy(0.6f) else Color(0xFF047857)

    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).glassCard(isDark = isDarkMode, shape = RoundedCornerShape(28.dp), backgroundColor = sheetBg).padding(16.dp)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(imageVector = Icons.Default.Place, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(text = stop, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = titleColor)
                    Text(text = "Live ETA • Tap map stop to switch", fontSize = 11.sp, color = subtitleColor)
                }
            }

            Spacer(Modifier.height(12.dp))

            appRoutes.forEachIndexed { index, route ->
                val selected = route.stops.find { it.name == stop } ?: return@forEachIndexed
                val minutes = estimateEta(index, selected.t, units)

                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(12.dp).clip(CircleShape).background(route.color))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(text = route.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = titleColor)
                        Text(text = "Fare from ₱${route.fare}", fontSize = 11.sp, color = subtitleColor)
                    }

                    val etaText = when {
                        minutes == null -> "No unit"
                        minutes <= 1 -> "Arriving"
                        else -> "$minutes min"
                    }

                    Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(route.color.copy(if (isDarkMode) 0.20f else 0.15f)).border(1.dp, route.color.copy(0.5f), RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 5.dp)) {
                        Text(text = etaText, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = route.color)
                    }
                }
            }
        }
    }
}

@Composable
private fun RoutesScreen(units: List<Ej>, onOpenRoute: (Int) -> Unit, isDarkMode: Boolean) {
    val titleColor = if (isDarkMode) Color.White else Color(0xFF064E3B)
    val subtitleColor = if (isDarkMode) Color.White.copy(0.75f) else Color(0xFF047857)

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        itemsIndexed(appRoutes, key = { _, r -> r.name }) { index, route ->
            Box(modifier = Modifier.fillMaxWidth().glassCard(isDark = isDarkMode, shape = RoundedCornerShape(24.dp)).clickable { onOpenRoute(index) }.padding(18.dp)) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(route.color))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = route.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = titleColor, modifier = Modifier.weight(1f))
                        Box(modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(if (isDarkMode) Color.White.copy(0.12f) else Color(0xFF10B981).copy(0.15f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                            Text(text = "₱${route.fare}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color(0xFF064E3B))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = route.stops.joinToString(" → ") { it.name }, fontSize = 13.sp, color = subtitleColor)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(text = "${units.count { it.route == index }} units running • ${route.loopMin.toInt()}m loop", fontSize = 12.sp, color = route.color)
                        Text(text = "Tap to view on map →", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = subtitleColor.copy(0.6f))
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertsScreen(isDarkMode: Boolean) {
    val titleColor = if (isDarkMode) Color.White else Color(0xFF064E3B)
    val bodyColor = if (isDarkMode) Color.White.copy(0.8f) else Color(0xFF047857)
    val timestampColor = if (isDarkMode) Color.White.copy(0.45f) else Color(0xFF047857).copy(0.55f)

    val alerts = listOf(
        Triple("Traffic Delay Near Madrigal", "Heavy traffic along Madrigal Avenue. Expect +5 min delay on Alabang - Filinvest line.", "5 min ago"),
        Triple("Unit 07 In Charging Station", "Alabang - Putatan line is running with 1 less unit for the next 20 minutes.", "18 min ago"),
        Triple("Poblacion Service Normal", "Alabang - Poblacion e-jeepneys are operating smoothly on schedule.", "1 hr ago")
    )

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        items(alerts, key = { it.first }) { alert ->
            Box(modifier = Modifier.fillMaxWidth().glassCard(isDark = isDarkMode, shape = RoundedCornerShape(24.dp)).padding(18.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFF59E0B).copy(0.20f)).border(1.dp, Color(0xFFF59E0B).copy(0.4f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = alert.first, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = titleColor)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = alert.second, fontSize = 13.sp, color = bodyColor)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = alert.third, fontSize = 11.sp, color = timestampColor)
                    }
                }
            }
        }
    }
}
