@file:OptIn(FlowPreview::class)
@file:Suppress("OPT_IN_USAGE")

package com.example.wear.run.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.connectivity.domain.messaging.MessagingAction
import com.example.core.domain.util.Result
import com.example.core.notification.service.ActiveRunService
import com.example.wear.run.domain.ExerciseTracker
import com.example.wear.run.domain.PhoneConnector
import com.example.wear.run.domain.RunningTracker
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class TrackerViewModel(
    private val exerciseTracker: ExerciseTracker,
    private val phoneConnector: PhoneConnector,
    private val runningTracker: RunningTracker
) : ViewModel() {

    var state by mutableStateOf(
        TrackerState(
            hasStartedRunning = ActiveRunService.isServiceRunning.value,
            isRunActive = ActiveRunService.isServiceRunning.value && runningTracker.isTracking.value,
            isTrackable = ActiveRunService.isServiceRunning.value
        )
    )
        private set

    private val eventChannel = Channel<TrackerEvent>()
    val events = eventChannel.receiveAsFlow()

    private val hasBodySensorPermission = MutableStateFlow(false)

    private val isTracking = snapshotFlow {
        state.isRunActive && state.isTrackable && state.isConnectedPhoneNearby
    }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    init {
        phoneConnector
            .connectedNode
            .filterNotNull()
            .onEach { connectedNode ->
                state = state.copy(
                    isConnectedPhoneNearby = connectedNode.isNearby
                )
            }
            .combine(isTracking) { _, istracking ->
                if (istracking.not()) {
                    phoneConnector.sendActionToPhone(MessagingAction.ConnectionRequest)
                }
            }
            .launchIn(viewModelScope)

        runningTracker
            .isTrackable
            .onEach { isTrackable ->
                state = state.copy(isTrackable = isTrackable)
            }
            .launchIn(viewModelScope)

        isTracking
            .onEach { isTracking ->
                val result = when {
                    isTracking && state.hasStartedRunning.not() -> {
                        exerciseTracker.startExercise()
                    }

                    isTracking && state.hasStartedRunning -> {
                        exerciseTracker.resumeExercise()
                    }

                    isTracking.not() && state.hasStartedRunning -> {
                        exerciseTracker.pauseExercise()
                    }

                    else -> Result.Success(Unit)
                }

                if (result is Result.Error) {
                    result.error.toUiText()?.let {
                        eventChannel.send(TrackerEvent.Error(it))
                    }
                }

                if (isTracking) {
                    state = state.copy(hasStartedRunning = true)
                }
                runningTracker.setIstracking(isTracking)

            }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            val isHeartRateTrackingSupported = exerciseTracker.isHeartRateTrackingSupported()
            state = state.copy(canTrackHeartRate = isHeartRateTrackingSupported)
        }

        val isAmbientMode = snapshotFlow { state.isAmbientModeActive }

        isAmbientMode
            .flatMapLatest { isAmbientMode ->
                if (isAmbientMode) {
                    runningTracker
                        .heartRate
                        .sample(10.seconds)
                } else {
                    runningTracker.heartRate
                }
            }.onEach {
                state = state.copy(heartRate = it)
            }
            .launchIn(viewModelScope)

        isAmbientMode
            .flatMapLatest { isAmbientMode ->
                if (isAmbientMode) {
                    runningTracker
                        .distanceMeters
                        .sample(10.seconds)
                } else {
                    runningTracker.distanceMeters
                }
            }.onEach {
                state = state.copy(distanceMeters = it)
            }
            .launchIn(viewModelScope)

        isAmbientMode
            .flatMapLatest { isAmbientMode ->
                if (isAmbientMode) {
                    runningTracker
                        .elapsedTime
                        .sample(10.seconds)
                } else {
                    runningTracker.elapsedTime
                }
            }.onEach {
                state = state.copy(elapsedDuration = it)
            }
            .launchIn(viewModelScope)

        listenToPhoneActions()
    }

    fun onAction(action: TrackerAction, triggeredOnPhone: Boolean = false) {
        if (triggeredOnPhone.not()) {
            sendActionToPhone(action)
        }
        when (action) {
            is TrackerAction.OnBodySensorPermissionResult -> {
                hasBodySensorPermission.update { action.isGranted }

                if (action.isGranted) {
                    viewModelScope.launch {
                        val isHeartTrackingSupported =
                            exerciseTracker.isHeartRateTrackingSupported()
                        state = state.copy(
                            canTrackHeartRate = isHeartTrackingSupported
                        )
                        exerciseTracker.prepareExercise()
                        exerciseTracker.startExercise()
                    }
                }
            }

            TrackerAction.OnFinishRunClick -> {
                viewModelScope.launch {
                    exerciseTracker.stopExercise()
                    eventChannel.send(TrackerEvent.RunFinished)
                }

                state = state.copy(
                    elapsedDuration = Duration.ZERO,
                    distanceMeters = 0,
                    heartRate = 0,
                    hasStartedRunning = false,
                    isRunActive = false
                )
            }

            TrackerAction.OnToggleRunClick -> {
                if (state.isTrackable) {
                    state = state.copy(
                        isRunActive = state.isRunActive.not()
                    )
                }
            }

            is TrackerAction.OnEnterAmbientMode -> {
                state = state.copy(
                    isAmbientModeActive = true,
                    burnInProtectionRequired = action.burnInProtectionRequired
                )
            }

            TrackerAction.OnExitAmbientMode -> {
                state = state.copy(
                    isAmbientModeActive = false
                )
            }
        }
    }

    private fun sendActionToPhone(action: TrackerAction) {
        viewModelScope.launch {
            val messagingAction = when (action) {
                is TrackerAction.OnFinishRunClick -> MessagingAction.Finish
                is TrackerAction.OnToggleRunClick -> {
                    if (state.isRunActive) {
                        MessagingAction.Pause
                    } else {
                        MessagingAction.StartOrResume
                    }
                }

                else -> null
            }

            messagingAction?.let {
                val result = phoneConnector.sendActionToPhone(it)
                if (result is Result.Error) {
                    println("error ${result.error}")
                }
            }
        }
    }

    private fun listenToPhoneActions() {
        phoneConnector
            .messagingActions
            .onEach { action ->
                when (action) {
                    MessagingAction.Finish -> {
                        onAction(TrackerAction.OnFinishRunClick, triggeredOnPhone = true)
                    }

                    MessagingAction.Pause -> {
                        if (state.isTrackable) {
                            state = state.copy(isRunActive = false)
                        }
                    }

                    MessagingAction.StartOrResume -> {
                        if (state.isTrackable) {
                            state = state.copy(isRunActive = true)
                        }
                    }

                    MessagingAction.Trackable -> {
                        state = state.copy(isTrackable = true)
                    }

                    MessagingAction.Untrackable -> {
                        state = state.copy(isTrackable = false)
                    }

                    else -> Unit
                }
            }
            .launchIn(viewModelScope)
    }
}