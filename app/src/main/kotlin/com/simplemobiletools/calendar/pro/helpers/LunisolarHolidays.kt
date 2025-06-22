package com.simplemobiletools.calendar.pro.helpers

import android.content.Context
import org.joda.time.DateTime

/**
 * Miseri Calendar Holiday System
 * Based on full moons of the lunisolar year
 */
object LunisolarHolidays {

    data class Holiday(
        val name: String,
        val abbreviation: String,
        val startDate: DateTime,
        val duration: Int = 3  // All holidays last 3 days
    )

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
                    // Get start dates for the 1st, 3rd, 6th, and 9th months
                    val month1Start = LunisolarCalendar.lunarToGregorian(lunarYear, 1, 1)
                    val month3Start = LunisolarCalendar.lunarToGregorian(lunarYear, 3, 1)
                    val month6Start = LunisolarCalendar.lunarToGregorian(lunarYear, 6, 1)
                    val month9Start = LunisolarCalendar.lunarToGregorian(lunarYear, 9, 1)
                    
                    // Only add holidays if they fall within the requested Gregorian year
                    if (month1Start != null && month1Start.first == gregorianYear) {
                        val (year, month, day) = month1Start
                        holidays.add(Holiday("Yule", "YUL", DateTime(year, month, day, 0, 0), 3))
                    }
                    
                    if (month3Start != null && month3Start.first == gregorianYear) {
                        val (year, month, day) = month3Start
                        holidays.add(Holiday("Sumarmal", "SUM", DateTime(year, month, day, 0, 0), 3))
                    }
                    
                    if (month6Start != null && month6Start.first == gregorianYear) {
                        val (year, month, day) = month6Start
                        holidays.add(Holiday("Midsummer", "MID", DateTime(year, month, day, 0, 0), 3))
                    }
                    
                    if (month9Start != null && month9Start.first == gregorianYear) {
                        val (year, month, day) = month9Start
                        holidays.add(Holiday("Winter Nights", "WIN", DateTime(year, month, day, 0, 0), 3))
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
        val holidays = getHolidaysForYear(date.year)
        return holidays.find { holiday ->
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
} 