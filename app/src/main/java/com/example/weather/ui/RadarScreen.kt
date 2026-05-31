package com.example.weather.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
    var state by remember { mutableStateOf<RadarUiState>(RadarUiState.Loading) }

    LaunchedEffect(location, refreshKey) {
        state = RadarUiState.Loading
        state = runCatching { loadRadar(location) }
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
                Text("最新 ${radar.frame.validTime.toDisplayRadarTime()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                RadarTileGrid(radar)
            }
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
        }
    }
}

private suspend fun loadRadar(location: WeatherLocation): RadarUiState.Ready {
    val client = AppServices.radarClient
    val frame = client.latestFrame()
    val zoom = 8
    val centerX = lonToTileX(location.longitude, zoom)
    val centerY = latToTileY(location.latitude, zoom)
    val tiles = coroutineScope {
        (-1..1).flatMap { dy ->
            (-1..1).map { dx ->
                async {
                    val x = centerX + dx
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
    return RadarUiState.Ready(frame, tiles)
}

private sealed interface RadarUiState {
    data object Loading : RadarUiState
    data class Error(val message: String) : RadarUiState
    data class Ready(val frame: RadarFrame, val tiles: List<RadarTile>) : RadarUiState
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

private fun String.toDisplayRadarTime(): String {
    return if (length >= 12) "${substring(4, 6)}/${substring(6, 8)} ${substring(8, 10)}:${substring(10, 12)}" else this
}
