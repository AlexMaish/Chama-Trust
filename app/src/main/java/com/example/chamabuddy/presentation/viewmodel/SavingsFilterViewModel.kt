package com.example.chamabuddy.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chamabuddy.domain.repository.SavingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SavingsFilterViewModel @Inject constructor(
    private val savingsRepository: SavingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow<SavingsFilterState>(SavingsFilterState.Loading)
    val state: StateFlow<SavingsFilterState> = _state.asStateFlow()

    private val _filterType = MutableStateFlow(FilterType.DATE)
    val filterType: StateFlow<FilterType> = _filterType.asStateFlow()

    private val _savingsByDate = MutableStateFlow<Map<Long, List<SavingsEntry>>>(emptyMap())
    val savingsByDate: StateFlow<Map<Long, List<SavingsEntry>>> = _savingsByDate.asStateFlow()

    private val _savingsByMonth = MutableStateFlow<Map<String, List<SavingsEntry>>>(emptyMap())
    val savingsByMonth: StateFlow<Map<String, List<SavingsEntry>>> = _savingsByMonth.asStateFlow()

    private val _savingsByMappedMonth = MutableStateFlow<Map<String, List<SavingsEntry>>>(emptyMap())
    val savingsByMappedMonth: StateFlow<Map<String, List<SavingsEntry>>> = _savingsByMappedMonth.asStateFlow()

    fun setFilterType(type: FilterType) {
        _filterType.value = type
    }

    /**
     * Load all savings entries for the group, map to SavingsEntry (including member name),
     * then populate flows grouped by day, by month and by mapped month (monthYear).
     */
    fun loadGroupSavings(groupId: String) {
        viewModelScope.launch {
            _state.value = SavingsFilterState.Loading
            try {
                val entries = savingsRepository.getGroupSavingsEntries(groupId)

                // Map DB entries to UI entries and resolve member names (falls back to memberId)
                val savingsEntries = entries.map { entry ->
                    val memberName = try {
                        // Repository-supplied lookup; fall back to memberId if unavailable
                        savingsRepository.getMemberName(entry.memberId) ?: entry.memberId
                    } catch (t: Throwable) {
                        entry.memberId
                    }

                    SavingsEntry(
                        id = entry.entryId,
                        memberName = memberName,
                        amount = entry.amount,
                        entryDate = entry.entryDate,
                        monthYear = entry.monthYear // ensure monthYear is passed through
                    )
                }

                // Group by date (normalized to start of day in millis)
                val byDate = savingsEntries.groupBy {
                    val calendar = Calendar.getInstance().apply { timeInMillis = it.entryDate }
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    calendar.timeInMillis
                }

                // Group by month string "MonthName YEAR" (derived from entryDate)
                val byMonth = savingsEntries.groupBy {
                    val calendar = Calendar.getInstance().apply { timeInMillis = it.entryDate }
                    "${calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())} ${calendar.get(Calendar.YEAR)}"
                }

                // Group by mapped month coming from the monthYear field (expected format "MM/YYYY" or similar)
                val byMappedMonth = savingsEntries.groupBy { entry ->
                    try {
                        val parts = entry.monthYear.split("/")
                        if (parts.size == 2) {
                            val month = parts[0].toIntOrNull()
                            val year = parts[1].toIntOrNull()
                            if (month != null && year != null) {
                                val calendar = Calendar.getInstance().apply {
                                    set(Calendar.MONTH, month - 1)
                                    set(Calendar.YEAR, year)
                                }
                                "${calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())} $year"
                            } else {
                                entry.monthYear
                            }
                        } else {
                            entry.monthYear
                        }
                    } catch (e: Exception) {
                        entry.monthYear
                    }
                }

                _savingsByDate.value = byDate
                _savingsByMonth.value = byMonth
                _savingsByMappedMonth.value = byMappedMonth
                _state.value = SavingsFilterState.Loaded
            } catch (e: Exception) {
                _state.value = SavingsFilterState.Error(e.message ?: "Failed to load savings")
            }
        }
    }
}

/* --- helper types --- */

sealed class SavingsFilterState {
    object Loading : SavingsFilterState()
    object Loaded : SavingsFilterState()
    data class Error(val message: String) : SavingsFilterState()
}

enum class FilterType {
    DATE, MONTH, MAPPED_MONTH
}

data class SavingsEntry(
    val id: String,
    val memberName: String,
    val amount: Int,
    val entryDate: Long,
    val monthYear: String
)
