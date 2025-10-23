package com.example.chamabuddy.util

import com.example.chamabuddy.domain.model.MonthlySavingEntry
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

object ChartDataUtils {

    fun generateCumulativeSavingsData(entries: List<MonthlySavingEntry>): Map<String, List<Int>> {
        val data = mutableMapOf<String, MutableList<Int>>()

        val sortedEntries = entries.sortedBy { it.entryDate }
        val monthlySums = sortedEntries.groupBy { it.monthYear }
            .mapValues { (_, entries) -> entries.sumOf { it.amount } }
            .toSortedMap(compareBy {
                val parts = it.split("/")
                parts[1].toInt() * 100 + parts[0].toInt()
            })

        var cumulative = 0
        val cumulativeData = mutableMapOf<String, Int>()

        monthlySums.forEach { (month, amount) ->
            cumulative += amount
            cumulativeData[month] = cumulative
        }

        val dateFormat = SimpleDateFormat("MM/yyyy", Locale.getDefault())
        val calendar = Calendar.getInstance()
        val result = mutableListOf<Int>()
        val monthLabels = mutableListOf<String>()

        for (i in 11 downTo 0) {
            calendar.time = Date()
            calendar.add(Calendar.MONTH, -i)
            val monthYear = dateFormat.format(calendar.time)

            monthLabels.add(monthYear)
            result.add(cumulativeData[monthYear] ?: (result.lastOrNull() ?: 0))
        }

        return mapOf("Cumulative" to result)
    }

    fun generateGroupWiseCumulativeData(entries: List<MonthlySavingEntry>): Map<String, List<Int>> {
        val data = mutableMapOf<String, MutableList<Int>>()

        val groupMonthData = entries.groupBy { it.groupId }
            .mapValues { (_, groupEntries) ->
                groupEntries.groupBy { it.monthYear }
                    .mapValues { (_, monthEntries) -> monthEntries.sumOf { it.amount } }
                    .toSortedMap(compareBy {
                        val parts = it.split("/")
                        parts[1].toInt() * 100 + parts[0].toInt()
                    })
            }

        val result = mutableMapOf<String, List<Int>>()

        groupMonthData.forEach { (groupId, monthlyData) ->
            var cumulative = 0
            val cumulativeValues = mutableListOf<Int>()

            val dateFormat = SimpleDateFormat("MM/yyyy", Locale.getDefault())
            val calendar = Calendar.getInstance()

            for (i in 11 downTo 0) {
                calendar.time = Date()
                calendar.add(Calendar.MONTH, -i)
                val monthYear = dateFormat.format(calendar.time)

                cumulative += monthlyData[monthYear] ?: 0
                cumulativeValues.add(cumulative)
            }

            result[groupId] = cumulativeValues
        }

        return result
    }

    fun generateUserCumulativeData(entries: List<MonthlySavingEntry>, userId: String): Map<String, List<Int>> {
        val userEntries = entries.filter { it.memberId == userId }
        return generateCumulativeSavingsData(userEntries)
    }
}