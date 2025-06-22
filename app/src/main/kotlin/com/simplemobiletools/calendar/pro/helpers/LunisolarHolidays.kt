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
     * - Yule: 1st full moon of the new year (1st month start)
     * - Sumarmal: 3rd full moon (3rd month start) 
     * - Midsummer: 6th full moon (6th month start)
     * - Winter Nights: 9th full moon (9th month start)
     */
    fun getHolidaysForYear(gregorianYear: Int): List<Holiday> {
        val holidays = mutableListOf<Holiday>()
        
        try {
            // Get full moon dates for the lunisolar year
            val fullMoons = getFullMoonsForLunarYear(gregorianYear)
            
            if (fullMoons.size >= 9) {
                // Yule: 1st full moon of the new year
                holidays.add(Holiday("Yule", "YUL", fullMoons[0]))
                
                // Sumarmal: 3rd full moon
                holidays.add(Holiday("Sumarmal", "SUM", fullMoons[2]))
                
                // Midsummer: 6th full moon
                holidays.add(Holiday("Midsummer", "MID", fullMoons[5]))
                
                // Winter Nights: 9th full moon
                holidays.add(Holiday("Winter Nights", "WIN", fullMoons[8]))
            }
        } catch (e: Exception) {
            // If calculation fails, return empty list
        }
        
        return holidays
    }

    /**
     * Get full moon dates for the lunisolar year
     * Each month in the lunisolar calendar starts with a full moon
     */
    private fun getFullMoonsForLunarYear(gregorianYear: Int): List<DateTime> {
        val fullMoons = mutableListOf<DateTime>()
        
        try {
            // Get the lunar new year date (first full moon after winter solstice)
            val lunarNewYearJD = LunisolarCalendar.calculateLunarNewYearJD(gregorianYear)
            val (year, month, day) = LunisolarCalendar.julianDayToGregorian(lunarNewYearJD)
            var currentFullMoon = DateTime(year, month, day, 0, 0)
            
            // Add the first full moon (Lunar New Year)
            fullMoons.add(currentFullMoon)
            
            // Calculate subsequent full moons (approximately every 29.53 days)
            for (monthIndex in 1 until 12) {
                // Alternate between 29 and 30 day months
                val daysInMonth = if (monthIndex % 2 == 0) 30 else 29
                currentFullMoon = currentFullMoon.plusDays(daysInMonth)
                fullMoons.add(currentFullMoon)
            }
        } catch (e: Exception) {
            // If calculation fails, return empty list
        }
        
        return fullMoons
    }

    /**
     * Check if a date falls within any holiday period
     */
    fun getHolidayForDate(date: DateTime): Holiday? {
        val holidays = getHolidaysForYear(date.year)
        return holidays.find { holiday ->
            val startDate = holiday.startDate.toDateMidnight()
            val endDate = startDate.plusDays(holiday.duration - 1)
            val checkDate = date.toDateMidnight()
            
            checkDate >= startDate && checkDate <= endDate
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
        val daysDiff = date.toDateMidnight().millis - holiday.startDate.toDateMidnight().millis
        val dayNumber = (daysDiff / (24 * 60 * 60 * 1000)) + 1
        return dayNumber.toInt()
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