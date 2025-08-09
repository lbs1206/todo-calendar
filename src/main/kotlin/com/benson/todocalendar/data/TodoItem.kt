package com.benson.todocalendar.data

import java.time.LocalDate
import java.util.UUID

// 상태값 (대기, 진행중, 완료)
enum class Status(val displayName: String) {
    WAITING("대기"),
    IN_PROGRESS("진행중"),
    DONE("완료");

    companion object {
        fun fromDisplayName(name: String): Status = values().firstOrNull { it.displayName == name } ?: WAITING
    }
}

data class TodoItem(
    val id: String = UUID.randomUUID().toString(),
    var taskName: String,
    var importance: Importance,
    var priority: Int, // 1-10
    var description: String = "",
    var startDate: LocalDate,
    var endDate: LocalDate,
    var status: Status = Status.WAITING,
    var isCompleted: Boolean = false // kept for compatibility; mirrors status == DONE
)

enum class Importance(val displayName: String) {
    LOW("낮음"),
    MEDIUM("보통"),
    HIGH("높음"),
    CRITICAL("긴급")
}
