package com.example.weather

import android.Manifest
import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.weather.data.model.AppUpdateInfo
import com.example.weather.data.model.DisasterSummary
import com.example.weather.data.model.NotificationSettings
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
                    val askedLocation =
                        permissions.containsKey(Manifest.permission.ACCESS_FINE_LOCATION) ||
                            permissions.containsKey(Manifest.permission.ACCESS_COARSE_LOCATION)
                    val locationGranted =
                        permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
                    if (!askedLocation) {
                        return@rememberLauncherForActivityResult
                    } else if (locationGranted) {
                        viewModel.refreshUsingDeviceLocation()
                    } else {
                        viewModel.refreshSelected()
                    }
                }

                LaunchedEffect(Unit) {
                    if (viewModel.needsPermissionPrompt()) {
                        permissionLauncher.launch(initialPermissions())
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && viewModel.needsNotificationPermission()) {
                            permissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                        }
                        viewModel.refreshOnLaunch()
                    }
                    viewModel.checkForUpdate()
                }

                WeatherApp(
                    state = state,
                    onRefresh = viewModel::refreshSelected,
                    onUseDeviceLocation = viewModel::refreshUsingDeviceLocation,
                    onSelectLocation = viewModel::selectLocation,
                    onSearchLocations = viewModel::searchLocations,
                    onMoveLocation = viewModel::moveLocation,
                    onDeleteLocation = viewModel::deleteLocation,
                    onUpdateNotificationSettings = viewModel::updateNotificationSettings,
                    onInstallUpdate = viewModel::installUpdate,
                    onOpenUpdateInBrowser = viewModel::openUpdateInBrowser,
                    onDismissUpdate = viewModel::dismissUpdate,
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
            combine(
                repository.weather,
                repository.selectedLocation,
                repository.savedLocations,
                repository.notificationSettings,
            ) { weather, location, savedLocations, notificationSettings ->
                WeatherStateBundle(weather, location, savedLocations, notificationSettings)
            }.collect { bundle ->
                _uiState.update {
                    it.copy(
                        snapshot = bundle.weather,
                        selectedLocation = bundle.location,
                        savedLocations = bundle.savedLocations,
                        notificationSettings = bundle.notificationSettings,
                    )
                }
            }
        }
    }

    fun needsPermissionPrompt(): Boolean = !locationProvider.hasLocationPermission()

    fun needsNotificationPermission(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            getApplication<Application>().checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
    }

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
            _uiState.update { it.copy(searchResults = emptyList()) }
            refresh(location)
        }
    }

    fun moveLocation(location: WeatherLocation, direction: Int) {
        viewModelScope.launch {
            repository.moveLocation(location, direction)
        }
    }

    fun deleteLocation(location: WeatherLocation) {
        viewModelScope.launch {
            repository.deleteLocation(location)
        }
    }

    fun updateNotificationSettings(settings: NotificationSettings) {
        viewModelScope.launch {
            repository.saveNotificationSettings(settings)
        }
    }

    fun searchLocations(query: String) {
        viewModelScope.launch {
            val normalized = query.trim()
            if (normalized.length < 2) {
                _uiState.update { it.copy(searchResults = emptyList(), isSearchingLocation = false) }
                return@launch
            }
            _uiState.update { it.copy(isSearchingLocation = true) }
            repository.searchLocations(normalized)
                .onSuccess { results ->
                    _uiState.update { it.copy(searchResults = results, isSearchingLocation = false) }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            searchResults = emptyList(),
                            isSearchingLocation = false,
                            errorMessage = "地点検索に失敗しました。",
                        )
                    }
                }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun dismissUpdate() {
        _uiState.update { it.copy(updateInfo = null) }
    }

    fun checkForUpdate() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingUpdate = true) }
            AppServices.updateClient.checkForUpdate(BuildConfig.VERSION_CODE)
                .onSuccess { info ->
                    _uiState.update { it.copy(updateInfo = info, isCheckingUpdate = false) }
                }
                .onFailure {
                    _uiState.update { it.copy(isCheckingUpdate = false) }
                }
        }
    }

    fun installUpdate() {
        val info = uiState.value.updateInfo ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isDownloadingUpdate = true) }
            runCatching { AppServices.updateInstaller.downloadAndOpenInstaller(info) }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(errorMessage = error.message ?: "更新APKを開けませんでした。")
                    }
                }
            _uiState.update { it.copy(isDownloadingUpdate = false) }
        }
    }

    fun openUpdateInBrowser() {
        val info = uiState.value.updateInfo ?: return
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(info.apkUrl))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        getApplication<Application>().startActivity(intent)
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
            refreshDisaster(location)
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private suspend fun refreshDisaster(location: WeatherLocation) {
        AppServices.disasterClient.fetchSummary(location)
            .onSuccess { summary ->
                _uiState.update { it.copy(disasterSummary = summary) }
            }
    }
}

private fun initialPermissions(): Array<String> {
    val permissions = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissions += Manifest.permission.POST_NOTIFICATIONS
    }
    return permissions.toTypedArray()
}

data class WeatherUiState(
    val snapshot: WeatherSnapshot? = null,
    val selectedLocation: WeatherLocation = PresetLocations.first(),
    val savedLocations: List<WeatherLocation> = PresetLocations,
    val searchResults: List<WeatherLocation> = emptyList(),
    val updateInfo: AppUpdateInfo? = null,
    val disasterSummary: DisasterSummary? = null,
    val notificationSettings: NotificationSettings = NotificationSettings(),
    val isRefreshing: Boolean = false,
    val isSearchingLocation: Boolean = false,
    val isCheckingUpdate: Boolean = false,
    val isDownloadingUpdate: Boolean = false,
    val errorMessage: String? = null,
)

private data class WeatherStateBundle(
    val weather: WeatherSnapshot?,
    val location: WeatherLocation,
    val savedLocations: List<WeatherLocation>,
    val notificationSettings: NotificationSettings,
)

@Composable
private fun WeatherTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Color(0xFF000000),
            surface = Color(0xFF151518),
            surfaceVariant = Color(0xFF202126),
            primary = Color(0xFFBFFF3C),
            secondary = Color(0xFF64D2FF),
            tertiary = Color(0xFFFFD166),
            onBackground = Color(0xFFF7F7F8),
            onSurface = Color(0xFFF7F7F8),
            onSurfaceVariant = Color(0xFFC7C7CC),
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
    onSearchLocations: (String) -> Unit,
    onMoveLocation: (WeatherLocation, Int) -> Unit,
    onDeleteLocation: (WeatherLocation) -> Unit,
    onUpdateNotificationSettings: (NotificationSettings) -> Unit,
    onInstallUpdate: () -> Unit,
    onOpenUpdateInBrowser: () -> Unit,
    onDismissUpdate: () -> Unit,
    onDismissError: () -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("ホーム", "雨雲", "時間", "週間")
    val icons = listOf("●", "雨", "時", "週")

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF09090B)) {
                tabs.forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Text(icons[index]) },
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
                    0 -> HomeScreen(
                        state = state,
                        onRefresh = onRefresh,
                        onUseDeviceLocation = onUseDeviceLocation,
                        onSelectLocation = onSelectLocation,
                        onSearchLocations = onSearchLocations,
                        onMoveLocation = onMoveLocation,
                        onDeleteLocation = onDeleteLocation,
                        onUpdateNotificationSettings = onUpdateNotificationSettings,
                        onDismissError = onDismissError,
                    )
                    1 -> RadarScreen(state.selectedLocation)
                    2 -> HourlyScreen(state.snapshot)
                    3 -> WeeklyScreen(state.snapshot)
                }
            }
        }
    }

    val updateInfo = state.updateInfo
    if (updateInfo != null) {
        AlertDialog(
            onDismissRequest = onDismissUpdate,
            title = { Text("アップデートがあります") },
            text = {
                Text(
                    if (state.isDownloadingUpdate) {
                        "version ${updateInfo.versionName} をダウンロードしています。"
                    } else {
                        "新しい version ${updateInfo.versionName} をインストールできます。初回だけ「この提供元のアプリを許可」が必要な場合があります。"
                    },
                )
            },
            confirmButton = {
                Row {
                    TextButton(
                        onClick = onOpenUpdateInBrowser,
                        enabled = !state.isDownloadingUpdate,
                    ) {
                        Text("ブラウザで開く")
                    }
                    TextButton(
                        onClick = onInstallUpdate,
                        enabled = !state.isDownloadingUpdate,
                    ) {
                        Text(if (state.isDownloadingUpdate) "準備中" else "更新する")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissUpdate, enabled = !state.isDownloadingUpdate) {
                    Text("あとで")
                }
            },
        )
    }
}
