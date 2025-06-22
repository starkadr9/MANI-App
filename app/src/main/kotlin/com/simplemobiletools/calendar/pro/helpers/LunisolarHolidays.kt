package com.simplemobiletools.calendar.pro.helpers

/**
 * Germanic Lunisolar Holiday System
 * Calculates traditional Germanic holidays based on astronomical events and lunar calendar
 */
object LunisolarHolidays {

    data class Holiday(
        val name: String,
        val description: String,
        val type: HolidayType,
        val color: Int = 0xFF4CAF50.toInt() // Default green
    )

    enum class HolidayType {
        ASTRONOMICAL,    // Based on solstices/equinoxes
        LUNAR,          // Based on moon phases
        FIXED_LUNAR,    // Fixed lunar date
        SEASONAL        // Based on astronomical + lunar calculations
    }

    /**
     * Get all holidays for a specific Gregorian year
     */
    fun getHolidaysForYear(gregorianYear: Int): Map<String, Holiday> {
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
                "Winter Solstice, longest night of the year, rebirth of the sun",
                HolidayType.ASTRONOMICAL,
                0xFF4CAF50.toInt()
            )
            
            // Spring Equinox - Ostara
            val springEquinox = LunisolarCalendar.calculateSolsticeEquinoxJDE(year, 1)
            val (seYear, seMonth, seDay) = LunisolarCalendar.julianDayToGregorian(springEquinox)
            val ostaraDayCode = "${seYear}${seMonth.toString().padStart(2, '0')}${seDay.toString().padStart(2, '0')}"
            holidays[ostaraDayCode] = Holiday(
                "Ostara - Eostre",
                "Spring Equinox, balance of light and dark, renewal and fertility",
                HolidayType.ASTRONOMICAL,
                0xFF9C27B0.toInt()
            )
            
            // Summer Solstice - Litha
            val summerSolstice = LunisolarCalendar.calculateSolsticeEquinoxJDE(year, 2)
            val (ssYear, ssMonth, ssDay) = LunisolarCalendar.julianDayToGregorian(summerSolstice)
            val lithaDayCode = "${ssYear}${ssMonth.toString().padStart(2, '0')}${ssDay.toString().padStart(2, '0')}"
            holidays[lithaDayCode] = Holiday(
                "Litha - Midsummer",
                "Summer Solstice, longest day of the year, peak of solar power",
                HolidayType.ASTRONOMICAL,
                0xFFFF9800.toInt()
            )
            
            // Fall Equinox - Harvest Home
            val fallEquinox = LunisolarCalendar.calculateSolsticeEquinoxJDE(year, 3)
            val (feYear, feMonth, feDay) = LunisolarCalendar.julianDayToGregorian(fallEquinox)
            val harvestDayCode = "${feYear}${feMonth.toString().padStart(2, '0')}${feDay.toString().padStart(2, '0')}"
            holidays[harvestDayCode] = Holiday(
                "Harvest Home - Mabon",
                "Autumn Equinox, second harvest, balance and thanksgiving",
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
            // Lunar New Year - start of lunisolar calendar
            val lunarNewYearJD = LunisolarCalendar.calculateLunarNewYearJD(year)
            val (lnyYear, lnyMonth, lnyDay) = LunisolarCalendar.julianDayToGregorian(lunarNewYearJD)
            val newYearDayCode = "${lnyYear}${lnyMonth.toString().padStart(2, '0')}${lnyDay.toString().padStart(2, '0')}"
            holidays[newYearDayCode] = Holiday(
                "Wolf Moon Rising",
                "Lunisolar New Year, first full moon after winter solstice",
                HolidayType.LUNAR,
                0xFF3F51B5.toInt()
            )
            
            // Mid-year celebration - 6th or 7th month
            val isLeapYear = LunisolarCalendar.isLunarLeapYear(year)
            val midYearMonth = if (isLeapYear) 7 else 6
            val midYearGregorian = LunisolarCalendar.lunarToGregorian(year, midYearMonth, 15)
            if (midYearGregorian != null) {
                val (myYear, myMonth, myDay) = midYearGregorian
                val midYearDayCode = "${myYear}${myMonth.toString().padStart(2, '0')}${myDay.toString().padStart(2, '0')}"
                holidays[midYearDayCode] = Holiday(
                    "Mead Moon Festival",
                    "Mid-year celebration, time of abundance and community",
                    HolidayType.LUNAR,
                    0xFFFFEB3B.toInt()
                )
            }
            
        } catch (e: Exception) {
            // If calculation fails, skip lunar holidays for this year
        }
        
        return holidays
    }

    /**
     * Get seasonal holidays that combine astronomical and cultural timing
     */
    private fun getSeasonalHolidays(year: Int): Map<String, Holiday> {
        val holidays = mutableMapOf<String, Holiday>()
        
        try {
            // Winter Nights - traditionally October/November
            // Calculate as first new moon after October 1st
            val oct1JD = LunisolarCalendar.gregorianToJulianDay(year, 10, 1)
            val winterNightsJD = LunisolarCalendar.findNextPhaseJD(oct1JD, 0) // Next new moon
            val (wnYear, wnMonth, wnDay) = LunisolarCalendar.julianDayToGregorian(winterNightsJD)
            val winterNightsDayCode = "${wnYear}${wnMonth.toString().padStart(2, '0')}${wnDay.toString().padStart(2, '0')}"
            holidays[winterNightsDayCode] = Holiday(
                "Winter Nights - Vetrnáttablót",
                "Beginning of winter season, honoring ancestors and the Wild Hunt",
                HolidayType.SEASONAL,
                0xFF607D8B.toInt()
            )
            
            // Dísablót - early spring (between winter solstice and spring equinox)
            val winterSolstice = LunisolarCalendar.calculateSolsticeEquinoxJDE(year, 0)
            val springEquinox = LunisolarCalendar.calculateSolsticeEquinoxJDE(year, 1)
            val midWinterSpring = (winterSolstice + springEquinox) / 2
            val (dsYear, dsMonth, dsDay) = LunisolarCalendar.julianDayToGregorian(midWinterSpring)
            val disablotDayCode = "${dsYear}${dsMonth.toString().padStart(2, '0')}${dsDay.toString().padStart(2, '0')}"
            holidays[disablotDayCode] = Holiday(
                "Dísablót",
                "Honoring the Dísir (female spirits), protection and prosperity",
                HolidayType.SEASONAL,
                0xFFE91E63.toInt()
            )
            
        } catch (e: Exception) {
            // If calculation fails, skip seasonal holidays for this year
        }
        
        return holidays
    }

    /**
     * Check if a specific date is a holiday
     */
    fun getHolidayForDate(year: Int, month: Int, day: Int): Holiday? {
        val dayCode = "${year}${month.toString().padStart(2, '0')}${day.toString().padStart(2, '0')}"
        return getHolidaysForYear(year)[dayCode]
    }

    /**
     * Get all holiday names (for settings/customization)
     */
    fun getAvailableHolidayTypes(): List<String> {
        return listOf(
            "Astronomical Events (Solstices/Equinoxes)",
            "Lunar Celebrations", 
            "Seasonal Festivals",
            "Germanic Traditional Holidays"
        )
    }
} 