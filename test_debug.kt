// Quick debug test for lunar year calculations
fun main() {
    println("=== LUNAR YEAR DEBUG TEST ===")
    
    for (year in 2024..2027) {
        println("\n--- Year $year ---")
        
        // Test winter solstice calculation
        val wsJD = calculateSolsticeEquinoxJDE(year - 1, 0)
        val wsDate = julianDayToGregorian(wsJD)
        println("Winter Solstice ${year-1}: ${wsDate.first}-${wsDate.second}-${wsDate.third}")
        
        // Test new year calculation
        val newYearJD = calculateLunarNewYearJD(year)
        val newYearDate = julianDayToGregorian(newYearJD)
        println("Lunar New Year $year: ${newYearDate.first}-${newYearDate.second}-${newYearDate.third}")
        
        // Test month count
        val monthCount = getLunarMonthsInYear(year)
        println("Months in $year: $monthCount")
        
        // Test first day of each quarter
        for (month in listOf(1, 3, 6, 9)) {
            if (month <= monthCount) {
                val monthStart = lunarToGregorian(year, month, 1)
                if (monthStart != null) {
                    println("Month $month/1: ${monthStart.first}-${monthStart.second}-${monthStart.third}")
                }
            }
        }
    }
} 