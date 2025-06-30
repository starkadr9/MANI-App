package com.simplemobiletools.calendar.pro.helpers

import android.content.Context
import org.joda.time.DateTime

/**
 * Miseri Calendar Holiday System
 * Based on full moons of the lunisolar year
 */
object LunisolarHolidays {

    enum class HolidayType {
        MAJOR_HOLIDAY,
        SOLSTICE_OR_EQUINOX
    }

    data class Holiday(
        val name: String,
        val abbreviation: String,
        val startDate: DateTime,
        val duration: Int = 3,  // All holidays last 3 days
        val type: HolidayType = HolidayType.MAJOR_HOLIDAY
    )

    /**
     * Find the actual 1st day of a lunar month by scanning for when lunar day = 1
     */
    private fun findActualMonthStart(lunarYear: Int, lunarMonth: Int): DateTime? {
        try {
            // Start from approximate date and scan around it
            val approximateStart = LunisolarCalendar.lunarToGregorian(lunarYear, lunarMonth, 1)
            if (approximateStart == null) return null
            
            val (year, month, day) = approximateStart
            val baseDate = DateTime(year, month, day, 0, 0)
            
            // Scan backward and forward from approximate date to find actual lunar day 1
            for (offset in -3..3) {
                val testDate = baseDate.plusDays(offset)
                val lunar = LunisolarCalendar.gregorianToLunar(testDate.year, testDate.monthOfYear, testDate.dayOfMonth)
                
                if (lunar.lunarYear == lunarYear && lunar.lunarMonth == lunarMonth && lunar.lunarDay == 1) {
                    return testDate
                }
            }
            
            // Fallback to approximate if exact not found
            return baseDate
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * Get the four major holidays for a lunisolar year
     * - Yule: 1st month start (1st full moon of the new year)
     * - Sumarmal: 3rd month start (3rd full moon) 
     * - Midsummer: 6th month start (6th full moon)
     * - Winter Nights: 9th month start (9th full moon)
     */
    fun getHolidaysForYear(gregorianYear: Int): List<Holiday> {
        val holidays = mutableListOf<Holiday>()
        
        try {
            // Find which lunisolar year(s) overlap with this Gregorian year
            // Check both the current year and previous year since lunisolar years
            // can start in the previous Gregorian year
            val possibleLunarYears = listOf(gregorianYear - 1, gregorianYear, gregorianYear + 1)
            
            for (lunarYear in possibleLunarYears) {
                try {
                    // Find actual start dates for the 1st, 3rd, 6th, and 9th months
                    val month1Start = findActualMonthStart(lunarYear, 1)
                    val month3Start = findActualMonthStart(lunarYear, 3)
                    val month6Start = findActualMonthStart(lunarYear, 6)
                    val month9Start = findActualMonthStart(lunarYear, 9)
                    
                    // Only add holidays if they fall within the requested Gregorian year
                    if (month1Start != null && month1Start.year == gregorianYear) {
                        holidays.add(Holiday("Yule", "YUL", month1Start, 3))
                    }
                    
                    if (month3Start != null && month3Start.year == gregorianYear) {
                        holidays.add(Holiday("Sumarmal", "SUM", month3Start, 3))
                    }
                    
                    if (month6Start != null && month6Start.year == gregorianYear) {
                        holidays.add(Holiday("Midsummer", "MID", month6Start, 3))
                    }
                    
                    if (month9Start != null && month9Start.year == gregorianYear) {
                        holidays.add(Holiday("Winter Nights", "WIN", month9Start, 3))
                    }
                } catch (e: Exception) {
                    // Continue to next possible lunar year
                }
            }
        } catch (e: Exception) {
            // If calculation fails, return empty list
        }
        
        return holidays
    }

    /**
     * Check if a date falls within any holiday period
     */
    fun getHolidayForDate(date: DateTime): Holiday? {
        // Check holidays from both current year and previous year
        // since holidays can span across year boundaries
        val currentYearHolidays = getHolidaysForYear(date.year)
        val previousYearHolidays = getHolidaysForYear(date.year - 1)
        val allHolidays = currentYearHolidays + previousYearHolidays
        
        return allHolidays.find { holiday ->
            // Use simple date comparison for the 3-day period
            val startDate = holiday.startDate
            val checkDate = date
            
            // Check if date is exactly day 1, day 2, or day 3 of the holiday
            val day1 = startDate
            val day2 = startDate.plusDays(1)
            val day3 = startDate.plusDays(2)
            
            checkDate.toLocalDate() == day1.toLocalDate() ||
            checkDate.toLocalDate() == day2.toLocalDate() ||
            checkDate.toLocalDate() == day3.toLocalDate()
        }
    }

    /**
     * Check if a date is a holiday
     */
    fun isHolidayDate(date: DateTime): Boolean {
        return getHolidayForDate(date) != null
    }

    /**
     * Get which day of the holiday this date represents (1, 2, or 3)
     */
    fun getHolidayDayNumber(date: DateTime): Int? {
        val holiday = getHolidayForDate(date) ?: return null
        val startDate = holiday.startDate
        val checkDate = date
        
        return when (checkDate.toLocalDate()) {
            startDate.toLocalDate() -> 1
            startDate.plusDays(1).toLocalDate() -> 2
            startDate.plusDays(2).toLocalDate() -> 3
            else -> null
        }
    }

    /**
     * Get holiday information for display purposes
     */
    fun getHolidayDisplayInfo(date: DateTime): String? {
        val holiday = getHolidayForDate(date) ?: return null
        val dayNumber = getHolidayDayNumber(date) ?: return null
        return "${holiday.abbreviation}$dayNumber"
    }

    /**
     * Get astronomical holidays (solstices/equinoxes) for a year
     */
    fun getAstronomicalEventsForYear(year: Int): List<Holiday> {
        val events = mutableListOf<Holiday>()
        
        // Winter Solstice
        val wsJD = LunisolarCalendar.calculateWinterSolstice(year)
        val wsDate = DateTime(LunisolarCalendar.julianDayToGregorian(wsJD).toMillis())
        events.add(Holiday("Winter Solstice", "WS", wsDate, 1, HolidayType.SOLSTICE_OR_EQUINOX))
        
        // Spring Equinox
        val seJD = LunisolarCalendar.calculateSpringEquinox(year)
        val seDate = DateTime(LunisolarCalendar.julianDayToGregorian(seJD).toMillis())
        events.add(Holiday("Spring Equinox", "SE", seDate, 1, HolidayType.SOLSTICE_OR_EQUINOX))
        
        // Summer Solstice
        val ssJD = LunisolarCalendar.calculateSummerSolstice(year)
        val ssDate = DateTime(LunisolarCalendar.julianDayToGregorian(ssJD).toMillis())
        events.add(Holiday("Summer Solstice", "SS", ssDate, 1, HolidayType.SOLSTICE_OR_EQUINOX))
        
        // Fall Equinox
        val feJD = LunisolarCalendar.calculateFallEquinox(year)
        val feDate = DateTime(LunisolarCalendar.julianDayToGregorian(feJD).toMillis())
        events.add(Holiday("Fall Equinox", "FE", feDate, 1, HolidayType.SOLSTICE_OR_EQUINOX))
        
        return events
    }
}

private fun Triple<Int, Int, Int>.toMillis(): Long {
    return DateTime(this.first, this.second, this.third, 12, 0).millis
} 