package com.simplemobiletools.calendar.pro.fragments

import android.content.res.Resources
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import androidx.viewpager.widget.ViewPager
import com.simplemobiletools.calendar.pro.activities.MainActivity
import com.simplemobiletools.calendar.pro.adapters.MyMonthPagerAdapter
import com.simplemobiletools.calendar.pro.databinding.FragmentMonthsHolderBinding
import com.simplemobiletools.calendar.pro.extensions.config
import com.simplemobiletools.calendar.pro.extensions.getMonthCode
import com.simplemobiletools.calendar.pro.helpers.DAY_CODE
import com.simplemobiletools.calendar.pro.helpers.Formatter
import com.simplemobiletools.calendar.pro.helpers.LunisolarCalendar
import com.simplemobiletools.calendar.pro.helpers.MONTHLY_VIEW
import com.simplemobiletools.calendar.pro.interfaces.NavigationListener
import com.simplemobiletools.commons.extensions.beGone
import com.simplemobiletools.commons.extensions.getAlertDialogBuilder
import com.simplemobiletools.commons.extensions.getProperBackgroundColor
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.views.MyViewPager
import org.joda.time.DateTime

class MonthFragmentsHolder : MyFragmentHolder(), NavigationListener {
    private val PREFILLED_MONTHS = 251

    private lateinit var viewPager: MyViewPager
    private var defaultMonthlyPage = 0
    private var todayDayCode = ""
    private var currentDayCode = ""
    private var isGoToTodayVisible = false

    override val viewType = MONTHLY_VIEW

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentDayCode = arguments?.getString(DAY_CODE) ?: ""
        todayDayCode = Formatter.getTodayCode()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentMonthsHolderBinding.inflate(inflater, container, false)
        binding.root.background = ColorDrawable(requireContext().getProperBackgroundColor())
        viewPager = binding.fragmentMonthsViewpager
        viewPager.id = (System.currentTimeMillis() % 100000).toInt()
        setupFragment()
        return binding.root
    }

    private fun setupFragment() {
        val codes = getMonths(currentDayCode)
        val monthlyAdapter = MyMonthPagerAdapter(requireActivity().supportFragmentManager, codes, this)
        
        // Find the correct center position for lunar calendar
        var centerPosition = codes.size / 2 // Default fallback
        
        if (requireContext().config.useLunisolarCalendar) {
            val startDate = Formatter.getDateTimeFromCode(currentDayCode)
            val targetLunar = LunisolarCalendar.gregorianToLunar(startDate.year, startDate.monthOfYear, startDate.dayOfMonth)
            
            if (targetLunar.lunarDay != 0) {
                // Find which position in our generated list corresponds to the current lunar month
                for (i in codes.indices) {
                    val codeDate = Formatter.getDateTimeFromCode(codes[i])
                    val codeLunar = LunisolarCalendar.gregorianToLunar(codeDate.year, codeDate.monthOfYear, codeDate.dayOfMonth)
                    
                    if (codeLunar.lunarYear == targetLunar.lunarYear && codeLunar.lunarMonth == targetLunar.lunarMonth) {
                        centerPosition = i
                        break
                    }
                }
            }
        }
        
        defaultMonthlyPage = centerPosition

        viewPager.apply {
            adapter = monthlyAdapter
            addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                override fun onPageScrollStateChanged(state: Int) {
                }

                override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                }

                override fun onPageSelected(position: Int) {
                    currentDayCode = codes[position]
                    val shouldGoToTodayBeVisible = shouldGoToTodayBeVisible()
                    if (isGoToTodayVisible != shouldGoToTodayBeVisible) {
                        (activity as? MainActivity)?.toggleGoToTodayVisibility(shouldGoToTodayBeVisible)
                        isGoToTodayVisible = shouldGoToTodayBeVisible
                    }
                }
            })
            currentItem = defaultMonthlyPage
        }
    }

    private fun getMonths(code: String): List<String> {
        val months = ArrayList<String>(PREFILLED_MONTHS)
        val today = Formatter.getDateTimeFromCode(code)

        if (requireContext().config.useLunisolarCalendar) {
            val date = LunisolarCalendar.gregorianToLunar(today.year, today.monthOfYear, today.dayOfMonth)
            if (date.lunarDay != 0) {
                var lunarDate = date
                // prefill previous months
                for (i in 0 until PREFILLED_MONTHS / 2) {
                    val prevMonth = LunisolarCalendar.getPreviousMonth(lunarDate.lunarYear, lunarDate.lunarMonth)
                    lunarDate = LunisolarCalendar.gregorianToLunar(prevMonth.year, prevMonth.monthOfYear, prevMonth.dayOfMonth)
                }

                for (i in 0 until PREFILLED_MONTHS) {
                    val gregorianDate = LunisolarCalendar.lunarToGregorian(lunarDate.lunarYear, lunarDate.lunarMonth, 1)
                    if (gregorianDate != null) {
                        months.add(Formatter.getDayCodeFromDate(gregorianDate.first, gregorianDate.second, gregorianDate.third))
                    }

                    val nextMonth = LunisolarCalendar.getNextMonth(lunarDate.lunarYear, lunarDate.lunarMonth)
                    lunarDate = LunisolarCalendar.gregorianToLunar(nextMonth.year, nextMonth.monthOfYear, nextMonth.dayOfMonth)
                }
                return months
            }
        }

        val firstDay = today.withDayOfMonth(1)
        for (i in -PREFILLED_MONTHS / 2..PREFILLED_MONTHS / 2) {
            months.add(Formatter.getDayCodeFromDateTime(firstDay.plusMonths(i)))
        }
        return months
    }

    override fun goLeft() {
        viewPager.currentItem = viewPager.currentItem - 1
    }

    override fun goRight() {
        viewPager.currentItem = viewPager.currentItem + 1
    }

    override fun goToDateTime(dateTime: DateTime) {
        currentDayCode = Formatter.getDayCodeFromDateTime(dateTime)
        setupFragment()
    }

    override fun goToToday() {
        currentDayCode = todayDayCode
        setupFragment()
    }

    override fun showGoToDateDialog() {
        if (activity == null) {
            return
        }

        val datePicker = getDatePickerView()
        datePicker.findViewById<View>(Resources.getSystem().getIdentifier("day", "id", "android")).beGone()

        val dateTime = getCurrentDate()!!
        datePicker.init(dateTime.year, dateTime.monthOfYear - 1, 1, null)

        activity?.getAlertDialogBuilder()!!
            .setNegativeButton(com.simplemobiletools.commons.R.string.cancel, null)
            .setPositiveButton(com.simplemobiletools.commons.R.string.ok) { _, _ -> datePicked(dateTime, datePicker) }
            .apply {
                activity?.setupDialogStuff(datePicker, this)
            }
    }

    private fun datePicked(dateTime: DateTime, datePicker: DatePicker) {
        val month = datePicker.month + 1
        val year = datePicker.year
        val newDateTime = dateTime.withDate(year, month, 1)
        goToDateTime(newDateTime)
    }

    override fun refreshEvents() {
        (viewPager.adapter as? MyMonthPagerAdapter)?.updateCalendars(viewPager.currentItem)
    }

    override fun shouldGoToTodayBeVisible() = currentDayCode.getMonthCode() != todayDayCode.getMonthCode()

    override fun getNewEventDayCode() = if (shouldGoToTodayBeVisible()) currentDayCode else todayDayCode

    override fun printView() {
        (viewPager.adapter as? MyMonthPagerAdapter)?.printCurrentView(viewPager.currentItem)
    }

    override fun getCurrentDate(): DateTime? {
        return if (currentDayCode != "") {
            DateTime(Formatter.getDateTimeFromCode(currentDayCode).toString())
        } else {
            null
        }
    }
}
