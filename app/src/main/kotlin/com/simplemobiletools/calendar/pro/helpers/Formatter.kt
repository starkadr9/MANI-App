package com.simplemobiletools.calendar.pro.helpers

import android.content.Context
import com.simplemobiletools.calendar.pro.extensions.config
import com.simplemobiletools.calendar.pro.extensions.seconds
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat

object Formatter {
    const val DAYCODE_PATTERN = "YYYYMMdd"
    const val YEAR_PATTERN = "YYYY"
    const val TIME_PATTERN = "HHmmss"
    private const val MONTH_PATTERN = "MMM"
    private const val DAY_PATTERN = "d"
    private const val DAY_OF_WEEK_PATTERN = "EEE"
    private const val DATE_DAY_PATTERN = "d EEEE"
    private const val PATTERN_TIME_12 = "hh:mm a"
    private const val PATTERN_TIME_24 = "HH:mm"

    private const val PATTERN_HOURS_12 = "h a"
    private const val PATTERN_HOURS_24 = "HH"

    fun getDateFromCode(context: Context, dayCode: String, shortMonth: Boolean = false): String {
        val dateTime = getDateTimeFromCode(dayCode)
        val day = dateTime.toString(DAY_PATTERN)
        val year = dateTime.toString(YEAR_PATTERN)
        val monthIndex = Integer.valueOf(dayCode.substring(4, 6))
        var month = getMonthName(context, monthIndex)
        if (shortMonth) {
            month = month.substring(0, Math.min(month.length, 3))
        }

        var date = "$month $day"
        if (year != DateTime().toString(YEAR_PATTERN)) {
            date += " $year"
        }

        return date
    }

    fun getDayTitle(context: Context, dayCode: String, addDayOfWeek: Boolean = true): String {
        val date = getDateFromCode(context, dayCode)
        val dateTime = getDateTimeFromCode(dayCode)
        val day = dateTime.toString(DAY_OF_WEEK_PATTERN)
        return if (addDayOfWeek)
            "$date ($day)"
        else
            date
    }

    fun getDateDayTitle(dayCode: String): String {
        val dateTime = getDateTimeFromCode(dayCode)
        return dateTime.toString(DATE_DAY_PATTERN)
    }

    fun getLongMonthYear(context: Context, dayCode: String): String {
        val dateTime = getDateTimeFromCode(dayCode)
        val monthIndex = Integer.valueOf(dayCode.substring(4, 6))
        val month = getMonthName(context, monthIndex)
        val year = dateTime.toString(YEAR_PATTERN)
        var date = month

        if (year != DateTime().toString(YEAR_PATTERN)) {
            date += " $year"
        }

        return date
    }

    fun getDate(context: Context, dateTime: DateTime, addDayOfWeek: Boolean = true) = getDayTitle(context, getDayCodeFromDateTime(dateTime), addDayOfWeek)

    fun getFullDate(context: Context, dateTime: DateTime): String {
        val day = dateTime.toString(DAY_PATTERN)
        val year = dateTime.toString(YEAR_PATTERN)
        val monthIndex = dateTime.monthOfYear
        val month = getMonthName(context, monthIndex)
        return "$month $day $year"
    }

    fun getTodayCode() = getDayCodeFromTS(getNowSeconds())

    fun getTodayDayNumber() = getDateTimeFromTS(getNowSeconds()).toString(DAY_PATTERN)

    fun getCurrentMonthShort() = getDateTimeFromTS(getNowSeconds()).toString(MONTH_PATTERN)

    fun getTime(context: Context, dateTime: DateTime) = dateTime.toString(getTimePattern(context))

    fun getDateTimeFromCode(dayCode: String) = DateTimeFormat.forPattern(DAYCODE_PATTERN).withZone(DateTimeZone.UTC).parseDateTime(dayCode)

    fun getLocalDateTimeFromCode(dayCode: String) =
        DateTimeFormat.forPattern(DAYCODE_PATTERN).withZone(DateTimeZone.getDefault()).parseLocalDate(dayCode).toDateTimeAtStartOfDay()

    fun getTimeFromTS(context: Context, ts: Long) = getTime(context, getDateTimeFromTS(ts))

    fun getDayStartTS(dayCode: String) = getLocalDateTimeFromCode(dayCode).seconds()

    fun getDayEndTS(dayCode: String) = getLocalDateTimeFromCode(dayCode).plusDays(1).minusMinutes(1).seconds()

    fun getDayCodeFromDateTime(dateTime: DateTime) = dateTime.toString(DAYCODE_PATTERN)

    fun getDateFromTS(ts: Long) = LocalDate(ts * 1000L, DateTimeZone.getDefault())

    fun getDateTimeFromTS(ts: Long) = DateTime(ts * 1000L, DateTimeZone.getDefault())

    fun getUTCDateTimeFromTS(ts: Long) = DateTime(ts * 1000L, DateTimeZone.UTC)

    // use manually translated month names, as DateFormat and Joda have issues with a lot of languages
    fun getMonthName(context: Context, id: Int) = if (context.config.useLunisolarCalendar) {
        getLunisolarMonthName(context, id)
    } else {
        context.resources.getStringArray(com.simplemobiletools.commons.R.array.months)[id - 1]
    }

    fun getShortMonthName(context: Context, id: Int) = if (context.config.useLunisolarCalendar) {
        getLunisolarMonthName(context, id).take(3)
    } else {
        context.resources.getStringArray(com.simplemobiletools.commons.R.array.months_short)[id - 1]
    }

    /**
     * Get lunisolar month name based on configuration
     */
    fun getLunisolarMonthName(context: Context, lunarMonth: Int): String {
        val monthNamesString = context.config.lunisolarMonthNames
        val monthNames = monthNamesString.split(",")
        return if (lunarMonth in 1..monthNames.size) monthNames[lunarMonth - 1] else "Unknown Moon"
    }

    fun getHourPattern(context: Context) = if (context.config.use24HourFormat) PATTERN_HOURS_24 else PATTERN_HOURS_12

    fun getTimePattern(context: Context) = if (context.config.use24HourFormat) PATTERN_TIME_24 else PATTERN_TIME_12

    fun getExportedTime(ts: Long): String {
        val dateTime = DateTime(ts, DateTimeZone.UTC)
        return "${dateTime.toString(DAYCODE_PATTERN)}T${dateTime.toString(TIME_PATTERN)}Z"
    }

    fun getDayCodeFromTS(ts: Long): String {
        val daycode = getDateTimeFromTS(ts).toString(DAYCODE_PATTERN)
        return if (daycode.isNotEmpty()) {
            daycode
        } else {
            "0"
        }
    }

    fun getDayCodeFromDate(year: Int, month: Int, day: Int): String {
        return String.format("%04d%02d%02d", year, month, day)
    }

    fun getUTCDayCodeFromTS(ts: Long) = getUTCDateTimeFromTS(ts).toString(DAYCODE_PATTERN)

    fun getYearFromDayCode(dayCode: String) = getDateTimeFromCode(dayCode).toString(YEAR_PATTERN)

    fun getShiftedTS(dateTime: DateTime, toZone: DateTimeZone) = dateTime.withTimeAtStartOfDay().withZoneRetainFields(toZone).seconds()

    fun getShiftedLocalTS(ts: Long) = getShiftedTS(dateTime = getUTCDateTimeFromTS(ts), toZone = DateTimeZone.getDefault())

    fun getShiftedUtcTS(ts: Long) = getShiftedTS(dateTime = getDateTimeFromTS(ts), toZone = DateTimeZone.UTC)

    // === LUNISOLAR CALENDAR FUNCTIONS ===
    
    /**
     * Convert Gregorian date to Lunisolar display format
     */
    fun getLunisolarDateFromCode(context: Context, dayCode: String): String {
        val dateTime = getDateTimeFromCode(dayCode)
        val lunarDay = LunisolarCalendar.gregorianToLunar(
            dateTime.year, dateTime.monthOfYear, dateTime.dayOfMonth
        )
        
        if (lunarDay.lunarDay == 0) return getDateFromCode(context, dayCode) // Fallback to Gregorian
        
        val monthName = getLunisolarMonthName(context, lunarDay.lunarMonth)
        return "${lunarDay.lunarDay} $monthName ${lunarDay.lunarYear}"
    }

    /**
     * Get lunisolar month name with leap month indication and Eld year
     */
    fun getLunisolarMonthYear(context: Context, lunarYear: Int, lunarMonth: Int): String {
        val monthName = getLunisolarMonthName(context, lunarMonth)
        val isLeapYear = LunisolarCalendar.isLunarLeapYear(lunarYear)
        val leapIndicator = if (isLeapYear && lunarMonth == 13) " (Leap)" else ""
        val eldYear = LunisolarCalendar.calculateEldYear(lunarYear)
        return "$monthName $lunarYear • $eldYear Eld$leapIndicator"
    }

    /**
     * Convert Unix timestamp to lunisolar date components
     */
    fun getUnixTSToLunarComponents(ts: Long): Triple<Int, Int, Int> {
        val dateTime = getDateTimeFromTS(ts)
        val lunarDay = LunisolarCalendar.gregorianToLunar(
            dateTime.year, dateTime.monthOfYear, dateTime.dayOfMonth
        )
        return Triple(lunarDay.lunarYear, lunarDay.lunarMonth, lunarDay.lunarDay)
    }
}
