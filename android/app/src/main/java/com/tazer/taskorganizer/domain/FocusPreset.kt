package com.tazer.taskorganizer.domain

enum class FocusPresetMode {
    DEEP_WORK, POMODORO, CUSTOM,
}

data class FocusPreset(
    val mode: FocusPresetMode = FocusPresetMode.DEEP_WORK,
    val customWorkMinutes: Int = 50,
    val customBreakMinutes: Int = 10,
) {
    init {
        require(customWorkMinutes in WORK_RANGE && customBreakMinutes in BREAK_RANGE)
    }

    val workMinutes: Int get() = when (mode) {
        FocusPresetMode.DEEP_WORK -> 50
        FocusPresetMode.POMODORO -> 25
        FocusPresetMode.CUSTOM -> customWorkMinutes
    }

    val breakMinutes: Int get() = when (mode) {
        FocusPresetMode.DEEP_WORK -> 10
        FocusPresetMode.POMODORO -> 5
        FocusPresetMode.CUSTOM -> customBreakMinutes
    }

    internal val minimumMinutes: Int get() = minOf(25, workMinutes)
    internal val breakMs: Long get() = breakMinutes * Scheduler.MINUTE_MS

    companion object {
        val WORK_RANGE = 15..120
        val BREAK_RANGE = 1..30
        val DEFAULT = FocusPreset()
    }
}
