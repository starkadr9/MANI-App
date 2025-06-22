package com.simplemobiletools.calendar.pro.views

import android.content.Context
import android.graphics.*
import android.text.TextPaint
import android.text.TextUtils
import android.util.AttributeSet
import android.util.SparseIntArray
import android.view.View
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.extensions.*
import com.simplemobiletools.calendar.pro.helpers.COLUMN_COUNT
import com.simplemobiletools.calendar.pro.helpers.Formatter
import com.simplemobiletools.calendar.pro.helpers.LunisolarCalendar
import com.simplemobiletools.calendar.pro.helpers.LunisolarHolidays
import com.simplemobiletools.calendar.pro.helpers.ROW_COUNT
import com.simplemobiletools.calendar.pro.models.DayMonthly
import com.simplemobiletools.calendar.pro.models.Event
import com.simplemobiletools.calendar.pro.models.MonthViewEvent
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.HIGHER_ALPHA
import com.simplemobiletools.commons.helpers.LOWER_ALPHA
import com.simplemobiletools.commons.helpers.MEDIUM_ALPHA
import org.joda.time.DateTime
import org.joda.time.Days
import kotlin.math.max
import kotlin.math.min

// used in the Monthly view fragment, 1 view per screen
class MonthView(context: Context, attrs: AttributeSet, defStyle: Int) : View(context, attrs, defStyle) {
    private val BG_CORNER_RADIUS = 8f

    private var textPaint: Paint
    private var eventTitlePaint: TextPaint
    private var gridPaint: Paint
    private var circleStrokePaint: Paint
    private var config = context.config
    private var dayWidth = 0f
    private var dayHeight = 0f
    private var primaryColor = 0
    private var textColor = 0
    private var weekendsTextColor = 0
    private var weekDaysLetterHeight = 0
    private var eventTitleHeight = 0
    private var currDayOfWeek = 0
    private var smallPadding = 0
    private var maxEventsPerDay = 0
    private var horizontalOffset = 0
    private var showWeekNumbers = false
    private var dimPastEvents = true
    private var dimCompletedTasks = true
    private var highlightWeekends = false
    private var isPrintVersion = false
    private var isMonthDayView = false
    private var allEvents = ArrayList<MonthViewEvent>()
    private var bgRectF = RectF()
    private var dayTextRect = Rect()
    private var dayLetters = ArrayList<String>()
    private var days = ArrayList<DayMonthly>()
    private var dayVerticalOffsets = SparseIntArray()
    private var selectedDayCoords = Point(-1, -1)

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0)

    init {
        primaryColor = context.getProperPrimaryColor()
        textColor = context.getProperTextColor()
        weekendsTextColor = config.highlightWeekendsColor
        showWeekNumbers = config.showWeekNumbers
        dimPastEvents = config.dimPastEvents
        dimCompletedTasks = config.dimCompletedTasks
        highlightWeekends = config.highlightWeekends

        smallPadding = resources.displayMetrics.density.toInt()
        val normalTextSize = resources.getDimensionPixelSize(com.simplemobiletools.commons.R.dimen.normal_text_size)
        weekDaysLetterHeight = normalTextSize * 2

        textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = normalTextSize.toFloat()
            textAlign = Paint.Align.CENTER
        }

        gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor.adjustAlpha(LOWER_ALPHA)
        }

        circleStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = resources.getDimension(R.dimen.circle_stroke_width)
            color = primaryColor
        }

        val smallerTextSize = resources.getDimensionPixelSize(com.simplemobiletools.commons.R.dimen.smaller_text_size)
        eventTitleHeight = smallerTextSize
        eventTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = smallerTextSize.toFloat()
            textAlign = Paint.Align.LEFT
        }

        initWeekDayLetters()
        setupCurrentDayOfWeekIndex()
    }

    fun updateDays(newDays: ArrayList<DayMonthly>, isMonthDayView: Boolean) {
        this.isMonthDayView = isMonthDayView
        days = newDays
        showWeekNumbers = config.showWeekNumbers
        horizontalOffset = if (showWeekNumbers) eventTitleHeight * 2 else 0
        initWeekDayLetters()
        setupCurrentDayOfWeekIndex()
        groupAllEvents()
        invalidate()
    }

    private fun groupAllEvents() {
        days.forEach { day ->
            val dayIndexOnMonthView = day.indexOnMonthView

            day.dayEvents.forEach { event ->
                // make sure we properly handle events lasting multiple days and repeating ones
                val validDayEvent = isDayValid(event, day.code)
                val lastEvent = allEvents.lastOrNull { it.id == event.id }
                val notYetAddedOrIsRepeatingEvent = lastEvent == null || lastEvent.endTS <= event.startTS

                // handle overlapping repeating events e.g. an event that lasts 3 days, but repeats every 2 days has a one day overlap
                val canOverlap = event.endTS - event.startTS > event.repeatInterval
                val shouldAddEvent = notYetAddedOrIsRepeatingEvent || canOverlap && (lastEvent!!.startTS < event.startTS)

                if (shouldAddEvent && !validDayEvent) {
                    val daysCnt = getEventLastingDaysCount(event)

                    val monthViewEvent = MonthViewEvent(
                        id = event.id!!,
                        title = event.title,
                        startTS = event.startTS,
                        endTS = event.endTS,
                        color = event.color,
                        startDayIndex = dayIndexOnMonthView,
                        daysCnt = daysCnt,
                        originalStartDayIndex = dayIndexOnMonthView,
                        isAllDay = event.getIsAllDay(),
                        isPastEvent = event.isPastEvent,
                        isTask = event.isTask(),
                        isTaskCompleted = event.isTaskCompleted(),
                        isAttendeeInviteDeclined = event.isAttendeeInviteDeclined()
                    )
                    allEvents.add(monthViewEvent)
                }
            }
        }

        allEvents = allEvents.asSequence().sortedWith(
            compareBy({ -it.daysCnt }, { !it.isAllDay }, { it.startTS }, { it.endTS }, { it.startDayIndex }, { it.title })
        ).toMutableList() as ArrayList<MonthViewEvent>
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        dayVerticalOffsets.clear()
        measureDaySize(canvas)

        if (config.showGrid && !isMonthDayView) {
            drawGrid(canvas)
        }

        addWeekDayLetters(canvas)
        if (showWeekNumbers && days.isNotEmpty()) {
            addWeekNumbers(canvas)
        }

        var curId = 0
        for (y in 0 until ROW_COUNT) {
            for (x in 0 until COLUMN_COUNT) {
                val day = days.getOrNull(curId)
                if (day != null) {
                    dayVerticalOffsets.put(day.indexOnMonthView, dayVerticalOffsets[day.indexOnMonthView] + weekDaysLetterHeight)
                    val verticalOffset = dayVerticalOffsets[day.indexOnMonthView]
                    val xPos = x * dayWidth + horizontalOffset
                    val yPos = y * dayHeight + verticalOffset
                    val xPosCenter = xPos + dayWidth / 2
                    val dayNumber = day.value.toString()

                    val textPaint = getTextPaint(day)
                    
                    // Check for astronomical events and highlight the entire cell
                    if (context.config.useLunisolarCalendar) {
                        val dateTime = Formatter.getDateTimeFromCode(day.code)
                        val astronomicalEvent = getAstronomicalEvent(dateTime.year, dateTime.monthOfYear, dateTime.dayOfMonth)
                        if (astronomicalEvent != null) {
                            val highlightColor = when (astronomicalEvent) {
                                "WS" -> 0x334CAF50.toInt() // Winter Solstice - Green background
                                "SS" -> 0x33FF9800.toInt() // Summer Solstice - Orange background
                                "SE" -> 0x339C27B0.toInt() // Spring Equinox - Purple background
                                "FE" -> 0x33795548.toInt() // Fall Equinox - Brown background
                                else -> 0x33CCCCCC.toInt()
                            }
                            val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = highlightColor
                                style = Paint.Style.FILL
                            }
                            // Draw background highlight for the entire cell
                            canvas.drawRect(xPos, yPos - weekDaysLetterHeight, xPos + dayWidth, yPos + dayHeight - weekDaysLetterHeight, highlightPaint)
                        }
                    }
                    
                    if (selectedDayCoords.x != -1 && x == selectedDayCoords.x && y == selectedDayCoords.y) {
                        canvas.drawCircle(xPosCenter, yPos + textPaint.textSize * 0.7f, textPaint.textSize * 0.8f, circleStrokePaint)
                        if (day.isToday) {
                            textPaint.color = textColor
                        }
                    } else if (day.isToday && !isPrintVersion) {
                        canvas.drawCircle(xPosCenter, yPos + textPaint.textSize * 0.7f, textPaint.textSize * 0.8f, getCirclePaint(day))
                    }

                    // mark days with events with a dot
                    if (isMonthDayView && day.dayEvents.isNotEmpty()) {
                        getCirclePaint(day).getTextBounds(dayNumber, 0, dayNumber.length, dayTextRect)
                        val height = dayTextRect.height() * 1.25f
                        canvas.drawCircle(
                            xPosCenter,
                            yPos + height + textPaint.textSize / 2,
                            textPaint.textSize * 0.2f,
                            getDayEventColor(day.dayEvents.first())
                        )
                    }

                    canvas.drawText(dayNumber, xPosCenter, yPos + textPaint.textSize, textPaint)
                    
                    // Draw moon phase icon for lunisolar calendar
                    if (context.config.useLunisolarCalendar) {
                        drawMoonPhaseIcon(canvas, day, xPosCenter, yPos + textPaint.textSize * 1.5f)
                        
                        // Astronomical events (solstices/equinoxes) are now handled as cell highlights above
                        // Germanic holidays are now real calendar events, not visual indicators
                    }
                    
                    dayVerticalOffsets.put(day.indexOnMonthView, (verticalOffset + textPaint.textSize * 2).toInt())
                }
                curId++
            }
        }

        if (!isMonthDayView) {
            for (event in allEvents) {
                drawEvent(event, canvas)
            }
        }
    }

    private fun drawGrid(canvas: Canvas) {
        // vertical lines
        for (i in 0 until COLUMN_COUNT) {
            var lineX = i * dayWidth
            if (showWeekNumbers) {
                lineX += horizontalOffset
            }
            canvas.drawLine(lineX, 0f, lineX, canvas.height.toFloat(), gridPaint)
        }

        // horizontal lines
        canvas.drawLine(0f, 0f, canvas.width.toFloat(), 0f, gridPaint)
        for (i in 0 until ROW_COUNT) {
            canvas.drawLine(0f, i * dayHeight + weekDaysLetterHeight, canvas.width.toFloat(), i * dayHeight + weekDaysLetterHeight, gridPaint)
        }
        canvas.drawLine(0f, canvas.height.toFloat(), canvas.width.toFloat(), canvas.height.toFloat(), gridPaint)
    }

    private fun addWeekDayLetters(canvas: Canvas) {
        for (i in 0 until COLUMN_COUNT) {
            val xPos = horizontalOffset + (i + 1) * dayWidth - dayWidth / 2
            var weekDayLetterPaint = textPaint
            if (i == currDayOfWeek && !isPrintVersion) {
                weekDayLetterPaint = getColoredPaint(primaryColor)
            } else if (highlightWeekends && context.isWeekendIndex(i)) {
                weekDayLetterPaint = getColoredPaint(weekendsTextColor)
            }
            canvas.drawText(dayLetters[i], xPos, weekDaysLetterHeight * 0.7f, weekDayLetterPaint)
        }
    }

    private fun addWeekNumbers(canvas: Canvas) {
        val weekNumberPaint = Paint(textPaint)
        weekNumberPaint.textAlign = Paint.Align.RIGHT

        for (i in 0 until ROW_COUNT) {
            val weekDays = days.subList(i * 7, i * 7 + 7)
            weekNumberPaint.color = if (weekDays.any { it.isToday && !isPrintVersion }) primaryColor else textColor

            // fourth day of the week determines the week of the year number
            val weekOfYear = days.getOrNull(i * 7 + 3)?.weekOfYear ?: 1
            val id = "$weekOfYear:"
            val yPos = i * dayHeight + weekDaysLetterHeight
            canvas.drawText(id, horizontalOffset.toFloat() * 0.9f, yPos + textPaint.textSize, weekNumberPaint)
        }
    }

    private fun measureDaySize(canvas: Canvas) {
        dayWidth = (canvas.width - horizontalOffset) / 7f
        dayHeight = (canvas.height - weekDaysLetterHeight) / ROW_COUNT.toFloat()
        val availableHeightForEvents = dayHeight.toInt() - weekDaysLetterHeight
        maxEventsPerDay = availableHeightForEvents / eventTitleHeight
    }

    private fun drawEvent(event: MonthViewEvent, canvas: Canvas) {
        var verticalOffset = 0
        for (i in 0 until min(event.daysCnt, 7 - event.startDayIndex % 7)) {
            verticalOffset = max(verticalOffset, dayVerticalOffsets[event.startDayIndex + i])
        }
        val xPos = event.startDayIndex % 7 * dayWidth + horizontalOffset
        val yPos = (event.startDayIndex / 7) * dayHeight
        val xPosCenter = xPos + dayWidth / 2

        if (verticalOffset - eventTitleHeight * 2 > dayHeight) {
            val paint = getTextPaint(days[event.startDayIndex])
            paint.color = textColor
            canvas.drawText("...", xPosCenter, yPos + verticalOffset - eventTitleHeight / 2, paint)
            return
        }

        // event background rectangle
        val backgroundY = yPos + verticalOffset
        val bgLeft = xPos + smallPadding
        val bgTop = backgroundY + smallPadding - eventTitleHeight
        var bgRight = xPos - smallPadding + dayWidth * event.daysCnt
        val bgBottom = backgroundY + smallPadding * 2
        if (bgRight > canvas.width.toFloat()) {
            bgRight = canvas.width.toFloat() - smallPadding
            val newStartDayIndex = (event.startDayIndex / 7 + 1) * 7
            if (newStartDayIndex < 42) {
                val newEvent = event.copy(startDayIndex = newStartDayIndex, daysCnt = event.daysCnt - (newStartDayIndex - event.startDayIndex))
                drawEvent(newEvent, canvas)
            }
        }

        val startDayIndex = days[event.originalStartDayIndex]
        val endDayIndex = days[min(event.startDayIndex + event.daysCnt - 1, 41)]
        bgRectF.set(bgLeft, bgTop, bgRight, bgBottom)
        canvas.drawRoundRect(bgRectF, BG_CORNER_RADIUS, BG_CORNER_RADIUS, getEventBackgroundColor(event, startDayIndex, endDayIndex))

        val specificEventTitlePaint = getEventTitlePaint(event, startDayIndex, endDayIndex)
        var taskIconWidth = 0
        if (event.isTask) {
            val taskIcon = resources.getColoredDrawableWithColor(R.drawable.ic_task_vector, specificEventTitlePaint.color).mutate()
            val taskIconY = yPos.toInt() + verticalOffset - eventTitleHeight + smallPadding * 2
            taskIcon.setBounds(xPos.toInt() + smallPadding * 2, taskIconY, xPos.toInt() + eventTitleHeight + smallPadding * 2, taskIconY + eventTitleHeight)
            taskIcon.draw(canvas)
            taskIconWidth += eventTitleHeight + smallPadding
        }

        drawEventTitle(event, canvas, xPos + taskIconWidth, yPos + verticalOffset, bgRight - bgLeft - smallPadding - taskIconWidth, specificEventTitlePaint)

        for (i in 0 until min(event.daysCnt, 7 - event.startDayIndex % 7)) {
            dayVerticalOffsets.put(event.startDayIndex + i, verticalOffset + eventTitleHeight + smallPadding * 2)
        }
    }

    private fun drawEventTitle(event: MonthViewEvent, canvas: Canvas, x: Float, y: Float, availableWidth: Float, paint: Paint) {
        val ellipsized = TextUtils.ellipsize(event.title, eventTitlePaint, availableWidth - smallPadding, TextUtils.TruncateAt.END)
        canvas.drawText(event.title, 0, ellipsized.length, x + smallPadding * 2, y, paint)
    }

    private fun getTextPaint(startDay: DayMonthly): Paint {
        var paintColor = textColor
        if (!isPrintVersion) {
            if (startDay.isToday) {
                paintColor = primaryColor.getContrastColor()
            } else if (highlightWeekends && startDay.isWeekend) {
                paintColor = weekendsTextColor
            }
        }

        if (!startDay.isThisMonth) {
            paintColor = paintColor.adjustAlpha(MEDIUM_ALPHA)
        }

        return getColoredPaint(paintColor)
    }

    private fun getColoredPaint(color: Int): Paint {
        val curPaint = Paint(textPaint)
        curPaint.color = color
        return curPaint
    }

    private fun getEventBackgroundColor(event: MonthViewEvent, startDay: DayMonthly, endDay: DayMonthly): Paint {
        var paintColor = event.color

        val adjustAlpha = when {
            event.isTask -> dimCompletedTasks && event.isTaskCompleted
            !startDay.isThisMonth && !endDay.isThisMonth -> true
            else -> dimPastEvents && event.isPastEvent && !isPrintVersion
        }

        if (adjustAlpha) {
            paintColor = paintColor.adjustAlpha(MEDIUM_ALPHA)
        }

        return getColoredPaint(paintColor)
    }

    private fun getEventTitlePaint(event: MonthViewEvent, startDay: DayMonthly, endDay: DayMonthly): Paint {
        var paintColor = event.color.getContrastColor()
        val adjustAlpha = when {
            event.isTask -> dimCompletedTasks && event.isTaskCompleted
            !startDay.isThisMonth && !endDay.isThisMonth -> true
            else -> dimPastEvents && event.isPastEvent && !isPrintVersion
        }

        if (adjustAlpha) {
            paintColor = paintColor.adjustAlpha(HIGHER_ALPHA)
        }

        val curPaint = Paint(eventTitlePaint)
        curPaint.color = paintColor
        curPaint.isStrikeThruText = event.shouldStrikeThrough()
        return curPaint
    }

    private fun getCirclePaint(day: DayMonthly): Paint {
        val curPaint = Paint(textPaint)
        var paintColor = primaryColor
        if (!day.isThisMonth) {
            paintColor = paintColor.adjustAlpha(MEDIUM_ALPHA)
        }
        curPaint.color = paintColor
        return curPaint
    }

    private fun getDayEventColor(event: Event): Paint {
        val curPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        curPaint.color = event.color
        return curPaint
    }

    private fun initWeekDayLetters() {
        dayLetters = context.withFirstDayOfWeekToFront(context.resources.getStringArray(com.simplemobiletools.commons.R.array.week_day_letters).toList())
    }

    private fun setupCurrentDayOfWeekIndex() {
        if (days.firstOrNull { it.isToday && it.isThisMonth } == null) {
            currDayOfWeek = -1
            return
        }

        currDayOfWeek = context.getProperDayIndexInWeek(DateTime())
    }

    // take into account cases when an event starts on the previous screen, subtract those days
    private fun getEventLastingDaysCount(event: Event): Int {
        val startDateTime = Formatter.getDateTimeFromTS(event.startTS)
        val endDateTime = Formatter.getDateTimeFromTS(event.endTS)
        val code = days.first().code
        val screenStartDateTime = Formatter.getDateTimeFromCode(code).toLocalDate()
        var eventStartDateTime = Formatter.getDateTimeFromTS(startDateTime.seconds()).toLocalDate()
        val eventEndDateTime = Formatter.getDateTimeFromTS(endDateTime.seconds()).toLocalDate()
        val diff = Days.daysBetween(screenStartDateTime, eventStartDateTime).days
        if (diff < 0) {
            eventStartDateTime = screenStartDateTime
        }

        val isMidnight = Formatter.getDateTimeFromTS(endDateTime.seconds()) == Formatter.getDateTimeFromTS(endDateTime.seconds()).withTimeAtStartOfDay()
        val numDays = Days.daysBetween(eventStartDateTime, eventEndDateTime).days
        val daysCnt = if (numDays == 1 && isMidnight) 0 else numDays
        return daysCnt + 1
    }

    private fun isDayValid(event: Event, code: String): Boolean {
        val date = Formatter.getDateTimeFromCode(code)
        return event.startTS != event.endTS && Formatter.getDateTimeFromTS(event.endTS) == Formatter.getDateTimeFromTS(date.seconds()).withTimeAtStartOfDay()
    }

    fun togglePrintMode() {
        isPrintVersion = !isPrintVersion
        textColor = if (isPrintVersion) {
            resources.getColor(com.simplemobiletools.commons.R.color.theme_light_text_color)
        } else {
            context.getProperTextColor()
        }

        textPaint.color = textColor
        gridPaint.color = textColor.adjustAlpha(LOWER_ALPHA)
        invalidate()
        initWeekDayLetters()
    }

    fun updateCurrentlySelectedDay(x: Int, y: Int) {
        selectedDayCoords = Point(x, y)
        invalidate()
    }

    private fun drawMoonPhaseIcon(canvas: Canvas, day: DayMonthly, xCenter: Float, yPos: Float) {
        try {
            // Get Gregorian date components
            val dayCode = day.code
            val dateTime = Formatter.getDateTimeFromCode(dayCode)
            
            // Calculate moon phase for this day
            val moonPhase = LunisolarCalendar.calculateMoonPhase(dateTime.year, dateTime.monthOfYear, dateTime.dayOfMonth)
            
            // Create moon phase paint
            val moonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (day.isThisMonth) textColor else textColor.adjustAlpha(MEDIUM_ALPHA)
                style = Paint.Style.FILL
            }
            
            val moonStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = moonPaint.color
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }
            
            val radius = textPaint.textSize * 0.3f  // Much larger moon icons
            
            // Draw moon phase based on calculation
            when (moonPhase) {
                LunisolarCalendar.MoonPhase.FULL_MOON -> {
                    // Full moon - filled circle (months start with full moon)
                    canvas.drawCircle(xCenter, yPos, radius, moonPaint)
                }
                LunisolarCalendar.MoonPhase.NEW_MOON -> {
                    // New moon - empty circle  
                    canvas.drawCircle(xCenter, yPos, radius, moonStrokePaint)
                }
                LunisolarCalendar.MoonPhase.FIRST_QUARTER -> {
                    // First quarter - half filled (right side)
                    canvas.drawCircle(xCenter, yPos, radius, moonStrokePaint)
                    canvas.drawArc(xCenter - radius, yPos - radius, xCenter + radius, yPos + radius, 
                                  -90f, 180f, true, moonPaint)
                }
                LunisolarCalendar.MoonPhase.LAST_QUARTER -> {
                    // Last quarter - half filled (left side)
                    canvas.drawCircle(xCenter, yPos, radius, moonStrokePaint)
                    canvas.drawArc(xCenter - radius, yPos - radius, xCenter + radius, yPos + radius, 
                                  90f, 180f, true, moonPaint)
                }
                LunisolarCalendar.MoonPhase.WAXING_CRESCENT -> {
                    // Waxing crescent - small slice on right
                    canvas.drawCircle(xCenter, yPos, radius, moonStrokePaint)
                    canvas.drawArc(xCenter - radius, yPos - radius, xCenter + radius, yPos + radius, 
                                  -45f, 90f, true, moonPaint)
                }
                LunisolarCalendar.MoonPhase.WANING_CRESCENT -> {
                    // Waning crescent - small slice on left
                    canvas.drawCircle(xCenter, yPos, radius, moonStrokePaint)
                    canvas.drawArc(xCenter - radius, yPos - radius, xCenter + radius, yPos + radius, 
                                  135f, 90f, true, moonPaint)
                }
                LunisolarCalendar.MoonPhase.WAXING_GIBBOUS -> {
                    // Waxing gibbous - mostly full, missing left slice
                    canvas.drawCircle(xCenter, yPos, radius, moonPaint)
                    canvas.drawArc(xCenter - radius, yPos - radius, xCenter + radius, yPos + radius, 
                                  90f, 90f, true, getColoredPaint(context.getProperBackgroundColor()))
                }
                LunisolarCalendar.MoonPhase.WANING_GIBBOUS -> {
                    // Waning gibbous - mostly full, missing right slice
                    canvas.drawCircle(xCenter, yPos, radius, moonPaint)
                    canvas.drawArc(xCenter - radius, yPos - radius, xCenter + radius, yPos + radius, 
                                  -90f, 90f, true, getColoredPaint(context.getProperBackgroundColor()))
                }
            }
        } catch (e: Exception) {
            // If moon phase calculation fails, don't draw anything
        }
    }

    private fun drawAstronomicalEvent(canvas: Canvas, day: DayMonthly, xCenter: Float, yPos: Float) {
        try {
            val dayCode = day.code
            val dateTime = Formatter.getDateTimeFromCode(dayCode)
            
            // Check if this date is a solstice or equinox
            val astronomicalEvent = getAstronomicalEvent(dateTime.year, dateTime.monthOfYear, dateTime.dayOfMonth)
            
            if (astronomicalEvent != null) {
                val eventPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = when (astronomicalEvent) {
                        "WS" -> 0xFF4CAF50.toInt() // Winter Solstice - Green
                        "SS" -> 0xFFFF9800.toInt() // Summer Solstice - Orange  
                        "SE" -> 0xFF9C27B0.toInt() // Spring Equinox - Purple
                        "FE" -> 0xFF795548.toInt() // Fall Equinox - Brown
                        else -> primaryColor
                    }
                    style = Paint.Style.FILL
                    textSize = textPaint.textSize * 0.4f
                    textAlign = Paint.Align.CENTER
                }
                
                // Draw larger colored dot with text
                val radius = textPaint.textSize * 0.2f  // Much larger
                canvas.drawCircle(xCenter, yPos, radius, eventPaint)
                
                // Draw abbreviation below in larger text
                canvas.drawText(astronomicalEvent, xCenter, yPos + radius + eventPaint.textSize, eventPaint)
            }
        } catch (e: Exception) {
            // If calculation fails, don't draw anything
        }
    }
    
    private fun getAstronomicalEvent(year: Int, month: Int, day: Int): String? {
        try {
            // Calculate solstices and equinoxes for this year
            val winterSolstice = LunisolarCalendar.calculateSolsticeEquinoxJDE(year, 0)
            val springEquinox = LunisolarCalendar.calculateSolsticeEquinoxJDE(year, 1)  
            val summerSolstice = LunisolarCalendar.calculateSolsticeEquinoxJDE(year, 2)
            val fallEquinox = LunisolarCalendar.calculateSolsticeEquinoxJDE(year, 3)
            
            val currentJD = LunisolarCalendar.gregorianToJulianDay(year, month, day)
            
            // Check if current date matches any astronomical event (within 1 day tolerance)
            return when {
                kotlin.math.abs(currentJD - winterSolstice) < 0.5 -> "WS"
                kotlin.math.abs(currentJD - springEquinox) < 0.5 -> "SE"
                kotlin.math.abs(currentJD - summerSolstice) < 0.5 -> "SS"
                kotlin.math.abs(currentJD - fallEquinox) < 0.5 -> "FE"
                else -> null
            }
        } catch (e: Exception) {
            return null
        }
    }

    // Germanic holidays are now handled as real calendar events
}
