package com.simplemobiletools.calendar.pro.helpers

import android.content.Context
import com.simplemobiletools.calendar.pro.extensions.config
import com.simplemobiletools.calendar.pro.extensions.eventsHelper
import com.simplemobiletools.calendar.pro.extensions.getProperDayIndexInWeek
import com.simplemobiletools.calendar.pro.extensions.isWeekendIndex
import com.simplemobiletools.calendar.pro.extensions.seconds
import com.simplemobiletools.calendar.pro.interfaces.MonthlyCalendar
import com.simplemobiletools.calendar.pro.models.DayMonthly
import com.simplemobiletools.calendar.pro.models.Event
import org.joda.time.DateTime
import kotlin.math.min

class MonthlyCalendarImpl(val callback: MonthlyCalendar, val context: Context) {
    private val DAYS_CNT = 42
    private val YEAR_PATTERN = "YYYY"

    private val mToday: String = DateTime().toString(Formatter.DAYCODE_PATTERN)
    private var mEvents = ArrayList<Event>()

    lateinit var mTargetDate: DateTime

    fun updateMonthlyCalendar(targetDate: DateTime) {
        mTargetDate = targetDate
        val startTS = mTargetDate.minusDays(7).seconds()
        val endTS = mTargetDate.plusDays(43).seconds()
        context.eventsHelper.getEvents(startTS, endTS) {
            gotEvents(it)
        }
    }

    fun getMonth(targetDate: DateTime) {
        updateMonthlyCalendar(targetDate)
    }

    fun getDays(markDaysWithEvents: Boolean) {
        val days = if (context.config.useLunisolarCalendar) {
            getLunisolarDays()
        } else {
            getGregorianDays()
        }

        if (markDaysWithEvents) {
            markDaysWithEvents(days)
        } else {
            callback.updateMonthlyCalendar(context, monthName, days, false, mTargetDate)
        }
    }

    private fun getGregorianDays(): ArrayList<DayMonthly> {
        val days = ArrayList<DayMonthly>(DAYS_CNT)
        val firstDayOfMonth = mTargetDate.withDayOfMonth(1)
        val firstDayIndex = context.getProperDayIndexInWeek(firstDayOfMonth)

        val currMonthDays = mTargetDate.dayOfMonth().maximumValue
        val prevMonthDays = mTargetDate.minusMonths(1).dayOfMonth().maximumValue

        var isThisMonth = false
        var isToday: Boolean
        var value = prevMonthDays - firstDayIndex + 1
        var curDay = mTargetDate

        for (i in 0 until DAYS_CNT) {
            when {
                i < firstDayIndex -> {
                    isThisMonth = false
                    curDay = mTargetDate.withDayOfMonth(1).minusMonths(1)
                }
                i == firstDayIndex -> {
                    value = 1
                    isThisMonth = true
                    curDay = mTargetDate
                }
                value == currMonthDays + 1 -> {
                    value = 1
                    isThisMonth = false
                    curDay = mTargetDate.withDayOfMonth(1).plusMonths(1)
                }
            }

            isToday = isToday(curDay, value)

            val newDay = curDay.withDayOfMonth(value)
            val dayCode = Formatter.getDayCodeFromDateTime(newDay)
            val day = DayMonthly(value, isThisMonth, isToday, dayCode, newDay.weekOfWeekyear, ArrayList(), i, context.isWeekendIndex(i))
            days.add(day)
            value++
        }
        return days
    }

    private fun getLunisolarDays(): ArrayList<DayMonthly> {
        // Convert current Gregorian date to lunisolar to find current lunar month
        val currentLunar = LunisolarCalendar.gregorianToLunar(mTargetDate.year, mTargetDate.monthOfYear, mTargetDate.dayOfMonth)
        
        // Handle edge cases
        if (currentLunar.lunarDay == 0) return getGregorianDays()
        
        val lunarYear = currentLunar.lunarYear
        val lunarMonth = currentLunar.lunarMonth
        val monthLength = LunisolarCalendar.getLunarMonthLength(lunarYear, lunarMonth)
        
        // Calculate grid size (need at least 5 weeks for 29-30 days)
        val gridSize = if (monthLength <= 28) 35 else 42 // 5 or 6 weeks
        val days = ArrayList<DayMonthly>(gridSize)
        
        // Get first day of lunar month in Gregorian
        val firstDayGregorian = LunisolarCalendar.lunarToGregorian(lunarYear, lunarMonth, 1)
        if (firstDayGregorian == null) return getGregorianDays()
        
        val firstDayJD = LunisolarCalendar.gregorianToJulianDay(firstDayGregorian.first, firstDayGregorian.second, firstDayGregorian.third)
        val firstDayOfWeek = LunisolarCalendar.calculateWeekday(firstDayGregorian.first, firstDayGregorian.second, firstDayGregorian.third)
        val firstDayIndex = firstDayOfWeek.ordinal // 0=Sunday, 1=Monday, etc.
        
        // Build the grid
        for (i in 0 until gridSize) {
            val currentJD = firstDayJD - firstDayIndex + i
            val (gYear, gMonth, gDay) = LunisolarCalendar.julianDayToGregorian(currentJD)
            
            // Convert each day to lunar date
            val dayLunar = LunisolarCalendar.gregorianToLunar(gYear, gMonth, gDay)
            
            // Determine if this day is in the current lunar month
            val isThisMonth = dayLunar.lunarYear == lunarYear && dayLunar.lunarMonth == lunarMonth
            
            // Always use lunar day number (even for preview days from other months)
            val dayValue = if (dayLunar.lunarDay > 0) dayLunar.lunarDay else gDay // Fallback to Gregorian if lunar conversion fails
            
            val gregorianDateTime = DateTime(gYear, gMonth, gDay, 0, 0)
            val isToday = isToday(gregorianDateTime, gDay)
            val dayCode = Formatter.getDayCodeFromDateTime(gregorianDateTime)
            
            val day = DayMonthly(
                value = dayValue,
                isThisMonth = isThisMonth,
                isToday = isToday,
                code = dayCode,
                weekOfYear = gregorianDateTime.weekOfWeekyear,
                dayEvents = ArrayList(),
                indexOnMonthView = i,
                isWeekend = context.isWeekendIndex(i % 7)
            )
            days.add(day)
        }
        
        return days
    }

    // it works more often than not, don't touch
    private fun markDaysWithEvents(days: ArrayList<DayMonthly>) {
        val dayEvents = HashMap<String, ArrayList<Event>>()
        mEvents.forEach { event ->
            val startDateTime = Formatter.getDateTimeFromTS(event.startTS)
            val endDateTime = Formatter.getDateTimeFromTS(event.endTS)
            val endCode = Formatter.getDayCodeFromDateTime(endDateTime)

            var currDay = startDateTime
            var dayCode = Formatter.getDayCodeFromDateTime(currDay)
            var currDayEvents = dayEvents[dayCode] ?: ArrayList()
            currDayEvents.add(event)
            dayEvents[dayCode] = currDayEvents

            while (Formatter.getDayCodeFromDateTime(currDay) != endCode) {
                currDay = currDay.plusDays(1)
                dayCode = Formatter.getDayCodeFromDateTime(currDay)
                currDayEvents = dayEvents[dayCode] ?: ArrayList()
                currDayEvents.add(event)
                dayEvents[dayCode] = currDayEvents
            }
        }

        days.filter { dayEvents.keys.contains(it.code) }.forEach {
            it.dayEvents = dayEvents[it.code]!!
        }
        callback.updateMonthlyCalendar(context, monthName, days, true, mTargetDate)
    }

    private fun isToday(targetDate: DateTime, curDayInMonth: Int): Boolean {
        val targetMonthDays = targetDate.dayOfMonth().maximumValue
        return targetDate.withDayOfMonth(min(curDayInMonth, targetMonthDays)).toString(Formatter.DAYCODE_PATTERN) == mToday
    }

    private val monthName: String
        get() {
            return if (context.config.useLunisolarCalendar) {
                val currentLunar = LunisolarCalendar.gregorianToLunar(mTargetDate.year, mTargetDate.monthOfYear, mTargetDate.dayOfMonth)
                if (currentLunar.lunarDay != 0) {
                    Formatter.getLunisolarMonthYear(context, currentLunar.lunarYear, currentLunar.lunarMonth)
                } else {
                    // Fallback to Gregorian
                    var month = Formatter.getMonthName(context, mTargetDate.monthOfYear)
                    val targetYear = mTargetDate.toString(YEAR_PATTERN)
                    if (targetYear != DateTime().toString(YEAR_PATTERN)) {
                        month += " $targetYear"
                    }
                    month
                }
            } else {
                var month = Formatter.getMonthName(context, mTargetDate.monthOfYear)
                val targetYear = mTargetDate.toString(YEAR_PATTERN)
                if (targetYear != DateTime().toString(YEAR_PATTERN)) {
                    month += " $targetYear"
                }
                month
            }
        }

    private fun gotEvents(events: ArrayList<Event>) {
        mEvents = events
        getDays(true)
    }
}
