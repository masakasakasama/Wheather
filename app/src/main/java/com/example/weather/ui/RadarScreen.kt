package com.example.weather.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.weather.AppServices
import com.example.weather.data.model.RadarFrame
import com.example.weather.data.model.WeatherLocation
import com.example.weather.data.model.isInJapan
import com.example.weather.data.model.toRadarDisplayTime
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.math.PI
import kotlin.math.asinh
import kotlin.math.floor
import kotlin.math.tan

private val RadarBlue = Color(0xFF5BA2FF)
private val RadarText = Color(0xFFF2F7FC)
private val RadarMuted = Color(0xFF9BAFC2)
private val RadarSoftBlue = Color(0xFF17283B)

@Composable
fun RadarScreen(location: WeatherLocation) {
    if (!location.isInJapan()) {
        Column(
            Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("雨雲レーダー", color = RadarText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121A24)),
                shape = RoundedCornerShape(22.dp),
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(location.name, color = RadarText, fontWeight = FontWeight.Bold)
                    Text("気象庁レーダーは日本国内の地点のみ対応", color = RadarMuted, fontSize = 12.sp)
                }
            }
        }
        return
    }

    var refreshKey by remember { mutableIntStateOf(0) }
    var zoom by remember(location) { mutableIntStateOf(DEFAULT_RADAR_ZOOM) }
    var tileOffsetX by remember(location) { mutableIntStateOf(0) }
    var tileOffsetY by remember(location) { mutableIntStateOf(0) }
    var state by remember { mutableStateOf<RadarUiState>(RadarUiState.Loading) }

    LaunchedEffect(location, refreshKey, zoom, tileOffsetX, tileOffsetY) {
        state = RadarUiState.Loading
        state = runCatching { loadRadar(location, zoom, tileOffsetX, tileOffsetY) }
            .getOrElse { RadarUiState.Error("雨雲レーダーを取得できません") }
    }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("雨雲レーダー", color = RadarText, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Text(location.name, color = RadarMuted, fontSize = 12.sp)
            }
            FilledIconButton(
                onClick = { refreshKey++ },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = RadarSoftBlue,
                    contentColor = RadarBlue,
                ),
            ) {
                Icon(Icons.Outlined.Refresh, "更新")
            }
        }

        when (val radar = state) {
            RadarUiState.Loading -> {
                Box(Modifier.fillMaxWidth().height(430.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RadarBlue)
                }
            }
            is RadarUiState.Error -> {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF3A1E22)),
                    shape = RoundedCornerShape(22.dp),
                ) {
                    Text(radar.message, Modifier.padding(18.dp), color = MaterialTheme.colorScheme.error)
                }
            }
            is RadarUiState.Ready -> {
                RadarMapCard(radar, Modifier.fillMaxWidth().height(430.dp))
                RadarControls(
                    zoom = zoom,
                    onZoomIn = { zoom = (zoom + 1).coerceAtMost(MAX_RADAR_ZOOM) },
                    onZoomOut = { zoom = (zoom - 1).coerceAtLeast(MIN_RADAR_ZOOM) },
                    onMove = { dx, dy ->
                        tileOffsetX += dx
                        tileOffsetY += dy
                    },
                    onReset = {
                        zoom = DEFAULT_RADAR_ZOOM
                        tileOffsetX = 0
                        tileOffsetY = 0
                    },
                )
                Card(
                    colors = CardDefaults.cardColors(containerColor = RadarSoftBlue),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text("気象庁レーダー（実況）", color = RadarText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("現在の降雨判定に最優先", color = RadarMuted, fontSize = 10.sp)
                        }
                        Text(radar.frame.validTime.toRadarDisplayTime(), color = RadarBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun RadarMapCard(radar: RadarUiState.Ready, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121A24)),
        shape = RoundedCornerShape(26.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Box(Modifier.fillMaxSize()) {
            RadarTileGrid(radar, Modifier.fillMaxSize())
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF101820).copy(alpha = 0.94f))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(RadarBlue))
                Spacer(Modifier.width(6.dp))
                Text("最新 ${radar.frame.validTime.toRadarDisplayTime()}", color = RadarText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            Text(
                "Z${radar.zoom} · ${radar.centerLabel}",
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF101820).copy(alpha = 0.94f))
                    .padding(horizontal = 9.dp, vertical = 6.dp),
                color = RadarMuted,
                fontSize = 10.sp,
            )
        }
    }
}

@Composable
fun RadarPreview(location: WeatherLocation, refreshKey: Long, modifier: Modifier = Modifier) {
    if (!location.isInJapan()) return
    var state by remember(location) { mutableStateOf<RadarUiState>(RadarUiState.Loading) }
    LaunchedEffect(location.latitude, location.longitude, refreshKey) {
        state = RadarUiState.Loading
        state = runCatching { loadRadar(location, DEFAULT_RADAR_ZOOM, 0, 0) }
            .getOrElse { RadarUiState.Error("雨雲レーダーを取得できません") }
    }
    Box(modifier.clip(RoundedCornerShape(20.dp)).background(Color(0xFF121A24)), contentAlignment = Alignment.Center) {
        when (val radar = state) {
            RadarUiState.Loading -> CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp, color = RadarBlue)
            is RadarUiState.Error -> Text(radar.message, color = RadarMuted, fontSize = 11.sp)
            is RadarUiState.Ready -> {
                RadarTileGrid(radar, Modifier.fillMaxSize())
                Text(
                    "気象庁レーダー ${radar.frame.validTime.toRadarDisplayTime()}",
                    modifier = Modifier.align(Alignment.BottomStart).background(Color(0xFF101820).copy(alpha = 0.94f)).padding(7.dp),
                    color = RadarText,
                    fontSize = 10.sp,
                )
            }
        }
    }
}

@Composable
private fun RadarControls(
    zoom: Int,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onMove: (Int, Int) -> Unit,
    onReset: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121A24)),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalButton(onClick = onZoomOut, enabled = zoom > MIN_RADAR_ZOOM) { Text("−") }
                Text("ズーム $zoom", color = RadarText, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                FilledTonalButton(onClick = onZoomIn, enabled = zoom < MAX_RADAR_ZOOM) { Text("＋") }
                FilledTonalButton(onClick = onReset) {
                    Icon(Icons.Outlined.MyLocation, null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("現在地")
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                FilledTonalButton(onClick = { onMove(-1, 0) }) { Text("←") }
                Spacer(Modifier.width(7.dp))
                FilledTonalButton(onClick = { onMove(0, -1) }) { Text("↑") }
                Spacer(Modifier.width(7.dp))
                FilledTonalButton(onClick = { onMove(0, 1) }) { Text("↓") }
                Spacer(Modifier.width(7.dp))
                FilledTonalButton(onClick = { onMove(1, 0) }) { Text("→") }
            }
        }
    }
}

@Composable
private fun RadarTileGrid(radar: RadarUiState.Ready, modifier: Modifier) {
    BoxWithConstraints(modifier.background(Color(0xFF0C1219)), contentAlignment = Alignment.Center) {
        val tileSize = maxWidth / 3
        Box(Modifier.size(maxWidth)) {
            radar.tiles.forEach { tile ->
                val tileModifier = Modifier.size(tileSize).offset(tileSize * tile.dx, tileSize * tile.dy)
                tile.base?.let {
                    Image(it.asImageBitmap(), null, modifier = tileModifier, contentScale = ContentScale.FillBounds)
                }
                tile.radar?.let {
                    Image(it.asImageBitmap(), null, modifier = tileModifier.alpha(0.72f), contentScale = ContentScale.FillBounds)
                }
            }
            Box(
                Modifier.align(Alignment.Center).size(28.dp).clip(CircleShape).background(Color(0xFF101820).copy(alpha = 0.94f)),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(RadarBlue))
            }
        }
    }
}

private suspend fun loadRadar(location: WeatherLocation, zoom: Int, tileOffsetX: Int, tileOffsetY: Int): RadarUiState.Ready {
    val client = AppServices.radarClient
    val frame = client.latestFrame()
    val centerX = (lonToTileX(location.longitude, zoom) + tileOffsetX).floorMod(1 shl zoom)
    val centerY = (latToTileY(location.latitude, zoom) + tileOffsetY).coerceIn(1, (1 shl zoom) - 2)
    val tiles = coroutineScope {
        (-1..1).flatMap { dy ->
            (-1..1).map { dx ->
                async {
                    val x = (centerX + dx).floorMod(1 shl zoom)
                    val y = centerY + dy
                    val baseUrl = "https://tile.openstreetmap.org/$zoom/$x/$y.png"
                    val radarUrl = frame.tileTemplate
                        .replace("{z}", zoom.toString())
                        .replace("{x}", x.toString())
                        .replace("{y}", y.toString())
                    RadarTile(
                        dx = dx + 1,
                        dy = dy + 1,
                        base = client.fetchBitmap(baseUrl),
                        radar = client.fetchBitmap(radarUrl),
                    )
                }
            }
        }.awaitAll()
    }
    return RadarUiState.Ready(frame, zoom, centerLabel(tileOffsetX, tileOffsetY), tiles)
}

private sealed interface RadarUiState {
    data object Loading : RadarUiState
    data class Error(val message: String) : RadarUiState
    data class Ready(val frame: RadarFrame, val zoom: Int, val centerLabel: String, val tiles: List<RadarTile>) : RadarUiState
}

private data class RadarTile(val dx: Int, val dy: Int, val base: Bitmap?, val radar: Bitmap?)

private fun lonToTileX(lon: Double, zoom: Int): Int {
    val n = 1 shl zoom
    return floor((lon + 180.0) / 360.0 * n).toInt()
}

private fun latToTileY(lat: Double, zoom: Int): Int {
    val n = 1 shl zoom
    val latRad = lat * PI / 180.0
    return floor((1.0 - asinh(tan(latRad)) / PI) / 2.0 * n).toInt()
}

private fun centerLabel(offsetX: Int, offsetY: Int): String {
    if (offsetX == 0 && offsetY == 0) return "現在地"
    val eastWest = when {
        offsetX > 0 -> "東${offsetX}"
        offsetX < 0 -> "西${-offsetX}"
        else -> ""
    }
    val northSouth = when {
        offsetY > 0 -> "南${offsetY}"
        offsetY < 0 -> "北${-offsetY}"
        else -> ""
    }
    return listOf(northSouth, eastWest).filter { it.isNotBlank() }.joinToString(" ")
}

private fun Int.floorMod(divisor: Int): Int = ((this % divisor) + divisor) % divisor

private const val MIN_RADAR_ZOOM = 6
private const val DEFAULT_RADAR_ZOOM = 8
private const val MAX_RADAR_ZOOM = 10
