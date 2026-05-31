package com.example.weather

import android.Manifest
import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.weather.data.model.PresetLocations
import com.example.weather.data.model.WeatherLocation
import com.example.weather.data.model.WeatherSnapshot
import com.example.weather.location.LocationProvider
import com.example.weather.ui.HomeScreen
import com.example.weather.ui.HourlyScreen
import com.example.weather.ui.RadarScreen
import com.example.weather.ui.WeeklyScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppServices.init(this)

        setContent {
            WeatherTheme {
                val viewModel: WeatherViewModel = viewModel()
                val state by viewModel.uiState.collectAsState()
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions(),
                ) { permissions ->
                    if (permissions.values.any { it }) {
                        viewModel.refreshUsingDeviceLocation()
                    } else {
                        viewModel.refreshSelected()
                    }
                }

                LaunchedEffect(Unit) {
                    if (viewModel.needsPermissionPrompt()) {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                            ),
                        )
                    } else {
                        viewModel.refreshOnLaunch()
                    }
                }

                WeatherApp(
                    state = state,
                    onRefresh = viewModel::refreshSelected,
                    onUseDeviceLocation = viewModel::refreshUsingDeviceLocation,
                    onSelectLocation = viewModel::selectLocation,
                    onDismissError = viewModel::dismissError,
                )
            }
        }
    }
}

class WeatherViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppServices.repository
    private val locationProvider = LocationProvider(application)
    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(repository.weather, repository.selectedLocation) { weather, location ->
                weather to location
            }.collect { (weather, location) ->
                _uiState.update { it.copy(snapshot = weather, selectedLocation = location) }
            }
        }
    }

    fun needsPermissionPrompt(): Boolean = !locationProvider.hasLocationPermission()

    fun refreshOnLaunch() {
        if (uiState.value.snapshot == null) refreshUsingDeviceLocation() else refreshSelected()
    }

    fun refreshUsingDeviceLocation() {
        refresh(locationProvider.currentOrDefault())
    }

    fun refreshSelected() {
        refresh(uiState.value.selectedLocation)
    }

    fun selectLocation(location: WeatherLocation) {
        viewModelScope.launch {
            repository.saveLocation(location)
            refresh(location)
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun refresh(location: WeatherLocation) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            repository.refresh(location)
                .onFailure { error ->
                    _uiState.update {
                        it.copy(errorMessage = "更新できませんでした。最後に成功したデータを表示します。${error.message.orEmpty()}")
                    }
                }
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }
}

data class WeatherUiState(
    val snapshot: WeatherSnapshot? = null,
    val selectedLocation: WeatherLocation = PresetLocations.first(),
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
)

@Composable
private fun WeatherTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Color(0xFF050505),
            surface = Color(0xFF111111),
            surfaceVariant = Color(0xFF1B1B1B),
            primary = Color(0xFFC8F048),
            secondary = Color(0xFF87D7FF),
            onBackground = Color(0xFFF4F4F4),
            onSurface = Color(0xFFF4F4F4),
            onSurfaceVariant = Color(0xFFC8C8C8),
        ),
        content = content,
    )
}

@Composable
private fun WeatherApp(
    state: WeatherUiState,
    onRefresh: () -> Unit,
    onUseDeviceLocation: () -> Unit,
    onSelectLocation: (WeatherLocation) -> Unit,
    onDismissError: () -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("ホーム", "雨雲", "時間別", "週間")

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF0B0B0B)) {
                tabs.forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Text(listOf("●", "雨", "時", "週")[index]) },
                        label = { Text(label) },
                    )
                }
            }
        },
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(Modifier.fillMaxSize()) {
                when (selectedTab) {
                    0 -> HomeScreen(state, onRefresh, onUseDeviceLocation, onSelectLocation, onDismissError)
                    1 -> RadarScreen(state.selectedLocation)
                    2 -> HourlyScreen(state.snapshot)
                    3 -> WeeklyScreen(state.snapshot)
                }
            }
        }
    }
}
