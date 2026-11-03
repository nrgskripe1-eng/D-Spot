package com.mika.dspot.expect.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.AVFoundation.AVAudioSession

@Composable
actual fun rememberDeviceVolumeController(): DeviceVolumeController? {
    return remember {
        object : DeviceVolumeController {
            override val volumeFraction: Float
                get() = AVAudioSession.sharedInstance().outputVolume

            override fun setVolumeFraction(fraction: Float) {
                // iOS controls hardware volume via MPVolumeView slider, direct setter is restricted by OS
            }
        }
    }
}

