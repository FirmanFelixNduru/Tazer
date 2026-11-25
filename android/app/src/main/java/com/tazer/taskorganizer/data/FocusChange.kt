package com.tazer.taskorganizer.data

import com.tazer.taskorganizer.domain.SchedulePlan

data class FocusChange(val active: WorkSession?, val plan: SchedulePlan? = null)
