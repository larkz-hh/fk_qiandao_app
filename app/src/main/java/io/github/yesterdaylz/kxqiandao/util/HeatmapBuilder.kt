package io.github.yesterdaylz.kxqiandao.util

import io.github.yesterdaylz.kxqiandao.data.model.RecordItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

/** 热力图里的一天 */
data class HeatCell(
    val date: String,
    val minutes: Int,
    val level: Int,// 0 = 没签到，1-4 越多越深
)

/**
 * 7 行 × N 列，每列一周（周一到周日），未来日期为 null
 */
data class CheckInHeatmap(
    val weeks: List<List<HeatCell?>>,
    val totalMinutes: Int,
    val activeDays: Int,
    val startDate: String,
    val endDate: String,
)

object HeatmapBuilder {

    private const val DAY_MILLIS = 24 * 60 * 60 * 1000L
    private const val MAX_MINUTES_PER_RECORD = 24 * 60

    /** 把签到记录折算成每天的分钟数，跨天的算在开始那天 */
    fun dailyMinutes(records: List<RecordItem>): Map<String, Int> {
        val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT)
        val minutesByDay = HashMap<String, Int>()
        for (record in records) {
            val day = record.start?.take(10) ?: continue
            minutesByDay[day] = (minutesByDay[day] ?: 0) + minutesOf(record, timeFormat)
        }
        return minutesByDay
    }

    /** 单条记录的时长（分钟） */
    fun durationMinutes(record: RecordItem): Int =
        minutesOf(record, SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT))

    /** days 是 日期 -> 分钟，单条记录最长按 24 小时算 */
    fun build(days: Map<String, Int>, months: Int = 6, now: Calendar = Calendar.getInstance()): CheckInHeatmap {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)

        val today = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val from = (today.clone() as Calendar).apply { add(Calendar.MONTH, -months) }
        // 起点对齐到周一
        from.add(Calendar.DAY_OF_YEAR, -((from.get(Calendar.DAY_OF_WEEK) + 5) % 7))

        val totalDays = ((today.timeInMillis - from.timeInMillis) / DAY_MILLIS).toInt() + 1
        val columns = (totalDays + 6) / 7

        val weeks = ArrayList<List<HeatCell?>>(columns)
        var totalMinutes = 0
        var activeDays = 0
        val cursor = from.clone() as Calendar

        repeat(columns) {
            val column = ArrayList<HeatCell?>(7)
            repeat(7) {
                if (cursor.timeInMillis > today.timeInMillis) {
                    column.add(null)
                } else {
                    val date = dateFormat.format(cursor.time)
                    val minutes = days[date] ?: 0
                    if (minutes > 0) {
                        activeDays++
                        totalMinutes += minutes
                    }
                    column.add(HeatCell(date, minutes, levelOf(minutes)))
                }
                cursor.add(Calendar.DAY_OF_YEAR, 1)
            }
            weeks.add(column)
        }

        return CheckInHeatmap(
            weeks = weeks,
            totalMinutes = totalMinutes,
            activeDays = activeDays,
            startDate = dateFormat.format(from.time),
            endDate = dateFormat.format(today.time),
        )
    }

    private fun levelOf(minutes: Int): Int = when {
        minutes <= 0 -> 0
        minutes <= 60 -> 1
        minutes <= 120 -> 2
        minutes <= 240 -> 3
        else -> 4
    }

    private fun minutesOf(record: RecordItem, timeFormat: SimpleDateFormat): Int {
        record.accumulatedTime?.toDoubleOrNull()?.let {
            return (it * 60).roundToInt().coerceIn(0, MAX_MINUTES_PER_RECORD)
        }
        val start = parseTime(record.start, timeFormat) ?: return 0
        val end = parseTime(record.end, timeFormat)
            ?: if (record.status == "在线") System.currentTimeMillis()
            else return 0
        return ((end - start) / 60000L).toInt().coerceIn(0, MAX_MINUTES_PER_RECORD)
    }

    private fun parseTime(value: String?, format: SimpleDateFormat): Long? {
        if (value.isNullOrBlank()) return null
        return runCatching { format.parse(value)?.time }.getOrNull()
    }
}
