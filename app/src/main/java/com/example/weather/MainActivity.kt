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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.weather.data.model.AppUpdateInfo
import com.example.weather.data.model.DisasterSummary
import com.example.weather.data.model.NotificationSettings
import com.example.weather.data.model.PresetLocations
import com.example.weather.data.model.WeatherLocation
import com.example.weather.data.model.WeatherSnapshot
import com.example.weather.data.model.sameForecastPlaceAs
import com.example.weather.location.LocationProvider
import com.example.weather.ui.HomeScreen
import com.example.weather.ui.WeatherTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppServices.init(this)

        setContent {
            WeatherTheme {
                val viewModel: WeatherViewModel = viewModel()
                val state by viewModel.uiState.collectAsState()
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner, viewModel) {
                    val observer = LifecycleEventObserver { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_START -> viewModel.startForegroundRefresh()
                            Lifecycle.Event.ON_STOP -> viewModel.stopForegroundRefresh()
                            else -> Unit
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                        viewModel.startForegroundRefresh()
                    }
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                        viewModel.stopForegroundRefresh()
                    }
                }
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
                    appVersionName = BuildConfig.VERSION_NAME,
                    onRefresh = viewModel::refreshSelected,
                    onUseDeviceLocation = {
                        if (viewModel.needsPermissionPrompt()) {
                            permissionLauncher.launch(locationPermissions())
                        } else {
                            viewModel.refreshUsingDeviceLocation()
                        }
                    },
                    onSelectLocation = viewModel::selectLocation,
                    onSearchLocations = viewModel::searchLocations,
                    onMoveLocation = viewModel::moveLocation,
                    onDeleteLocation = viewModel::deleteLocation,
                    onUpdateNotificationSettings = viewModel::updateNotificationSettings,
                    onCheckUpdate = { viewModel.checkForUpdate(manual = true) },
                    onDismissUpdateCheckMessage = viewModel::dismissUpdateCheckMessage,
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
    private var locationSearchJob: Job? = null
    private var locationSearchSequence = 0
    private var weatherRefreshJob: Job? = null
    private var weatherRefreshSequence = 0
    private var foregroundRefreshJob: Job? = null

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
                        snapshot = bundle.weather?.takeIf {
                            it.location.sameForecastPlaceAs(bundle.location)
                        },
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
        viewModelScope.launch {
            val cachedWeather = repository.cachedWeatherOnce()
            if (cachedWeather == null) {
                val initialLocation = locationProvider.currentOrDefault()
                repository.saveLocation(initialLocation)
                refresh(initialLocation)
            } else {
                refresh(repository.selectedLocationOnce())
            }
        }
    }

    fun refreshUsingDeviceLocation() {
        val location = locationProvider.currentOrDefault()
        viewModelScope.launch {
            repository.saveLocation(location)
            refresh(location)
        }
    }

    fun refreshSelected() {
        refresh(uiState.value.selectedLocation)
    }

    fun startForegroundRefresh() {
        if (foregroundRefreshJob?.isActive == true) return
        val snapshot = uiState.value.snapshot
        if (
            snapshot != null &&
            System.currentTimeMillis() - snapshot.updatedAtMillis >= FOREGROUND_REFRESH_INTERVAL_MILLIS &&
            weatherRefreshJob?.isActive != true
        ) {
            refreshSelected()
        }
        foregroundRefreshJob = viewModelScope.launch {
            delay(nextAlignedWeatherRefreshDelay(System.currentTimeMillis()))
            while (isActive) {
                if (weatherRefreshJob?.isActive != true) refreshSelected()
                delay(FOREGROUND_REFRESH_INTERVAL_MILLIS)
            }
        }
    }

    fun stopForegroundRefresh() {
        foregroundRefreshJob?.cancel()
        foregroundRefreshJob = null
    }

    fun selectLocation(location: WeatherLocation) {
        viewModelScope.launch {
            repository.saveLocation(location)
            _uiState.update { it.copy(searchResults = emptyList(), locationSearchMessage = null) }
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
            repository.deleteLocation(location)?.let { replacement ->
                refresh(replacement)
            }
        }
    }

    fun updateNotificationSettings(settings: NotificationSettings) {
        viewModelScope.launch {
            repository.saveNotificationSettings(settings)
        }
    }

    fun searchLocations(query: String) {
        val normalized = query.trim()
        locationSearchJob?.cancel()
        val sequence = ++locationSearchSequence
        if (normalized.length < 2) {
            _uiState.update {
                it.copy(
                    searchResults = emptyList(),
                    isSearchingLocation = false,
                    locationSearchMessage = "都市名を2文字以上入力してください",
                )
            }
            return
        }
        locationSearchJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    searchResults = emptyList(),
                    isSearchingLocation = true,
                    locationSearchMessage = null,
                )
            }
            repository.searchLocations(normalized)
                .onSuccess { results ->
                    if (sequence != locationSearchSequence) return@onSuccess
                    _uiState.update {
                        it.copy(
                            searchResults = results,
                            isSearchingLocation = false,
                            locationSearchMessage = if (results.isEmpty()) {
                                "「$normalized」は見つかりません。綴りや言語を変えてください"
                            } else {
                                "${results.size}件見つかりました"
                            },
                        )
                    }
                }
                .onFailure {
                    if (sequence != locationSearchSequence) return@onFailure
                    _uiState.update {
                        it.copy(
                            searchResults = emptyList(),
                            isSearchingLocation = false,
                            locationSearchMessage = "検索できませんでした。通信状態を確認して再試行してください",
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

    fun checkForUpdate(manual: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingUpdate = true, updateCheckMessage = null) }
            AppServices.updateClient.checkForUpdate(BuildConfig.VERSION_CODE)
                .onSuccess { info ->
                    _uiState.update {
                        it.copy(
                            updateInfo = info,
                            isCheckingUpdate = false,
                            updateCheckMessage = when {
                                info != null -> null
                                manual -> "お使いのバージョンが最新です"
                                else -> null
                            },
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isCheckingUpdate = false,
                            updateCheckMessage = if (manual) "アップデートを確認できませんでした" else null,
                        )
                    }
                }
        }
    }

    fun dismissUpdateCheckMessage() {
        _uiState.update { it.copy(updateCheckMessage = null) }
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
        weatherRefreshJob?.cancel()
        val sequence = ++weatherRefreshSequence
        weatherRefreshJob = viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            repository.refresh(location)
                .onFailure { error ->
                    if (sequence != weatherRefreshSequence) return@onFailure
                    _uiState.update {
                        val message = if (it.snapshot != null) {
                            "更新できませんでした。最後に成功したこの地点のデータを表示します。"
                        } else {
                            "この地点の天気を取得できませんでした。"
                        }
                        it.copy(errorMessage = message + error.message.orEmpty())
                    }
                }
            if (sequence != weatherRefreshSequence) return@launch
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

internal fun nextAlignedWeatherRefreshDelay(nowMillis: Long): Long {
    val remainder = Math.floorMod(nowMillis, FOREGROUND_REFRESH_INTERVAL_MILLIS)
    val candidate = Math.floorMod(FOREGROUND_REFRESH_OFFSET_MILLIS - remainder, FOREGROUND_REFRESH_INTERVAL_MILLIS)
    return if (candidate < MIN_FOREGROUND_INITIAL_DELAY_MILLIS) {
        candidate + FOREGROUND_REFRESH_INTERVAL_MILLIS
    } else {
        candidate
    }
}

private const val FOREGROUND_REFRESH_INTERVAL_MILLIS = 10 * 60 * 1000L
private const val FOREGROUND_REFRESH_OFFSET_MILLIS = 5 * 60 * 1000L
private const val MIN_FOREGROUND_INITIAL_DELAY_MILLIS = 60 * 1000L

private fun initialPermissions(): Array<String> {
    val permissions = locationPermissions().toMutableList()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissions += Manifest.permission.POST_NOTIFICATIONS
    }
    return permissions.toTypedArray()
}

private fun locationPermissions(): Array<String> = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

data class WeatherUiState(
    val snapshot: WeatherSnapshot? = null,
    val selectedLocation: WeatherLocation = PresetLocations.first(),
    val savedLocations: List<WeatherLocation> = PresetLocations,
    val searchResults: List<WeatherLocation> = emptyList(),
    val locationSearchMessage: String? = null,
    val updateInfo: AppUpdateInfo? = null,
    val disasterSummary: DisasterSummary? = null,
    val notificationSettings: NotificationSettings = NotificationSettings(),
    val isRefreshing: Boolean = false,
    val isSearchingLocation: Boolean = false,
    val isCheckingUpdate: Boolean = false,
    val isDownloadingUpdate: Boolean = false,
    val updateCheckMessage: String? = null,
    val errorMessage: String? = null,
)

private data class WeatherStateBundle(
    val weather: WeatherSnapshot?,
    val location: WeatherLocation,
    val savedLocations: List<WeatherLocation>,
    val notificationSettings: NotificationSettings,
)

@Composable
private fun WeatherApp(
    state: WeatherUiState,
    appVersionName: String,
    onRefresh: () -> Unit,
    onUseDeviceLocation: () -> Unit,
    onSelectLocation: (WeatherLocation) -> Unit,
    onSearchLocations: (String) -> Unit,
    onMoveLocation: (WeatherLocation, Int) -> Unit,
    onDeleteLocation: (WeatherLocation) -> Unit,
    onUpdateNotificationSettings: (NotificationSettings) -> Unit,
    onCheckUpdate: () -> Unit,
    onDismissUpdateCheckMessage: () -> Unit,
    onInstallUpdate: () -> Unit,
    onOpenUpdateInBrowser: () -> Unit,
    onDismissUpdate: () -> Unit,
    onDismissError: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        color = MaterialTheme.colorScheme.background,
    ) {
        HomeScreen(
            state = state,
            appVersionName = appVersionName,
            onRefresh = onRefresh,
            onUseDeviceLocation = onUseDeviceLocation,
            onSelectLocation = onSelectLocation,
            onSearchLocations = onSearchLocations,
            onMoveLocation = onMoveLocation,
            onDeleteLocation = onDeleteLocation,
            onUpdateNotificationSettings = onUpdateNotificationSettings,
            onCheckUpdate = onCheckUpdate,
            onDismissUpdateCheckMessage = onDismissUpdateCheckMessage,
            onDismissError = onDismissError,
        )
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
