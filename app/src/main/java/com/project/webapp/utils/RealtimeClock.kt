// utils/RealtimeClock.kt
package com.project.webapp.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import kotlinx.coroutines.delay

object RealtimeClock {
    private const val UPDATE_INTERVAL_MS = 60_000L // Update every minute (or 10_000L for smoother)

    @Composable
    fun currentTimeMillis(): State<Long> {
        return produceState(initialValue = System.currentTimeMillis()) {
            while (true) {
                value = System.currentTimeMillis()
                delay(UPDATE_INTERVAL_MS)
            }
        }
    }
}