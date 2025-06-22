import org.joda.time.DateTime

fun main() {
    println("=== CURRENT CALENDAR CALCULATIONS ===")
    
    for (year in 2024..2026) {
        val newYearJD = LunisolarCalendar.calculateLunarNewYearJD(year)
        val newYearDate = LunisolarCalendar.julianDayToGregorian(newYearJD)
        val monthCount = LunisolarCalendar.getLunarMonthsInYear(year)
        
        println("Lunar $year: ${newYearDate.first}/${newYearDate.second}/${newYearDate.third}, $monthCount months")
    }
    
    println("\n=== EXPECTED (MANI) ===")
    println("Lunar 2024: 2024/1/25, ? months")
    println("Lunar 2025: 2025/1/14, 13 months") 
    println("Lunar 2026: 2026/2/2, 12 months")
}
