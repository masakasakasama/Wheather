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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.weather.AppServices
import com.example.weather.data.model.RadarFrame
import com.example.weather.data.model.WeatherLocation
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.math.PI
import kotlin.math.asinh
import kotlin.math.floor
import kotlin.math.tan

@Composable
fun RadarScreen(location: WeatherLocation) {
    var refreshKey by remember { mutableIntStateOf(0) }
    var zoom by remember(location) { mutableIntStateOf(8) }
    var tileOffsetX by remember(location) { mutableIntStateOf(0) }
    var tileOffsetY by remember(location) { mutableIntStateOf(0) }
    var state by remember { mutableStateOf<RadarUiState>(RadarUiState.Loading) }

    LaunchedEffect(location, refreshKey, zoom, tileOffsetX, tileOffsetY) {
        state = RadarUiState.Loading
        state = runCatching { loadRadar(location, zoom, tileOffsetX, tileOffsetY) }
            .getOrElse { RadarUiState.Error("雨雲レーダーを取得できません") }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("雨雲レーダー", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(location.name, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = { refreshKey++ }) { Text("更新") }
        }
        when (val radar = state) {
            RadarUiState.Loading -> CircularProgressIndicator()
            is RadarUiState.Error -> Text(radar.message, color = MaterialTheme.colorScheme.error)
            is RadarUiState.Ready -> {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("最新 ${radar.frame.validTime.toDisplayRadarTime()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Z${radar.zoom} ${radar.centerLabel}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
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
                RadarTileGrid(radar)
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            FilledTonalButton(onClick = onZoomOut, enabled = zoom > MIN_RADAR_ZOOM) { Text("-") }
            Text("ズーム $zoom", color = MaterialTheme.colorScheme.onSurfaceVariant)
            FilledTonalButton(onClick = onZoomIn, enabled = zoom < MAX_RADAR_ZOOM) { Text("+") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onReset) { Text("現在地へ") }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            FilledTonalButton(onClick = { onMove(0, -1) }) { Text("↑") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FilledTonalButton(onClick = { onMove(-1, 0) }) { Text("←") }
                Text("移動", color = MaterialTheme.colorScheme.onSurfaceVariant)
                FilledTonalButton(onClick = { onMove(1, 0) }) { Text("→") }
            }
            FilledTonalButton(onClick = { onMove(0, 1) }) { Text("↓") }
        }
    }
}

@Composable
private fun RadarTileGrid(radar: RadarUiState.Ready) {
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF111111)),
        contentAlignment = Alignment.Center,
    ) {
        val tileSize = maxWidth / 3
        Box(Modifier.size(maxWidth)) {
            radar.tiles.forEach { tile ->
                val modifier = Modifier
                    .size(tileSize)
                    .offset(tileSize * tile.dx, tileSize * tile.dy)
                tile.base?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = null,
                        modifier = modifier,
                        contentScale = ContentScale.FillBounds,
                    )
                }
                tile.radar?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = null,
                        modifier = modifier.alpha(0.7f),
                        contentScale = ContentScale.FillBounds,
                    )
                }
            }
            Text(
                "＋",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                "中心",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 26.dp),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

private suspend fun loadRadar(
    location: WeatherLocation,
    zoom: Int,
    tileOffsetX: Int,
    tileOffsetY: Int,
): RadarUiState.Ready {
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
    return RadarUiState.Ready(
        frame = frame,
        zoom = zoom,
        centerLabel = centerLabel(tileOffsetX, tileOffsetY),
        tiles = tiles,
    )
}

private sealed interface RadarUiState {
    data object Loading : RadarUiState
    data class Error(val message: String) : RadarUiState
    data class Ready(
        val frame: RadarFrame,
        val zoom: Int,
        val centerLabel: String,
        val tiles: List<RadarTile>,
    ) : RadarUiState
}

private data class RadarTile(
    val dx: Int,
    val dy: Int,
    val base: Bitmap?,
    val radar: Bitmap?,
)

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

private fun String.toDisplayRadarTime(): String {
    return if (length >= 12) "${substring(4, 6)}/${substring(6, 8)} ${substring(8, 10)}:${substring(10, 12)}" else this
}

private const val MIN_RADAR_ZOOM = 6
private const val DEFAULT_RADAR_ZOOM = 8
private const val MAX_RADAR_ZOOM = 10
