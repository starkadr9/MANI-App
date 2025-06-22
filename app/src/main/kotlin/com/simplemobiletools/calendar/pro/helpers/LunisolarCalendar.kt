package com.simplemobiletools.calendar.pro.helpers

import kotlin.math.*

/**
 * Lunisolar Calendar Implementation
 * Converted from C implementation - handles astronomical moon phase calculations
 */
object LunisolarCalendar {
    // Constants from C code
    private var germanicEpochBC = 750  // Configurable epoch
    private const val PI = 3.14159265358979323846
    private const val LUNAR_CYCLE_DAYS = 29.530588861
    private const val DAYS_PER_JULIAN_CENTURY = 36525.0
    private const val J2000_EPOCH = 2451545.0
    private const val YEARS_PER_METONIC_CYCLE = 19

    // Configurable month names
    private var customMonthNames = arrayOf(
        "Wulf Moon", "Horn Moon", "Lenting Moon", "Eostre Moon", "Thrimilce Moon", "Æftera Litha",
        "Weod Moon", "Hægest Moon", "Halig Moon", "Winter Moon", "Blot Moon", "Yule Moon", "Long Night Moon"
    )

    // Moon phases
    enum class MoonPhase {
        NEW_MOON, WAXING_CRESCENT, FIRST_QUARTER, WAXING_GIBBOUS,
        FULL_MOON, WANING_GIBBOUS, LAST_QUARTER, WANING_CRESCENT
    }

    // Weekdays
    enum class Weekday {
        SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY
    }

    data class LunarDay(
        val lunarYear: Int,
        val lunarMonth: Int,
        val lunarDay: Int,
        val gregorianYear: Int,
        val gregorianMonth: Int,
        val gregorianDay: Int,
        val weekday: Weekday,
        val moonPhase: MoonPhase,
        val eldYear: Int,
        val metonicYear: Int,
        val metonicCycle: Int
    )

    // Utility functions
    private fun radToDeg(rad: Double) = rad * 180.0 / PI
    private fun degToRad(deg: Double) = deg * PI / 180.0

    /**
     * Convert Gregorian date to Julian day (UT)
     */
    fun gregorianToJulianDay(year: Int, month: Int, day: Int, hour: Double = 12.0): Double {
        var adjustedYear = year
        var adjustedMonth = month
        
        if (month <= 2) {
            adjustedYear -= 1
            adjustedMonth += 12
        }
        
        val a = floor(adjustedYear / 100.0)
        val b = 2 - a + floor(a / 4.0)
        
        val jd0h = floor(365.25 * (adjustedYear + 4716)) +
                   floor(30.6001 * (adjustedMonth + 1)) +
                   day + b - 1524.5
                   
        return jd0h + (hour / 24.0)
    }

    /**
     * Convert Julian day to Gregorian date
     */
    fun julianDayToGregorian(julianDay: Double): Triple<Int, Int, Int> {
        val Z = floor(julianDay + 0.5)
        val F = julianDay + 0.5 - Z

        val A = if (Z < 2299161) {
            Z
        } else {
            val alpha = floor((Z - 1867216.25) / 36524.25)
            Z + 1 + alpha - floor(alpha / 4.0)
        }

        val B = A + 1524
        val C = floor((B - 122.1) / 365.25)
        val D = floor(365.25 * C)
        val E = floor((B - D) / 30.6001)

        val day = floor(B - D - floor(30.6001 * E) + F).toInt()
        val month = if (E < 14) (E - 1).toInt() else (E - 13).toInt()
        val year = if (month > 2) (C - 4716).toInt() else (C - 4715).toInt()
        
        return Triple(year, month, day)
    }

    /**
     * Calculate weekday using Zeller's congruence
     */
    fun calculateWeekday(year: Int, month: Int, day: Int): Weekday {
        var adjustedYear = year
        var adjustedMonth = month
        
        if (month < 3) {
            adjustedMonth += 12
            adjustedYear--
        }
        
        val K = adjustedYear % 100
        val J = adjustedYear / 100
        val h = (day + floor(13.0 * (adjustedMonth + 1.0) / 5.0).toInt() + K + K/4 + J/4 + 5*J) % 7
        
        return Weekday.values()[(h + 1) % 7]
    }

    /**
     * Calculate solstice/equinox Julian day
     * season: 0=winter solstice, 1=spring equinox, 2=summer solstice, 3=fall equinox
     */
    fun calculateSolsticeEquinoxJDE(year: Int, season: Int): Double {
        val y = (year - 2000.0) / 1000.0
        
        return when(season) {
            0 -> 2451900.05952 + 365242.74049 * y + 0.00278 * y * y // Winter Solstice
            1 -> 2451623.80984 + 365242.37404 * y + 0.05169 * y * y // Spring Equinox
            2 -> 2451716.56767 + 365241.62603 * y + 0.00325 * y * y // Summer Solstice
            3 -> 2451810.21715 + 365242.01767 * y - 0.11575 * y * y // Fall Equinox
            else -> 0.0
        }
    }

    /**
     * Calculate true Julian Day for moon phase
     * phaseType: 0=NM, 1=FQ, 2=FM, 3=LQ
     */
    private fun calculateTruePhaseJD(k: Double, phaseType: Int): Double {
        val kAdjusted = k + phaseType / 4.0
        val T = kAdjusted / 1236.85
        
        val jdeMean = 2451550.09766 + LUNAR_CYCLE_DAYS * kAdjusted +
                      (0.00015437 * T * T) -
                      (0.000000150 * T * T * T) +
                      (0.00000000073 * T * T * T * T)
        
        val TCorr = (jdeMean - J2000_EPOCH) / DAYS_PER_JULIAN_CENTURY
        val MSun = degToRad((357.5291 + 35999.0503 * TCorr) % 360.0)
        val MMoon = degToRad((134.9634 + 477198.8675 * TCorr) % 360.0)
        val FMoon = degToRad((93.2721 + 483202.0175 * TCorr) % 360.0)
        val E = 1.0 - 0.002516 * TCorr - 0.0000074 * TCorr * TCorr
        val E2 = E * E
        
        var corrections = 0.0
        when (phaseType) {
            0 -> { // New Moon
                corrections += -0.40720 * sin(MMoon) + 0.17241 * E * sin(MSun)
                corrections += 0.01608 * sin(2.0 * MMoon) + 0.01039 * sin(2.0 * FMoon)
                corrections += 0.00739 * E * sin(MMoon - MSun) - 0.00514 * E * sin(MMoon + MSun)
                corrections += 0.00208 * E2 * sin(2.0 * MSun)
            }
            1 -> { // First Quarter
                corrections += -0.62801 * sin(MMoon) + 0.17172 * E * sin(MSun)
                corrections += -0.01183 * E * sin(MSun + MMoon) + 0.00871 * sin(2 * MMoon)
                corrections += 0.00800 * E * sin(MMoon - MSun) + 0.00690 * sin(2 * FMoon)
            }
            2 -> { // Full Moon
                corrections += -0.40614 * sin(MMoon) + 0.17302 * E * sin(MSun)
                corrections += 0.01614 * sin(2.0 * MMoon) + 0.01043 * sin(2.0 * FMoon)
                corrections += 0.00734 * E * sin(MMoon - MSun) - 0.00515 * E * sin(MMoon + MSun)
                corrections += 0.00209 * E2 * sin(2.0 * MSun)
            }
            3 -> { // Last Quarter
                corrections += -0.62581 * sin(MMoon) + 0.17226 * E * sin(MSun)
                corrections += -0.01186 * E * sin(MSun + MMoon) + 0.00867 * sin(2 * MMoon)
                corrections += 0.00797 * E * sin(MMoon - MSun) + 0.00691 * sin(2 * FMoon)
            }
        }
        
        return jdeMean + corrections
    }

    /**
     * Find next moon phase after given Julian Day
     */
    fun findNextPhaseJD(startJD: Double, phaseType: Int): Double {
        val kApprox = (startJD - 2451550.09766) / LUNAR_CYCLE_DAYS - phaseType / 4.0
        var k = floor(kApprox)
        val epsilon = 1e-5
        
        repeat(5) {
            val phaseJD = calculateTruePhaseJD(k, phaseType)
            if (phaseJD >= startJD - epsilon) {
                return if (phaseJD < startJD + epsilon) {
                    calculateTruePhaseJD(k + 1.0, phaseType)
                } else {
                    phaseJD
                }
            }
            k += 1.0
        }
        
        return calculateTruePhaseJD(floor(kApprox) + 1.0, phaseType)
    }

    /**
     * Calculate lunar new year (first full moon after winter solstice)
     */
    fun calculateLunarNewYearJD(gregorianYearOfStart: Int): Double {
        val wsYear = gregorianYearOfStart - 1
        val wsJD = calculateSolsticeEquinoxJDE(wsYear, 0) // Winter solstice
        val firstNMJD = findNextPhaseJD(wsJD, 0) // First new moon after WS
        return findNextPhaseJD(firstNMJD, 2) // First full moon after that NM
    }

    /**
     * Get number of months in lunar year (12 or 13)
     */
    fun getLunarMonthsInYear(lunarYearIdentifier: Int): Int {
        val yearStartJD = calculateLunarNewYearJD(lunarYearIdentifier)
        val nextYearStartJD = calculateLunarNewYearJD(lunarYearIdentifier + 1)
        
        var fullMoonCount = 0
        var currentFMJD = yearStartJD
        val epsilon = 1e-5
        
        while (true) {
            currentFMJD = findNextPhaseJD(currentFMJD, 2)
            if (currentFMJD < nextYearStartJD - epsilon) {
                fullMoonCount++
            } else {
                break
            }
        }
        
        return fullMoonCount + 1
    }

    /**
     * Check if lunar year is leap year (13 months)
     */
    fun isLunarLeapYear(lunarYearIdentifier: Int): Boolean {
        return getLunarMonthsInYear(lunarYearIdentifier) == 13
    }

    /**
     * Convert Gregorian date to Lunar date
     */
    fun gregorianToLunar(year: Int, month: Int, day: Int): LunarDay {
        val weekday = calculateWeekday(year, month, day)
        val targetJD = gregorianToJulianDay(year, month, day, 12.0)
        val eldYear = year + germanicEpochBC
        
        // Determine which lunar year this date falls in
        var lunarYearId = year
        val yearStartJDGuess = calculateLunarNewYearJD(year)
        val epsilon = 1e-5
        
        lunarYearId = when {
            targetJD < yearStartJDGuess - epsilon -> year - 1
            targetJD >= calculateLunarNewYearJD(year + 1) - epsilon -> year + 1
            else -> year
        }
        
        val yearStartJD = calculateLunarNewYearJD(lunarYearId)
        val monthsInThisYear = getLunarMonthsInYear(lunarYearId)
        
        // Find which month and day
        var lunarMonthNum = 1
        var monthStartJD = yearStartJD
        
        while (lunarMonthNum <= monthsInThisYear) {
            val nextMonthStartJD = findNextPhaseJD(monthStartJD, 2)
            
            if (targetJD >= monthStartJD - epsilon && targetJD < nextMonthStartJD - epsilon) {
                val lunarDay = floor(targetJD - monthStartJD).toInt() + 1
                
                // Calculate metonic position
                val yearSince1AD = maxOf(lunarYearId - 1, 0)
                val metonicCycleNum = yearSince1AD / YEARS_PER_METONIC_CYCLE + 1
                val metonicYearPos = (yearSince1AD % YEARS_PER_METONIC_CYCLE) + 1
                
                return LunarDay(
                    lunarYear = lunarYearId,
                    lunarMonth = lunarMonthNum,
                    lunarDay = lunarDay,
                    gregorianYear = year,
                    gregorianMonth = month,
                    gregorianDay = day,
                    weekday = weekday,
                    moonPhase = MoonPhase.FULL_MOON, // Simplified for now
                    eldYear = eldYear,
                    metonicYear = metonicYearPos,
                    metonicCycle = metonicCycleNum
                )
            }
            
            monthStartJD = nextMonthStartJD
            lunarMonthNum++
        }
        
        // Error case - return invalid
        return LunarDay(0, 0, 0, year, month, day, weekday, MoonPhase.NEW_MOON, eldYear, 0, 0)
    }

    /**
     * Convert Lunar date to Gregorian date
     */
    fun lunarToGregorian(lunarYear: Int, lunarMonth: Int, lunarDay: Int): Triple<Int, Int, Int>? {
        val monthsInYear = getLunarMonthsInYear(lunarYear)
        if (lunarMonth < 1 || lunarMonth > monthsInYear || lunarDay < 1) {
            return null
        }
        
        val yearStartJD = calculateLunarNewYearJD(lunarYear)
        var monthStartJD = yearStartJD
        
        // Navigate to the correct month
        repeat(lunarMonth - 1) {
            monthStartJD = findNextPhaseJD(monthStartJD, 2)
        }
        
        // Validate day within month bounds
        val nextMonthStartJD = findNextPhaseJD(monthStartJD, 2)
        val monthLength = floor(nextMonthStartJD - monthStartJD + 0.5).toInt()
        if (lunarDay > monthLength) {
            return null
        }
        
        val targetJD = monthStartJD + (lunarDay - 1)
        return julianDayToGregorian(targetJD)
    }

    /**
     * Get month length for a specific lunar month
     */
    fun getLunarMonthLength(lunarYear: Int, lunarMonth: Int): Int {
        val monthsInYear = getLunarMonthsInYear(lunarYear)
        if (lunarMonth < 1 || lunarMonth > monthsInYear) return 0
        
        val yearStartJD = calculateLunarNewYearJD(lunarYear)
        var monthStartJD = yearStartJD
        
        repeat(lunarMonth - 1) {
            monthStartJD = findNextPhaseJD(monthStartJD, 2)
        }
        
        val nextMonthStartJD = findNextPhaseJD(monthStartJD, 2)
        return floor(nextMonthStartJD - monthStartJD + 0.5).toInt()
    }

    // === TEST FUNCTIONS ===
    
    /**
     * Test function to verify lunisolar calculations
     */
    fun runBasicTests(): String {
        val results = StringBuilder()
        
        // Test 1: Current date conversion
        results.append("=== BASIC CONVERSION TESTS ===\n")
        val testDates = listOf(
            Triple(2024, 12, 15),
            Triple(2024, 1, 1),
            Triple(2024, 6, 21), // Summer solstice
            Triple(2024, 12, 21)  // Winter solstice
        )
        
        testDates.forEach { (year, month, day) ->
            val lunar = gregorianToLunar(year, month, day)
            results.append("$year-$month-$day → Lunar: ${lunar.lunarYear}/${lunar.lunarMonth}/${lunar.lunarDay}\n")
        }
        
        // Test 2: New Year calculations
        results.append("\n=== NEW YEAR TESTS ===\n")
        for (year in 2024..2026) {
            val newYearJD = calculateLunarNewYearJD(year)
            val (gYear, gMonth, gDay) = julianDayToGregorian(newYearJD)
            val monthsInYear = getLunarMonthsInYear(year)
            val isLeap = isLunarLeapYear(year)
            results.append("Lunar $year starts: $gYear-$gMonth-$gDay, $monthsInYear months${if (isLeap) " (LEAP)" else ""}\n")
        }
        
        // Test 3: Month lengths
        results.append("\n=== MONTH LENGTH TESTS ===\n")
        val testYear = 2024
        val monthsInTestYear = getLunarMonthsInYear(testYear)
        for (month in 1..monthsInTestYear) {
            val length = getLunarMonthLength(testYear, month)
            results.append("Lunar $testYear/$month: $length days\n")
        }
        
        // Test 4: Round-trip conversion
        results.append("\n=== ROUND-TRIP TESTS ===\n")
        val testLunarDates = listOf(
            Triple(2024, 1, 1),
            Triple(2024, 1, 15),
            Triple(2024, 6, 1),
            Triple(2024, 12, 1)
        )
        
        testLunarDates.forEach { (lunarYear, lunarMonth, lunarDay) ->
            val gregorian = lunarToGregorian(lunarYear, lunarMonth, lunarDay)
            if (gregorian != null) {
                val backToLunar = gregorianToLunar(gregorian.first, gregorian.second, gregorian.third)
                val success = backToLunar.lunarYear == lunarYear && 
                            backToLunar.lunarMonth == lunarMonth && 
                            backToLunar.lunarDay == lunarDay
                results.append("Lunar $lunarYear/$lunarMonth/$lunarDay ↔ Gregorian ${gregorian.first}-${gregorian.second}-${gregorian.third} ${if (success) "✓" else "✗"}\n")
            }
        }
        
        return results.toString()
    }

    // === CONFIGURATION FUNCTIONS ===
    
    /**
     * Configuration functions for customizing the calendar
     */
    fun setCustomEpoch(epochBC: Int) {
        germanicEpochBC = epochBC
    }

    fun setCustomMonthNames(monthNames: Array<String>) {
        if (monthNames.size == 13) {
            customMonthNames = monthNames
        }
    }

    fun getCustomMonthName(monthNum: Int): String {
        return if (monthNum in 1..13) customMonthNames[monthNum - 1] else "Unknown Moon"
    }

    fun getCustomMonthNames(): Array<String> = customMonthNames.copyOf()

    fun getCurrentEpoch(): Int = germanicEpochBC

    fun calculateEldYear(gregorianYear: Int): Int {
        return gregorianYear + germanicEpochBC
    }

    /**
     * Calculate moon phase for a specific date (simplified)
     */
    fun calculateMoonPhase(year: Int, month: Int, day: Int): MoonPhase {
        try {
            val currentJD = gregorianToJulianDay(year, month, day)
            
            // Find the last new moon before this date
            val newMoonJD = findLastPhaseJD(currentJD, 0)
            val daysSinceNewMoon = currentJD - newMoonJD
            
            // Approximate moon phase based on days since new moon
            return when {
                daysSinceNewMoon < 1 -> MoonPhase.NEW_MOON
                daysSinceNewMoon < 6 -> MoonPhase.WAXING_CRESCENT
                daysSinceNewMoon < 8 -> MoonPhase.FIRST_QUARTER
                daysSinceNewMoon < 13 -> MoonPhase.WAXING_GIBBOUS
                daysSinceNewMoon < 16 -> MoonPhase.FULL_MOON
                daysSinceNewMoon < 21 -> MoonPhase.WANING_GIBBOUS
                daysSinceNewMoon < 23 -> MoonPhase.LAST_QUARTER
                daysSinceNewMoon < 29 -> MoonPhase.WANING_CRESCENT
                else -> MoonPhase.NEW_MOON
            }
        } catch (e: Exception) {
            return MoonPhase.NEW_MOON
        }
    }

    /**
     * Find last moon phase before given Julian Day
     */
    private fun findLastPhaseJD(targetJD: Double, phaseType: Int): Double {
        val kApprox = (targetJD - 2451550.09766) / LUNAR_CYCLE_DAYS - phaseType / 4.0
        var k = floor(kApprox)
        
        while (true) {
            val jd = calculateTruePhaseJD(k + phaseType / 4.0, phaseType)
            if (jd <= targetJD) {
                return jd
            }
            k -= 1
        }
    }
} 