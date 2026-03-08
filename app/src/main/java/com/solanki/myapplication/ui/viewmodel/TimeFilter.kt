package com.solanki.myapplication.ui.viewmodel

import java.util.Calendar

enum class TimeFilter(val label: String) {
    LAST_7_DAYS("7D"),
    LAST_14_DAYS("14D"),
    LAST_30_DAYS("30D"),
    LAST_12_WEEKS("12W"),
    THREE_MONTHS("3M"),
    SIX_MONTHS("6M"),
    ONE_YEAR("1Y"),
    TWO_YEARS("2Y"),
    CUSTOM("Custom Range"),
    ALL("All"),
    THIS_MONTH("This Month");

    fun getStartTime(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        when (this) {
            LAST_7_DAYS -> calendar.add(Calendar.DAY_OF_YEAR, -7)
            LAST_14_DAYS -> calendar.add(Calendar.DAY_OF_YEAR, -14)
            LAST_30_DAYS -> calendar.add(Calendar.DAY_OF_YEAR, -30)
            LAST_12_WEEKS -> calendar.add(Calendar.WEEK_OF_YEAR, -12)
            THREE_MONTHS -> calendar.add(Calendar.MONTH, -3)
            SIX_MONTHS -> calendar.add(Calendar.MONTH, -6)
            ONE_YEAR -> calendar.add(Calendar.YEAR, -1)
            TWO_YEARS -> calendar.add(Calendar.YEAR, -2)
            THIS_MONTH -> calendar.set(Calendar.DAY_OF_MONTH, 1)
            ALL, CUSTOM -> return 0L
        }
        return calendar.timeInMillis
    }
}
