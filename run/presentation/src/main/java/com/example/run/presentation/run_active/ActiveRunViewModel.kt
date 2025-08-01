package com.example.run.presentation.run_active

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.connectivity.domain.messaging.MessagingAction
import com.example.core.domain.location.Location
import com.example.core.domain.run.Run
import com.example.core.domain.run.RunRepository
import com.example.core.domain.util.Result
import com.example.core.presentation.ui.asUiText
import com.example.run.domain.LocationDataCalculator
import com.example.run.domain.RunningTracker
import com.example.run.domain.WatchConnector
import com.example.core.notification.service.ActiveRunService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.roundToInt

class ActiveRunViewModel(
    private val runningTracker: RunningTracker,
    private val runRepository: RunRepository,
    private val watchConnector: WatchConnector,
    private val applicationScope: CoroutineScope
) : ViewModel() {

    var state by mutableStateOf(
        ActiveRunState(
            shouldTrack = ActiveRunService.isServiceRunning.value && runningTracker.isTracking.value,
            hasStartedRunning = ActiveRunService.isServiceRunning.value
        )
    )
        private set

    private val eventChannel = Channel<ActiveRunEvent>()
    val events = eventChannel.receiveAsFlow()

    private val shouldTrack = snapshotFlow { state.shouldTrack }
        .stateIn(viewModelScope, SharingStarted.Lazily, state.shouldTrack)
    private val hasLocationPermission = MutableStateFlow(false)

    private val isTracking = combine(
        shouldTrack,
        hasLocationPermission
    ) { shouldTrack, hasPermission ->
        shouldTrack && hasPermission
    }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    init {
        watchConnector
            .connectedDevice
            .filterNotNull()
            .onEach {
                Timber.d("New device detected: ${it.displayName}")
            }
            .launchIn(viewModelScope)

        hasLocationPermission
            .onEach { hasPermission ->
                if (hasPermission) {
                    runningTracker.startObservingLocation()
                } else {
                    runningTracker.stopObservingLocation()
                }
            }
            .launchIn(viewModelScope)

        isTracking
            .onEach { isTracking ->
                runningTracker.setIsTracking(isTracking)
            }.launchIn(viewModelScope)

        runningTracker
            .currentLocation
            .onEach {
                state = state.copy(currentLocation = it?.location)
            }
            .launchIn(viewModelScope)

        runningTracker
            .runData
            .onEach {
                state = state.copy(runData = it)
            }
            .launchIn(viewModelScope)

        runningTracker
            .elapsedTime
            .onEach {
                state = state.copy(elapsedTime = it)
            }
            .launchIn(viewModelScope)

        listenToWatchUpdates()
    }

    fun onAction(action: ActiveRunAction, triggeredOnWatch: Boolean = false) {
        if (triggeredOnWatch.not()) {
            val messagingAction = when (action) {
                ActiveRunAction.OnFinishRunClick -> MessagingAction.Finish
                ActiveRunAction.OnResumeRunClick -> MessagingAction.StartOrResume
                ActiveRunAction.OnToggleRunClick -> {
                    if (state.hasStartedRunning) {
                        MessagingAction.Pause
                    } else {
                        MessagingAction.StartOrResume
                    }
                }

                else -> null
            }
            messagingAction?.let {
                viewModelScope.launch {
                    watchConnector.sendActionToWatch(it)
                }
            }
        }
        when (action) {
            ActiveRunAction.OnBackClick -> {
                state = state.copy(shouldTrack = false)
            }

            ActiveRunAction.OnFinishRunClick -> {
                state = state.copy(
                    isRunFinished = true,
                    isSavingRun = true
                )
            }

            ActiveRunAction.OnResumeRunClick -> {
                state = state.copy(
                    shouldTrack = true
                )
            }

            ActiveRunAction.OnToggleRunClick -> {
                state = state.copy(
                    hasStartedRunning = true,
                    shouldTrack = !state.shouldTrack
                )
            }

            is ActiveRunAction.SubmitLocationPermissionInfo -> {

                hasLocationPermission.update { action.acceptedLocationPermission }
                state = state.copy(
                    showLocationRationale = action.showLocationRationale
                )
            }

            is ActiveRunAction.SubmitNotificationPermissionInfo -> {
                state = state.copy(
                    showLocationRationale = action.showNotificationRationale
                )
            }

            ActiveRunAction.DismissRationaleDialog -> {
                state = state.copy(
                    showNotificationRationale = false,
                    showLocationRationale = false
                )
            }

            is ActiveRunAction.OnRunProcessed -> {
                finishRun(action.mapPictureBytes)
            }

            else -> Unit
        }
    }

    private fun finishRun(mapPictureBytes: ByteArray) {
        val locations = state.runData.locations
        if (locations.isEmpty() || locations.first().size <= 1) {
            state = state.copy(isSavingRun = false)
            return
        }
        viewModelScope.launch {
            val run = Run(
                id = null,
                duration = state.elapsedTime,
                dateTimeUtc = ZonedDateTime.now().withZoneSameInstant(ZoneId.of("UTC")),
                distanceMeters = state.runData.distanceInMeters,
                location = state.currentLocation ?: Location(0.0, 0.0),
                maxSpeedKmh = LocationDataCalculator.getMaxSpeedKmh(locations),
                totalElevationMeters = LocationDataCalculator.getTotalElevationMeters(locations),
                mapPictureUrl = null,
                avgHeartRate = if (state.runData.heartRates.isEmpty())
                    null
                else {
                    state.runData.heartRates.average().roundToInt()
                },
                maxHeartRate = if (state.runData.heartRates.isEmpty())
                    null
                else {
                    state.runData.heartRates.max()
                },
            )

            runningTracker.finishRun()
            when (val result = runRepository.upsertRun(run, mapPictureBytes)) {
                is Result.Error -> {
                    eventChannel.send(ActiveRunEvent.Error(result.error.asUiText()))
                }

                is Result.Success -> {
                    eventChannel.send(ActiveRunEvent.RunSaved)
                }
            }

            state = state.copy(
                isSavingRun = false
            )
        }
    }

    private fun listenToWatchUpdates() {
        watchConnector
            .messagingActions
            .onEach { action ->
                when (action) {
                    MessagingAction.ConnectionRequest -> {
                        if (isTracking.value) {
                            watchConnector.sendActionToWatch(MessagingAction.StartOrResume)
                        }
                    }

                    MessagingAction.Finish -> {
                        onAction(
                            action = ActiveRunAction.OnFinishRunClick,
                            triggeredOnWatch = true
                        )
                    }

                    MessagingAction.Pause -> {
                        onAction(
                            action = ActiveRunAction.OnToggleRunClick,
                            triggeredOnWatch = true
                        )
                    }

                    MessagingAction.StartOrResume -> {
                        if (isTracking.value.not()) {
                            if (state.hasStartedRunning) {
                                onAction(
                                    action = ActiveRunAction.OnResumeRunClick,
                                    triggeredOnWatch = true
                                )
                            } else {
                                onAction(
                                    action = ActiveRunAction.OnToggleRunClick,
                                    triggeredOnWatch = true
                                )
                            }
                        }
                    }

                    else -> Unit
                }
            }.launchIn(viewModelScope)
    }

    override fun onCleared() {
        super.onCleared()
        if (com.example.core.notification.service.ActiveRunService.isServiceRunning.value.not()) {
            applicationScope.launch {
                watchConnector.sendActionToWatch(MessagingAction.Untrackable)
            }
            viewModelScope
            runningTracker.stopObservingLocation()
        }
    }
}