package com.simplemobiletools.calendar.pro.helpers

import android.content.Context
import org.joda.time.DateTime

/**
 * Germanic Lunisolar Holiday System
 * Simplified version for basic holiday information
 */
object LunisolarHolidays {

    data class Holiday(
        val name: String,
        val description: String,
        val type: HolidayType,
        val color: Int = 0xFF4CAF50.toInt()
    )

    enum class HolidayType {
        ASTRONOMICAL,    // Based on solstices/equinoxes
        LUNAR,          // Based on moon phases
        SEASONAL        // Based on astronomical + lunar calculations
    }

    /**
     * Get available years for holiday creation (current year ± 5 years)
     */
    fun getAvailableYearsForHolidays(): List<Int> {
        val currentYear = DateTime.now().year
        return (currentYear - 5..currentYear + 5).toList()
    }

    /**
     * Get all Germanic holidays for a specific year
     */
    fun getGermanicHolidaysForYear(gregorianYear: Int): Map<String, Holiday> {
        val holidays = mutableMapOf<String, Holiday>()
        
        // Add astronomical holidays
        holidays.putAll(getAstronomicalHolidays(gregorianYear))
        
        // Add lunar holidays
        holidays.putAll(getLunarHolidays(gregorianYear))
        
        // Add seasonal holidays
        holidays.putAll(getSeasonalHolidays(gregorianYear))
        
        return holidays
    }

    /**
     * Get holidays based on solstices and equinoxes
     */
    private fun getAstronomicalHolidays(year: Int): Map<String, Holiday> {
        val holidays = mutableMapOf<String, Holiday>()
        
        try {
            // Winter Solstice - Yule
            val winterSolstice = LunisolarCalendar.calculateSolsticeEquinoxJDE(year, 0)
            val (wsYear, wsMonth, wsDay) = LunisolarCalendar.julianDayToGregorian(winterSolstice)
            val yuleDayCode = "${wsYear}${wsMonth.toString().padStart(2, '0')}${wsDay.toString().padStart(2, '0')}"
            holidays[yuleDayCode] = Holiday(
                "Yule - Jól", 
                "Winter Solstice, longest night of the year, rebirth of the sun.",
                HolidayType.ASTRONOMICAL,
                0xFF4CAF50.toInt()
            )
            
            // Spring Equinox - Ostara
            val springEquinox = LunisolarCalendar.calculateSolsticeEquinoxJDE(year, 1)
            val (seYear, seMonth, seDay) = LunisolarCalendar.julianDayToGregorian(springEquinox)
            val ostaraDayCode = "${seYear}${seMonth.toString().padStart(2, '0')}${seDay.toString().padStart(2, '0')}"
            holidays[ostaraDayCode] = Holiday(
                "Ostara - Eostre",
                "Spring Equinox, balance of light and dark, renewal and fertility.",
                HolidayType.ASTRONOMICAL,
                0xFF9C27B0.toInt()
            )
            
            // Summer Solstice - Litha
            val summerSolstice = LunisolarCalendar.calculateSolsticeEquinoxJDE(year, 2)
            val (ssYear, ssMonth, ssDay) = LunisolarCalendar.julianDayToGregorian(summerSolstice)
            val lithaDayCode = "${ssYear}${ssMonth.toString().padStart(2, '0')}${ssDay.toString().padStart(2, '0')}"
            holidays[lithaDayCode] = Holiday(
                "Litha - Midsummer",
                "Summer Solstice, longest day of the year, peak of solar power.",
                HolidayType.ASTRONOMICAL,
                0xFFFF9800.toInt()
            )
            
            // Fall Equinox - Harvest Home
            val fallEquinox = LunisolarCalendar.calculateSolsticeEquinoxJDE(year, 3)
            val (feYear, feMonth, feDay) = LunisolarCalendar.julianDayToGregorian(fallEquinox)
            val harvestDayCode = "${feYear}${feMonth.toString().padStart(2, '0')}${feDay.toString().padStart(2, '0')}"
            holidays[harvestDayCode] = Holiday(
                "Harvest Home - Mabon",
                "Autumn Equinox, second harvest, balance and thanksgiving.",
                HolidayType.ASTRONOMICAL,
                0xFF795548.toInt()
            )
            
        } catch (e: Exception) {
            // If calculation fails, skip astronomical holidays for this year
        }
        
        return holidays
    }

    /**
     * Get holidays based on lunar phases
     */
    private fun getLunarHolidays(year: Int): Map<String, Holiday> {
        val holidays = mutableMapOf<String, Holiday>()
        
        try {
            // Wolf Moon Rising (first full moon of lunisolar year)
            val lunarNewYearJD = LunisolarCalendar.calculateLunarNewYearJD(year)
            val (lnyYear, lnyMonth, lnyDay) = LunisolarCalendar.julianDayToGregorian(lunarNewYearJD)
            val newYearDayCode = "${lnyYear}${lnyMonth.toString().padStart(2, '0')}${lnyDay.toString().padStart(2, '0')}"
            holidays[newYearDayCode] = Holiday(
                "Wolf Moon Rising",
                "First full moon of the lunisolar year, marking the beginning of the Germanic calendar.",
                HolidayType.LUNAR,
                0xFF2196F3.toInt()
            )
            
            // Mead Moon Festival (summer full moon - approximate)
            val summerSolstice = LunisolarCalendar.calculateSolsticeEquinoxJDE(year, 2)
            // Find full moon closest to summer solstice
            val meadMoonJD = summerSolstice + 15 // Approximate
            val (mmYear, mmMonth, mmDay) = LunisolarCalendar.julianDayToGregorian(meadMoonJD)
            val meadMoonDayCode = "${mmYear}${mmMonth.toString().padStart(2, '0')}${mmDay.toString().padStart(2, '0')}"
            holidays[meadMoonDayCode] = Holiday(
                "Mead Moon Festival",
                "Summer full moon celebration, time of abundance and community gathering.",
                HolidayType.LUNAR,
                0xFFFFEB3B.toInt()
            )
            
        } catch (e: Exception) {
            // If calculation fails, skip lunar holidays for this year
        }
        
        return holidays
    }

    /**
     * Get seasonal holidays based on combined calculations
     */
    private fun getSeasonalHolidays(year: Int): Map<String, Holiday> {
        val holidays = mutableMapOf<String, Holiday>()
        
        try {
            // Winter Nights (mid-October, preparation for winter)
            val winterNightsDate = DateTime(year, 10, 15, 0, 0)
            val winterNightsDayCode = "${year}1015"
            holidays[winterNightsDayCode] = Holiday(
                "Winter Nights - Vetrnáttablót",
                "Beginning of winter season, honoring the ancestors and preparing for the dark months.",
                HolidayType.SEASONAL,
                0xFF3F51B5.toInt()
            )
            
            // Dísablót (early February, honoring female spirits)
            val disablotDate = DateTime(year, 2, 2, 0, 0)
            val disablotDayCode = "${year}0202"
            holidays[disablotDayCode] = Holiday(
                "Dísablót - Disting",
                "Festival honoring the Dísir (female spirits/goddesses), protection of family and community.",
                HolidayType.SEASONAL,
                0xFFE91E63.toInt()
            )
            
        } catch (e: Exception) {
            // If calculation fails, skip seasonal holidays for this year
        }
        
        return holidays
    }

    /**
     * Get list of Germanic holiday names for filtering
     */
    fun getGermanicHolidayNames(): List<String> {
        return listOf(
            "Yule", "Jól", "Ostara", "Eostre", "Litha", "Midsummer", 
            "Harvest Home", "Mabon", "Wolf Moon", "Mead Moon", 
            "Winter Nights", "Vetrnáttablót", "Dísablót", "Disting"
        )
    }
} 