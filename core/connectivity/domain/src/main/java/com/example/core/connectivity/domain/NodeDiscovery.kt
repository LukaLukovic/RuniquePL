package com.example.core.connectivity.domain

import kotlinx.coroutines.flow.Flow

interface NodeDiscovery {

    fun observceConnectedDevices(localDeviceType: DeviceType): Flow<Set<DeviceNode>>

}